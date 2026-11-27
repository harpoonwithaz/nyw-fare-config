package ca.sfu.cmpt276.nywfareconfig.availability.controller;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.repository.AvailabilityRuleRepository;
import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalUnavailableException;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import ca.sfu.cmpt276.nywfareconfig.security.model.AppUser;
import ca.sfu.cmpt276.nywfareconfig.security.model.Role;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.TenantService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
    private static final AppUserDetails CLIENT_SERVICES_ADMIN = new AppUserDetails(
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
                new CaracalCatalogDtos.SiteSummary("MIDTOWN", "Midtown / W. 39th St", ZoneId.of("America/New_York")),
                new CaracalCatalogDtos.SiteSummary("PAULSHK", "Paulus Hook", ZoneId.of("America/New_York"))
        ));
        when(caracalApiClient.fetchProducts(TENANT)).thenReturn(List.of(
                new CaracalCatalogDtos.ProductSummary("ADULT_ONEWAY", "Adult One-Way", true),
                new CaracalCatalogDtos.ProductSummary("FULL_MONTHLY", "Full Monthly Pass", true)
        ));
        when(caracalApiClient.fetchRateTable(new CaracalCatalogDtos.RateTableGetRequest(TENANT, "RT-MIDTOWN")))
                .thenReturn(Optional.of(List.of(
                        new CaracalCatalogDtos.RateAssignmentSummary("ADULT_ONEWAY", "VALUE", "OD-ADULT_ONEWAY"),
                        new CaracalCatalogDtos.RateAssignmentSummary("FULL_MONTHLY", "NOT_SOLD", "OD-FULL_MONTHLY")
                )));
        when(caracalApiClient.simulatePrice(any())).thenReturn(
                new CaracalSimulationDtos.PriceSimulationResult(
                        new CaracalSimulationDtos.PriceSimulateResponse(
                                new CaracalSimulationDtos.PricingDetail("OD-ADULT_ONEWAY", new BigDecimal("13.25"), new BigDecimal("13.25"))),
                        null));

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

    private MockHttpServletRequestBuilder inTenant(MockHttpServletRequestBuilder builder) {
        builder.session(session).with(user(OPERATOR)).with(csrf());
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
    void selectedTenantRendersCanonicalProductsAndPersistsChanges() throws Exception {
        String payload = """
            [
              {
                "siteCode": "MIDTOWN",
                "productCode": "ADULT_ONEWAY",
                "available": false,
                "conditionType": "RECURRING_WEEKLY",
                "daysOfWeek": [1, 2, 3, 4, 5],
                "startTime": "06:00:00",
                "endTime": "22:00:00"
              }
            ]
            """;

        mockMvc.perform(inTenant(post("/availability/api/stage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));

        assertThat(availabilityRuleRepository.findByTenantCodeAndSiteCode("NYW_SANDBOX", "MIDTOWN"))
            .hasSize(1)
            .first()
            .satisfies(rule -> {
                assertThat(rule.getProductCode()).isEqualTo("ADULT_ONEWAY");
                assertThat(rule.isAvailable()).isFalse();
                assertThat(rule.getUpdatedBy()).contains("operator@nyw.com");
                assertThat(rule.getTemporalCondition()).isNotNull();
                assertThat(rule.getTemporalCondition().getDaysOfWeek()).containsExactly(1, 2, 3, 4, 5);
            });

        mockMvc.perform(inTenant(get("/availability?site=MIDTOWN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("ADULT_ONEWAY")))
            .andExpect(content().string(containsString("Unavailable")))
            .andExpect(content().string(containsString("Mon, Tue, Wed, Thu, Fri (06:00-22:00)")))
            .andExpect(content().string(containsString("Saved in DB")));
    }

    @Test
    void caracalOutageShowsErrorBannerInsteadOfFallback() throws Exception {
        when(caracalApiClient.fetchSites(TENANT))
                .thenThrow(new CaracalUnavailableException("Unable to reach the Caracal platform.", new RuntimeException()));

        mockMvc.perform(inTenant(get("/availability")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Unable to reach the Caracal platform.")));
    }

    @Test
    void operatorCannotModifyPhasedOutStateWhileClientServicesCan() throws Exception {
        AvailabilityRule phasedOut = new AvailabilityRule(TENANT, "MIDTOWN", "ALL", "FULL_MONTHLY", false);
        phasedOut.setPhasedOut(true);
        availabilityRuleRepository.save(phasedOut);

        String reactivatePayload = """
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

        mockMvc.perform(inTenant(post("/availability/api/stage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reactivatePayload))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value("ERROR"));

        MockHttpSession adminSession = new MockHttpSession();
        Cookie adminCookie = mockMvc.perform(post("/context/tenant")
                .session(adminSession)
                .with(user(CLIENT_SERVICES_ADMIN))
                .with(csrf())
                .param("tenant", TENANT)
                .param("returnUrl", "/availability"))
            .andReturn()
            .getResponse()
            .getCookie("SESSION");

        MockHttpServletRequestBuilder adminPost = post("/availability/api/stage")
                .session(adminSession)
                .with(user(CLIENT_SERVICES_ADMIN))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reactivatePayload);
        if (adminCookie != null) {
            adminPost.cookie(adminCookie);
        }

        mockMvc.perform(adminPost)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
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