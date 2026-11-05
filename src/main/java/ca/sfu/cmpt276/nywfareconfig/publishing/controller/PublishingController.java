package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ReviewView;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ScheduleChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.StagingService;

@Controller
public class PublishingController {

    private final StagingService stagingService;

    // Spring Boot automatically provides the service
    public PublishingController(StagingService stagingService) {
        this.stagingService = stagingService;
    }

    
    @GetMapping("/publishing/review")
        public String showReviewPage(Model model) {

            // Retrieve sample fare changes
            List<ReviewView> fareChanges =
                    stagingService.getPendingChanges();

            // Retrieve sample schedule changes
            List<ScheduleChangeView> scheduleChanges =
                    stagingService.getScheduleChanges();

            //Count changed fares.
            long changedFares = fareChanges.stream()
                    .filter(ReviewView::isChanged)
                    .count();
            
            //Count changed schedules
            long changedSchedules = scheduleChanges.stream()
                    .filter(ScheduleChangeView::isChanged)
                    .count();

            // Pass data to Thymeleaf
            model.addAttribute("tenantCode", "NYW_SANDBOX");

            model.addAttribute("pendingChanges", fareChanges);
            model.addAttribute("scheduleChanges", scheduleChanges);

            // Summary counts across both categories
            model.addAttribute("totalCount",
                fareChanges.size() + scheduleChanges.size());

            model.addAttribute("changedCount",
                changedFares + changedSchedules);

            model.addAttribute("unchangedCount",
                fareChanges.size() + scheduleChanges.size()
                - changedFares - changedSchedules);

            return "publishing/review";
        }

}

