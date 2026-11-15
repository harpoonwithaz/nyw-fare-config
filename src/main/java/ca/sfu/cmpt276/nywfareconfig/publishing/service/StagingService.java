package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ScheduleChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.publishing.repository.FareDraftRepository;

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
            // NY Waterway: Edgewater to Midtown
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

            // NY Waterway: Port Imperial to Midtown
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

            // NY Waterway: Midtown to Edgewater
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

            // Metra: Zone 1 to Zone 2, Adult One-Way
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

            // Unchanged example for filter testing
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

    // Returns staged Epic 3 availability changes from PostgreSQL if present, otherwise default sample rows
    public List<ScheduleChangeView> getScheduleChanges() {
        List<FareDraft> stagedDrafts = fareDraftRepository.findByStatus("STAGED");
        if (!stagedDrafts.isEmpty()) {
            return stagedDrafts.stream()
                .filter(d -> "AVAILABILITY_TOGGLE".equals(d.getChangeType()))
                .map(d -> new ScheduleChangeView(
                    d.getOrigin() + " - " + d.getProductCode(),
                    "Previous State",
                    d.getSummary()
                ))
                .toList();
        }

        return List.of(
            new ScheduleChangeView(
                "Weekend Service",
                "Disabled",
                "Enabled"
            ),
            new ScheduleChangeView(
                "Monthly Pass Sales Window",
                "Days 1-5",
                "Days 1-10"
            ),
            new ScheduleChangeView(
                "Weekday Service Start",
                "06:00 AM",
                "06:00 AM"
            )
        );
    }

    // Persists Epic 3 availability changes to PostgreSQL fare_drafts table
    public void stageAvailabilityChange(String tenantCode, AvailabilityRule rule) {
        String details = (rule.isAvailable() ? "Enabled" : "Disabled");
        if (rule.getTemporalCondition() != null) {
            details += " (" + rule.getTemporalCondition().getConditionType() + ")";
        }

        FareDraft draft = new FareDraft(
            tenantCode,
            "AVAILABILITY_TOGGLE",
            rule.getSiteCode(),
            rule.getProductCode(),
            details
        );
        fareDraftRepository.save(draft);
    }
}