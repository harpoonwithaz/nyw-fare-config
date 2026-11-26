package ca.sfu.cmpt276.nywfareconfig.fare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.SiteSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulateRequest;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulateResponse;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulationResult;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PricingDetail;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalZonalDtos.DimensionValueSummary;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.CatalogOption;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareGrid;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareRule;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.DraftStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareCell;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.repository.FareDraftRepository;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

@ExtendWith(MockitoExtension.class)
class FareServiceZonalHydrationTests {

    private static final ZoneId CHICAGO_ZONE = ZoneId.of("America/Chicago");

    @Mock
    private FareDraftRepository fareDraftRepository;

    @Mock
    private CaracalApiClient caracalApiClient;

    private FareService fareService;
    private Tenant zonalTenant;

    @BeforeEach
    void setUp() {
        fareService = new FareService(fareDraftRepository, caracalApiClient);
        zonalTenant = new Tenant("metra", "Metra Rail", "USD", "en-US", GridMode.ZONAL, true, List.of());
    }

    @Test
    void getZonesAndFareClassesNormalizeValuesAndHandleBlankTenants() {
        assertTrue(fareService.getZones(null).isEmpty());
        assertTrue(fareService.getZones("   ").isEmpty());
        assertTrue(fareService.getFareClasses(null).isEmpty());
        assertTrue(fareService.getFareClasses("   ").isEmpty());
        verifyNoInteractions(caracalApiClient);

        when(caracalApiClient.fetchZones("METRA")).thenReturn(List.of(
                new DimensionValueSummary(" a ", "Zone A"),
                new DimensionValueSummary("b", " "),
                new DimensionValueSummary(" ", "Ignored")));
        when(caracalApiClient.fetchFareClasses("METRA")).thenReturn(List.of(
                new DimensionValueSummary("full", "Full"),
                new DimensionValueSummary("reduced", "Reduced")));

        List<CatalogOption> zones = fareService.getZones(" metra ");
        assertEquals(List.of(
                new CatalogOption("A", "Zone A"),
                new CatalogOption("B", "B")), zones);

        List<CatalogOption> fareClasses = fareService.getFareClasses("metra");
        assertEquals(List.of(
                new CatalogOption("FULL", "Full"),
                new CatalogOption("REDUCED", "Reduced")), fareClasses);
    }

    @Test
    void resolveZonalPricingTimestampUsesFirstNonNullTenantTimezoneSkipsFetchForOffsetAndFallsBackToUtc() {
        when(caracalApiClient.fetchSites("METRA")).thenReturn(List.of(
                new SiteSummary("ST013", "35th/Lou Jones (RI)", null),
                new SiteSummary("ST051", "Union Station (BNSF)", CHICAGO_ZONE)));

        // CDT (Labor Day) -> -05:00
        assertEquals("2026-09-07T10:00:00-05:00",
                fareService.resolveZonalPricingTimestamp(" metra ", "2026-09-07"));
        // CST (Winter) -> -06:00
        assertEquals("2026-12-15T10:00:00-06:00",
                fareService.resolveZonalPricingTimestamp("METRA", "2026-12-15"));

        // Already an ISO-8601 offset datetime -> should NOT call fetchSites again
        assertEquals("2026-09-07T14:30:00-05:00",
                fareService.resolveZonalPricingTimestamp("METRA", "2026-09-07T14:30:00-05:00"));
        verify(caracalApiClient, times(2)).fetchSites("METRA");

        // Fallback to UTC when no site carries a timezone
        when(caracalApiClient.fetchSites("METRA")).thenReturn(List.of(
                new SiteSummary("ST051", "Union Station (BNSF)", null)));
        assertEquals("2026-09-07T10:00:00Z",
                fareService.resolveZonalPricingTimestamp("METRA", "2026-09-07"));
    }

