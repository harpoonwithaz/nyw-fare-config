package ca.sfu.cmpt276.nywfareconfig.availability.controller;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.repository.AvailabilityRuleRepository;
import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import ca.sfu.cmpt276.nywfareconfig.security.model.AppUser;
import ca.sfu.cmpt276.nywfareconfig.security.model.Role;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.TenantService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:availability-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.session.jdbc.initialize-schema=embedded"
})
class AvailabilityControllerMockMvcTests {

    private static final String TENANT = "NYW_SANDBOX";
    private static final AppUserDetails OPERATOR = new AppUserDetails(
        new AppUser("operator@nyw.com", "operator", "password", Role.OPERATOR, true)
    );
    private static final AppUserDetails CLIENT_SERVICES_USER = new AppUserDetails(
        new AppUser("admin@nyw.com", "admin", "password", Role.CLIENT_SERVICES, true)
    );
    private static final Tenant MOCK_TENANT = new Tenant(
        TENANT, "New York Waterway Sandbox", "USD", "en-US", GridMode.OD, true, List.of("en-US", "es"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AvailabilityRuleRepository availabilityRuleRepository;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private CaracalApiClient caracalApiClient;

    private MockHttpSession session;
    private Cookie cookie;

    @BeforeEach
    void setUp() throws Exception {
        when(tenantService.getAvailableTenants()).thenReturn(List.of(MOCK_TENANT));
        when(tenantService.getTenantByCode(TENANT)).thenReturn(MOCK_TENANT);

        when(caracalApiClient.fetchSites(TENANT)).thenReturn(List.of(
            new CaracalCatalogDtos.SiteSummary("s-1", "MIDTOWN", "Midtown", "TERMINAL", "America/New_York"),
            new CaracalCatalogDtos.SiteSummary("s-2", "EDGEWTR", "Edgewater", "TERMINAL", "America/New_York"),
            new CaracalCatalogDtos.SiteSummary("s-3", "BELFORD", "Belford", "TERMINAL", "America/New_York")
        ));
        when(caracalApiClient.fetchProducts(TENANT)).thenReturn(List.of(
            new CaracalCatalogDtos.ProductSummary("ADULT_ONEWAY", "One Way - Adult", true),
            new CaracalCatalogDtos.ProductSummary("SENIOR_ONEWAY", "One Way - Senior (62+)", true),
            new CaracalCatalogDtos.ProductSummary("FULL_MONTHLY", "Monthly Pass", true)
        ));
        when(caracalApiClient.fetchRateTables(TENANT)).thenReturn(List.of(
            new CaracalCatalogDtos.RateTableSummary("rt-1", "RT-MIDTOWN", "ACTIVE", "MIDTOWN", "2026-10-01T00:00:00Z")
        ));
        when(caracalApiClient.fetchRateTable(TENANT, "RT-MIDTOWN")).thenReturn(
            new CaracalCatalogDtos.RateTableGetResponse("rt-1", "RT-MIDTOWN", "ACTIVE", "s-1", List.of(
                new CaracalCatalogDtos.RateAssignmentSummary(
                    "fee195cd-1111-2222-3333-444455556666",
                    "ADULT_ONEWAY", "VALUE", 0, 0, null, "OD-ADULT_ONEWAY"),
                new CaracalCatalogDtos.RateAssignmentSummary(
                    "063638d6-1111-2222-3333-444455556666",
                    "FULL_MONTHLY", "VALUE", 0, 1, null, "OD-FULL_MONTHLY")
            ))
        );
        when(caracalApiClient.simulateAvailability(eq(TENANT), eq("MIDTOWN"), anyMap())).thenReturn(List.of(
            new CaracalCatalogDtos.SimulatedProductAvailability("ADULT_ONEWAY", "SELLABLE", null, null, null),
            new CaracalCatalogDtos.SimulatedProductAvailability("FULL_MONTHLY", "SELLABLE", "MONTHLY_SALE_WINDOW", null, null)
        ));

        availabilityRuleRepository.deleteAll();
        session = new MockHttpSession();
        cookie = mockMvc.perform(post("/context/tenant")
                .session(session)
                .with(user(OPERATOR))
                .with(csrf())
                .param("tenant", TENANT)
                .param("returnUrl", "/availability"))
            .andExpect(status().is3xxRedirection())
            .andReturn()
            .getResponse()
            .getCookie("SESSION");
    }

    private MockHttpServletRequestBuilder inTenant(MockHttpServletRequestBuilder builder, AppUserDetails principal) {
        builder.session(session).with(user(principal)).with(csrf());
        if (cookie != null) {
            builder.cookie(cookie);
        }
        return builder;
    }

    @Test
    void noTenantShowsSelectTenantPrompt() throws Exception {
        mockMvc.perform(get("/availability").with(user(OPERATOR)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("No Tenant Selected")));
    }

    @Test
    void rendersAllCaracalStationsRoutesAndLiveRateTableMetadata() throws Exception {
        mockMvc.perform(inTenant(get("/availability?site=MIDTOWN&route=MIDTOWN->EDGEWTR"), OPERATOR))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Belford (BELFORD)")))
            .andExpect(content().string(containsString("Edgewater (EDGEWTR)")))
            .andExpect(content().string(containsString("MIDTOWN-&gt;EDGEWTR")))
            .andExpect(content().string(containsString("Caracal: MONTHLY_SALE_WINDOW")));
    }

    @Test
    void selectedTenantRendersProductsAndPersistsRouteAndTemporalDatesAndHours() throws Exception {
        String payload = """
            [
              {
                "siteCode": "MIDTOWN",
                "routeCode": "MIDTOWN->EDGEWTR",
                "productCode": "ADULT_ONEWAY",
                "available": false,
                "conditionType": "RECURRING_WEEKLY",
                "discountLabel": "Weekday Commuter",
                "daysOfWeek": [1, 2, 3, 4, 5],
                "startDate": "2026-11-01",
                "endDate": "2026-12-31",
                "startTime": "06:00:00",
                "endTime": "22:00:00"
              }
            ]
            """;

        mockMvc.perform(inTenant(post("/availability/api/stage"), OPERATOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));

        assertThat(availabilityRuleRepository.findByTenantCodeAndSiteCodeAndRouteCode(
                TENANT, "MIDTOWN", "MIDTOWN->EDGEWTR"))
            .hasSize(1)
            .first()
            .satisfies(rule -> {
                assertThat(rule.getProductCode()).isEqualTo("ADULT_ONEWAY");
                assertThat(rule.getRateAssignmentId()).isEqualTo("fee195cd-1111-2222-3333-444455556666");
                assertThat(rule.isAvailable()).isFalse();
                assertThat(rule.getUpdatedBy()).contains("operator@nyw.com");
                assertThat(rule.getTemporalCondition()).isNotNull();
                assertThat(rule.getTemporalCondition().getDaysOfWeek()).containsExactly(1, 2, 3, 4, 5);
                assertThat(rule.getTemporalCondition().getStartDate()).hasToString("2026-11-01");
                assertThat(rule.getTemporalCondition().getEndDate()).hasToString("2026-12-31");
            });

        mockMvc.perform(inTenant(get("/availability?site=MIDTOWN&route=MIDTOWN->EDGEWTR"), OPERATOR))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("ADULT_ONEWAY")))
            .andExpect(content().string(containsString("Unavailable")))
            .andExpect(content().string(containsString("Weekday Commuter: 5 Days (06:00-22:00) [2026-11-01 to 2026-12-31]")));
    }

