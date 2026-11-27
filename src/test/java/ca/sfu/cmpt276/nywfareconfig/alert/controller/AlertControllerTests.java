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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ca.sfu.cmpt276.nywfareconfig.alert.dto.AlertForm;
import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertStatus;
import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import ca.sfu.cmpt276.nywfareconfig.alert.model.TriggerType;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.ServiceAlertRepository;
import ca.sfu.cmpt276.nywfareconfig.alert.service.AlertLimits;
import ca.sfu.cmpt276.nywfareconfig.alert.service.AlertService;
import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.ProductSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.SiteSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalUnavailableException;
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
@ActiveProfiles("test")
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

    // Stands in for the live Caracal site-list and product-list; FareService reads from it
    @MockitoBean
    private CaracalApiClient caracal;

    private MockHttpSession session;
    private Cookie cookie;

    @BeforeEach
    void selectTenant() throws Exception {
        when(tenantService.getAvailableTenants()).thenReturn(List.of(MOCK_TENANT));
        when(tenantService.getTenantByCode(TENANT)).thenReturn(MOCK_TENANT);
        when(caracal.fetchSites(TENANT)).thenReturn(List.of(
            new SiteSummary("PIER11", "Pier 11", null),
            new SiteSummary("HOB14ST", "Hoboken 14th St", null),
            new SiteSummary("EDGEWTR", "Edgewater", null)));
        when(caracal.fetchProducts(TENANT)).thenReturn(List.of(
            new ProductSummary("ADULT_ONEWAY", "Adult One-Way", true),
            new ProductSummary("OLD_PASS", "Old Pass", false)));
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
        // tenantCode is the hidden field the alert form posts back
        builder.session(session).with(user(who)).with(csrf()).param("tenantCode", TENANT);
        if (cookie != null) {
            builder.cookie(cookie);
        }
        return builder;
    }

    private ServiceAlert saveDraft(String message) {
        AlertForm form = new AlertForm();
        form.setRouteKeys(List.of(ROUTE));
        form.text("en").setMessage(message);
        assertTrue(alertService.save(TENANT, null, form, "test", List.of("en", "es")).isEmpty());
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
                .param("texts[en].message", "No weekend service")
                .param("texts[es].message", "Sin servicio el fin de semana"))
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
            .andExpect(content().string(containsString("Message in English is required.")));

        assertTrue(alertRepository.findAll().isEmpty());
    }

    @Test
    void messageOver200CharactersIsRejected() throws Exception {
        mockMvc.perform(inTenant(post("/fares/alerts"), OPERATOR)
                .param("routeKeys", ROUTE)
                .param("triggerType", "WEEKENDS")
                .param("texts[en].message", "x".repeat(201)))
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
            .andExpect(content().string(containsString("Adult One-Way (ADULT_ONEWAY)")))
            .andExpect(content().string(not(containsString("Old Pass"))))
            .andExpect(content().string(containsString("Edgewater (EDGEWTR)")));
    }

    // Eldon, Nov 23: select every route, or every route from one origin
    @Test
    void formOffersSelectAllForEveryOriginAndOverall() throws Exception {
        mockMvc.perform(inTenant(get("/fares/alerts/new"), OPERATOR))
            .andExpect(content().string(containsString("id=\"selectAllRoutes\"")))
            .andExpect(content().string(containsString("data-select-group=\"PIER11\"")))
            .andExpect(content().string(containsString("data-group=\"PIER11\"")));
    }

    @Test
    void retiredTicketStaysSelectableAndSavesOnEdit() throws Exception {
        ServiceAlert saved = saveDraft("Retired ticket");
        saved.setProductCode("OLD_PASS");
        alertRepository.save(saved);

        mockMvc.perform(inTenant(get("/fares/alerts/" + saved.getId() + "/edit"), OPERATOR))
            .andExpect(content().string(containsString("OLD_PASS (not in catalog)")));

        mockMvc.perform(inTenant(post("/fares/alerts/" + saved.getId()), OPERATOR)
                .param("routeKeys", ROUTE)
                .param("triggerType", "WEEKENDS")
                .param("productCode", "OLD_PASS")
                .param("texts[en].message", "Retired ticket, new wording"))
            .andExpect(status().is3xxRedirection());

        assertEquals("OLD_PASS", alertService.list(TENANT, "new wording", null, null).get(0).getProductCode());
    }

    @Test
    void ticketListFailureStillShowsWarning() throws Exception {
        when(caracal.fetchProducts(TENANT)).thenThrow(new CaracalUnavailableException("down", null));

        mockMvc.perform(inTenant(get("/fares/alerts/new"), OPERATOR))
            .andExpect(model().attribute("catalogMissing", true));
    }

    @Test
    void savedTicketIsSelectedOnEdit() throws Exception {
        AlertForm form = new AlertForm();
        form.setRouteKeys(List.of(ROUTE));
        form.text("en").setMessage("Ticket kept");
        form.setProductCode("ADULT_ONEWAY");
        assertTrue(alertService.save(TENANT, null, form, "test", List.of("en", "es")).isEmpty());
        ServiceAlert saved = alertService.list(TENANT, "Ticket kept", null, null).get(0);
        assertEquals("ADULT_ONEWAY", saved.getProductCode());

        mockMvc.perform(inTenant(get("/fares/alerts/" + saved.getId() + "/edit"), OPERATOR))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("value=\"ADULT_ONEWAY\" selected")))
            .andExpect(content().string(not(containsString("not in catalog"))));
    }

    @Test
    void caracalDownShowsRouteWarning() throws Exception {
        when(caracal.fetchSites(TENANT)).thenThrow(new CaracalUnavailableException("down", null));
        when(caracal.fetchProducts(TENANT)).thenThrow(new CaracalUnavailableException("down", null));

        mockMvc.perform(inTenant(get("/fares/alerts/new"), OPERATOR))
            .andExpect(status().isOk())
            .andExpect(model().attribute("catalogMissing", true))
            .andExpect(content().string(containsString("Routes could not be loaded from Caracal")));
    }

    @Test
    void spanishFieldsShowForSpanishTenant() throws Exception {
        mockMvc.perform(inTenant(get("/fares/alerts/new"), OPERATOR))
            .andExpect(model().attribute("languages", List.of("en", "es")))
            .andExpect(content().string(containsString("name=\"texts[es].message\"")))
            .andExpect(content().string(containsString("placeholder=\"Continuar\"")));
    }

    @Test
    void englishOnlyTenantHidesSpanishButKeepsSavedText() throws Exception {
        // Switching to a different tenant code reloads it, now with only English configured
        String englishTenant = "EN_ONLY";
        when(tenantService.getTenantByCode(englishTenant)).thenReturn(
            new Tenant(englishTenant, "English only", "USD", "en-US", GridMode.OD, true, List.of("en-US")));
        when(caracal.fetchSites(englishTenant)).thenReturn(List.of(
            new SiteSummary("PIER11", "Pier 11", null), new SiteSummary("HOB14ST", "Hoboken 14th St", null)));
        when(caracal.fetchProducts(englishTenant)).thenReturn(List.of());
        mockMvc.perform(inTenant(post("/context/tenant"), OPERATOR)
                .param("tenant", englishTenant)
                .param("returnUrl", "/fares/alerts"))
            .andExpect(status().is3xxRedirection());

        AlertForm form = new AlertForm();
        form.setRouteKeys(List.of(ROUTE));
        form.text("en").setMessage("English only alert");
        form.text("es").setMessage("Texto guardado");
        assertTrue(alertService.save(englishTenant, null, form, "test", List.of("en")).isEmpty());
        ServiceAlert saved = alertService.list(englishTenant, "English only alert", null, null).get(0);

        mockMvc.perform(inTenant(get("/fares/alerts/" + saved.getId() + "/edit"), OPERATOR))
            .andExpect(model().attribute("languages", List.of("en")))
            .andExpect(content().string(not(containsString("id=\"message-es\""))))
            .andExpect(content().string(containsString("value=\"Texto guardado\"")));
    }

    // Story 4.5
    @Test
    void blankLabelsFallBackToDefaults() throws Exception {
        mockMvc.perform(inTenant(post("/fares/alerts"), OPERATOR)
                .param("routeKeys", ROUTE)
                .param("triggerType", "WEEKENDS")
                .param("texts[en].message", "Default labels")
                .param("texts[en].confirmLabel", "  "))
            .andExpect(status().is3xxRedirection());

        ServiceAlert saved = alertService.list(TENANT, "Default labels", null, null).get(0);
        assertEquals("Continue", saved.getTexts().get("en").getConfirmLabel());
        assertEquals("Continuar", saved.getTexts().get("es").getConfirmLabel());
        assertEquals("Cancel", saved.getTexts().get("en").getDismissLabel());
        assertEquals("Cancelar", saved.getTexts().get("es").getDismissLabel());
    }

    @Test
    void typedLabelsAreKept() throws Exception {
        mockMvc.perform(inTenant(post("/fares/alerts"), OPERATOR)
                .param("routeKeys", ROUTE)
                .param("triggerType", "WEEKENDS")
                .param("texts[en].message", "Custom labels")
                .param("texts[en].confirmLabel", "Buy for Monday")
                .param("texts[es].confirmLabel", "Comprar el lunes"))
            .andExpect(status().is3xxRedirection());

        ServiceAlert saved = alertService.list(TENANT, "Custom labels", null, null).get(0);
        assertEquals("Buy for Monday", saved.getTexts().get("en").getConfirmLabel());
        assertEquals("Comprar el lunes", saved.getTexts().get("es").getConfirmLabel());
        assertEquals("Cancel", saved.getTexts().get("en").getDismissLabel());
    }

    @Test
    void suggestedLabelMatchesTrigger() {
        assertEquals("Buy for [Day]", TriggerType.WEEKENDS.getSuggestedConfirm("en"));
        assertEquals("Buy for [Day]", TriggerType.OUTSIDE_SERVICE_HOURS.getSuggestedConfirm("en"));
        assertEquals("Buy now", TriggerType.BEFORE_START_DATE.getSuggestedConfirm("en"));
        assertEquals("Comprar ahora", TriggerType.BEFORE_START_DATE.getSuggestedConfirm("es"));
        // Every alert language has a suggestion, and it fits on the TVM button
        for (TriggerType t : TriggerType.values()) {
            for (String suggestion : t.getSuggestedConfirm().values()) {
                assertTrue(suggestion.length() <= AlertLimits.LABEL_MAX_LENGTH, suggestion);
            }
        }
    }

    @Test
    void formOffersSuggestionForEachTrigger() throws Exception {
        mockMvc.perform(inTenant(get("/fares/alerts/new"), OPERATOR))
            .andExpect(status().isOk())
            // Suggestions go to the page script as a trigger -> language -> text map
            .andExpect(content().string(containsString("\"BEFORE_START_DATE\":{")))
            .andExpect(content().string(containsString("Buy now")))
            .andExpect(content().string(containsString("placeholder=\"Cancelar\"")));
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

    // Oliver, Nov 27: a select-all alert listed every route and filled the screen
    @Test
    void longRouteListFoldsAfterThreeAndFilterOnlyListsUsedRoutes() throws Exception {
        AlertForm form = new AlertForm();
        form.setRouteKeys(List.of("PIER11:HOB14ST", "PIER11:EDGEWTR", "HOB14ST:PIER11", "HOB14ST:EDGEWTR", "EDGEWTR:PIER11"));
        form.text("en").setMessage("Many routes");
        assertTrue(alertService.save(TENANT, null, form, "test", List.of("en", "es")).isEmpty());

        mockMvc.perform(inTenant(get("/fares/alerts"), OPERATOR))
            .andExpect(content().string(containsString("+2 more")))
            // Filter lists each station once, not every route pair
            .andExpect(content().string(containsString("<option value=\"EDGEWTR\">")))
            .andExpect(content().string(not(containsString("value=\"EDGEWTR:PIER11\""))));

        // Picking a station shows alerts to or from it
        mockMvc.perform(inTenant(get("/fares/alerts"), OPERATOR).param("route", "EDGEWTR"))
            .andExpect(content().string(containsString("Many routes")));
        mockMvc.perform(inTenant(get("/fares/alerts"), OPERATOR).param("route", "MIDTOWN"))
            .andExpect(content().string(not(containsString("Many routes"))));
    }

    // A form opened for one client and submitted after switching to another in a second tab
    @Test
    void saveIsRefusedWhenClientChangedSinceFormOpened() throws Exception {
        mockMvc.perform(post("/fares/alerts").session(session).cookie(cookie).with(user(OPERATOR)).with(csrf())
                .param("tenantCode", "METRA")
                .param("routeKeys", ROUTE)
                .param("triggerType", "WEEKENDS")
                .param("texts[en].message", "Wrong client"))
            .andExpect(status().is3xxRedirection())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                .attribute("error", containsString("client changed")));

        assertTrue(alertRepository.findAll().isEmpty());
    }

    @Test
    void formCarriesItsClient() throws Exception {
        mockMvc.perform(inTenant(get("/fares/alerts/new"), OPERATOR))
            .andExpect(content().string(containsString("name=\"tenantCode\" value=\"NYW_SANDBOX\"")));
    }

    @Test
    void secondActivateOfSameDraftIsRefused() {
        ServiceAlert draft = saveDraft("Activate twice");

        assertTrue(alertService.publish(TENANT, List.of(draft.getId()), "test").isEmpty());
        assertEquals(List.of("Alert " + draft.getId() + " is not a draft."),
            alertService.publish(TENANT, List.of(draft.getId()), "test"));
    }

    @Test
    void archivingLiveAlertDiscardsItsPendingEdit() throws Exception {
        ServiceAlert live = saveDraft("Live then archived");
        assertTrue(alertService.publish(TENANT, List.of(live.getId()), "test").isEmpty());
        AlertForm edit = new AlertForm();
        edit.setRouteKeys(List.of(ROUTE));
        edit.text("en").setMessage("Pending edit");
        assertTrue(alertService.save(TENANT, live.getId(), edit, "test", List.of("en", "es")).isEmpty());
        ServiceAlert pending = alertService.list(TENANT, "Pending edit", null, null).get(0);

        mockMvc.perform(inTenant(post("/fares/alerts/" + live.getId() + "/archive"), ADMIN))
            .andExpect(status().is3xxRedirection());

        assertEquals(AlertStatus.ARCHIVED, alertRepository.findById(live.getId()).orElseThrow().getStatus());
        assertTrue(alertRepository.findById(pending.getId()).isEmpty());
    }

    @Test
    void draftOfAnAlertThatIsNoLongerLiveCannotBeActivated() {
        ServiceAlert live = saveDraft("Parent gone");
        assertTrue(alertService.publish(TENANT, List.of(live.getId()), "test").isEmpty());
        AlertForm edit = new AlertForm();
        edit.setRouteKeys(List.of(ROUTE));
        edit.text("en").setMessage("Orphan edit");
        assertTrue(alertService.save(TENANT, live.getId(), edit, "test", List.of("en", "es")).isEmpty());
        ServiceAlert orphan = alertService.list(TENANT, "Orphan edit", null, null).get(0);
        // Parent archived directly, as old data might have it
        ServiceAlert parent = alertRepository.findById(live.getId()).orElseThrow();
        parent.setStatus(AlertStatus.ARCHIVED);
        alertRepository.save(parent);

        List<String> errors = alertService.publish(TENANT, List.of(orphan.getId()), "test");

        assertTrue(errors.get(0).contains("no longer active"), errors.toString());
        assertEquals(AlertStatus.DRAFT, alertRepository.findById(orphan.getId()).orElseThrow().getStatus());
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
                .param("texts[en].message", "New wording"))
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
