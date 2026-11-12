package ca.sfu.cmpt276.nywfareconfig.alert.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

@Controller
public class AlertController {

    private final TenantContext tenantContext;

    public AlertController(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    // Service alerts for the selected tenant, with optional filters (Story 4.1)
    @GetMapping("/fares/alerts")
    public String list(@RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String route,
            Model model) {
        // No tenant picked yet: the page shows the "select a tenant" prompt, like the fare grid
        if (tenantContext.getSelectedTenant() == null) {
            return "alerts/index";
        }
        String tenantCode = tenantContext.getSelectedTenant().getCode();

        // TODO: swap for alertService.list(tenantCode, search, status, route) once the backend is merged
        model.addAttribute("alerts", List.of());
        // TODO: swap for alertService.routeOptionsByTerminal(tenantCode)
        model.addAttribute("routeOptions", Map.of());

        // Keep the filter values so the form shows what was searched
        model.addAttribute("tenantCode", tenantCode);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("route", route);
        return "alerts/index";
    }
}
