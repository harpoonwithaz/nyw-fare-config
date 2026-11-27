package ca.sfu.cmpt276.nywfareconfig.availability.controller;

import org.springframework.test.context.ActiveProfiles;

import ca.sfu.cmpt276.nywfareconfig.availability.repository.AvailabilityRuleRepository;
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
@ActiveProfiles("test")
class AvailabilityControllerMockMvcTests {

    private static final String TENANT = "NYW_SANDBOX";
    private static final AppUserDetails OPERATOR = new AppUserDetails(
        new AppUser("operator@nyw.com", "operator", "password", Role.OPERATOR, true)
    );
    private static final Tenant MOCK_TENANT = new Tenant(
        TENANT, "New York Waterway Sandbox", "USD", "en-US", GridMode.OD, true, List.of("en-US", "es"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AvailabilityRuleRepository availabilityRuleRepository;

    @MockitoBean
    private TenantService tenantService;

    private MockHttpSession session;
    private Cookie cookie;

    @BeforeEach
    void setUp() throws Exception {
        when(tenantService.getAvailableTenants()).thenReturn(List.of(MOCK_TENANT));
        when(tenantService.getTenantByCode(TENANT)).thenReturn(MOCK_TENANT);
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

        // Verify refreshing GET /availability renders saved state without LazyInitializationException
        mockMvc.perform(inTenant(get("/availability?site=MIDTOWN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("ADULT_ONEWAY")))
            .andExpect(content().string(containsString("Unavailable")))
            .andExpect(content().string(containsString("5 Days (06:00-22:00)")));
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