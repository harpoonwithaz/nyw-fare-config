package ca.sfu.cmpt276.nywfareconfig.fare.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareDraftService;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

@ExtendWith(MockitoExtension.class)
class FareControllerDraftTests {

    @Mock
    private TenantContext tenantContext;

    @Mock
    private FareService fareService;

    @Mock
    private FareDraftService fareDraftService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FareController controller = new FareController(
                tenantContext, fareService, fareDraftService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void stageFareDraftsReturnsOkWithCountsForValidPayload() throws Exception {
        Tenant activeTenant = new Tenant("NYW_SANDBOX", "NY Waterway", "USD", "en-US", GridMode.OD, true);
        when(tenantContext.getSelectedTenant()).thenReturn(activeTenant);
        when(fareDraftService.stageChanges(eq(activeTenant), anyList())).thenReturn(1);
        when(fareDraftService.countPendingDrafts("NYW_SANDBOX")).thenReturn(3L);

        String validJson = """
                {
                  "changes": [
                    {
                      "tenantCode": "NYW_SANDBOX",
                      "gridMode": "OD",
                      "originIdentifier": "EDGEWTR",
                      "destIdentifier": "Midtown / W. 39th St",
                      "productCode": "Adult One-Way",
                      "newPrice": 14.25,
                      "targetStatus": "ACTIVE",
                      "actionType": "INLINE_EDIT",
                      "tierCode": null
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/fares/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(1))
                .andExpect(jsonPath("$.totalPendingCount").value(3));
    }

    @Test
    void stageFareDraftsReturnsBadRequestWhenPriceIsNegative() throws Exception {
        String negativePriceJson = """
                {
                  "changes": [
                    {
                      "tenantCode": "NYW_SANDBOX",
                      "gridMode": "OD",
                      "originIdentifier": "EDGEWTR",
                      "destIdentifier": "Midtown / W. 39th St",
                      "productCode": "Adult One-Way",
                      "newPrice": -5.00,
                      "targetStatus": "ACTIVE",
                      "actionType": "INLINE_EDIT"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/fares/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negativePriceJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(fareDraftService);
    }

    @Test
    void stageFareDraftsReturnsBadRequestWhenNoTenantSelected() throws Exception {
        when(tenantContext.getSelectedTenant()).thenReturn(null);

        String validJson = """
                {
                  "changes": [
                    {
                      "tenantCode": "NYW_SANDBOX",
                      "gridMode": "OD",
                      "originIdentifier": "EDGEWTR",
                      "destIdentifier": "Midtown / W. 39th St",
                      "productCode": "Adult One-Way",
                      "newPrice": 14.00,
                      "targetStatus": "ACTIVE",
                      "actionType": "INLINE_EDIT"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/fares/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No active tenant selected in session."));
    }
}