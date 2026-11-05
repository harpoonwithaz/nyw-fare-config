package ca.sfu.cmpt276.nywfareconfig.fare.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.fare.model.TenantContext;
import ca.sfu.cmpt276.nywfareconfig.fare.service.*;

@Controller
@RequestMapping("/fares")
public class FareController {

    private final TenantContext tenantContext;
    private final TenantService tenantService;

    public FareController(TenantContext tenantContext, TenantService tenantService) {
        this.tenantContext = tenantContext;
        this.tenantService = tenantService;
    }

    // Empty fair view
    @GetMapping("")
    public String getFares(
            @RequestParam(required = false) String tenant,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String product,
            Model model) {

        // Sync tenant if provided and changed
        if (tenant != null && !tenantContext.hasTenant(tenant)) {
            Tenant resolvedTenant = tenantService.getTenantById(tenant);
            if (resolvedTenant != null) {
                tenantContext.resetForTenant(resolvedTenant);
            }
        }

        // Sync secondary attributes based on resolved tenant's gridMode
        if (tenantContext.hasTenant()) {
            GridMode mode = tenantContext.getSelectedTenant().getGridMode();

            if (mode == GridMode.OD && origin != null) {
                tenantContext.setSelectedOrigin(origin);
            } else if (mode == GridMode.ZONAL && product != null) {
                tenantContext.setSelectedProduct(product);
            }
        }

        // Populate dropdown list (selected item marked active)
        String activeTenantId = tenantContext.hasTenant()
                ? tenantContext.getSelectedTenant().getId()
                : null;

        List<Tenant> availableTenants = tenantService.getTenantsList().stream()
                .map(t -> new Tenant(
                        t.getId(),
                        t.getDisplayName(),
                        t.getGridMode(),
                        t.getId().equalsIgnoreCase(activeTenantId)))
                .toList();

        model.addAttribute("availableTenants", availableTenants);
        model.addAttribute("selectedTenant", tenantContext.getSelectedTenant());
        model.addAttribute("selectedOrigin", tenantContext.getSelectedOrigin());
        model.addAttribute("selectedProduct", tenantContext.getSelectedProduct());

        return "fare/fare";
    }

}
