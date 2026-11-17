package ca.sfu.cmpt276.nywfareconfig.availability.controller;

import ca.sfu.cmpt276.nywfareconfig.availability.repository.AvailabilityRuleRepository;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.security.AppUser;
import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import ca.sfu.cmpt276.nywfareconfig.security.Role;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AvailabilityRuleRepository availabilityRuleRepository;

    private AppUserDetails operatorUser;

    @BeforeEach
    void setUp() {
        availabilityRuleRepository.deleteAll();
        operatorUser = new AppUserDetails(
            new AppUser("operator@nyw.com", "operator", "password", Role.OPERATOR, true)
        );
    }

    @Test
    void noTenantShowsSelectTenantPrompt() throws Exception {
        mockMvc.perform(get("/availability").with(user(operatorUser)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("No Tenant Selected")));
    }

    @Test
    void selectedTenantRendersCanonicalProductsAndPersistsChanges() throws Exception {
        MockHttpSession session = new MockHttpSession();
        TenantContext context = new TenantContext();
        context.setSelectedTenant(new Tenant("NYW_SANDBOX", "NY Waterway", GridMode.OD, true));
        session.setAttribute("scopedTarget.tenantContext", context);

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

        mockMvc.perform(post("/availability/api/stage")
                .session(session)
                .with(user(operatorUser))
                .with(csrf())
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
        mockMvc.perform(get("/availability?site=MIDTOWN")
                .session(session)
                .with(user(operatorUser)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("ADULT_ONEWAY")))
            .andExpect(content().string(containsString("Unavailable")))
            .andExpect(content().string(containsString("5 Days (06:00-22:00)")));
    }

    @Test
    void stageWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/availability/api/stage")
                .with(user(operatorUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isForbidden());
    }
}