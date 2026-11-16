package ca.sfu.cmpt276.nywfareconfig.fare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareChangeRequest;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareDraftBatchRequest;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.DraftStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareAction;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class FareDraftAndBatchRequestTests {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void fareDraftComputesPriceDeltaAndDefaultsToPendingStatus() {
        FareDraft draft = new FareDraft();
        draft.setOriginalPrice(new BigDecimal("13.25"));
        draft.setTargetStatus(FareStatus.ACTIVE);
        draft.setDraftPrice(new BigDecimal("15.00"));

        assertEquals(DraftStatus.PENDING, draft.getDraftStatus());
        assertEquals(new BigDecimal("1.75"), draft.getPriceDelta());
    }

    @Test
    void fareDraftNullsOutDraftPriceWhenTargetStatusIsNotSold() {
        FareDraft draft = new FareDraft(
                "METRA",
                "operator:test@nyw.com",
                GridMode.ZONAL,
                "Zone 1",
                "Zone 2",
                "ADULT_ONEWAY",
                "TIER_B",
                new BigDecimal("4.00"),
                new BigDecimal("5.00"),
                FareStatus.NOT_SOLD,
                FareAction.STATUS_TOGGLE,
                DraftStatus.PENDING);

        assertNull(draft.getDraftPrice());
        assertNull(draft.getPriceDelta());

        draft.setTargetStatus(FareStatus.ACTIVE);
        draft.setDraftPrice(new BigDecimal("4.50"));
        assertEquals(new BigDecimal("4.50"), draft.getDraftPrice());

        draft.setTargetStatus(FareStatus.NOT_SOLD);
        assertNull(draft.getDraftPrice());
    }

    @Test
    void batchRequestRejectsEmptyListAndCascadesValidationToNestedItems() {
        FareDraftBatchRequest emptyBatch = new FareDraftBatchRequest(List.of());
        assertFalse(validator.validate(emptyBatch).isEmpty());

        FareChangeRequest invalidItem = new FareChangeRequest(
                "NYW_SANDBOX",
                GridMode.OD,
                "EDGEWTR",
                "Midtown / W. 39th St",
                "Adult One-Way",
                new BigDecimal("-2.00"),
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                null);

        FareDraftBatchRequest batchWithInvalidItem = new FareDraftBatchRequest(List.of(invalidItem));
        Set<ConstraintViolation<FareDraftBatchRequest>> violations = validator.validate(batchWithInvalidItem);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("changes[0].newPrice")));
    }
}