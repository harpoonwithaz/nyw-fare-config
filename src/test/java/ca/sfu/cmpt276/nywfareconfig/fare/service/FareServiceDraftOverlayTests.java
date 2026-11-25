package ca.sfu.cmpt276.nywfareconfig.fare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
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

    @Mock
    private CaracalApiClient caracalApiClient;

    private FareService fareService;
    private Tenant odTenant;
    private Tenant zonalTenant;

    @BeforeEach
    void setUp() {
        fareService = new FareService(fareDraftRepository, caracalApiClient);
        odTenant = new Tenant("NYW_SANDBOX", "NY Waterway", "USD", "en-US", GridMode.OD, true, List.of());
        zonalTenant = new Tenant("METRA", "Metra Rail", "USD", "en-US", GridMode.ZONAL, true, List.of());
    }

    @Test
    void getBaselineFareRuleReturnsEmptyWithoutQueryingDependenciesWhenInputsAreNull() {
        assertTrue(fareService.getBaselineFareRule(null, "EDGEWTR", "MIDTOWN", "ADULT_ONEWAY").isEmpty());
        assertTrue(fareService.getBaselineFareRule(odTenant, null, "MIDTOWN", "ADULT_ONEWAY").isEmpty());
        assertTrue(fareService.getBaselineFareRule(odTenant, "EDGEWTR", null, "ADULT_ONEWAY").isEmpty());
        assertTrue(fareService.getBaselineFareRule(odTenant, "EDGEWTR", "MIDTOWN", null).isEmpty());

        verifyNoInteractions(fareDraftRepository, caracalApiClient);
    }

    @Test
    void buildGridReturnsNullWhenTenantIsNullWithoutQueryingDependencies() {
        assertNull(fareService.buildGrid(null, "EDGEWTR"));
        verifyNoInteractions(fareDraftRepository, caracalApiClient);
    }

    @Test
    void getBaselineZonalFareRulesReturnsUntouchedRatesWithoutQueryingRepositoryOrApi() {
        List<FareRule> baselineRules = fareService.getBaselineZonalFareRules("METRA", "ADULT_ONEWAY");

        assertEquals(9, baselineRules.size());
        assertTrue(baselineRules.stream().noneMatch(FareRule::isHasPendingDraft));

        FareRule zone1ToZone2 = baselineRules.stream()
                .filter(r -> "Zone 1".equals(r.getOriginIdentifier())
                        && "Zone 2".equals(r.getDestIdentifier()))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("4.00"), zone1ToZone2.getPrice());
        assertEquals(FareStatus.ACTIVE, zone1ToZone2.getStatus());
        verifyNoInteractions(fareDraftRepository, caracalApiClient);
    }

    @Test
    void getZonalFareRulesOverlaysPendingDraftCaseInsensitivelyAndLeavesOtherRulesUntouched() {
        FareDraft stagedDraft = new FareDraft(
                "METRA",
                "operator:operator@metra.com",
                GridMode.ZONAL,
                "  zone 1  ",
                "ZONE 2",
                "adult_oneway",
                "TIER_C",
                new BigDecimal("4.00"),
                new BigDecimal("5.25"),
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                DraftStatus.PENDING);

        when(fareDraftRepository.findByTenantCodeAndDraftStatus("METRA", DraftStatus.PENDING))
                .thenReturn(List.of(stagedDraft));

        List<FareRule> rules = fareService.getZonalFareRules("metra", "ADULT_ONEWAY");

        FareRule overlaidRule = rules.stream()
                .filter(r -> "Zone 1".equals(r.getOriginIdentifier())
                        && "Zone 2".equals(r.getDestIdentifier()))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("5.25"), overlaidRule.getPrice());
        assertEquals("TIER_C", overlaidRule.getTierCode());
        assertEquals(FareStatus.ACTIVE, overlaidRule.getStatus());
        assertTrue(overlaidRule.isHasPendingDraft());

        FareRule untouchedRule = rules.stream()
                .filter(r -> "Zone 1".equals(r.getOriginIdentifier())
                        && "Zone 1".equals(r.getDestIdentifier()))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("2.50"), untouchedRule.getPrice());
        assertFalse(untouchedRule.isHasPendingDraft());
        verifyNoInteractions(caracalApiClient);
    }

    @Test
    void buildGridForZonalTenantOverlaysPendingPriceAndNotSoldStatusOntoFareCells() {
        FareDraft priceDraft = new FareDraft(
                "METRA",
                "operator:operator@metra.com",
                GridMode.ZONAL,
                "Zone 1",
                "Zone 2",
                "ADULT_ONEWAY",
                null,
                new BigDecimal("4.00"),
                new BigDecimal("5.50"),
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                DraftStatus.PENDING);

        FareDraft notSoldDraft = new FareDraft(
                "METRA",
                "operator:operator@metra.com",
                GridMode.ZONAL,
                "Zone 1",
                "Zone 3",
                "ADULT_ONEWAY",
                null,
                new BigDecimal("6.50"),
                null,
                FareStatus.NOT_SOLD,
                FareAction.INLINE_EDIT,
                DraftStatus.PENDING);

        when(fareDraftRepository.findByTenantCodeAndDraftStatus("METRA", DraftStatus.PENDING))
                .thenReturn(List.of(priceDraft, notSoldDraft));

        FareGrid grid = fareService.buildGrid(zonalTenant, "ADULT_ONEWAY");
        assertNotNull(grid);

        // Row 0, Col 0 = Zone 1 -> Zone 1 (Untouched Baseline)
        FareCell untouchedCell = grid.getRows().get(0).get(0);
        assertEquals(new BigDecimal("2.50"), untouchedCell.getPrice());
        assertEquals("$2.50", untouchedCell.getFormattedPrice());
        assertEquals(FareStatus.ACTIVE, untouchedCell.getStatus());
        assertTrue(untouchedCell.isEditable());
        assertFalse(untouchedCell.isHasPendingDraft());

        // Row 0, Col 1 = Zone 1 -> Zone 2 (Staged Price Draft Overlaid)
        FareCell editedCell = grid.getRows().get(0).get(1);
        assertEquals(new BigDecimal("5.50"), editedCell.getPrice());
        assertEquals("$5.50", editedCell.getFormattedPrice());
        assertEquals(FareStatus.ACTIVE, editedCell.getStatus());
        assertTrue(editedCell.isEditable());
        assertTrue(editedCell.isHasPendingDraft());

        // Row 0, Col 2 = Zone 1 -> Zone 3 (Staged NOT_SOLD Draft Overlaid)
        FareCell notSoldCell = grid.getRows().get(0).get(2);
        assertNull(notSoldCell.getPrice());
        assertEquals(FareStatus.NOT_SOLD, notSoldCell.getStatus());
        assertFalse(notSoldCell.isEditable());
        assertTrue(notSoldCell.isHasPendingDraft());
    }

    @Test
    void getBaselineFareRuleReturnsUntouchedZonalRuleWithoutQueryingDependencies() {
        Optional<FareRule> baseline = fareService.getBaselineFareRule(
                zonalTenant, "  zone 1 ", "ZONE 2", "adult_oneway");

        assertTrue(baseline.isPresent());
        assertEquals("METRA", baseline.get().getTenantCode());
        assertEquals("Zone 1", baseline.get().getOriginIdentifier());
        assertEquals("Zone 2", baseline.get().getDestIdentifier());
        assertEquals("ADULT_ONEWAY", baseline.get().getProductCode());
        assertEquals("TIER_B", baseline.get().getTierCode());
        assertEquals(new BigDecimal("4.00"), baseline.get().getPrice());
        assertEquals(FareStatus.ACTIVE, baseline.get().getStatus());
        assertFalse(baseline.get().isHasPendingDraft());
        verifyNoInteractions(fareDraftRepository, caracalApiClient);
    }
}