package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.sfu.cmpt276.nywfareconfig.publishing.service.ValidationService;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.ValidationService.ValidationResult;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

@RestController
@RequestMapping("/api/publishing")
public class ValidationController {

    private final ValidationService validationService;
    private final TenantContext tenantContext;

    public ValidationController(
            ValidationService validationService,
            TenantContext tenantContext) {

        this.validationService = validationService;
        this.tenantContext = tenantContext;
    }

    // Read-only validation endpoint.
    @GetMapping("/validate")
    public ResponseEntity<ValidationResult> validateReadOnly(
            @RequestParam String tenantCode) {

        return ResponseEntity.ok(
                validationService.validate(tenantCode));
    }

    // Run validation without imposing publication restrictions.
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validate(
            @RequestParam String tenantCode) {

        Tenant tenant = tenantContext.getSelectedTenant();

        if (tenant == null || !tenantCode.equals(tenant.getCode())) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                validationService.validate(tenantCode));
    }
}