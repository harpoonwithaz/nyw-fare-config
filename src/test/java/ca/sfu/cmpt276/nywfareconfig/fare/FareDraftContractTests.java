package ca.sfu.cmpt276.nywfareconfig.fare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareChangeRequest;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.DraftStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareAction;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;

class FareDraftContractTests {

    @Test
    void notSoldChangeCannotCarryPrice() {
        FareChangeRequest request = new FareChangeRequest(
                "TENANT",
                GridMode.OD,
                "ORIGIN",
                "DEST",
                "PRODUCT",
                BigDecimal.TEN,
                FareStatus.NOT_SOLD,
                FareAction.INLINE_EDIT,
                null);

        assertFalse(request.isPriceValidForTargetStatus());
    }

    @Test
    void fareDraftUsesExplicitPendingStatus() {
        FareDraft draft = new FareDraft(
                "TENANT",
                "actor@example.com",
                GridMode.OD,
                "ORIGIN",
                "DEST",
                "PRODUCT",
                null,
                BigDecimal.ONE,
                BigDecimal.TEN,
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                DraftStatus.PENDING);

        assertEquals(DraftStatus.PENDING, draft.getDraftStatus());
    }

    @Test
    void notSoldFareDraftNormalizesPriceToNull() {
        FareDraft draft = new FareDraft(
                "TENANT",
                "actor@example.com",
                GridMode.OD,
                "ORIGIN",
                "DEST",
                "PRODUCT",
                null,
                BigDecimal.TEN,
                BigDecimal.TEN,
                FareStatus.NOT_SOLD,
                FareAction.INLINE_EDIT,
                DraftStatus.PENDING);

        assertNull(draft.getDraftPrice());
    }
}
