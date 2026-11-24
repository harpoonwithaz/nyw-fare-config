package ca.sfu.cmpt276.nywfareconfig.availability.controller;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.repository.AvailabilityRuleRepository;
import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvailabilityControllerMockMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AvailabilityRuleRepository availabilityRuleRepository;

    @MockitoBean
    private CaracalApiClient caracalApiClient;

    private MockHttpSession sessionWithTenant;

    @BeforeEach
    void setUp() {
        availabilityRuleRepository.deleteAll();

        Tenant sandboxTenant = new Tenant(
                "NYW_SANDBOX",
                "NY Waterway Sandbox",
                "USD",
                "en-US",
                GridMode.OD,
                true,
                List.of("en-US"));
        TenantContext context = new TenantContext();
        context.setSelectedTenant(sandboxTenant);

        sessionWithTenant = new MockHttpSession();
        sessionWithTenant.setAttribute("scopedTarget.tenantContext", context);

        when(caracalApiClient.fetchTenants()).thenReturn(List.of(
                new CaracalCatalogDtos.TenantSummary("NYW_SANDBOX", "NY Waterway Sandbox")));
        when(caracalApiClient.fetchSites("NYW_SANDBOX")).thenReturn(List.of(
                new CaracalCatalogDtos.SiteSummary("MIDTOWN", "Midtown / W. 39th St"),
                new CaracalCatalogDtos.SiteSummary("PIER11", "Pier 11 / Wall St")));
        when(caracalApiClient.fetchProducts("NYW_SANDBOX")).thenReturn(List.of(
                new CaracalCatalogDtos.ProductSummary("ADULT_ONEWAY", "Adult One-Way", true),
                new CaracalCatalogDtos.ProductSummary("FULL_MONTHLY", "Full Monthly Pass", true)));
        when(caracalApiClient.fetchRateTables("NYW_SANDBOX")).thenReturn(List.of(
                new CaracalCatalogDtos.RateTableSummary("rt-1", "RT-MIDTOWN", "ACTIVE", "MIDTOWN", null)));
        when(caracalApiClient.fetchRateTable(any())).thenReturn(Optional.of(List.of(
                new CaracalCatalogDtos.RateAssignmentSummary("assign-1", "ADULT_ONEWAY", "VALUE", "SCHED_1", "OD-ADULT"))));
        when(caracalApiClient.simulateAvailability(eq("NYW_SANDBOX"), anyString(), anyMap())).thenReturn(List.of());
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void getAvailabilityPageRendersStationsRoutesAndProducts() throws Exception {
        mockMvc.perform(get("/availability")
                        .param("site", "MIDTOWN")
                        .param("route", "MIDTOWN->PIER11")
                        .session(sessionWithTenant))
                .andExpect(status().isOk())
                .andExpect(view().name("availability/availability"))
                .andExpect(model().attribute("siteCode", "MIDTOWN"))
                .andExpect(model().attribute("routeCode", "MIDTOWN->PIER11"))
                .andExpect(model().attributeExists("terminals", "routes", "availabilityView"));
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void operatorCannotReactivatePermanentlyPhasedOutProduct() throws Exception {
        AvailabilityRule phasedOutRule = new AvailabilityRule("NYW_SANDBOX", "MIDTOWN", "ALL", "FULL_MONTHLY", false);
        phasedOutRule.setPhasedOut(true);
        availabilityRuleRepository.save(phasedOutRule);

        String payload = """
                [
                  {
                    "siteCode": "MIDTOWN",
                    "routeCode": "ALL",
                    "productCode": "FULL_MONTHLY",
                    "available": true,
                    "phasedOut": false
                  }
                ]
                """;

        mockMvc.perform(post("/availability/api/stage")
                        .with(csrf())
                        .session(sessionWithTenant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "CLIENT_SERVICES")
    void clientServicesAdminCanReactivatePhasedOutProductAndSyncToCaracal() throws Exception {
        AvailabilityRule phasedOutRule = new AvailabilityRule("NYW_SANDBOX", "MIDTOWN", "ALL", "ADULT_ONEWAY", false);
        phasedOutRule.setPhasedOut(true);
        availabilityRuleRepository.save(phasedOutRule);

        String payload = """
                [
                  {
                    "siteCode": "MIDTOWN",
                    "routeCode": "ALL",
                    "productCode": "ADULT_ONEWAY",
                    "available": true,
                    "phasedOut": false,
                    "conditionType": "TEMPORARY_PROMO",
                    "discountLabel": "WINTER_PROMO",
                    "startDate": "2026-12-01",
                    "endDate": "2026-12-31",
                    "startTime": "08:00",
                    "endTime": "20:00"
                  }
                ]
                """;

        mockMvc.perform(post("/availability/api/stage")
                        .with(csrf())
                        .session(sessionWithTenant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/availability/api/sync")
                        .with(csrf())
                        .session(sessionWithTenant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(caracalApiClient).createWhenCondition(any(), anyString());
        verify(caracalApiClient).updateRateAssignment(any(), anyString());

        List<AvailabilityRule> saved = availabilityRuleRepository.findByTenantCode("NYW_SANDBOX");
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).isPhasedOut()).isFalse();
        assertThat(saved.get(0).isAvailable()).isTrue();
        assertThat(saved.get(0).isSyncedToCaracal()).isTrue();
    }
}