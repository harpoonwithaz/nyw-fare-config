package ca.sfu.cmpt276.nywfareconfig.fare.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareGrid;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareService;
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

    private static final AppUserDetails OPERATOR_USER = new AppUserDetails(
            new AppUser("operator@example.com", "test-operator", "password", Role.OPERATOR, true));
    private static final AppUserDetails ADMIN_USER = new AppUserDetails(
            new AppUser("admin@example.com", "test-admin", "password", Role.CLIENT_SERVICES, true));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FareService fareService;

    @MockitoBean
    private TenantService tenantService;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testTenant = new Tenant();
        testTenant.setCode("TEST_TENANT");
        testTenant.setDisplayName("Test Tenant");
        testTenant.setGridMode(GridMode.OD);

        when(tenantService.getAvailableTenants()).thenReturn(List.of(testTenant));
        when(tenantService.getTenantByCode(anyString())).thenReturn(testTenant);
        when(fareService.getSecondaryFilterOptions(any(), any())).thenReturn(List.of());
        when(fareService.buildGrid(any(), anyString(), nullable(String.class))).thenReturn(mock(FareGrid.class));
    }

    private Cookie selectTenant(MockHttpSession session, String tenantCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/context/tenant")
                .session(session)
                .with(user(OPERATOR_USER))
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
    void getFaresUnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/fares"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void getFaresWithoutSelectedTenantReturnsOk() throws Exception {
        mockMvc.perform(get("/fares")
                .with(user(OPERATOR_USER)))
                .andExpect(status().isOk());
    }

    @Test
    void getFaresAsClientServicesReturnsOk() throws Exception {
        mockMvc.perform(get("/fares")
                .with(user(ADMIN_USER)))
                .andExpect(status().isOk());
    }

    @Test
    void getFaresWithSelectedTenantReturnsOk() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Cookie sessionCookie = selectTenant(session, testTenant.getCode());

        mockMvc.perform(withSessionState(get("/fares"), session, sessionCookie)
                .with(user(OPERATOR_USER)))
                .andExpect(status().isOk());
    }

    @Test
    void getFaresWithSelectedTenantAndSecondaryFilterReturnsOk() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Cookie sessionCookie = selectTenant(session, testTenant.getCode());

        mockMvc.perform(withSessionState(get("/fares"), session, sessionCookie)
                .with(user(OPERATOR_USER))
                .param("secondaryFilter", "ANY_FILTER"))
                .andExpect(status().isOk());
    }

    @Test
    void getFaresWhenCaracalApiFailsStillReturnsOk() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Cookie sessionCookie = selectTenant(session, testTenant.getCode());

        when(fareService.getSecondaryFilterOptions(any(), any()))
                .thenThrow(new CaracalApiException("Caracal API unavailable"));

        mockMvc.perform(withSessionState(get("/fares"), session, sessionCookie)
                .with(user(OPERATOR_USER)))
                .andExpect(status().isOk());
    }

    @Test
    void switchTenantWithoutCsrfReturnsForbidden() throws Exception {
        mockMvc.perform(post("/context/tenant")
                .with(user(OPERATOR_USER))
                .param("tenant", testTenant.getCode())
                .param("returnUrl", "/fares"))
                .andExpect(status().isForbidden());
    }
}