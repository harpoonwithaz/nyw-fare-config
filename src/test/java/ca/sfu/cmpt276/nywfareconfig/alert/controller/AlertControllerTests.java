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
import ca.sfu.cmpt276.nywfareconfig.alert.dto.ProductOption;
import ca.sfu.cmpt276.nywfareconfig.alert.dto.SiteOption;
import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertStatus;
import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import ca.sfu.cmpt276.nywfareconfig.alert.model.TriggerType;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.ServiceAlertRepository;
import ca.sfu.cmpt276.nywfareconfig.alert.service.AlertService;
import ca.sfu.cmpt276.nywfareconfig.alert.service.CatalogLookup;
import ca.sfu.cmpt276.nywfareconfig.alert.service.CatalogUnavailableException;
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

    // Stands in for the live Caracal site-list and product-list
    @MockitoBean
    private CatalogLookup catalog;

    private MockHttpSession session;
    private Cookie cookie;

    @BeforeEach
    void selectTenant() throws Exception {
        when(tenantService.getAvailableTenants()).thenReturn(List.of(MOCK_TENANT));
        when(tenantService.getTenantByCode(TENANT)).thenReturn(MOCK_TENANT);
        when(catalog.sites(TENANT)).thenReturn(List.of(
            new SiteOption("PIER11", "Pier 11"),
            new SiteOption("HOB14ST", "Hoboken 14th St"),
            new SiteOption("EDGEWTR", "Edgewater")));
        when(catalog.products(TENANT)).thenReturn(List.of(new ProductOption("ADULT_ONEWAY", "Adult One-Way")));
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

    // Iteration 3: form options come from the Caracal catalog and the tenant's locales
    @Test
    void formListsCaracalRoutesAndTickets() throws Exception {
        mockMvc.perform(inTenant(get("/fares/alerts/new"), OPERATOR))
            .andExpect(status().isOk())
            .andExpect(model().attribute("catalogMissing", false))
            .andExpect(content().string(containsString("value=\"EDGEWTR:PIER11\"")))
            .andExpect(content().string(containsString("Edgewater to Pier 11")))
            .andExpect(content().string(containsString("All tickets on the route")))
            .andExpect(content().string(containsString("Adult One-Way (ADULT_ONEWAY)")));
    }

    @Test
    void savedTicketIsSelectedOnEdit() throws Exception {
        AlertForm form = new AlertForm();
        form.setRouteKeys(List.of(ROUTE));
        form.setMessageEn("Ticket kept");
        form.setProductCode("ADULT_ONEWAY");
        assertTrue(alertService.save(TENANT, null, form, "test").isEmpty());
        ServiceAlert saved = alertService.list(TENANT, "Ticket kept", null, null).get(0);
        assertEquals("ADULT_ONEWAY", saved.getProductCode());

        mockMvc.perform(inTenant(get("/fares/alerts/" + saved.getId() + "/edit"), OPERATOR))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("value=\"ADULT_ONEWAY\" selected")))
            .andExpect(content().string(not(containsString("not in catalog"))));
    }

    @Test
    void caracalDownShowsRouteWarning() throws Exception {
        when(catalog.sites(TENANT)).thenThrow(new CatalogUnavailableException("down"));
        when(catalog.products(TENANT)).thenThrow(new CatalogUnavailableException("down"));

        mockMvc.perform(inTenant(get("/fares/alerts/new"), OPERATOR))
            .andExpect(status().isOk())
            .andExpect(model().attribute("catalogMissing", true))
            .andExpect(content().string(containsString("Routes could not be loaded from Caracal")));
    }

    @Test
    void spanishFieldsShowForSpanishTenant() throws Exception {
        mockMvc.perform(inTenant(get("/fares/alerts/new"), OPERATOR))
            .andExpect(model().attribute("spanishEnabled", true))
            .andExpect(content().string(containsString("Spanish message")))
            .andExpect(content().string(containsString("Continue button (ES)")));
    }

    @Test
    void englishOnlyTenantHidesSpanishButKeepsSavedText() throws Exception {
        // Switching to a different tenant code reloads it, now with only English configured
        String englishTenant = "EN_ONLY";
        when(tenantService.getTenantByCode(englishTenant)).thenReturn(
            new Tenant(englishTenant, "English only", "USD", "en-US", GridMode.OD, true, List.of("en-US")));
        when(catalog.sites(englishTenant)).thenReturn(List.of(
            new SiteOption("PIER11", "Pier 11"), new SiteOption("HOB14ST", "Hoboken 14th St")));
        mockMvc.perform(inTenant(post("/context/tenant"), OPERATOR)
                .param("tenant", englishTenant)
                .param("returnUrl", "/fares/alerts"))
            .andExpect(status().is3xxRedirection());

        AlertForm form = new AlertForm();
        form.setRouteKeys(List.of(ROUTE));
        form.setMessageEn("English only alert");
        form.setMessageEs("Texto guardado");
        assertTrue(alertService.save(englishTenant, null, form, "test").isEmpty());
        ServiceAlert saved = alertService.list(englishTenant, "English only alert", null, null).get(0);

        mockMvc.perform(inTenant(get("/fares/alerts/" + saved.getId() + "/edit"), OPERATOR))
            .andExpect(model().attribute("spanishEnabled", false))
            .andExpect(content().string(not(containsString("Spanish message"))))
            .andExpect(content().string(containsString("value=\"Texto guardado\"")));
    }

    // Story 4.5
    @Test
    void blankLabelsFallBackToDefaults() throws Exception {
        mockMvc.perform(inTenant(post("/fares/alerts"), OPERATOR)
                .param("routeKeys", ROUTE)
                .param("triggerType", "WEEKENDS")
                .param("messageEn", "Default labels")
                .param("confirmLabelEn", "  "))
            .andExpect(status().is3xxRedirection());

        ServiceAlert saved = alertService.list(TENANT, "Default labels", null, null).get(0);
        assertEquals("Continue", saved.getConfirmLabelEn());
        assertEquals("Continuar", saved.getConfirmLabelEs());
        assertEquals("Cancel", saved.getDismissLabelEn());
        assertEquals("Cancelar", saved.getDismissLabelEs());
    }

    @Test
    void typedLabelsAreKept() throws Exception {
        mockMvc.perform(inTenant(post("/fares/alerts"), OPERATOR)
                .param("routeKeys", ROUTE)
                .param("triggerType", "WEEKENDS")
                .param("messageEn", "Custom labels")
                .param("confirmLabelEn", "Buy for Monday")
                .param("confirmLabelEs", "Comprar el lunes"))
            .andExpect(status().is3xxRedirection());

        ServiceAlert saved = alertService.list(TENANT, "Custom labels", null, null).get(0);
        assertEquals("Buy for Monday", saved.getConfirmLabelEn());
        assertEquals("Comprar el lunes", saved.getConfirmLabelEs());
        assertEquals("Cancel", saved.getDismissLabelEn());
    }

    @Test
    void suggestedLabelMatchesTrigger() {
        assertEquals("Buy for [Day]", TriggerType.WEEKENDS.getSuggestedConfirmEn());
        assertEquals("Buy for [Day]", TriggerType.OUTSIDE_SERVICE_HOURS.getSuggestedConfirmEn());
        assertEquals("Buy now", TriggerType.BEFORE_START_DATE.getSuggestedConfirmEn());
        assertEquals("Comprar ahora", TriggerType.BEFORE_START_DATE.getSuggestedConfirmEs());
        // Suggestions must fit on the TVM button too
        for (TriggerType t : TriggerType.values()) {
            assertTrue(t.getSuggestedConfirmEn().length() <= 20);
            assertTrue(t.getSuggestedConfirmEs().length() <= 20);
        }
    }

    @Test
    void formOffersSuggestionForEachTrigger() throws Exception {
        mockMvc.perform(inTenant(get("/fares/alerts/new"), OPERATOR))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-suggest-en=\"Buy now\"")))
            .andExpect(content().string(containsString("data-suggest-es=\"Comprar para [día]\"")))
            .andExpect(content().string(containsString("Blank labels use Continue")));
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
