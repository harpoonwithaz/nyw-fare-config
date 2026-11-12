package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ScheduleChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.StagingService;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;

@Controller
public class PublishingController {

    private final StagingService stagingService;

    // Spring Boot automatically provides the service
    public PublishingController(StagingService stagingService) {
        this.stagingService = stagingService;
    }

    
    @GetMapping("/publishing/review")
        public String showReviewPage(Model model) {

            // STORY 5.1: Use the same configuration records as the REST API.
            List<ConfigurationChangeView> fareChanges =
                    stagingService.getConfigurationChanges();

            // Retrieve sample schedule changes
            List<ScheduleChangeView> scheduleChanges =
                    stagingService.getScheduleChanges();

            //Count changed fares
            long changedFares = fareChanges.stream()
                    .filter(ConfigurationChangeView::isChanged)
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

