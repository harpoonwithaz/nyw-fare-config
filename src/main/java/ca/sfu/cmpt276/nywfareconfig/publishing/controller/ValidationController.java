package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.sfu.cmpt276.nywfareconfig.publishing.service.ValidationService;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.ValidationService.ValidationResult;

@RestController
@RequestMapping("/api/publishing")
public class ValidationController {

    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidationResult> validate(
            @RequestParam String tenantCode) {

        return ResponseEntity.ok(
                validationService.validate(tenantCode)
        );
    }
}