    @Test
    void buildGridForZonalTenantSimulatesLiveMatrixMaps4xxAndSynthesizesAscendingTiers() {
        when(caracalApiClient.fetchZones("METRA")).thenReturn(List.of(
                new DimensionValueSummary("A", "Zone A"),
                new DimensionValueSummary("H", "Zone H")));
        when(caracalApiClient.fetchSites("METRA")).thenReturn(List.of(
                new SiteSummary("ST051", "Union Station (BNSF)", CHICAGO_ZONE)));
        when(fareDraftRepository.findByTenantCodeAndDraftStatus("METRA", DraftStatus.PENDING))
                .thenReturn(List.of());

        String expectedAt = "2026-09-07T10:00:00-05:00";

        // A -> A ($4.00 -> TIER_A), A -> H ($8.25 -> TIER_B)
        when(caracalApiClient.simulatePrice(new PriceSimulateRequest(
                "METRA", "ONEWAY", null,
                Map.of("ORIGIN_ZONE", "A", "DEST_ZONE", "A", "FARE_CLASS", "FULL"),
                expectedAt, "UNATTENDED")))
                .thenReturn(activeSimulation("PR-ONEWAY", "4.0"));
        when(caracalApiClient.simulatePrice(new PriceSimulateRequest(
                "METRA", "ONEWAY", null,
                Map.of("ORIGIN_ZONE", "A", "DEST_ZONE", "H", "FARE_CLASS", "FULL"),
                expectedAt, "UNATTENDED")))
                .thenReturn(activeSimulation("PR-ONEWAY", "8.25"));

        // H -> A (condition 4xx -> NOT_SOLD), H -> H (no PriceRule 4xx -> NA)
        when(caracalApiClient.simulatePrice(new PriceSimulateRequest(
                "METRA", "ONEWAY", null,
                Map.of("ORIGIN_ZONE", "H", "DEST_ZONE", "A", "FARE_CLASS", "FULL"),
                expectedAt, "UNATTENDED")))
                .thenReturn(new PriceSimulationResult(
                        null,
                        "No rate assignment's condition matches the requested moment or channel"));
        when(caracalApiClient.simulatePrice(new PriceSimulateRequest(
                "METRA", "ONEWAY", null,
                Map.of("ORIGIN_ZONE", "H", "DEST_ZONE", "H", "FARE_CLASS", "FULL"),
                expectedAt, "UNATTENDED")))
                .thenReturn(new PriceSimulationResult(
                        null,
                        "no PriceRule matched the resolved dimension context"));

        FareGrid grid = fareService.buildGrid(zonalTenant, "  oneway ", "2026-09-07");

        assertNotNull(grid);
        assertEquals(List.of("A", "H"), grid.getRowHeaders().stream().map(CatalogOption::code).toList());
        assertEquals(List.of("A", "H"), grid.getColumnHeaders().stream().map(CatalogOption::code).toList());

        FareCell aToA = grid.getRows().get(0).get(0);
        assertEquals(FareStatus.ACTIVE, aToA.getStatus());
        assertEquals(new BigDecimal("4.00"), aToA.getPrice());
        assertEquals("TIER_A", aToA.getTierCode());

        FareCell aToH = grid.getRows().get(0).get(1);
        assertEquals(FareStatus.ACTIVE, aToH.getStatus());
        assertEquals(new BigDecimal("8.25"), aToH.getPrice());
        assertEquals("TIER_B", aToH.getTierCode());

        FareCell hToA = grid.getRows().get(1).get(0);
        assertEquals(FareStatus.NOT_SOLD, hToA.getStatus());
        assertNull(hToA.getPrice());
        assertNull(hToA.getTierCode());

        FareCell hToH = grid.getRows().get(1).get(1);
        assertEquals(FareStatus.NA, hToH.getStatus());
        assertNull(hToH.getPrice());
        assertNull(hToH.getTierCode());

        // Timezone fetched once for the entire zonal matrix build
        verify(caracalApiClient, times(1)).fetchSites("METRA");
    }

