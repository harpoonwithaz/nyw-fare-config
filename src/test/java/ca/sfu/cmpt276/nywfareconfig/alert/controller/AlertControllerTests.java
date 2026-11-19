package ca.sfu.cmpt276.nywfareconfig.alert.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ca.sfu.cmpt276.nywfareconfig.alert.dto.AlertForm;
import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertStatus;
import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.ServiceAlertRepository;
import ca.sfu.cmpt276.nywfareconfig.alert.service.AlertService;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import ca.sfu.cmpt276.nywfareconfig.security.model.AppUser;
import ca.sfu.cmpt276.nywfareconfig.security.model.Role;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.TenantService;

// Full context like FareControllerMockMvcTests, since TenantContext is session scoped
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:alert-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.session.jdbc.initialize-schema=embedded"
})
@AutoConfigureMockMvc
class AlertControllerTests {

    private static final AppUserDetails OPERATOR = new AppUserDetails(
        new AppUser("operator@example.com", "operator", "password", Role.OPERATOR, true));
    private static final AppUserDetails ADMIN = new AppUserDetails(
        new AppUser("admin@example.com", "admin", "password", Role.CLIENT_SERVICES, true));

    private static final String TENANT = "NYW_SANDBOX";
    private static final String ROUTE = "PIER11:HOB14ST";
    private static final String NO_TENANT = "No Tenant Selected";
    private static final Tenant MOCK_TENANT = new Tenant(
        TENANT, "New York Waterway Sandbox", "USD", "en-US", GridMode.OD, true, List.of("en-US", "es"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertService alertService;

    @Autowired
    private ServiceAlertRepository alertRepository;

    @MockitoBean
    private TenantService tenantService;

    private MockHttpSession session;
    private Cookie cookie;

    @BeforeEach
    void selectTenant() throws Exception {
        when(tenantService.getAvailableTenants()).thenReturn(List.of(MOCK_TENANT));
        when(tenantService.getTenantByCode(TENANT)).thenReturn(MOCK_TENANT);
        alertRepository.deleteAll();
        session = new MockHttpSession();
        cookie = mockMvc.perform(post("/context/tenant").session(session).with(user(OPERATOR)).with(csrf())
                .param("tenant", TENANT)
                .param("returnUrl", "/fares/alerts"))
            .andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getCookie("SESSION");
    }

    // Request in the session that has NYW_SANDBOX selected
    private MockHttpServletRequestBuilder inTenant(MockHttpServletRequestBuilder builder, AppUserDetails who) {
        builder.session(session).with(user(who)).with(csrf());
        if (cookie != null) {
            builder.cookie(cookie);
        }
        return builder;
    }

    private ServiceAlert saveDraft(String message) {
        AlertForm form = new AlertForm();
        form.setRouteKeys(List.of(ROUTE));
        form.setMessageEn(message);
        assertTrue(alertService.save(TENANT, null, form, "test").isEmpty());
        return alertService.list(TENANT, message, null, null).get(0);
    }

    // Story 4.1
    @Test
    void loggedOutUserIsSentToLogin() throws Exception {
        mockMvc.perform(get("/fares/alerts"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void noTenantShowsSelectTenantPrompt() throws Exception {
        mockMvc.perform(get("/fares/alerts").with(user(OPERATOR)))
            .andExpect(status().isOk())
            .andExpect(view().name("alerts/alerts"))
            .andExpect(model().attributeDoesNotExist("alerts"))
            .andExpect(content().string(containsString(NO_TENANT)));
    }

    @Test
    void selectedTenantListsItsAlerts() throws Exception {
        saveDraft("Ferry does not run on weekends");

        mockMvc.perform(inTenant(get("/fares/alerts"), OPERATOR).param("status", "DRAFT"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("status", "DRAFT"))
            .andExpect(content().string(not(containsString(NO_TENANT))))
            .andExpect(content().string(containsString("Ferry does not run on weekends")))
            .andExpect(content().string(containsString("PIER11 → HOB14ST")));
    }

    // Story 4.2
    @Test
    void newFormRenders() throws Exception {
        mockMvc.perform(inTenant(get("/fares/alerts/new"), OPERATOR))
            .andExpect(status().isOk())
            .andExpect(view().name("alerts/form"))
            .andExpect(content().string(containsString("Save draft")));
    }

    @Test
    void validAlertIsSavedAsDraft() throws Exception {
        mockMvc.perform(inTenant(post("/fares/alerts"), OPERATOR)
                .param("routeKeys", ROUTE)
                .param("triggerType", "WEEKENDS")
                .param("messageEn", "No weekend service")
                .param("messageEs", "Sin servicio el fin de semana"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/fares/alerts"));

        List<ServiceAlert> saved = alertService.list(TENANT, "No weekend service", null, null);
        assertEquals(1, saved.size());
        assertEquals(AlertStatus.DRAFT, saved.get(0).getStatus());
    }

    @Test
    void missingEnglishMessageShowsError() throws Exception {
        mockMvc.perform(inTenant(post("/fares/alerts"), OPERATOR)
                .param("routeKeys", ROUTE)
                .param("triggerType", "WEEKENDS"))
            .andExpect(status().isOk())
            .andExpect(view().name("alerts/form"))
            .andExpect(content().string(containsString("English message is required.")));

        assertTrue(alertRepository.findAll().isEmpty());
    }

    @Test
    void messageOver200CharactersIsRejected() throws Exception {
        mockMvc.perform(inTenant(post("/fares/alerts"), OPERATOR)
                .param("routeKeys", ROUTE)
                .param("triggerType", "WEEKENDS")
                .param("messageEn", "x".repeat(201)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Message must be 200 characters or fewer.")));
    }

    // Story 4.3
    @Test
    void operatorCannotDelete() throws Exception {
        ServiceAlert draft = saveDraft("Operator cannot delete me");

        mockMvc.perform(inTenant(post("/fares/alerts/" + draft.getId() + "/delete"), OPERATOR))
            .andExpect(status().isForbidden());

        assertTrue(alertRepository.findById(draft.getId()).isPresent());
    }

    @Test
    void clientServicesCanDeleteDraft() throws Exception {
        ServiceAlert draft = saveDraft("Delete me");

        mockMvc.perform(inTenant(post("/fares/alerts/" + draft.getId() + "/delete"), ADMIN))
            .andExpect(status().is3xxRedirection());

        assertTrue(alertRepository.findById(draft.getId()).isEmpty());
    }

    @Test
    void clientServicesCanArchiveActiveAlert() throws Exception {
        ServiceAlert draft = saveDraft("Archive me");
        assertTrue(alertService.publish(TENANT, List.of(draft.getId()), "test").isEmpty());

        mockMvc.perform(inTenant(post("/fares/alerts/" + draft.getId() + "/archive"), ADMIN))
            .andExpect(status().is3xxRedirection());

        assertEquals(AlertStatus.ARCHIVED, alertRepository.findById(draft.getId()).orElseThrow().getStatus());
    }

    // Draft -> Active from the alerts page
    @Test
    void makeActiveTurnsDraftActive() throws Exception {
        ServiceAlert draft = saveDraft("Activate me");

        mockMvc.perform(inTenant(post("/fares/alerts/" + draft.getId() + "/activate"), OPERATOR))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/fares/alerts"));

        assertEquals(AlertStatus.ACTIVE, alertRepository.findById(draft.getId()).orElseThrow().getStatus());
    }

    @Test
    void activatingReplacementDraftArchivesOldAlert() throws Exception {
        ServiceAlert live = saveDraft("Old wording");
        assertTrue(alertService.publish(TENANT, List.of(live.getId()), "test").isEmpty());

        // Editing a live alert creates a draft copy that points back to it
        mockMvc.perform(inTenant(post("/fares/alerts/" + live.getId()), OPERATOR)
                .param("routeKeys", ROUTE)
                .param("triggerType", "WEEKENDS")
                .param("messageEn", "New wording"))
            .andExpect(status().is3xxRedirection());
        ServiceAlert replacement = alertService.list(TENANT, "New wording", null, null).get(0);
        assertEquals(live.getId(), replacement.getReplacesAlertId());

        mockMvc.perform(inTenant(post("/fares/alerts/" + replacement.getId() + "/activate"), OPERATOR))
            .andExpect(status().is3xxRedirection());

        assertEquals(AlertStatus.ACTIVE, alertRepository.findById(replacement.getId()).orElseThrow().getStatus());
        assertEquals(AlertStatus.ARCHIVED, alertRepository.findById(live.getId()).orElseThrow().getStatus());
    }

    @Test
    void activatingActiveAlertShowsError() throws Exception {
        ServiceAlert live = saveDraft("Already live");
        assertTrue(alertService.publish(TENANT, List.of(live.getId()), "test").isEmpty());

        mockMvc.perform(inTenant(post("/fares/alerts/" + live.getId() + "/activate"), OPERATOR))
            .andExpect(status().is3xxRedirection())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                .attributeExists("error"));
    }
}
