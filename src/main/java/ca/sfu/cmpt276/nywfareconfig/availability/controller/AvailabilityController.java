package ca.sfu.cmpt276.nywfareconfig.availability.controller;

import ca.sfu.cmpt276.nywfareconfig.availability.dto.AvailabilityDraftDTO;
import ca.sfu.cmpt276.nywfareconfig.availability.dto.ProductAvailabilityView;
import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.service.AvailabilityService;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalUnavailableException;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final TenantContext tenantContext;

    public AvailabilityController(AvailabilityService availabilityService, TenantContext tenantContext) {
        this.availabilityService = availabilityService;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    public String showAvailabilityPage(
            @RequestParam(value = "site", required = false) String siteCode,
            @RequestParam(value = "route", defaultValue = AvailabilityRule.ALL_ROUTES) String routeCode,
            Authentication authentication,
            Model model) {

        boolean isClientServices = authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_CLIENT_SERVICES".equals(a.getAuthority())
                                || "CLIENT_SERVICES".equals(a.getAuthority()));
        model.addAttribute("isClientServices", isClientServices);

        if (tenantContext.getSelectedTenant() != null) {
            String tenantCode = tenantContext.getSelectedTenant().getCode();

            List<AvailabilityService.StationOption> stations = availabilityService.getStationsForTenant(tenantCode);
            if (stations.isEmpty()) {
                stations = List.of(
                    new AvailabilityService.StationOption("MIDTOWN", "Midtown / W. 39th St", "TERMINAL"),
                    new AvailabilityService.StationOption("PIER11", "Pier 11 / Wall St", "TERMINAL"),
                    new AvailabilityService.StationOption("BROOKFD", "Brookfield Place", "TERMINAL"),
                    new AvailabilityService.StationOption("EDGEWTR", "Edgewater Ferry Landing", "TERMINAL")
                );
            }

            final String requestedSite = siteCode;
            String selectedSite = (requestedSite != null && !requestedSite.isBlank())
                    ? requestedSite
                    : stations.get(0).code();

            List<AvailabilityService.RouteOption> routes =
                    availabilityService.getRoutesForStation(tenantCode, selectedSite, stations);

            String selectedRoute = routes.stream().anyMatch(r -> r.code().equals(routeCode))
                    ? routeCode
                    : AvailabilityRule.ALL_ROUTES;

            ProductAvailabilityView view =
                    availabilityService.getProductAvailability(tenantCode, selectedSite, selectedRoute);

            model.addAttribute("siteCode", selectedSite);
            model.addAttribute("routeCode", selectedRoute);
            model.addAttribute("terminals", stations);
            model.addAttribute("routes", routes);
            model.addAttribute("availabilityView", view);
        } else {
            model.addAttribute("siteCode", siteCode != null ? siteCode : "MIDTOWN");
            model.addAttribute("routeCode", AvailabilityRule.ALL_ROUTES);
            model.addAttribute("terminals", List.of());
            model.addAttribute("routes", List.of());
        }

        return "availability/availability";
    }

    @PostMapping("/api/stage")
    @ResponseBody
    public ResponseEntity<?> stageAvailabilityDrafts(@RequestBody List<AvailabilityDraftDTO> drafts) {
        if (tenantContext.getSelectedTenant() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "No active tenant selected."
            ));
        }

        try {
            String tenantCode = tenantContext.getSelectedTenant().getCode();
            availabilityService.stageAvailabilityDrafts(tenantCode, drafts);
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", drafts.size() + " availability rules saved to DB."
            ));
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", "ERROR",
                "message", ex.getMessage()
            ));
        }
    }

    @PostMapping("/api/sync")
    @ResponseBody
    public ResponseEntity<?> syncAvailabilityToCaracal() {
        if (tenantContext.getSelectedTenant() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "No active tenant selected."
            ));
        }

        try {
            String tenantCode = tenantContext.getSelectedTenant().getCode();
            int synced = availabilityService.syncAvailabilityRulesToCaracal(tenantCode);
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Synchronized " + synced + " availability rule(s) with Caracal platform."
            ));
        } catch (CaracalApiException | CaracalUnavailableException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "status", "ERROR",
                "message", "Failed to synchronize availability with platform: " + ex.getMessage()
            ));
        }
    }
}