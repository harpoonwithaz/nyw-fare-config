package ca.sfu.cmpt276.nywfareconfig.availability.controller;

import ca.sfu.cmpt276.nywfareconfig.availability.dto.AvailabilityDraftDTO;
import ca.sfu.cmpt276.nywfareconfig.availability.dto.ProductAvailabilityView;
import ca.sfu.cmpt276.nywfareconfig.availability.service.AvailabilityService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
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
    private final TenantContext tenantContext;

    public AvailabilityController(AvailabilityService availabilityService, TenantContext tenantContext) {
        this.availabilityService = availabilityService;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    public String showAvailabilityPage(@RequestParam(value = "site", defaultValue = "MIDTOWN") String siteCode,
                                       Model model) {
        ProductAvailabilityView view = availabilityService.getProductAvailability(siteCode);

        model.addAttribute("siteCode", siteCode);
        model.addAttribute("availabilityView", view);

        model.addAttribute("terminals", List.of(
            Map.of("code", "MIDTOWN", "name", "Midtown / W. 39th St"),
            Map.of("code", "PIER11", "name", "Pier 11 / Wall St"),
            Map.of("code", "BROOKFD", "name", "Brookfield Place"),
            Map.of("code", "EDGEWTR", "name", "Edgewater Ferry Landing")
        ));

        return "availability/availability";
    }

    @PostMapping("/api/stage")
    @ResponseBody
    public ResponseEntity<?> stageAvailabilityDrafts(@RequestBody List<AvailabilityDraftDTO> drafts) {
        String tenantCode = (tenantContext != null && tenantContext.getSelectedTenant() != null)
                ? tenantContext.getSelectedTenant().getCode()
                : "NYW";

        availabilityService.stageAvailabilityDrafts(tenantCode, drafts);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", drafts.size() + " availability drafts saved to DB."));
    }
}