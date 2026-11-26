package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.sfu.cmpt276.nywfareconfig.publishing.service.PublishingService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/publishing")
public class PublicationActionController {

    private final PublishingService publishingService;
    private final TenantContext tenantContext;

    public PublicationActionController(
            PublishingService publishingService,
            TenantContext tenantContext) {

        this.publishingService = publishingService;
        this.tenantContext = tenantContext;
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publish() {

        Tenant tenant = tenantContext.getSelectedTenant();

        if (tenant == null || !"NYW_SANDBOX".equals(tenant.getCode())) {

            return ResponseEntity.badRequest().body(
                    "Select NYW_SANDBOX before publishing.");
        }

        try {

            JsonNode publication =
                    publishingService.publish(tenant);

            return ResponseEntity.ok(
                    "Configuration published successfully. Publication ID: "
                    + publication.path("id").asText());

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.badRequest()
                    .body(exception.getMessage());

        } catch (IllegalStateException exception) {

            return ResponseEntity.status(409)
                    .body(exception.getMessage());

        } catch (RuntimeException exception) {

                exception.printStackTrace();

                return ResponseEntity.status(502).body(
                        "Publication could not be completed or verified. "
                        + "Some changes may have been applied to Caracal.");
        }
    }
}
