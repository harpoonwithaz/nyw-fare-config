package ca.sfu.cmpt276.nywfareconfig.availability.controller;

import ca.sfu.cmpt276.nywfareconfig.availability.dto.AvailabilityDraftDTO;
import ca.sfu.cmpt276.nywfareconfig.availability.dto.ProductAvailabilityView;
import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.service.AvailabilityService;
import ca.sfu.cmpt276.nywfareconfig.availability.service.AvailabilityService.RouteOption;
import ca.sfu.cmpt276.nywfareconfig.availability.service.AvailabilityService.StationOption;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/availability")
public class AvailabilityController {

    private static final List<StationOption> FALLBACK_STATIONS = List.of(
        new StationOption("MIDTOWN", "Midtown / W. 39th St", "TERMINAL"),
        new StationOption("PIER11", "Pier 11 / Wall St", "TERMINAL"),
        new StationOption("BROOKFD", "Brookfield Place", "TERMINAL"),
        new StationOption("EDGEWTR", "Edgewater Ferry Landing", "TERMINAL")
    );

    private final AvailabilityService availabilityService;
    private final TenantContext tenantContext;

    public AvailabilityController(AvailabilityService availabilityService, TenantContext tenantContext) {
        this.availabilityService = availabilityService;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    public String showAvailabilityPage(@RequestParam(value = "site", required = false) String siteParam,
                                       @RequestParam(value = "dest", required = false) String destParam,
                                       @RequestParam(value = "route", required = false) String routeParam,
                                       Model model) {
        List<StationOption> terminals = FALLBACK_STATIONS;
        String tenantCode = null;

        if (tenantContext.getSelectedTenant() != null) {
            tenantCode = tenantContext.getSelectedTenant().getCode();
            List<StationOption> liveStations = availabilityService.getStationsForTenant(tenantCode);
            if (liveStations != null && !liveStations.isEmpty()) {
                terminals = liveStations;
            }
        }

        String candidateSite = (siteParam != null && !siteParam.isBlank())
                ? siteParam
                : terminals.get(0).code();

        boolean siteExists = terminals.stream().anyMatch(t -> t.code().equalsIgnoreCase(candidateSite));
        final String resolvedSite = siteExists ? candidateSite : terminals.get(0).code();

        String derivedDest = destParam;
        if ((derivedDest == null || derivedDest.isBlank()) && routeParam != null && routeParam.contains("->")) {
            String[] parts = routeParam.split("->", 2);
            if (parts.length == 2) {
                derivedDest = parts[1];
            }
        }

        String resolvedDest = (derivedDest == null || derivedDest.isBlank()
                || AvailabilityRule.ALL_ROUTES.equalsIgnoreCase(derivedDest)
                || resolvedSite.equalsIgnoreCase(derivedDest))
                ? AvailabilityRule.ALL_ROUTES
                : derivedDest;

        String routeCode = AvailabilityRule.ALL_ROUTES.equals(resolvedDest)
                ? AvailabilityRule.ALL_ROUTES
                : resolvedSite + "->" + resolvedDest;

        List<RouteOption> routes = availabilityService.getRoutesForStation(tenantCode, resolvedSite, terminals);

        model.addAttribute("terminals", terminals);
        model.addAttribute("routes", routes);
        model.addAttribute("siteCode", resolvedSite);
        model.addAttribute("destCode", resolvedDest);
        model.addAttribute("routeCode", routeCode);

        if (tenantCode != null) {
            ProductAvailabilityView view = availabilityService.getProductAvailability(
                    tenantCode, resolvedSite, routeCode);
            model.addAttribute("availabilityView", view);
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

        String tenantCode = tenantContext.getSelectedTenant().getCode();
        int syncedCount = availabilityService.syncAvailabilityRulesToCaracal(tenantCode);
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "syncedCount", syncedCount,
            "message", syncedCount + " availability rules synced to Caracal."
        ));
    }
}