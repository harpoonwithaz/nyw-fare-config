package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareDraftService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.TenantService;
import ca.sfu.cmpt276.nywfareconfig.security.controller.GlobalUserModelAdvice;
import ca.sfu.cmpt276.nywfareconfig.security.config.SecurityConfig;

@WebMvcTest(
    controllers = PublishingApiController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = GlobalUserModelAdvice.class
    )
)
@Import(SecurityConfig.class)
@WithMockUser(username = "testoperator", roles = {"OPERATOR"})
class PublishingApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FareDraftService fareDraftService;

    @MockitoBean
    private TenantContext tenantContext;

    @MockitoBean
    private TenantService tenantService;

    // Creates a test draft without connecting to PostgreSQL.
    private FareDraft createDraft(
            String tenantCode,
            String origin,
            String destination,
            String product,
            String originalPrice,
            String draftPrice) {

        FareDraft draft = new FareDraft();

        draft.setTenantCode(tenantCode);
        draft.setGridMode(GridMode.OD);
        draft.setOriginIdentifier(origin);
        draft.setDestIdentifier(destination);
        draft.setProductCode(product);
        draft.setOriginalPrice(new BigDecimal(originalPrice));
        draft.setDraftPrice(new BigDecimal(draftPrice));

        return draft;
    }

    // Verify that the endpoint retrieves drafts for the selected tenant.
    @Test
    void shouldReturnPendingDraftsForSelectedTenant() throws Exception {

        Tenant tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn("NYW_SANDBOX");
        when(tenantContext.getSelectedTenant()).thenReturn(tenant);

        FareDraft draft = createDraft(
            "NYW_SANDBOX",
            "EDGEWTR",
            "MIDTOWN",
            "ADULT_ONEWAY",
            "13.25",
            "14.00"
        );

        when(fareDraftService.getPendingDrafts("NYW_SANDBOX"))
            .thenReturn(List.of(draft));

        mockMvc.perform(get("/api/publishing/pending-changes")
                .with(user("testoperator").roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(
                "application/json"
            ))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].tenantCode")
                .value("NYW_SANDBOX"))
            .andExpect(jsonPath("$[0].gridMode")
                .value("OD"))
            .andExpect(jsonPath("$[0].selectedContext")
                .value("EDGEWTR"))
            .andExpect(jsonPath("$[0].rowKey")
                .value("MIDTOWN"))
            .andExpect(jsonPath("$[0].colKey")
                .value("ADULT_ONEWAY"))
            .andExpect(jsonPath("$[0].publishedPrice")
                .value(13.25))
            .andExpect(jsonPath("$[0].stagedPrice")
                .value(14.00));

        verify(fareDraftService).getPendingDrafts("NYW_SANDBOX");
    }

    // Verify that drafts from different origins appear together.
    @Test
    void shouldReturnDraftsAcrossMultipleOrigins() throws Exception {

        Tenant tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn("NYW_SANDBOX");
        when(tenantContext.getSelectedTenant()).thenReturn(tenant);

        FareDraft firstDraft = createDraft(
            "NYW_SANDBOX",
            "EDGEWTR",
            "MIDTOWN",
            "ADULT_ONEWAY",
            "13.25",
            "14.00"
        );

        FareDraft secondDraft = createDraft(
            "NYW_SANDBOX",
            "PORT_IMPERIAL",
            "MIDTOWN",
            "FULL_10TRIP",
            "112.63",
            "115.00"
        );

        when(fareDraftService.getPendingDrafts("NYW_SANDBOX"))
            .thenReturn(List.of(firstDraft, secondDraft));

        mockMvc.perform(get("/api/publishing/pending-changes")
                .with(user("testoperator").roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].selectedContext")
                .value("EDGEWTR"))
            .andExpect(jsonPath("$[1].selectedContext")
                .value("PORT_IMPERIAL"));

        verify(fareDraftService).getPendingDrafts("NYW_SANDBOX");
    }

    // Verify that no drafts are retrieved without a selected tenant.
    @Test
    void shouldReturnEmptyListWhenNoTenantSelected() throws Exception {

        when(tenantContext.getSelectedTenant()).thenReturn(null);

        mockMvc.perform(get("/api/publishing/pending-changes")
                .with(user("testoperator").roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));

        verifyNoInteractions(fareDraftService);
    }

    // Verify that the changed property is serialized for the frontend.
    @Test
    void shouldSerializeChangedProperty() throws Exception {

        Tenant tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn("NYW_SANDBOX");
        when(tenantContext.getSelectedTenant()).thenReturn(tenant);

        FareDraft draft = createDraft(
            "NYW_SANDBOX",
            "EDGEWTR",
            "MIDTOWN",
            "ADULT_ONEWAY",
            "13.25",
            "14.00"
        );

        when(fareDraftService.getPendingDrafts("NYW_SANDBOX"))
            .thenReturn(List.of(draft));

        mockMvc.perform(get("/api/publishing/pending-changes")
                .with(user("testoperator").roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].changed").value(true));
    }

    // Verify that unauthenticated users cannot access pending drafts.
    @Test
    @WithAnonymousUser
    void shouldRequireAuthentication() throws Exception {

        mockMvc.perform(get("/api/publishing/pending-changes"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }
}




