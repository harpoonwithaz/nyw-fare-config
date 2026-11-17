package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
    public String showReviewPage(Model model) {

        Tenant selectedTenant = tenantContext.getSelectedTenant();

        String tenantCode = selectedTenant != null
                ? selectedTenant.getCode()
                : null;

        List<ConfigurationChangeView> fareChanges =
                stagingService.getConfigurationChanges(tenantCode);

        long pendingCount = fareChanges.size();

        model.addAttribute("tenantCode", tenantCode);
        model.addAttribute("pendingChanges", fareChanges);

        model.addAttribute("totalCount", pendingCount);
        model.addAttribute("changedCount", pendingCount);
        model.addAttribute("unchangedCount", 0);

        return "publishing/review";
    }
}



