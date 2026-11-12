package ca.sfu.cmpt276.nywfareconfig.fare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareGrid;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.*;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.*;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.TenantService;

@Controller
@RequestMapping("/fares")
public class FareController {

    private final TenantContext tenantContext;
    private final FareService fareService;
    private final TenantService tenantService;

    public FareController(TenantContext tenantContext, FareService fareService, TenantService tenantService) {
        this.tenantContext = tenantContext;
        this.fareService = fareService;
        this.tenantService = tenantService;
    }

    @GetMapping
    public String getFares(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String product,
            Model model) {

        Tenant currentTenant = tenantContext.getSelectedTenant();
        if (currentTenant != null) {
            if (currentTenant.getGridMode() == GridMode.OD) {
                if (origin != null && !origin.isBlank()) {
                    tenantContext.setSelectedOrigin(origin);
                }
            } else if (currentTenant.getGridMode() == GridMode.ZONAL) {
                if (product != null && !product.isBlank()) {
                    tenantContext.setSelectedProduct(product);
                }
            }

            // Populate gridView only when both tenant and secondary filter are selected
            String activeFilter = (currentTenant.getGridMode() == GridMode.OD)
                    ? tenantContext.getSelectedOrigin()
                    : tenantContext.getSelectedProduct();

            if (activeFilter != null && !activeFilter.isBlank()) {
                FareGrid gridView = fareService.buildMockGrid(currentTenant, activeFilter);
                model.addAttribute("gridView", gridView);
            }
        }

        return "fare/fare";
    }

}
