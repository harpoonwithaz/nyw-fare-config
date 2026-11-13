package ca.sfu.cmpt276.nywfareconfig.fare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareChangeRequest;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareAction;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class FareChangeRequestValidationTests {

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

    private static FareChangeRequest validRequest(BigDecimal price, FareStatus status) {
        return new FareChangeRequest(
                "TENANT",
                GridMode.OD,
                "ORIGIN",
                "DEST",
                "PRODUCT",
                price,
                status,
                FareAction.INLINE_EDIT,
                null);
    }

    private static void assertHasViolationFor(FareChangeRequest request, String property) {
        Set<ConstraintViolation<FareChangeRequest>> violations = validator.validate(request);

        assertTrue(violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(property)));
    }

    @Test
    void acceptsActiveRequestWithPrice() {
        FareChangeRequest request = validRequest(BigDecimal.valueOf(12.50), FareStatus.ACTIVE);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsNotSoldRequestWithoutPrice() {
        FareChangeRequest request = validRequest(null, FareStatus.NOT_SOLD);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsNotSoldRequestWithPrice() {
        FareChangeRequest request = validRequest(BigDecimal.valueOf(12.50), FareStatus.NOT_SOLD);

        assertHasViolationFor(request, "priceValidForTargetStatus");
    }

    @Test
    void rejectsActiveRequestWithoutPrice() {
        FareChangeRequest request = validRequest(null, FareStatus.ACTIVE);

        assertHasViolationFor(request, "priceValidForTargetStatus");
    }

    @Test
    void rejectsNonEditableNaRequest() {
        FareChangeRequest request = validRequest(BigDecimal.valueOf(12.50), FareStatus.NA);

        assertHasViolationFor(request, "editable");
    }

    @Test
    void rejectsInvalidPriceRangeAndPrecision() {
        FareChangeRequest negativePrice = validRequest(BigDecimal.valueOf(-0.01), FareStatus.ACTIVE);
        FareChangeRequest excessivePrice = validRequest(BigDecimal.valueOf(10000), FareStatus.ACTIVE);
        FareChangeRequest excessivePrecision = validRequest(
                new BigDecimal("12.345"),
                FareStatus.ACTIVE);

        assertFalse(validator.validate(negativePrice).isEmpty());
        assertFalse(validator.validate(excessivePrice).isEmpty());
        assertFalse(validator.validate(excessivePrecision).isEmpty());
    }
}
