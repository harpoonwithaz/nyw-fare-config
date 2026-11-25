package ca.sfu.cmpt276.nywfareconfig.availability.controller;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.repository.AvailabilityRuleRepository;
import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import ca.sfu.cmpt276.nywfareconfig.security.model.AppUser;
import ca.sfu.cmpt276.nywfareconfig.security.model.Role;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

    @MockitoBean
    private TenantContext tenantContext;

    @MockitoBean
    private TenantService tenantService;

    private AppUserDetails operatorUser;
    private AppUserDetails adminUser;

    @BeforeEach
    void setUp() {
        availabilityRuleRepository.deleteAll();

        AppUser opEntity = new AppUser("op@nyw.example", "operator", "hashed", Role.OPERATOR, true);
        AppUser adminEntity = new AppUser("admin@nyw.example", "admin", "hashed", Role.CLIENT_SERVICES, true);
        operatorUser = new AppUserDetails(opEntity);
        adminUser = new AppUserDetails(adminEntity);

        Tenant sandboxTenant = new Tenant(
                "NYW_SANDBOX",
                "NY Waterway Sandbox",
                "USD",
                "en-US",
                GridMode.OD,
                true,
                List.of("en-US"));

        when(tenantContext.getSelectedTenant()).thenReturn(sandboxTenant);
        when(tenantService.getAvailableTenants()).thenReturn(List.of(sandboxTenant));
        when(tenantService.getTenantByCode("NYW_SANDBOX")).thenReturn(sandboxTenant);

        when(caracalApiClient.fetchTenants()).thenReturn(List.of(
                new CaracalCatalogDtos.TenantSummary("NYW_SANDBOX", "NY Waterway Sandbox")));
        ZoneId nyZone = ZoneId.of("America/New_York");
        when(caracalApiClient.fetchSites("NYW_SANDBOX")).thenReturn(List.of(
                new CaracalCatalogDtos.SiteSummary("MIDTOWN", "Midtown / W. 39th St", nyZone),
                new CaracalCatalogDtos.SiteSummary("PIER11", "Pier 11 / Wall St", nyZone)));
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
    void getAvailabilityPageRendersStationsRoutesAndProducts() throws Exception {
        mockMvc.perform(get("/availability")
                        .with(user(operatorUser))
                        .param("site", "MIDTOWN")
                        .param("route", "MIDTOWN->PIER11"))
                .andExpect(status().isOk())
                .andExpect(view().name("availability/availability"))
                .andExpect(model().attribute("siteCode", "MIDTOWN"))
                .andExpect(model().attribute("routeCode", "MIDTOWN->PIER11"))
                .andExpect(model().attributeExists("terminals", "routes", "availabilityView"));
    }

    @Test
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
                        .with(user(operatorUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
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
                        .with(user(adminUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/availability/api/sync")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(caracalApiClient).createWhenCondition(any());
        verify(caracalApiClient).updateRateAssignment(any());

        List<AvailabilityRule> saved = availabilityRuleRepository.findByTenantCode("NYW_SANDBOX");
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).isPhasedOut()).isFalse();
        assertThat(saved.get(0).isAvailable()).isTrue();
        assertThat(saved.get(0).isSyncedToCaracal()).isTrue();
    }
}