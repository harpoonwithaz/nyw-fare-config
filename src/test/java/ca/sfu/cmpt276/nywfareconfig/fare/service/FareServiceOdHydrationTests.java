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
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.ProductSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.RateAssignmentSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.RateTableGetRequest;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.SiteSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulateRequest;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulateResponse;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulationResult;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PricingDetail;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.CatalogOption;
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
class FareServiceOdHydrationTests {

    private static final ZoneId NY_ZONE = ZoneId.of("America/New_York");

    @Mock
    private FareDraftRepository fareDraftRepository;

    @Mock
    private CaracalApiClient caracalApiClient;

    private FareService fareService;
    private Tenant odTenant;

    @BeforeEach
    void setUp() {
        fareService = new FareService(fareDraftRepository, caracalApiClient);
        odTenant = new Tenant("nyw_sandbox", "NY Waterway", "USD", "en-US", GridMode.OD, true, List.of());
    }

    @Test
    void normalizePricingDateHandlesBlankDateOffsetAndRejectsMalformedValues() {
        assertNull(FareService.normalizePricingDate(null));
        assertNull(FareService.normalizePricingDate("   "));
        assertEquals("2026-10-24", FareService.normalizePricingDate("  2026-10-24  "));
        assertEquals(
                "2026-10-24",
                FareService.normalizePricingDate("2026-10-24T10:00:00-04:00"));

        assertThrows(IllegalArgumentException.class, () -> FareService.normalizePricingDate("not-a-date"));
        assertThrows(IllegalArgumentException.class, () -> FareService.normalizePricingDate("2026-02-31"));
    }

    @Test
    void resolvePricingTimestampUsesSiteTimezoneForLocalDateAndSkipsFetchForOffsetDatetime() {
        when(caracalApiClient.fetchSites("NYW_SANDBOX")).thenReturn(List.of(
                new SiteSummary("MIDTOWN", "Midtown / W. 39th St", NY_ZONE)));

        String edtTimestamp = fareService.resolvePricingTimestamp("nyw_sandbox", " midtown ", "2026-10-24");
        assertEquals("2026-10-24T10:00:00-04:00", edtTimestamp);

        String estTimestamp = fareService.resolvePricingTimestamp("nyw_sandbox", "midtown", "2026-12-15");
        assertEquals("2026-12-15T10:00:00-05:00", estTimestamp);

        // Already an ISO-8601 offset datetime -> should not call fetchSites again
        String alreadyFormatted = fareService.resolvePricingTimestamp(
                "nyw_sandbox", "midtown", "2026-10-24T15:30:00-04:00");
        assertEquals("2026-10-24T15:30:00-04:00", alreadyFormatted);

        verify(caracalApiClient, times(2)).fetchSites("NYW_SANDBOX");
    }

    @Test
    void resolvePricingTimestampFallsBackToUtcWhenSiteTimezoneIsMissing() {
        when(caracalApiClient.fetchSites("NYW_SANDBOX")).thenReturn(List.of(
                new SiteSummary("MIDTOWN", "Midtown / W. 39th St", null)));

        String utcTimestamp = fareService.resolvePricingTimestamp("NYW_SANDBOX", "MIDTOWN", "2026-10-24");
        assertEquals("2026-10-24T10:00:00Z", utcTimestamp);
    }

