package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
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

    // STORY 5.1: Retrieve pending fare drafts for the selected tenant.
    @GetMapping("/pending-changes")
    public ResponseEntity<List<ConfigurationChangeView>> getPendingChanges() {

        Tenant selectedTenant = tenantContext.getSelectedTenant();

        // No selected tenant means no pending changes to display.
        if (selectedTenant == null) {
            return ResponseEntity.ok(List.of());
        }

        String tenantCode = selectedTenant.getCode();

        // Retrieve saved pending drafts from PostgreSQL.
        // The backend teammate is implementing this service method.
        List<FareDraft> drafts =
                fareDraftService.getPendingDrafts(tenantCode);

        // Convert FareDraft entities into the existing review DTO.
        List<ConfigurationChangeView> changes = drafts.stream()
                .map(draft -> {

                    boolean isZonal =
                            draft.getGridMode() == GridMode.ZONAL;

                    return new ConfigurationChangeView(
                            draft.getTenantCode(),
                            draft.getGridMode(),

                            // OD: origin terminal
                            // ZONAL: selected fare product
                            isZonal
                                    ? draft.getProductCode()
                                    : draft.getOriginIdentifier(),

                            // OD: destination terminal
                            // ZONAL: origin zone
                            isZonal
                                    ? draft.getOriginIdentifier()
                                    : draft.getDestIdentifier(),

                            // OD: fare product
                            // ZONAL: destination zone
                            isZonal
                                    ? draft.getDestIdentifier()
                                    : draft.getProductCode(),

                            // Product display value
                            draft.getProductCode(),

                            // Original published price
                            draft.getOriginalPrice(),

                            // New pending draft price
                            draft.getDraftPrice()
                    );
                })
                .toList();

        return ResponseEntity.ok(changes);
    }
}



