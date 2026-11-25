package ca.sfu.cmpt276.nywfareconfig.tenant.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

@ControllerAdvice
public class GlobalTenantAdvice {

    private final TenantContext tenantContext;

    public GlobalTenantAdvice(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @ModelAttribute("tenantContext")
    public TenantContext getTenantContext() {
        return tenantContext;
    }

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}