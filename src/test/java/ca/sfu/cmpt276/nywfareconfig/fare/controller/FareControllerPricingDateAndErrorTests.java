package ca.sfu.cmpt276.nywfareconfig.fare.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.CatalogOption;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareGrid;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareDraftService;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

@ExtendWith(MockitoExtension.class)
class FareControllerPricingDateAndErrorTests {

    private static final String VALID_DRAFT_JSON = """
            {
              "changes": [
                {
                  "tenantCode": "NYW_SANDBOX",
                  "gridMode": "OD",
                  "originIdentifier": "HOBNJT",
                  "destIdentifier": "PIER11",
                  "productCode": "WEEKEND_TICKET",
                  "newPrice": 7.50,
                  "targetStatus": "ACTIVE",
                  "actionType": "INLINE_EDIT",
                  "tierCode": null
                }
              ]
            }
            """;

    @Mock
    private FareService fareService;

    @Mock
    private FareDraftService fareDraftService;

    private TenantContext tenantContext;
    private Tenant odTenant;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        odTenant = new Tenant("NYW_SANDBOX", "NY Waterway", "USD", "en-US", GridMode.OD, true, List.of());
        tenantContext = new TenantContext(odTenant, null, null);
        FareController controller = new FareController(tenantContext, fareService, fareDraftService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getFaresNormalizesFilterStoresPricingDateAndBuildsGrid() throws Exception {
        FareGrid mockGrid = mock(FareGrid.class);
        when(fareService.getSecondaryFilterOptions("NYW_SANDBOX", GridMode.OD))
                .thenReturn(List.of(new CatalogOption("MIDTOWN", "Midtown")));
        when(fareService.buildGrid(odTenant, "MIDTOWN", "2026-10-24")).thenReturn(mockGrid);

        mockMvc.perform(get("/fares")
                        .param("secondaryFilter", "  midtown ")
                        .param("pricingDate", "2026-10-24"))
                .andExpect(status().isOk())
                .andExpect(view().name("fare/fare"))
                .andExpect(model().attribute("gridView", mockGrid))
                .andExpect(model().attributeDoesNotExist("caracalGridErrorMessage"));

        assertEquals("MIDTOWN", tenantContext.getSecondaryFilter());
        assertEquals("2026-10-24", tenantContext.getSelectedDate());
    }

    @Test
    void getFaresClearsSelectedDateWhenBlankPricingDateIsSubmitted() throws Exception {
        tenantContext.setSecondaryFilter("MIDTOWN");
        tenantContext.setSelectedDate("2026-10-24");

        when(fareService.getSecondaryFilterOptions("NYW_SANDBOX", GridMode.OD)).thenReturn(List.of());
        when(fareService.buildGrid(odTenant, "MIDTOWN", null)).thenReturn(mock(FareGrid.class));

        mockMvc.perform(get("/fares").param("pricingDate", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("fare/fare"));

        assertNull(tenantContext.getSelectedDate());
        verify(fareService).buildGrid(odTenant, "MIDTOWN", null);
    }

    @Test
    void getFaresRejectsMalformedPricingDateWithoutCrashingOrPoisoningSession() throws Exception {
        tenantContext.setSecondaryFilter("MIDTOWN");
        tenantContext.setSelectedDate("2026-10-24");

        when(fareService.getSecondaryFilterOptions("NYW_SANDBOX", GridMode.OD)).thenReturn(List.of());

        mockMvc.perform(get("/fares").param("pricingDate", "not-a-date"))
                .andExpect(status().isOk())
                .andExpect(view().name("fare/fare"))
                .andExpect(model().attributeExists("caracalGridErrorMessage"))
                .andExpect(model().attributeDoesNotExist("gridView"));

        // Previous valid session date must remain intact
        assertEquals("2026-10-24", tenantContext.getSelectedDate());
        verify(fareService, never()).buildGrid(any(), any(), any());
    }

    @Test
    void getFaresDisplaysErrorBannerWhenCaracalApiFailsDuringGridBuild() throws Exception {
        tenantContext.setSecondaryFilter("MIDTOWN");
        when(fareService.getSecondaryFilterOptions("NYW_SANDBOX", GridMode.OD)).thenReturn(List.of());
        when(fareService.buildGrid(odTenant, "MIDTOWN", null))
                .thenThrow(new CaracalApiException("Caracal API timed out."));

        mockMvc.perform(get("/fares"))
                .andExpect(status().isOk())
                .andExpect(view().name("fare/fare"))
                .andExpect(model().attribute("caracalGridErrorMessage", "Caracal API timed out."));
    }

    @Test
    void getFaresClearsPoisonedSessionDateIfBuildGridThrowsIllegalArgumentException() throws Exception {
        tenantContext.setSecondaryFilter("MIDTOWN");
        tenantContext.setSelectedDate("bad-session-date");

        when(fareService.getSecondaryFilterOptions("NYW_SANDBOX", GridMode.OD)).thenReturn(List.of());
        when(fareService.buildGrid(odTenant, "MIDTOWN", "bad-session-date"))
                .thenThrow(new IllegalArgumentException("Invalid pricing date format"));

        mockMvc.perform(get("/fares"))
                .andExpect(status().isOk())
                .andExpect(view().name("fare/fare"))
                .andExpect(model().attribute("caracalGridErrorMessage", "Invalid pricing date format"));

        assertNull(tenantContext.getSelectedDate());
    }

    @Test
    void stageFareDraftsForwardsSelectedPricingDateToFareDraftService() throws Exception {
        tenantContext.setSelectedDate("2026-10-24");
        when(fareDraftService.stageChanges(eq(odTenant), anyList(), eq("2026-10-24"))).thenReturn(1);
        when(fareDraftService.countPendingDrafts("NYW_SANDBOX")).thenReturn(1L);

        mockMvc.perform(post("/fares/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRAFT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(1))
                .andExpect(jsonPath("$.totalPendingCount").value(1));

        verify(fareDraftService).stageChanges(eq(odTenant), anyList(), eq("2026-10-24"));
    }

    @Test
    void stageFareDraftsUsesTwoArgOverloadWhenNoPricingDateIsSelected() throws Exception {
        tenantContext.setSelectedDate(null);
        when(fareDraftService.stageChanges(eq(odTenant), anyList())).thenReturn(1);
        when(fareDraftService.countPendingDrafts("NYW_SANDBOX")).thenReturn(2L);

        mockMvc.perform(post("/fares/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRAFT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(1))
                .andExpect(jsonPath("$.totalPendingCount").value(2));

        verify(fareDraftService).stageChanges(eq(odTenant), anyList());
    }

    @Test
    void stageFareDraftsReturnsServiceUnavailableWhenCaracalApiFails() throws Exception {
        when(fareDraftService.stageChanges(eq(odTenant), anyList()))
                .thenThrow(new CaracalApiException("Caracal unavailable during baseline validation."));

        mockMvc.perform(post("/fares/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_DRAFT_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Caracal unavailable during baseline validation."));
    }
}