    @Test
    void phasedOutProductBlocksStandardOperatorFromReactivating() throws Exception {
        AvailabilityRule phased = new AvailabilityRule(TENANT, "MIDTOWN", "ALL", "SENIOR_ONEWAY", false);
        phased.setPhasedOut(true);
        availabilityRuleRepository.save(phased);

        String reEnablePayload = """
            [
              {
                "siteCode": "MIDTOWN",
                "routeCode": "ALL",
                "productCode": "SENIOR_ONEWAY",
                "available": true,
                "phasedOut": false
              }
            ]
            """;

        mockMvc.perform(inTenant(post("/availability/api/stage"), OPERATOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reEnablePayload))
            .andExpect(status().isForbidden());

        mockMvc.perform(inTenant(post("/availability/api/stage"), CLIENT_SERVICES_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reEnablePayload))
            .andExpect(status().isOk());
    }

    @Test
    void syncToCaracalDispatchesRateAssignmentUpdateAndWhenConditionCreate() throws Exception {
        when(caracalApiClient.createWhenCondition(any(), anyString())).thenReturn("wc-uuid-999");
        when(caracalApiClient.updateRateAssignment(any(), anyString())).thenReturn("fee195cd-1111-2222-3333-444455556666");

        String stagePayload = """
            [
              {
                "siteCode": "MIDTOWN",
                "routeCode": "ALL",
                "productCode": "ADULT_ONEWAY",
                "available": false,
                "conditionType": "TEMPORARY_PROMO",
                "discountLabel": "Holiday Special",
                "startDate": "2026-12-24",
                "endDate": "2026-12-26",
                "startTime": "08:00:00",
                "endTime": "20:00:00"
              }
            ]
            """;

        mockMvc.perform(inTenant(post("/availability/api/stage"), OPERATOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(stagePayload))
            .andExpect(status().isOk());

        mockMvc.perform(inTenant(post("/availability/api/sync"), OPERATOR))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(caracalApiClient).createWhenCondition(any(), eq("operator@nyw.com"));
        verify(caracalApiClient).updateRateAssignment(argThat(req ->
                "fee195cd-1111-2222-3333-444455556666".equals(req.assignmentId())
                        && "NOT_SOLD".equals(req.cellType())), eq("operator@nyw.com"));
    }

    @Test
    void syncFailureReturnsBadGatewayAndRetainsLocalState() throws Exception {
        AvailabilityRule unsynced = new AvailabilityRule(TENANT, "MIDTOWN", "ALL", "ADULT_ONEWAY", false);
        unsynced.setRateAssignmentId("fee195cd-1111-2222-3333-444455556666");
        unsynced.setSyncedToCaracal(false);
        availabilityRuleRepository.save(unsynced);

        when(caracalApiClient.updateRateAssignment(any(), any()))
            .thenThrow(new CaracalApiException("Caracal 500"));

        mockMvc.perform(inTenant(post("/availability/api/sync"), OPERATOR))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.message").value(containsString("Failed to synchronize availability with platform")));
    }

    @Test
    void stageWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/availability/api/stage")
                .with(user(OPERATOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isForbidden());
    }
}