package ca.sfu.cmpt276.nywfareconfig.availability.controller;

import ca.sfu.cmpt276.nywfareconfig.availability.dto.AvailabilityDraftDTO;
import ca.sfu.cmpt276.nywfareconfig.availability.dto.ProductAvailabilityView;
import ca.sfu.cmpt276.nywfareconfig.availability.service.AvailabilityService;
import ca.sfu.cmpt276.nywfareconfig.fare.model.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public String showAvailabilityPage(@RequestParam(value = "site", defaultValue = "MNHTN") String siteCode,
                                       TenantContext tenantContext,
                                       Model model) {
        ProductAvailabilityView view = availabilityService.getProductAvailability(siteCode);

        model.addAttribute("siteCode", siteCode);
        model.addAttribute("availabilityView", view);
        model.addAttribute("tenantContext", tenantContext);

        // List of official system terminal codes matching Oliver's rates layout
        model.addAttribute("terminals", List.of(
            Map.of("code", "MNHTN", "name", "Manhattan Terminal"),
            Map.of("code", "EDGEWTR", "name", "Edgewater Terminal"),
            Map.of("code", "WALLST", "name", "Wall Street Terminal"),
            Map.of("code", "BTRYPK", "name", "Battery Park Terminal")
        ));

        return "availability/availability";
    }

    @PostMapping("/api/stage")
    @ResponseBody
    public ResponseEntity<?> stageAvailabilityDrafts(@RequestBody List<AvailabilityDraftDTO> drafts,
                                                     TenantContext tenantContext) {
        String tenantCode = (tenantContext != null && tenantContext.getCurrentTenant() != null)
                ? tenantContext.getCurrentTenant().getCode()
                : "NYW";

        availabilityService.stageAvailabilityDrafts(tenantCode, drafts);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", drafts.size() + " availability drafts staged."));
    }
}