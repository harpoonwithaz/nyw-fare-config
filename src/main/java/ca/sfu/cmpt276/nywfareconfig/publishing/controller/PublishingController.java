
package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ScheduleChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.StagingService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

@Controller
public class PublishingController {

    private final StagingService stagingService;
    private final TenantContext tenantContext;

    //Inject the existing session tenant context.
    public PublishingController(
            StagingService stagingService,
            TenantContext tenantContext) {

        this.stagingService = stagingService;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/publishing/review")
    public String showReviewPage(Model model) {

        // Determine the active tenant.
        Tenant selectedTenant = tenantContext.getSelectedTenant();

        String tenantCode = selectedTenant != null
                ? selectedTenant.getCode()
                : null;

        // Filter mock records by active tenant.
        // This will later be replaced with the database retrieval.
        List<ConfigurationChangeView> fareChanges =
                tenantCode == null
                    ? List.of()
                    : stagingService.getConfigurationChanges()
                        .stream()
                        .filter(change ->
                            tenantCode.equalsIgnoreCase(change.tenantCode()))
                        .toList();

        // Existing mock schedule data.
        // TODO: Scope schedules by tenant when real schedule data is available.
        List<ScheduleChangeView> scheduleChanges =
                tenantCode == null
                    ? List.of()
                    : stagingService.getScheduleChanges();

        long changedFares = fareChanges.stream()
                .filter(ConfigurationChangeView::isChanged)
                .count();

        long changedSchedules = scheduleChanges.stream()
                .filter(ScheduleChangeView::isChanged)
                .count();

        model.addAttribute("tenantCode", tenantCode);
        model.addAttribute("pendingChanges", fareChanges);
        model.addAttribute("scheduleChanges", scheduleChanges);

        model.addAttribute(
                "totalCount",
                fareChanges.size() + scheduleChanges.size()
        );

        model.addAttribute(
                "changedCount",
                changedFares + changedSchedules
        );

        model.addAttribute(
                "unchangedCount",
                fareChanges.size() + scheduleChanges.size()
                    - changedFares - changedSchedules
        );

        return "publishing/review";
    }
}


