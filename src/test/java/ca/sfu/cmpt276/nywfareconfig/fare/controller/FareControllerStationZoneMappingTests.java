package ca.sfu.cmpt276.nywfareconfig.fare.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.CatalogOption;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareGrid;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.StationZoneMappingView;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareDraftService;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

@ExtendWith(MockitoExtension.class)
class FareControllerStationZoneMappingTests {

    @Mock
    private FareService fareService;

    @Mock
    private FareDraftService fareDraftService;

    private TenantContext tenantContext;
    private Tenant zonalTenant;
    private Tenant odTenant;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        zonalTenant = new Tenant("METRA", "Metra Rail", "USD", "en-US", GridMode.ZONAL, true, List.of());
        odTenant = new Tenant("NYW_SANDBOX", "NY Waterway", "USD", "en-US", GridMode.OD, true, List.of());
        tenantContext = new TenantContext(zonalTenant, null, null);
        FareController controller = new FareController(tenantContext, fareService, fareDraftService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getFaresHydratesStationZoneMappingsAndGroupsForZonalTenantWithActiveProduct() throws Exception {
        FareGrid mockGrid = mock(FareGrid.class);
        List<StationZoneMappingView> mappings = List.of(
                new StationZoneMappingView("ST051", "Union Station (BNSF)", "A", "Zone A"),
                new StationZoneMappingView("ST150", "Aurora (BNSF)", "H", "Zone H"));
        Map<CatalogOption, List<StationZoneMappingView>> groups = new LinkedHashMap<>();
        groups.put(new CatalogOption("A", "Zone A"), List.of(mappings.get(0)));
        groups.put(new CatalogOption("H", "Zone H"), List.of(mappings.get(1)));

        when(fareService.getSecondaryFilterOptions("METRA", GridMode.ZONAL))
                .thenReturn(List.of(new CatalogOption("ONEWAY", "One-Way")));
        when(fareService.buildGrid(zonalTenant, "ONEWAY", null)).thenReturn(mockGrid);
        when(fareService.getStationZoneMappings("METRA")).thenReturn(mappings);
        when(fareService.groupStationZoneMappings("METRA", mappings)).thenReturn(groups);

        mockMvc.perform(get("/fares").param("secondaryFilter", "oneway"))
                .andExpect(status().isOk())
                .andExpect(view().name("fare/fare"))
                .andExpect(model().attribute("gridView", mockGrid))
                .andExpect(model().attribute("stationZoneMappings", mappings))
                .andExpect(model().attribute("stationZoneGroups", groups))
                .andExpect(model().attributeDoesNotExist("stationZoneMappingErrorMessage"));
    }

    @Test
    void getFaresSkipsStationZoneMappingHydrationWhenZonalProductIsNotSelected() throws Exception {
        when(fareService.getSecondaryFilterOptions("METRA", GridMode.ZONAL))
                .thenReturn(List.of(new CatalogOption("ONEWAY", "One-Way")));

        mockMvc.perform(get("/fares"))
                .andExpect(status().isOk())
                .andExpect(view().name("fare/fare"))
                .andExpect(model().attributeDoesNotExist("stationZoneMappings", "stationZoneGroups"));

        verify(fareService, never()).getStationZoneMappings(anyString());
        verify(fareService, never()).groupStationZoneMappings(anyString(), any());
    }

    @Test
    void getFaresSkipsStationZoneMappingHydrationForOdTenant() throws Exception {
        tenantContext.setSelectedTenant(odTenant);
        tenantContext.setSecondaryFilter("MIDTOWN");

        FareGrid mockGrid = mock(FareGrid.class);
        when(fareService.getSecondaryFilterOptions("NYW_SANDBOX", GridMode.OD))
                .thenReturn(List.of(new CatalogOption("MIDTOWN", "Midtown")));
        when(fareService.buildGrid(odTenant, "MIDTOWN", null)).thenReturn(mockGrid);

        mockMvc.perform(get("/fares"))
                .andExpect(status().isOk())
                .andExpect(view().name("fare/fare"))
                .andExpect(model().attribute("gridView", mockGrid))
                .andExpect(model().attributeDoesNotExist("stationZoneMappings", "stationZoneGroups"));

        verify(fareService, never()).getStationZoneMappings(anyString());
        verify(fareService, never()).groupStationZoneMappings(anyString(), any());
    }

    @Test
    void getFaresKeepsGridIntactAndSetsDrawerErrorWhenStationMappingFetchFails() throws Exception {
        tenantContext.setSecondaryFilter("ONEWAY");
        FareGrid mockGrid = mock(FareGrid.class);

        when(fareService.getSecondaryFilterOptions("METRA", GridMode.ZONAL))
                .thenReturn(List.of(new CatalogOption("ONEWAY", "One-Way")));
        when(fareService.buildGrid(zonalTenant, "ONEWAY", null)).thenReturn(mockGrid);
        when(fareService.getStationZoneMappings("METRA"))
                .thenThrow(new CaracalApiException("Unable to load stations from Caracal."));

        mockMvc.perform(get("/fares"))
                .andExpect(status().isOk())
                .andExpect(view().name("fare/fare"))
                .andExpect(model().attribute("gridView", mockGrid))
                .andExpect(model().attribute("stationZoneMappingErrorMessage", "Unable to load stations from Caracal."))
                .andExpect(model().attributeDoesNotExist("stationZoneMappings", "stationZoneGroups"));
    }
}