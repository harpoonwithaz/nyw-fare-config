package ca.sfu.cmpt276.nywfareconfig.fare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.SiteSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalZonalDtos.DimensionValueSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalZonalDtos.SiteDimensionBinding;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.CatalogOption;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.StationZoneMappingView;
import ca.sfu.cmpt276.nywfareconfig.fare.repository.FareDraftRepository;

@ExtendWith(MockitoExtension.class)
class FareServiceStationZoneMappingTests {

    private static final ZoneId CHICAGO_ZONE = ZoneId.of("America/Chicago");

    @Mock
    private FareDraftRepository fareDraftRepository;

    @Mock
    private CaracalApiClient caracalApiClient;

    private FareService fareService;

    @BeforeEach
    void setUp() {
        fareService = new FareService(fareDraftRepository, caracalApiClient);
    }

    @Test
    void getStationZoneMappingsReturnsEmptyWithoutCallingDependenciesWhenTenantIsBlank() {
        assertTrue(fareService.getStationZoneMappings(null).isEmpty());
        assertTrue(fareService.getStationZoneMappings("   ").isEmpty());
        verifyNoInteractions(caracalApiClient, fareDraftRepository);
    }

    @Test
    void getStationZoneMappingsReturnsEmptyWhenNoSitesAreReturned() {
        when(caracalApiClient.fetchSites("METRA")).thenReturn(List.of());

        assertTrue(fareService.getStationZoneMappings(" metra ").isEmpty());
        verifyNoInteractions(fareDraftRepository);
    }

    @Test
    void getStationZoneMappingsJoinsSitesBindingsZonesFiltersNonOriginAndSortsAlphabetically() {
        when(caracalApiClient.fetchSites("METRA")).thenReturn(List.of(
                new SiteSummary("st051", "Union Station (BNSF)", CHICAGO_ZONE),
                new SiteSummary("ST521", "Union Station (HC)", CHICAGO_ZONE),
                new SiteSummary("ST150", "aurora (BNSF)", CHICAGO_ZONE),
                new SiteSummary("ST999", "  ", CHICAGO_ZONE),
                new SiteSummary("ST703", "Ravenswood (UP-N)", CHICAGO_ZONE),
                new SiteSummary("   ", "Blank Site Code", CHICAGO_ZONE)));

        when(caracalApiClient.fetchStationZoneBindings("METRA")).thenReturn(List.of(
                new SiteDimensionBinding("ST051", "ORIGIN_ZONE", "a"),
                // Non-ORIGIN_ZONE binding must be ignored
                new SiteDimensionBinding("ST051", "DEST_ZONE", "H"),
                new SiteDimensionBinding("st150", "origin_zone", "H"),
                // Blank dimensionKey is accepted as fallback
                new SiteDimensionBinding("ST521", "", "A"),
                // Zone code not present in fetchZones falls back to raw code
                new SiteDimensionBinding("ST703", "ORIGIN_ZONE", "Z")));

        when(caracalApiClient.fetchZones("METRA")).thenReturn(List.of(
                new DimensionValueSummary("A", "Zone A"),
                new DimensionValueSummary("H", "Zone H")));

        List<StationZoneMappingView> mappings = fareService.getStationZoneMappings(" metra ");

        assertEquals(5, mappings.size());

        // Sorted case-insensitively by stationName, then siteCode
        assertEquals(new StationZoneMappingView("ST150", "aurora (BNSF)", "H", "Zone H"), mappings.get(0));
        assertEquals(new StationZoneMappingView("ST703", "Ravenswood (UP-N)", "Z", "Z"), mappings.get(1));

        // Blank site name falls back to normalized siteCode; missing binding resolves to Unassigned
        StationZoneMappingView unassigned = mappings.get(2);
        assertEquals("ST999", unassigned.siteCode());
        assertEquals("ST999", unassigned.stationName());
        assertNull(unassigned.zoneCode());
        assertEquals("Unassigned", unassigned.zoneDisplayName());

        assertEquals(new StationZoneMappingView("ST051", "Union Station (BNSF)", "A", "Zone A"), mappings.get(3));
        assertEquals(new StationZoneMappingView("ST521", "Union Station (HC)", "A", "Zone A"), mappings.get(4));

        verifyNoInteractions(fareDraftRepository);
    }

    @Test
    void groupStationZoneMappingsPreservesZoneOrderIncludesEmptyZonesAndOmitsUnassignedWhenUnused() {
        when(caracalApiClient.fetchZones("METRA")).thenReturn(List.of(
                new DimensionValueSummary("A", "Zone A"),
                new DimensionValueSummary("B", "Zone B"),
                new DimensionValueSummary("C", "Zone C")));

        List<StationZoneMappingView> mappings = List.of(
                new StationZoneMappingView("ST702", "Clybourn (UP-N)", "A", "Zone A"),
                new StationZoneMappingView("ST051", "Union Station (BNSF)", "A", "Zone A"),
                new StationZoneMappingView("ST015", "Brainerd (RI)", "C", "Zone C"));

        Map<CatalogOption, List<StationZoneMappingView>> grouped =
                fareService.groupStationZoneMappings("metra", mappings);

        List<CatalogOption> groupKeys = new ArrayList<>(grouped.keySet());
        assertEquals(
                List.of(
                        new CatalogOption("A", "Zone A"),
                        new CatalogOption("B", "Zone B"),
                        new CatalogOption("C", "Zone C")),
                groupKeys);

        assertEquals(2, grouped.get(new CatalogOption("A", "Zone A")).size());
        assertEquals("ST702", grouped.get(new CatalogOption("A", "Zone A")).get(0).siteCode());
        assertEquals("ST051", grouped.get(new CatalogOption("A", "Zone A")).get(1).siteCode());
        assertTrue(grouped.get(new CatalogOption("B", "Zone B")).isEmpty());
        assertEquals(1, grouped.get(new CatalogOption("C", "Zone C")).size());
    }

    @Test
    void groupStationZoneMappingsAppendsUnassignedAndUnknownZonesAndHandlesEmptyInputs() {
        assertTrue(fareService.groupStationZoneMappings(null, List.of()).isEmpty());
        assertTrue(fareService.groupStationZoneMappings("METRA", null).isEmpty());
        assertTrue(fareService.groupStationZoneMappings("METRA", List.of()).isEmpty());
        verifyNoInteractions(caracalApiClient);

        when(caracalApiClient.fetchZones("METRA")).thenReturn(List.of(
                new DimensionValueSummary("A", "Zone A")));

        List<StationZoneMappingView> mappings = List.of(
                new StationZoneMappingView("ST051", "Union Station (BNSF)", "A", "Zone A"),
                new StationZoneMappingView("ST703", "Ravenswood (UP-N)", "Z", "Zone Z"),
                new StationZoneMappingView("ST999", "New Infill Station", null, "Unassigned"));

        Map<CatalogOption, List<StationZoneMappingView>> grouped =
                fareService.groupStationZoneMappings("METRA", mappings);

        List<CatalogOption> groupKeys = new ArrayList<>(grouped.keySet());
        assertEquals(
                List.of(
                        new CatalogOption("A", "Zone A"),
                        new CatalogOption("Z", "Zone Z"),
                        new CatalogOption("UNASSIGNED", "Unassigned")),
                groupKeys);
        assertEquals(1, grouped.get(new CatalogOption("UNASSIGNED", "Unassigned")).size());
        assertEquals("ST999", grouped.get(new CatalogOption("UNASSIGNED", "Unassigned")).get(0).siteCode());
    }
}