package ca.sfu.cmpt276.nywfareconfig.fare.controller;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import ca.sfu.cmpt276.nywfareconfig.security.model.AppUser;
import ca.sfu.cmpt276.nywfareconfig.security.model.Role;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.TenantService;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:fare-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.session.jdbc.initialize-schema=embedded"
})
@AutoConfigureMockMvc
class FareControllerMockMvcTests {

    private static final String NYW_TENANT = "NYW_SANDBOX";
    private static final String METRA_TENANT = "METRA";
    private static final AppUserDetails TEST_USER = new AppUserDetails(
        new AppUser("test@example.com", "test-user", "password", Role.OPERATOR, true));
    private static final Tenant MOCK_NYW_TENANT = new Tenant(
        NYW_TENANT, "New York Waterway Sandbox", "USD", "en-US", GridMode.OD, true, List.of("en-US", "es"));
    private static final Tenant MOCK_METRA_TENANT = new Tenant(
        METRA_TENANT, "Metra", "USD", "en-US", GridMode.ZONAL, true, List.of("en-US"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private CaracalApiClient caracalApiClient;

    @BeforeEach
    void setUpTenantMocks() {
        when(tenantService.getAvailableTenants()).thenReturn(List.of(MOCK_NYW_TENANT, MOCK_METRA_TENANT));
        when(tenantService.getTenantByCode(NYW_TENANT)).thenReturn(MOCK_NYW_TENANT);
        when(tenantService.getTenantByCode(METRA_TENANT)).thenReturn(MOCK_METRA_TENANT);
        when(caracalApiClient.fetchSites(NYW_TENANT)).thenReturn(List.of(
                new CaracalCatalogDtos.SiteSummary("EDGEWTR", "Edgewater"),
                new CaracalCatalogDtos.SiteSummary("PORTIMP", "Port Imperial"),
                new CaracalCatalogDtos.SiteSummary("MIDTOWN", "Midtown")));
        when(caracalApiClient.fetchProducts(METRA_TENANT)).thenReturn(List.of(
                new CaracalCatalogDtos.ProductSummary("ADULT_ONEWAY", "Adult One-Way", true)));
    }

    /**
     * Helper that switches the active tenant via POST /context/tenant and
     * preserves both MockHttpSession and Spring Session JDBC cookies.
     */
    private Cookie selectTenant(MockHttpSession session, String tenantCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/context/tenant")
                .session(session)
                .with(user(TEST_USER))
                .with(csrf())
                .param("tenant", tenantCode)
                .param("returnUrl", "/fares"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/fares"))
            .andReturn();

        return result.getResponse().getCookie("SESSION");
    }

    private MockHttpServletRequestBuilder withSessionState(
            MockHttpServletRequestBuilder builder, MockHttpSession session, Cookie sessionCookie) {
        builder.session(session);
        if (sessionCookie != null) {
            builder.cookie(sessionCookie);
        }
        return builder;
    }

    @Test
    // Set tenant to NYW_SANDBOX via POST /context/tenant, then execute GET /fares?origin=EDGEWTR
    // asserting HTTP 200 and gridMode=OD
    void getNywFaresRendersOdMatrix() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Cookie sessionCookie = selectTenant(session, "NYW_SANDBOX");

        mockMvc.perform(withSessionState(get("/fares"), session, sessionCookie)
                .with(user(TEST_USER))
                .param("origin", "EDGEWTR"))
            .andExpect(status().isOk())
            .andExpect(view().name("fare/fare"))
            .andExpect(model().attribute("gridView",
                hasProperty("tenant", hasProperty("gridMode", is(GridMode.OD)))));
    }

    @Test
    // Set tenant to METRA via POST /context/tenant, then execute GET /fares?product=ADULT_ONEWAY
    // asserting HTTP 200 and gridMode=ZONAL
    void getMetraFaresRendersZonalMatrix() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Cookie sessionCookie = selectTenant(session, "METRA");

        mockMvc.perform(withSessionState(get("/fares"), session, sessionCookie)
                .with(user(TEST_USER))
                .param("product", "ADULT_ONEWAY"))
            .andExpect(status().isOk())
            .andExpect(view().name("fare/fare"))
            .andExpect(model().attribute("gridView",
                hasProperty("tenant", hasProperty("gridMode", is(GridMode.ZONAL)))));
    }

    @Test
    // Verify that switching tenants clears the previous tenant's secondary filter selection
    void switchingTenantClearsSecondarySelection() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Cookie sessionCookie = selectTenant(session, "NYW_SANDBOX");

        // Select an origin terminal in OD mode
        MvcResult odResult = mockMvc.perform(withSessionState(get("/fares"), session, sessionCookie)
                .with(user(TEST_USER))
                .param("origin", "EDGEWTR"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("gridView"))
            .andReturn();

        Cookie updatedCookie = odResult.getResponse().getCookie("SESSION");
        if (updatedCookie != null) {
            sessionCookie = updatedCookie;
        }

        // Switch tenant to METRA via POST /context/tenant
        MvcResult switchResult = mockMvc.perform(
                withSessionState(post("/context/tenant"), session, sessionCookie)
                    .with(user(TEST_USER))
                    .with(csrf())
                    .param("tenant", "METRA")
                    .param("returnUrl", "/fares"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/fares"))
            .andReturn();

        Cookie metraCookie = switchResult.getResponse().getCookie("SESSION");
        if (metraCookie != null) {
            sessionCookie = metraCookie;
        }

        // GET /fares without a product should not render a grid yet because secondary state was reset
        mockMvc.perform(withSessionState(get("/fares"), session, sessionCookie)
                .with(user(TEST_USER)))
            .andExpect(status().isOk())
            .andExpect(view().name("fare/fare"))
            .andExpect(model().attribute("gridView", nullValue()));
    }
}