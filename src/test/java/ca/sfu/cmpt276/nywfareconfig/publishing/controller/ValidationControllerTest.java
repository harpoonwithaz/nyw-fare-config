package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ca.sfu.cmpt276.nywfareconfig.publishing.service.ValidationService;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.ValidationService.ValidationResult;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValidationControllerTest {

    private static final String TENANT = "NYW_SANDBOX";

    private ValidationService validationService;
    private ValidationController controller;
    private TenantContext tenantContext;

    @BeforeEach
    void setUp() {

        validationService = mock(ValidationService.class);
        tenantContext = mock(TenantContext.class);

        Tenant tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn(TENANT);
        when(tenantContext.getSelectedTenant()).thenReturn(tenant);

        controller = new ValidationController(
                validationService,
                tenantContext
        );
    }

    private void mockValidation(int discrepancies) {

        when(validationService.validate(TENANT))
                .thenReturn(new ValidationResult(
                        TENANT,
                        42,
                        295,
                        discrepancies,
                        List.of()
                ));
    }

    @Test
    void successfulValidationReturnsResults() {

        mockValidation(0);

        ResponseEntity<ValidationResult> response =
                controller.validate(TENANT);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().discrepancyCount());

        verify(validationService).validate(TENANT);
    }

    @Test
    void validationReturnsRemainingDiscrepancies() {

        mockValidation(47);

        ResponseEntity<ValidationResult> response =
                controller.validate(TENANT);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(47, response.getBody().discrepancyCount());

        verify(validationService).validate(TENANT);
    }

    @Test
    void validationFailureIsPropagated() {

        when(validationService.validate(TENANT))
                .thenThrow(new IllegalStateException("Validation failed"));

        assertThrows(
                IllegalStateException.class,
                () -> controller.validate(TENANT)
        );
    }

    @Test
    void missingTenantRejectsValidation() {

        when(tenantContext.getSelectedTenant()).thenReturn(null);

        var response = controller.validate(TENANT);

        assertEquals(400, response.getStatusCode().value());

        verifyNoInteractions(validationService);
    }

    @Test
    void mismatchedTenantRejectsValidation() {

        var response = controller.validate("OTHER_TENANT");

        assertEquals(400, response.getStatusCode().value());

        verifyNoInteractions(validationService);
    }

    @Test
    void readOnlyValidationReturnsResults() {

        mockValidation(47);

        var response = controller.validateReadOnly(TENANT);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(47, response.getBody().discrepancyCount());

        verify(validationService).validate(TENANT);
    }
}