    @Test
    void buildGridForOdTenantPrefiltersRateTableFetchesSitesOnceMapsErrorsAndHidesInactiveRows() {
        // Products returned in API order; buildOdGrid reverses them -> [WEEKEND_TICKET, SENIOR_ONEWAY, ADULT_ONEWAY]
        when(caracalApiClient.fetchProducts("NYW_SANDBOX")).thenReturn(List.of(
                new ProductSummary("adult_oneway", "Adult One-Way", true),
                new ProductSummary("INACTIVE_PROD", "Archived Ticket", false),
                new ProductSummary("SENIOR_ONEWAY", "Senior 65+", true),
                new ProductSummary("WEEKEND_TICKET", "Weekend Ticket", true)));

        when(caracalApiClient.fetchSites("NYW_SANDBOX")).thenReturn(List.of(
                new SiteSummary("MIDTOWN", "Midtown / W. 39th St", NY_ZONE),
                new SiteSummary("EDGEWTR", "Edgewater", NY_ZONE),
                new SiteSummary("UNSERVED", "Unserved Pier", NY_ZONE)));

        // SENIOR_ONEWAY is NOT_SOLD in rate table (should skip simulatePrice completely)
        // WEEKEND_TICKET has both a NOT_SOLD fallback and a VALUE assignment -> should be simulated
        when(caracalApiClient.fetchRateTable(new RateTableGetRequest("NYW_SANDBOX", "RT-MIDTOWN")))
                .thenReturn(Optional.of(List.of(
                        new RateAssignmentSummary("ADULT_ONEWAY", "VALUE", "PRS_ADULT"),
                        new RateAssignmentSummary("SENIOR_ONEWAY", "NOT_SOLD", null),
                        new RateAssignmentSummary("WEEKEND_TICKET", "NOT_SOLD", null),
                        new RateAssignmentSummary("WEEKEND_TICKET", "VALUE", "PRS_WEEKEND"))));

        String expectedAt = "2026-10-20T10:00:00-04:00";

        // EDGEWTR: ADULT_ONEWAY active ($13.25), WEEKEND_TICKET condition mismatch (NOT_SOLD)
        when(caracalApiClient.simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "ADULT_ONEWAY", "MIDTOWN", Map.of("DEST", "EDGEWTR"), expectedAt)))
                .thenReturn(new PriceSimulationResult(
                        new PriceSimulateResponse(new PricingDetail("PRS_ADULT", new BigDecimal("13.25"), new BigDecimal("13.25"))),
                        null));
        when(caracalApiClient.simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "WEEKEND_TICKET", "MIDTOWN", Map.of("DEST", "EDGEWTR"), expectedAt)))
                .thenReturn(new PriceSimulationResult(
                        null,
                        "No rate assignment's condition matches the requested moment or channel"));

        // UNSERVED: ADULT_ONEWAY returns mixed-case "no PriceRule..." (NA), WEEKEND_TICKET returns "not sold" (NOT_SOLD)
        // Entire UNSERVED row has 0 ACTIVE cells and 0 pending drafts -> must be hidden
        when(caracalApiClient.simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "ADULT_ONEWAY", "MIDTOWN", Map.of("DEST", "UNSERVED"), expectedAt)))
                .thenReturn(new PriceSimulationResult(
                        null,
                        "no PriceRule matched the resolved dimension context"));
        when(caracalApiClient.simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "WEEKEND_TICKET", "MIDTOWN", Map.of("DEST", "UNSERVED"), expectedAt)))
                .thenReturn(new PriceSimulationResult(null, "Product is not sold at this site"));

        when(fareDraftRepository.findByTenantCodeAndDraftStatus("NYW_SANDBOX", DraftStatus.PENDING))
                .thenReturn(List.of());

        FareGrid grid = fareService.buildGrid(odTenant, "  midtown  ", "2026-10-20");

        assertNotNull(grid);
        // Inactive product filtered out; order reversed
        assertEquals(
                List.of("WEEKEND_TICKET", "SENIOR_ONEWAY", "ADULT_ONEWAY"),
                grid.getColumnHeaders().stream().map(CatalogOption::code).toList());
        // MIDTOWN excluded as self; UNSERVED hidden because it has no active cells or drafts
        assertEquals(
                List.of("EDGEWTR"),
                grid.getRowHeaders().stream().map(CatalogOption::code).toList());
        assertEquals(1, grid.getRows().size());

        List<FareCell> edgewaterRow = grid.getRows().get(0);
        // Col 0: WEEKEND_TICKET -> condition 4xx mapped to NOT_SOLD
        assertEquals(FareStatus.NOT_SOLD, edgewaterRow.get(0).getStatus());
        assertNull(edgewaterRow.get(0).getPrice());
        // Col 1: SENIOR_ONEWAY -> skipped by rate-table pre-filter -> NOT_SOLD
        assertEquals(FareStatus.NOT_SOLD, edgewaterRow.get(1).getStatus());
        // Col 2: ADULT_ONEWAY -> ACTIVE $13.25
        assertEquals(FareStatus.ACTIVE, edgewaterRow.get(2).getStatus());
        assertEquals(new BigDecimal("13.25"), edgewaterRow.get(2).getPrice());
        assertEquals("$13.25", edgewaterRow.get(2).getFormattedPrice());

        // Single fetchSites call for the entire grid hydration (no redundant lookups in inner loop)
        verify(caracalApiClient, times(1)).fetchSites("NYW_SANDBOX");
        // SENIOR_ONEWAY was never sent to price-simulate
        verify(caracalApiClient, never()).simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "SENIOR_ONEWAY", "MIDTOWN", Map.of("DEST", "EDGEWTR"), expectedAt));
        verify(caracalApiClient, never()).simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "SENIOR_ONEWAY", "MIDTOWN", Map.of("DEST", "UNSERVED"), expectedAt));
    }

    @Test
    void buildGridForOdTenantReturnsEmptyRowsWithoutSimulatingWhenRateTableIsMissing() {
        when(caracalApiClient.fetchProducts("NYW_SANDBOX")).thenReturn(List.of(
                new ProductSummary("ADULT_ONEWAY", "Adult One-Way", true)));
        when(caracalApiClient.fetchSites("NYW_SANDBOX")).thenReturn(List.of(
                new SiteSummary("PAULSHK", "Paulus Hook", NY_ZONE),
                new SiteSummary("MIDTOWN", "Midtown / W. 39th St", NY_ZONE)));
        when(caracalApiClient.fetchRateTable(new RateTableGetRequest("NYW_SANDBOX", "RT-PAULSHK")))
                .thenReturn(Optional.empty());

        FareGrid grid = fareService.buildGrid(odTenant, "paulshk", null);

        assertNotNull(grid);
        assertTrue(grid.getRows().isEmpty());
        assertTrue(grid.getRowHeaders().isEmpty());
        verify(caracalApiClient, never()).simulatePrice(any());
    }

    @Test
    void buildGridForOdTenantOverlaysPendingDraftAndKeepsRowVisible() {
        when(caracalApiClient.fetchProducts("NYW_SANDBOX")).thenReturn(List.of(
                new ProductSummary("ADULT_ONEWAY", "Adult One-Way", true)));
        when(caracalApiClient.fetchSites("NYW_SANDBOX")).thenReturn(List.of(
                new SiteSummary("MIDTOWN", "Midtown / W. 39th St", NY_ZONE),
                new SiteSummary("EDGEWTR", "Edgewater", NY_ZONE)));
        when(caracalApiClient.fetchRateTable(new RateTableGetRequest("NYW_SANDBOX", "RT-MIDTOWN")))
                .thenReturn(Optional.of(List.of(
                        new RateAssignmentSummary("ADULT_ONEWAY", "VALUE", "PRS_ADULT"))));
        when(caracalApiClient.simulatePrice(any())).thenReturn(new PriceSimulationResult(
                new PriceSimulateResponse(new PricingDetail("PRS_ADULT", new BigDecimal("13.25"), new BigDecimal("13.25"))),
                null));

        FareDraft pendingDraft = new FareDraft(
                "NYW_SANDBOX",
                "operator:operator@nyw.com",
                GridMode.OD,
                "midtown",
                "edgewtr",
                "adult_oneway",
                null,
                new BigDecimal("13.25"),
                new BigDecimal("14.50"),
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                DraftStatus.PENDING);
        when(fareDraftRepository.findByTenantCodeAndDraftStatus("NYW_SANDBOX", DraftStatus.PENDING))
                .thenReturn(List.of(pendingDraft));

        FareGrid grid = fareService.buildGrid(odTenant, "MIDTOWN", null);

        assertEquals(1, grid.getRows().size());
        FareCell cell = grid.getRows().get(0).get(0);
        assertEquals(new BigDecimal("14.50"), cell.getPrice());
        assertEquals("$14.50", cell.getFormattedPrice());
        assertTrue(cell.isHasPendingDraft());
    }

    @Test
    void getBaselineFareRuleForOdTenantNormalizesCodesAndPassesFormattedTimestamp() {
        when(caracalApiClient.fetchSites("NYW_SANDBOX")).thenReturn(List.of(
                new SiteSummary("HOBNJT", "Hoboken NJT", NY_ZONE)));
        when(caracalApiClient.simulatePrice(any())).thenReturn(new PriceSimulationResult(
                new PriceSimulateResponse(new PricingDetail("PRS_WKND", new BigDecimal("6.5"), new BigDecimal("6.5"))),
                null));

        Optional<FareRule> result = fareService.getBaselineFareRule(
                odTenant, " hobnjt ", " pier11 ", " weekend_ticket ", "2026-10-24");

        assertTrue(result.isPresent());
        FareRule rule = result.get();
        assertEquals("NYW_SANDBOX", rule.getTenantCode());
        assertEquals("HOBNJT", rule.getOriginIdentifier());
        assertEquals("PIER11", rule.getDestIdentifier());
        assertEquals("WEEKEND_TICKET", rule.getProductCode());
        assertEquals("PRS_WKND", rule.getPriceRuleSetName());
        assertEquals(new BigDecimal("6.50"), rule.getPrice());
        assertEquals(FareStatus.ACTIVE, rule.getStatus());
        assertFalse(rule.isHasPendingDraft());

        ArgumentCaptor<PriceSimulateRequest> captor = ArgumentCaptor.forClass(PriceSimulateRequest.class);
        verify(caracalApiClient).simulatePrice(captor.capture());
        assertEquals("2026-10-24T10:00:00-04:00", captor.getValue().at());
        assertEquals(Map.of("DEST", "PIER11"), captor.getValue().dimensions());
        verifyNoInteractions(fareDraftRepository);
    }

    @Test
    void buildGridAndResolvePricingTimestampFailFastOnInvalidDateWithoutCallingApi() {
        assertThrows(IllegalArgumentException.class,
                () -> fareService.buildGrid(odTenant, "MIDTOWN", "not-a-date"));
        assertThrows(IllegalArgumentException.class,
                () -> fareService.resolvePricingTimestamp("NYW_SANDBOX", "MIDTOWN", "not-a-date"));

        verifyNoInteractions(caracalApiClient, fareDraftRepository);
    }
}