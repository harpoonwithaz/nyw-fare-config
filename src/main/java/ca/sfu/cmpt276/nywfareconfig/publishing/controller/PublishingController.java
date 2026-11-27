package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.StagingService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

@Controller
public class PublishingController {

    private final StagingService stagingService;
    private final TenantContext tenantContext;

    public PublishingController(
            StagingService stagingService,
            TenantContext tenantContext) {

        this.stagingService = stagingService;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/publishing/review")
    public String showReviewPage(
            Model model,
            Authentication authentication) {

        Tenant selectedTenant =
                tenantContext.getSelectedTenant();

        String tenantCode = selectedTenant != null
                ? selectedTenant.getCode()
                : null;

        List<ConfigurationChangeView> fareChanges =
                stagingService.getConfigurationChanges(tenantCode);

        List<AvailabilityRule> availabilityChanges =
                stagingService.getPendingAvailabilityChanges(tenantCode);

        long totalCount =
                fareChanges.size() + availabilityChanges.size();

        long changedCount = fareChanges.stream()
                .filter(ConfigurationChangeView::isChanged)
                .count();

        long unchangedCount =
                fareChanges.size() - changedCount;

        model.addAttribute("tenantCode", tenantCode);
        model.addAttribute("pendingChanges", fareChanges);
        model.addAttribute("availabilityChanges", availabilityChanges);
        model.addAttribute("availabilityCount", availabilityChanges.size());
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("changedCount", changedCount);
        model.addAttribute("unchangedCount", unchangedCount);

        boolean canPublish = authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                                "ROLE_CLIENT_SERVICES".equals(
                                        authority.getAuthority()));

        model.addAttribute("canPublish", canPublish);

        return "publishing/review";
    }
}


