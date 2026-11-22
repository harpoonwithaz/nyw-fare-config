package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareDraftService;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;

@Service
public class StagingService {

    private final FareDraftService fareDraftService;
    private final FareBaselineProvider fareBaselineProvider;

    public StagingService(FareDraftService fareDraftService,
        FareBaselineProvider fareBaselineProvider) {
        this.fareDraftService = fareDraftService;
        this.fareBaselineProvider = fareBaselineProvider;
    }

    /**
     * Retrieve pending fare changes for the selected tenant.
     * Only actual changes are included in the review.
     */
    public List<ConfigurationChangeView> getConfigurationChanges(
            String tenantCode) {

        if (tenantCode == null || tenantCode.isBlank()) {
            return List.of();
        }

        //retreive postgresql pending drafts
        List<FareDraft> drafts =
                fareDraftService.getPendingDrafts(tenantCode);
        
        //convert each draft into a configuration change view
        //and filter out non-changed entries
        return drafts.stream()
                .map(this::toConfigurationChangeView)
                .filter(ConfigurationChangeView::isChanged)
                .toList();
    }

    /**
     * Convert a saved fare draft into a review DTO.
     * Supports both O-D and zonal configurations.
     */
    private ConfigurationChangeView toConfigurationChangeView(
            FareDraft draft) {

        boolean isZonal = draft.getGridMode() == GridMode.ZONAL;

        BigDecimal baselinePrice =
                fareBaselineProvider.getBaselinePrice(draft);

        return new ConfigurationChangeView(
                draft.getTenantCode(),
                draft.getGridMode(),

                isZonal
                        ? draft.getProductCode()
                        : draft.getOriginIdentifier(),

                isZonal
                        ? draft.getOriginIdentifier()
                        : draft.getDestIdentifier(),

                isZonal
                        ? draft.getDestIdentifier()
                        : draft.getProductCode(),

                draft.getProductCode(),
                baselinePrice,
                draft.getDraftPrice()
        );
    }
}



