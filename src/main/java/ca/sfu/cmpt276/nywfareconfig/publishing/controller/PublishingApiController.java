
package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.StagingService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

@RestController
@RequestMapping("/api/publishing")
public class PublishingApiController {

    private final StagingService stagingService;
    private final TenantContext tenantContext;

    //Use the existing session tenant context.
    public PublishingApiController(
            StagingService stagingService,
            TenantContext tenantContext) {

        this.stagingService = stagingService;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/pending-changes")
    public ResponseEntity<List<ConfigurationChangeView>> getPendingChanges() {

        //Retrieve the active tenant.
        Tenant selectedTenant = tenantContext.getSelectedTenant();

        if (selectedTenant == null) {
            return ResponseEntity.ok(List.of());
        }

        String tenantCode = selectedTenant.getCode();

        //Only return changes for the active tenant.
        List<ConfigurationChangeView> changes =
                stagingService.getConfigurationChanges()
                    .stream()
                    .filter(change ->
                        tenantCode.equalsIgnoreCase(change.tenantCode()))
                    .toList();

        return ResponseEntity.ok(changes);
    }
}