    @Test
    void buildGridForZonalTenantForwardsExplicitFareClassAndDefaultsBlankToFull() {
        when(caracalApiClient.fetchZones("METRA")).thenReturn(List.of(
                new DimensionValueSummary("A", "Zone A")));
        when(fareDraftRepository.findByTenantCodeAndDraftStatus("METRA", DraftStatus.PENDING))
                .thenReturn(List.of());
        when(caracalApiClient.simulatePrice(any())).thenReturn(activeSimulation("PR-ONEWAY", "2.00"));

        fareService.buildGrid(zonalTenant, "ONEWAY", null, "  reduced ");
        fareService.buildGrid(zonalTenant, "ONEWAY", null, "   ");

        ArgumentCaptor<PriceSimulateRequest> captor = ArgumentCaptor.forClass(PriceSimulateRequest.class);
        verify(caracalApiClient, times(2)).simulatePrice(captor.capture());

        List<PriceSimulateRequest> requests = captor.getAllValues();
        assertNull(requests.get(0).siteCode());
        assertEquals("UNATTENDED", requests.get(0).channel());
        assertEquals("REDUCED", requests.get(0).dimensions().get("FARE_CLASS"));
        assertEquals("FULL", requests.get(1).dimensions().get("FARE_CLASS"));
    }

    @Test
    void buildGridForZonalTenantReturnsEmptyGridWithoutSimulatingWhenZonesAreUnavailable() {
        when(caracalApiClient.fetchZones("METRA")).thenReturn(List.of());

        FareGrid grid = fareService.buildGrid(zonalTenant, "ONEWAY", "2026-09-07", "FULL");

        assertNotNull(grid);
        assertTrue(grid.getRowHeaders().isEmpty());
        assertTrue(grid.getColumnHeaders().isEmpty());
        assertTrue(grid.getRows().isEmpty());
        verify(caracalApiClient, never()).simulatePrice(any());
        verify(caracalApiClient, never()).fetchSites(any());
        verifyNoInteractions(fareDraftRepository);
    }

    @Test
    void getBaselineFareRuleForZonalTenantSimulatesSingleCellWithFormattedTimestampAndFareClass() {
        when(caracalApiClient.fetchSites("METRA")).thenReturn(List.of(
                new SiteSummary("ST051", "Union Station (BNSF)", CHICAGO_ZONE)));
        when(caracalApiClient.simulatePrice(any())).thenReturn(activeSimulation("PR-ONEWAY", "4.0"));

        Optional<FareRule> result = fareService.getBaselineFareRule(
                zonalTenant, " a ", " h ", " oneway ", "2026-09-07", " reduced ");

        assertTrue(result.isPresent());
        FareRule rule = result.get();
        assertEquals("METRA", rule.getTenantCode());
        assertEquals("A", rule.getOriginIdentifier());
        assertEquals("H", rule.getDestIdentifier());
        assertEquals("ONEWAY", rule.getProductCode());
        assertEquals("PR-ONEWAY", rule.getPriceRuleSetName());
        assertEquals(new BigDecimal("4.00"), rule.getPrice());
        assertEquals(FareStatus.ACTIVE, rule.getStatus());
        assertFalse(rule.isHasPendingDraft());

        ArgumentCaptor<PriceSimulateRequest> captor = ArgumentCaptor.forClass(PriceSimulateRequest.class);
        verify(caracalApiClient).simulatePrice(captor.capture());
        PriceSimulateRequest sent = captor.getValue();
        assertNull(sent.siteCode());
        assertEquals("UNATTENDED", sent.channel());
        assertEquals("2026-09-07T10:00:00-05:00", sent.at());
        assertEquals(
                Map.of("ORIGIN_ZONE", "A", "DEST_ZONE", "H", "FARE_CLASS", "REDUCED"),
                sent.dimensions());
        verify(caracalApiClient, never()).fetchZones(any());
        verifyNoInteractions(fareDraftRepository);
    }

    @Test
    void buildGridAndResolveZonalPricingTimestampFailFastOnInvalidDateWithoutCallingApi() {
        assertThrows(IllegalArgumentException.class,
                () -> fareService.buildGrid(zonalTenant, "ONEWAY", "not-a-date", "FULL"));
        assertThrows(IllegalArgumentException.class,
                () -> fareService.resolveZonalPricingTimestamp("METRA", "not-a-date"));

        verifyNoInteractions(caracalApiClient, fareDraftRepository);
    }

    private static PriceSimulationResult activeSimulation(String ruleSetName, String price) {
        BigDecimal amount = new BigDecimal(price);
        return new PriceSimulationResult(
                new PriceSimulateResponse(new PricingDetail(ruleSetName, amount, amount)),
                null);
    }
}