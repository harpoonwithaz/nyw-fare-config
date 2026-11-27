package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.repository.AvailabilityRuleRepository;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareDraftService;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;

@Service
public class StagingService {

    private final FareDraftService fareDraftService;
    private final FareBaselineProvider fareBaselineProvider;
    private final AvailabilityRuleRepository availabilityRuleRepository;

    public StagingService(
            FareDraftService fareDraftService,
            FareBaselineProvider fareBaselineProvider,
            AvailabilityRuleRepository availabilityRuleRepository) {

        this.fareDraftService = fareDraftService;
        this.fareBaselineProvider = fareBaselineProvider;
        this.availabilityRuleRepository = availabilityRuleRepository;
    }

    /**
     * Retrieve pending fare changes for the selected tenant.
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
     * Retrieve pending availability drafts for the selected tenant.
     */
    public List<AvailabilityRule> getPendingAvailabilityChanges(
            String tenantCode) {

        if (tenantCode == null || tenantCode.isBlank()) {
            return List.of();
        }

        return availabilityRuleRepository
                .findByTenantCodeAndPendingPublishTrue(tenantCode);
    }

    /**
     * Convert a saved fare draft into a review DTO.
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



