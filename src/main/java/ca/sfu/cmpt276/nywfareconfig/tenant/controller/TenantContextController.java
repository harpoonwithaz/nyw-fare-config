package ca.sfu.cmpt276.nywfareconfig.tenant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.*;

@Controller
@RequestMapping("/context")
public class TenantContextController {
    private final TenantContext tenantContext;
    private final TenantService tenantService;

    public TenantContextController(TenantContext tenantContext, TenantService tenantService) {
        this.tenantContext = tenantContext;
        this.tenantService = tenantService;
    }

    @PostMapping("/tenant")
    public String switchTenant(
            @RequestParam(name = "tenant", required = false) String tenantCode,
            @RequestParam(name = "returnUrl", required = false) String returnUrl) {

        if (tenantCode == null || tenantCode.isBlank()) {
            // Operator reset dropdown to "-- Select Tenant --"
            tenantContext.setSelectedTenant(null);
            tenantContext.setSelectedOrigin(null);
            tenantContext.setSelectedProduct(null);
        } else {
            boolean isNewTenant = tenantContext.getSelectedTenant() == null ||
                    !tenantContext.getSelectedTenant().getCode().equalsIgnoreCase(tenantCode);

            if (isNewTenant) {
                Tenant resolvedTenant = tenantService.getTenantByCode(tenantCode);
                tenantContext.setSelectedTenant(resolvedTenant);

                // Clear secondary filter state when switching tenants
                tenantContext.setSelectedOrigin(null);
                tenantContext.setSelectedProduct(null);
            }
        }

        // Redirect back to the referring epic view (safe relative path check), defaulting to /fares
        if (returnUrl != null && returnUrl.startsWith("/") && !returnUrl.startsWith("//")) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/fares";
    }
}
