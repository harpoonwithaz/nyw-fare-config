package ca.sfu.cmpt276.nywfareconfig.tenant.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.TenantService;

// Automatically injects the active tenant context and current URI into every view model.
@ControllerAdvice
public class GlobalTenantAdvice {
    private final TenantContext tenantContext;
    private final TenantService tenantService;

    public GlobalTenantAdvice(TenantContext tenantContext, TenantService tenantService) {
        this.tenantContext = tenantContext;
        this.tenantService = tenantService;
    }

    @ModelAttribute("tenantContext")
    public TenantContext getTenantContext() {
        return tenantContext;
    }

    @ModelAttribute("availableTenants")
    public List<Tenant> getAvailableTenants() {
        return tenantService.getAvailableTenants();
    }

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}