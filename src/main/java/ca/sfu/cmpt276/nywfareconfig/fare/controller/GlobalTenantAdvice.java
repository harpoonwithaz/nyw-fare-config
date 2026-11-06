package ca.sfu.cmpt276.nywfareconfig.fare.controller;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ca.sfu.cmpt276.nywfareconfig.fare.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.fare.model.TenantContext;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareService;

// Automatically injects the active tenant context into every view model.
@ControllerAdvice
public class GlobalTenantAdvice {
    private final TenantContext tenantContext;
    private final FareService fareService;

    public GlobalTenantAdvice(TenantContext tenantContext, FareService fareService) {
        this.tenantContext = tenantContext;
        this.fareService = fareService;
    }

    @ModelAttribute("tenantContext")
    public TenantContext getTenantContext() {
        return tenantContext;
    }

    @ModelAttribute("availableTenants")
    public List<Tenant> getAvailableTenants() {
        return fareService.getAvailableTenants();
    }
}
