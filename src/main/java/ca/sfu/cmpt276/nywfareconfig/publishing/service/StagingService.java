package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareDraftService;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;

@Service
public class StagingService {

    private final FareDraftService fareDraftService;

    public StagingService(FareDraftService fareDraftService) {
        this.fareDraftService = fareDraftService;
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

        List<FareDraft> drafts =
                fareDraftService.getPendingDrafts(tenantCode);

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

        return new ConfigurationChangeView(
                draft.getTenantCode(),
                draft.getGridMode(),

                // O-D: origin terminal
                // ZONAL: selected fare product
                isZonal
                        ? draft.getProductCode()
                        : draft.getOriginIdentifier(),

                // O-D: destination terminal
                // ZONAL: origin zone
                isZonal
                        ? draft.getOriginIdentifier()
                        : draft.getDestIdentifier(),

                // O-D: fare product
                // ZONAL: destination zone
                isZonal
                        ? draft.getDestIdentifier()
                        : draft.getProductCode(),

                draft.getProductCode(),
                draft.getOriginalPrice(),
                draft.getDraftPrice()
        );
    }
}




