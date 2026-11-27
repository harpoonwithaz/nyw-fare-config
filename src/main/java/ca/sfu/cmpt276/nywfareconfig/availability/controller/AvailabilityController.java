package ca.sfu.cmpt276.nywfareconfig.availability.controller;

import ca.sfu.cmpt276.nywfareconfig.availability.dto.AvailabilityDraftDTO;
import ca.sfu.cmpt276.nywfareconfig.availability.dto.ProductAvailabilityView;
import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.service.AvailabilityService;
import ca.sfu.cmpt276.nywfareconfig.availability.service.AvailabilityService.StationOption;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/availability")
public class AvailabilityController {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityController.class);

    private final AvailabilityService availabilityService;
    private final TenantContext tenantContext;

    public AvailabilityController(AvailabilityService availabilityService, TenantContext tenantContext) {
        this.availabilityService = availabilityService;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    public String showAvailabilityPage(@RequestParam(value = "site", required = false) String siteParam,
                                       @RequestParam(value = "dest", required = false) String destParam,
                                       @RequestParam(value = "pricingDate", required = false) String pricingDateParam,
                                       HttpSession session,
                                       Model model) {
        Tenant activeTenant = tenantContext.getSelectedTenant();
        if (activeTenant == null) {
            return "availability/availability";
        }

        LocalDate resolvedDate = resolveSelectedDate(pricingDateParam);
        tenantContext.setSelectedDate(resolvedDate.toString());

        List<StationOption> terminals;
        try {
            terminals = availabilityService.getStationsForTenant(activeTenant.getCode());
        } catch (CaracalApiException ex) {
            log.warn("Unable to load stations from Caracal API: {}", ex.getMessage(), ex);
            model.addAttribute("caracalAvailabilityErrorMessage", ex.getMessage());
            return "availability/availability";
        }

        if (terminals.isEmpty()) {
            model.addAttribute("caracalAvailabilityErrorMessage",
                    "No terminal sites or stations are configured in Caracal for tenant " + activeTenant.getCode() + ".");
            return "availability/availability";
        }

        // Sync origin terminal with TenantContext.secondaryFilter in O-D mode so switching
        // between /fares and /availability preserves the active terminal.
        String candidateSite = null;
        if (siteParam != null && !siteParam.isBlank()) {
            candidateSite = siteParam.trim().toUpperCase(Locale.ROOT);
        } else if (activeTenant.getGridMode() == GridMode.OD
                && tenantContext.getSecondaryFilter() != null
                && !tenantContext.getSecondaryFilter().isBlank()) {
            candidateSite = tenantContext.getSecondaryFilter().trim().toUpperCase(Locale.ROOT);
        }

        String validSite = null;
        if (candidateSite != null) {
            for (StationOption option : terminals) {
                if (option.code().equalsIgnoreCase(candidateSite)) {
                    validSite = option.code();
                    break;
                }
            }
        }
        final String resolvedSite = validSite != null ? validSite : terminals.get(0).code();

        if (activeTenant.getGridMode() == GridMode.OD) {
            tenantContext.setSecondaryFilter(resolvedSite);
        }

        List<StationOption> connectedDestinations = List.of();
        String resolvedDest = AvailabilityRule.ALL_ROUTES;

        if (activeTenant.getGridMode() == GridMode.OD) {
            try {
                connectedDestinations = availabilityService.getConnectedDestinations(
                        activeTenant.getCode(), resolvedSite, terminals);
            } catch (CaracalApiException ex) {
                log.warn("Unable to filter connected destinations from Caracal API: {}", ex.getMessage(), ex);
                model.addAttribute("caracalAvailabilityErrorMessage", ex.getMessage());
                return "availability/availability";
            }

            String destSessionKey = "availabilityDest_" + activeTenant.getCode().toUpperCase(Locale.ROOT);
            String candidateDest = (destParam != null && !destParam.isBlank())
                    ? destParam.trim().toUpperCase(Locale.ROOT)
                    : (String) session.getAttribute(destSessionKey);

            if (candidateDest != null
                    && !AvailabilityRule.ALL_ROUTES.equals(candidateDest)
                    && !resolvedSite.equals(candidateDest)) {
                boolean isConnected = connectedDestinations.stream()
                        .anyMatch(t -> t.code().equalsIgnoreCase(candidateDest));
                if (isConnected) {
                    resolvedDest = candidateDest;
                }
            }
            session.setAttribute(destSessionKey, resolvedDest);
        }

        String routeCode = AvailabilityRule.ALL_ROUTES.equals(resolvedDest)
                ? AvailabilityRule.ALL_ROUTES
                : resolvedSite + "->" + resolvedDest;

        model.addAttribute("terminals", terminals);
        model.addAttribute("connectedDestinations", connectedDestinations);
        model.addAttribute("siteCode", resolvedSite);
        model.addAttribute("destCode", resolvedDest);
        model.addAttribute("routeCode", routeCode);
        model.addAttribute("selectedDate", resolvedDate.toString());

        try {
            ProductAvailabilityView view = availabilityService.getProductAvailability(
                    activeTenant, resolvedSite, routeCode, resolvedDate);
            model.addAttribute("availabilityView", view);
        } catch (CaracalApiException ex) {
            log.warn("Unable to load availability matrix from Caracal API: {}", ex.getMessage(), ex);
            model.addAttribute("caracalAvailabilityErrorMessage", ex.getMessage());
        }

        return "availability/availability";
    }

    private LocalDate resolveSelectedDate(String pricingDateParam) {
        if (pricingDateParam != null && !pricingDateParam.isBlank()) {
            try {
                return LocalDate.parse(pricingDateParam.trim());
            } catch (DateTimeParseException ignored) {
                // Fall back to existing TenantContext date or today
            }
        }
        String contextDate = tenantContext.getSelectedDate();
        if (contextDate != null && !contextDate.isBlank()) {
            try {
                return LocalDate.parse(contextDate.trim());
            } catch (DateTimeParseException ignored) {
                // Fall back to today
            }
        }
        return LocalDate.now();
    }

    @PostMapping("/api/stage")
    @ResponseBody
    public ResponseEntity<?> stageAvailabilityDrafts(@Valid @RequestBody List<AvailabilityDraftDTO> drafts) {
        if (tenantContext.getSelectedTenant() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "No active tenant selected."
            ));
        }

        String tenantCode = tenantContext.getSelectedTenant().getCode();
        availabilityService.stageAvailabilityDrafts(tenantCode, drafts);
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", drafts.size() + " availability rule(s) saved to PostgreSQL drafts."
        ));
    }

    @PostMapping("/api/discard")
    @ResponseBody
    public ResponseEntity<?> discardSavedDrafts(@RequestParam("site") String siteCode,
                                                @RequestParam(value = "route", defaultValue = "ALL") String routeCode) {
        if (tenantContext.getSelectedTenant() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "No active tenant selected."
            ));
        }

        availabilityService.discardSavedDrafts(tenantContext.getSelectedTenant().getCode(), siteCode, routeCode);
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Saved drafts discarded for " + siteCode + "."
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Invalid availability rule payload.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "ERROR",
                "message", message
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "ERROR",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", "ERROR",
                "message", ex.getMessage()
        ));
    }
}