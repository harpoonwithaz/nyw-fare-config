package ca.sfu.cmpt276.nywfareconfig.fare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulateRequest;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulateResponse;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulationResult;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PricingDetail;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalZonalDtos.DimensionValueSummary;
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
    void getBaselineZonalFareRulesReturnsUntouchedRatesAndSynthesizedTiersWithoutQueryingRepository() {
        stubThreeZoneMetraMatrix("ONEWAY");

        List<FareRule> baselineRules = fareService.getBaselineZonalFareRules("METRA", "ONEWAY");

        assertEquals(9, baselineRules.size());
        assertTrue(baselineRules.stream().noneMatch(FareRule::isHasPendingDraft));

        FareRule zoneAToZoneB = findRule(baselineRules, "A", "B");
        assertEquals(new BigDecimal("4.25"), zoneAToZoneB.getPrice());
        assertEquals("TIER_B", zoneAToZoneB.getTierCode());
        assertEquals("PR-ONEWAY", zoneAToZoneB.getPriceRuleSetName());
        assertEquals(FareStatus.ACTIVE, zoneAToZoneB.getStatus());

        FareRule zoneAToZoneA = findRule(baselineRules, "A", "A");
        assertEquals(new BigDecimal("4.00"), zoneAToZoneA.getPrice());
        assertEquals("TIER_A", zoneAToZoneA.getTierCode());

        FareRule zoneAToZoneC = findRule(baselineRules, "A", "C");
        assertEquals(new BigDecimal("5.50"), zoneAToZoneC.getPrice());
        assertEquals("TIER_C", zoneAToZoneC.getTierCode());

        verifyNoInteractions(fareDraftRepository);
    }

    @Test
    void getZonalFareRulesOverlaysPendingDraftCaseInsensitivelyAndLeavesOtherRulesUntouched() {
        stubThreeZoneMetraMatrix("ONEWAY");

        FareDraft stagedDraft = new FareDraft(
                "METRA",
                "operator:operator@metra.com",
                GridMode.ZONAL,
                "  a  ",
                "B",
                "oneway",
                "TIER_C",
                new BigDecimal("4.25"),
                new BigDecimal("5.25"),
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                DraftStatus.PENDING);

        when(fareDraftRepository.findByTenantCodeAndDraftStatus("METRA", DraftStatus.PENDING))
                .thenReturn(List.of(stagedDraft));

        List<FareRule> rules = fareService.getZonalFareRules("metra", "ONEWAY");

        FareRule overlaidRule = findRule(rules, "A", "B");
        assertEquals(new BigDecimal("5.25"), overlaidRule.getPrice());
        assertEquals("TIER_C", overlaidRule.getTierCode());
        assertEquals(FareStatus.ACTIVE, overlaidRule.getStatus());
        assertTrue(overlaidRule.isHasPendingDraft());

        FareRule untouchedRule = findRule(rules, "A", "A");
        assertEquals(new BigDecimal("4.00"), untouchedRule.getPrice());
        assertEquals("TIER_A", untouchedRule.getTierCode());
        assertFalse(untouchedRule.isHasPendingDraft());
    }

    @Test
    void buildGridForZonalTenantOverlaysPendingPriceAndNotSoldStatusOntoFareCells() {
        stubThreeZoneMetraMatrix("ONEWAY");

        FareDraft priceDraft = new FareDraft(
                "METRA",
                "operator:operator@metra.com",
                GridMode.ZONAL,
                "A",
                "B",
                "ONEWAY",
                "TIER_C",
                new BigDecimal("4.25"),
                new BigDecimal("5.50"),
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                DraftStatus.PENDING);

        FareDraft notSoldDraft = new FareDraft(
                "METRA",
                "operator:operator@metra.com",
                GridMode.ZONAL,
                "A",
                "C",
                "ONEWAY",
                null,
                new BigDecimal("5.50"),
                null,
                FareStatus.NOT_SOLD,
                FareAction.INLINE_EDIT,
                DraftStatus.PENDING);

        when(fareDraftRepository.findByTenantCodeAndDraftStatus("METRA", DraftStatus.PENDING))
                .thenReturn(List.of(priceDraft, notSoldDraft));

        FareGrid grid = fareService.buildGrid(zonalTenant, "ONEWAY");
        assertNotNull(grid);

        // Row 0, Col 0 = A -> A (Untouched Baseline)
        FareCell untouchedCell = grid.getRows().get(0).get(0);
        assertEquals(new BigDecimal("4.00"), untouchedCell.getPrice());
        assertEquals("$4.00", untouchedCell.getFormattedPrice());
        assertEquals("TIER_A", untouchedCell.getTierCode());
        assertEquals(FareStatus.ACTIVE, untouchedCell.getStatus());
        assertTrue(untouchedCell.isEditable());
        assertFalse(untouchedCell.isHasPendingDraft());

        // Row 0, Col 1 = A -> B (Staged Price + Tier Draft Overlaid)
        FareCell editedCell = grid.getRows().get(0).get(1);
        assertEquals(new BigDecimal("5.50"), editedCell.getPrice());
        assertEquals("$5.50", editedCell.getFormattedPrice());
        assertEquals("TIER_C", editedCell.getTierCode());
        assertEquals(FareStatus.ACTIVE, editedCell.getStatus());
        assertTrue(editedCell.isEditable());
        assertTrue(editedCell.isHasPendingDraft());

        // Row 0, Col 2 = A -> C (Staged NOT_SOLD Draft Overlaid)
        FareCell notSoldCell = grid.getRows().get(0).get(2);
        assertNull(notSoldCell.getPrice());
        assertEquals(FareStatus.NOT_SOLD, notSoldCell.getStatus());
        assertFalse(notSoldCell.isEditable());
        assertTrue(notSoldCell.isHasPendingDraft());
    }

    @Test
    void getBaselineFareRuleReturnsUntouchedZonalRuleFromCaracalWithoutQueryingRepository() {
        PriceSimulateRequest expectedRequest = new PriceSimulateRequest(
                "METRA",
                "ONEWAY",
                null,
                Map.of("ORIGIN_ZONE", "A", "DEST_ZONE", "B", "FARE_CLASS", "FULL"),
                null,
                "UNATTENDED");
        when(caracalApiClient.simulatePrice(expectedRequest))
                .thenReturn(activeSimulation("PR-ONEWAY", "4.25"));

        Optional<FareRule> baseline = fareService.getBaselineFareRule(
                zonalTenant, "  a ", "B", "oneway");

        assertTrue(baseline.isPresent());
        assertEquals("METRA", baseline.get().getTenantCode());
        assertEquals("A", baseline.get().getOriginIdentifier());
        assertEquals("B", baseline.get().getDestIdentifier());
        assertEquals("ONEWAY", baseline.get().getProductCode());
        assertEquals("PR-ONEWAY", baseline.get().getPriceRuleSetName());
        assertEquals(new BigDecimal("4.25"), baseline.get().getPrice());
        assertEquals(FareStatus.ACTIVE, baseline.get().getStatus());
        assertFalse(baseline.get().isHasPendingDraft());
        verify(caracalApiClient).simulatePrice(expectedRequest);
        verifyNoInteractions(fareDraftRepository);
    }

    private void stubThreeZoneMetraMatrix(String productCode) {
        when(caracalApiClient.fetchZones("METRA")).thenReturn(List.of(
                new DimensionValueSummary("A", "Zone A"),
                new DimensionValueSummary("B", "Zone B"),
                new DimensionValueSummary("C", "Zone C")));

        when(caracalApiClient.simulatePrice(any())).thenAnswer(invocation -> {
            PriceSimulateRequest req = invocation.getArgument(0);
            String origin = req.dimensions().get("ORIGIN_ZONE");
            String dest = req.dimensions().get("DEST_ZONE");
            int distance = Math.abs(origin.charAt(0) - dest.charAt(0));
            String price = switch (distance) {
                case 0 -> "4.00";
                case 1 -> "4.25";
                default -> "5.50";
            };
            return activeSimulation("PR-" + productCode, price);
        });
    }

    private static PriceSimulationResult activeSimulation(String ruleSetName, String price) {
        BigDecimal amount = new BigDecimal(price);
        return new PriceSimulationResult(
                new PriceSimulateResponse(new PricingDetail(ruleSetName, amount, amount)),
                null);
    }

    private static FareRule findRule(List<FareRule> rules, String origin, String dest) {
        return rules.stream()
                .filter(r -> origin.equals(r.getOriginIdentifier())
                        && dest.equals(r.getDestIdentifier()))
                .findFirst()
                .orElseThrow();
    }
}