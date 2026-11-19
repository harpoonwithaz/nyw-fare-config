package ca.sfu.cmpt276.nywfareconfig.fare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareGrid;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareRule;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.DraftStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareAction;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareCell;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.repository.FareDraftRepository;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

@ExtendWith(MockitoExtension.class)
class FareServiceDraftOverlayTests {

    @Mock
    private FareDraftRepository fareDraftRepository;

    private FareService fareService;

    @BeforeEach
    void setUp() {
        fareService = new FareService(fareDraftRepository);
    }

    @Test
    void getBaselineFareRuleReturnsUntouchedPriceWithoutQueryingRepository() {
        Optional<FareRule> baseline = fareService.getBaselineFareRule(
                "NYW_SANDBOX", "EDGEWTR", "Midtown / W. 39th St", "Adult One-Way");

        assertTrue(baseline.isPresent());
        assertEquals(new BigDecimal("13.25"), baseline.get().getPrice());
        assertFalse(baseline.get().isHasPendingDraft());
        verifyNoInteractions(fareDraftRepository);
    }

    @Test
    void buildMockGridOverlaysPendingDraftPriceAndSetsHasPendingDraftOnFareCell() {
        FareDraft stagedDraft = new FareDraft(
                "NYW_SANDBOX",
                "operator:operator@nyw.com",
                GridMode.OD,
                "EDGEWTR",
                "Midtown / W. 39th St",
                "Adult One-Way",
                null,
                new BigDecimal("13.25"),
                new BigDecimal("16.00"),
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                DraftStatus.PENDING);

        when(fareDraftRepository.findByTenantCodeAndDraftStatus("NYW_SANDBOX", DraftStatus.PENDING))
                .thenReturn(List.of(stagedDraft));

        Tenant tenant = new Tenant("NYW_SANDBOX", "NY Waterway", "USD", "en-US", GridMode.OD, true, List.of());
        FareGrid grid = fareService.buildMockGrid(tenant, "EDGEWTR");

        // Row 0 = Midtown / W. 39th St, Col 0 = Adult One-Way (Staged Draft Overlaid)
        FareCell editedCell = grid.getRows().get(0).get(0);
        assertEquals(new BigDecimal("16.00"), editedCell.getPrice());
        assertEquals("$16.00", editedCell.getFormattedPrice());
        assertTrue(editedCell.isHasPendingDraft());

        // Row 0, Col 1 = Child One-Way (Untouched Baseline)
        FareCell untouchedCell = grid.getRows().get(0).get(1);
        assertEquals(new BigDecimal("9.00"), untouchedCell.getPrice());
        assertFalse(untouchedCell.isHasPendingDraft());
    }
}