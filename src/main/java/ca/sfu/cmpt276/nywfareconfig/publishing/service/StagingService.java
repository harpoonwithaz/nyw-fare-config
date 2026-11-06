package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ReviewView;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ScheduleChangeView;

@Service
public class StagingService {

    // Sample fare configuration data
    public List<ReviewView> getPendingChanges() {

        return List.of(
            new ReviewView("Adult One-Way", 9.00, 10.00),
            new ReviewView("Child One-Way", 5.00, 5.00),
            new ReviewView("Senior One-Way", 7.00, 7.50),
            new ReviewView("Adult Round-Trip", 18.00, 20.00)
        );
    }

    // Sample schedule and availability configuration data
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


