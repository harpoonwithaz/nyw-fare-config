package ca.sfu.cmpt276.nywfareconfig.fare.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareDraftBatchRequest;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareGrid;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.*;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.*;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequestMapping("/fares")
public class FareController {

    private final TenantContext tenantContext;
    private final FareService fareService;
    private final FareDraftService fareDraftService;

    public FareController(TenantContext tenantContext, FareService fareService, FareDraftService fareDraftService) {
        this.tenantContext = tenantContext;
        this.fareService = fareService;
        this.fareDraftService = fareDraftService;
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

    /**
     * Stages a batch of validated rate changes into the PostgreSQL fare_drafts
     * table.
     */
    @PostMapping("/drafts")
    @ResponseBody
    public ResponseEntity<?> stageFareDrafts(
            @Valid @RequestBody FareDraftBatchRequest batchRequest) {

        Tenant activeTenant = tenantContext.getSelectedTenant();
        if (activeTenant == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No active tenant selected in session."));
        }

        int savedCount = fareDraftService.stageChanges(activeTenant, batchRequest.getChanges());
        long totalPendingCount = fareDraftService.countPendingDrafts(activeTenant.getCode());

        return ResponseEntity.ok(Map.of(
                "savedCount", savedCount,
                "totalPendingCount", totalPendingCount));
    }

    /**
     * Returns HTTP 400 with the first Bean Validation error message when a payload
     * is invalid.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<?> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Invalid fare change payload.");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    /**
     * Returns HTTP 400 when FareDraftService rejects an invalid or locked route
     * coordinate.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<?> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
