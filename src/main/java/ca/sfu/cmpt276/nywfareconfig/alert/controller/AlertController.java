package ca.sfu.cmpt276.nywfareconfig.alert.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ca.sfu.cmpt276.nywfareconfig.fare.model.TenantContext;

@Controller
public class AlertController {

    private static final String DEFAULT_TENANT = "NYW_SANDBOX";

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
        String tenantCode = currentTenantCode();

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

    // No tenant picked yet (e.g. straight from login) falls back to the sandbox, like the publishing page
    private String currentTenantCode() {
        if (tenantContext.getSelectedTenant() == null) {
            return DEFAULT_TENANT;
        }
        return tenantContext.getSelectedTenant().getCode();
    }
}
