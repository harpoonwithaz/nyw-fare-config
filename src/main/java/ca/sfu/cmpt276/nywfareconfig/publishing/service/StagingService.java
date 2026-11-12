
package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ReviewView;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ScheduleChangeView;

@Service
public class StagingService {

    /**
     * STORY 5.1: Mock configuration comparisons.
     *
     * Supports multiple origin terminals and zonal configurations.
     * Later, these records will come from PostgreSQL.
     */
    public List<ConfigurationChangeView> getConfigurationChanges() {

        return List.of(
            // NY Waterway: Edgewater to Midtown
            new ConfigurationChangeView(
                "NYW_SANDBOX",
                "OD",
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
                "OD",
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
                "OD",
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
                "ZONAL",
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
                "OD",
                "EDGEWTR",
                "Brookfield Place",
                "Adult One-Way",
                "Adult One-Way",
                new BigDecimal("15.00"),
                new BigDecimal("15.00")
            )
        );
    }

    // Existing sample fare data retained for the current Thymeleaf page.
    public List<ReviewView> getPendingChanges() {
        return List.of(
            new ReviewView("Adult One-Way", 9.00, 10.00),
            new ReviewView("Child One-Way", 5.00, 5.00),
            new ReviewView("Senior One-Way", 7.00, 7.50),
            new ReviewView("Adult Round-Trip", 18.00, 20.00)
        );
    }

    // Existing schedule data retained for the current Thymeleaf page.
    public List<ScheduleChangeView> getScheduleChanges() {
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
}



