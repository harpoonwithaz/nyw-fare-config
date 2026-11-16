package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareDraftService;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

@RestController
@RequestMapping("/api/publishing")
public class PublishingApiController {

    private final FareDraftService fareDraftService;
    private final TenantContext tenantContext;

    public PublishingApiController(
            FareDraftService fareDraftService,
            TenantContext tenantContext) {

        this.fareDraftService = fareDraftService;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/pending-changes")
    public ResponseEntity<List<ConfigurationChangeView>> getPendingChanges() {

        // Retrieve the currently selected tenant from the session.
        Tenant selectedTenant = tenantContext.getSelectedTenant();

        if (selectedTenant == null) {
            return ResponseEntity.ok(List.of());
        }

        String tenantCode = selectedTenant.getCode();

        // Retrieve all pending drafts for the selected tenant.
        List<FareDraft> drafts =
                fareDraftService.getPendingDrafts(tenantCode);

        // Convert database entities into the existing review DTO.
        List<ConfigurationChangeView> changes = drafts.stream()
                .map(draft -> new ConfigurationChangeView(
                        draft.getTenantCode(),
                        draft.getGridMode(),
                        draft.getOriginIdentifier(),
                        draft.getDestIdentifier(),
                        draft.getProductCode(),
                        draft.getProductCode(),
                        draft.getOriginalPrice(),
                        draft.getDraftPrice()
                ))
                .toList();

        return ResponseEntity.ok(changes);
    }
}


