package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.DraftStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareAction;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.repository.FareDraftRepository;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ScheduleChangeView;

@Service
public class StagingService {

    private final FareDraftRepository fareDraftRepository;

    public StagingService(FareDraftRepository fareDraftRepository) {
        this.fareDraftRepository = fareDraftRepository;
    }

    /**
     * STORY 5.1: Mock configuration comparisons.
     * Supports multiple origin terminals and zonal configurations.
     */
    public List<ConfigurationChangeView> getConfigurationChanges() {
        return List.of(
            new ConfigurationChangeView(
                "NYW_SANDBOX",
                GridMode.OD,
                "EDGEWTR",
                "Midtown / W. 39th St",
                "Adult One-Way",
                "Adult One-Way",
                new BigDecimal("13.25"),
                new BigDecimal("14.00")
            ),
            new ConfigurationChangeView(
                "NYW_SANDBOX",
                GridMode.OD,
                "PORTIMP",
                "Midtown / W. 39th St",
                "10-Trip Pass",
                "10-Trip Pass",
                new BigDecimal("112.63"),
                new BigDecimal("115.00")
            ),
            new ConfigurationChangeView(
                "NYW_SANDBOX",
                GridMode.OD,
                "MIDTOWN",
                "Edgewater",
                "Child One-Way",
                "Child One-Way",
                new BigDecimal("10.75"),
                new BigDecimal("11.00")
            ),
            new ConfigurationChangeView(
                "METRA",
                GridMode.ZONAL,
                "ADULT_ONEWAY",
                "Zone 1",
                "Zone 2",
                "Adult One-Way",
                new BigDecimal("4.00"),
                new BigDecimal("4.50")
            ),
            new ConfigurationChangeView(
                "NYW_SANDBOX",
                GridMode.OD,
                "EDGEWTR",
                "Brookfield Place",
                "Adult One-Way",
                "Adult One-Way",
                new BigDecimal("15.00"),
                new BigDecimal("15.00")
            )
        );
    }

    public List<ScheduleChangeView> getScheduleChanges() {
        List<FareDraft> pendingDrafts = fareDraftRepository.findByDraftStatus(DraftStatus.PENDING);
        if (!pendingDrafts.isEmpty()) {
            return pendingDrafts.stream()
                .map(d -> new ScheduleChangeView(
                    d.getOriginIdentifier() + " - " + d.getProductCode(),
                    "Previous State",
                    d.getTargetStatus().name()
                ))
                .toList();
        }

        return List.of(
            new ScheduleChangeView("Weekend Service", "Disabled", "Enabled"),
            new ScheduleChangeView("Monthly Pass Sales Window", "Days 1-5", "Days 1-10"),
            new ScheduleChangeView("Weekday Service Start", "06:00 AM", "06:00 AM")
        );
    }

    // Persists Epic 3 availability changes into the shared PostgreSQL fare_drafts table
    public void stageAvailabilityChange(String tenantCode, AvailabilityRule rule) {
        FareStatus targetStatus = rule.isAvailable() ? FareStatus.ACTIVE : FareStatus.NOT_SOLD;

        FareDraft draft = new FareDraft(
            tenantCode,
            "operator@nyw.com",
            GridMode.OD,
            rule.getSiteCode(),
            "ALL",
            rule.getProductCode(),
            null,
            null,
            null,
            targetStatus,
            FareAction.INLINE_EDIT,
            DraftStatus.PENDING
        );
        fareDraftRepository.save(draft);
    }
}