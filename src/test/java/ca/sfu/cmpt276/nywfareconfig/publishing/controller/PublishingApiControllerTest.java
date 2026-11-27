package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.StagingService;
import ca.sfu.cmpt276.nywfareconfig.security.config.SecurityConfig;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.TenantService;
import ca.sfu.cmpt276.nywfareconfig.security.controller.GlobalUserModelAdvice;

@WebMvcTest(
    controllers = PublishingApiController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = GlobalUserModelAdvice.class
    )
)
@Import(SecurityConfig.class)
@WithMockUser(username = "testoperator", roles = {"OPERATOR"})
@ActiveProfiles("test")
class PublishingApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StagingService stagingService;

    @MockitoBean
    private TenantContext tenantContext;

    @MockitoBean
    private TenantService tenantService;

    // Create a review DTO without connecting to PostgreSQL.
    private ConfigurationChangeView createChange(
            String tenantCode,
            GridMode gridMode,
            String selectedContext,
            String rowKey,
            String colKey,
            String product,
            String originalPrice,
            String draftPrice) {

        return new ConfigurationChangeView(
            tenantCode,
            gridMode,
            selectedContext,
            rowKey,
            colKey,
            product,
            new BigDecimal(originalPrice),
            new BigDecimal(draftPrice)
        );
    }

    // Verify that the endpoint retrieves changes for the selected tenant.
    @Test
    void shouldReturnPendingDraftsForSelectedTenant() throws Exception {

        Tenant tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn("NYW_SANDBOX");
        when(tenantContext.getSelectedTenant()).thenReturn(tenant);

        ConfigurationChangeView change = createChange(
            "NYW_SANDBOX",
            GridMode.OD,
            "EDGEWTR",
            "MIDTOWN",
            "ADULT_ONEWAY",
            "ADULT_ONEWAY",
            "13.25",
            "14.00"
        );

        when(stagingService.getConfigurationChanges("NYW_SANDBOX"))
            .thenReturn(List.of(change));

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

        verify(stagingService)
            .getConfigurationChanges("NYW_SANDBOX");
    }

    // Verify that changes from different origins appear together.
    @Test
    void shouldReturnDraftsAcrossMultipleOrigins() throws Exception {

        Tenant tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn("NYW_SANDBOX");
        when(tenantContext.getSelectedTenant()).thenReturn(tenant);

        ConfigurationChangeView firstChange = createChange(
            "NYW_SANDBOX",
            GridMode.OD,
            "EDGEWTR",
            "MIDTOWN",
            "ADULT_ONEWAY",
            "ADULT_ONEWAY",
            "13.25",
            "14.00"
        );

        ConfigurationChangeView secondChange = createChange(
            "NYW_SANDBOX",
            GridMode.OD,
            "PORT_IMPERIAL",
            "MIDTOWN",
            "FULL_10TRIP",
            "FULL_10TRIP",
            "112.63",
            "115.00"
        );

        when(stagingService.getConfigurationChanges("NYW_SANDBOX"))
            .thenReturn(List.of(firstChange, secondChange));

        mockMvc.perform(get("/api/publishing/pending-changes")
                .with(user("testoperator").roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].selectedContext")
                .value("EDGEWTR"))
            .andExpect(jsonPath("$[1].selectedContext")
                .value("PORT_IMPERIAL"));

        verify(stagingService)
            .getConfigurationChanges("NYW_SANDBOX");
    }

    // Verify that no changes are retrieved without a selected tenant.
    @Test
    void shouldReturnEmptyListWhenNoTenantSelected() throws Exception {

        when(tenantContext.getSelectedTenant()).thenReturn(null);

        mockMvc.perform(get("/api/publishing/pending-changes")
                .with(user("testoperator").roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));

        verifyNoInteractions(stagingService);
    }

    // Verify that the changed property is serialized for the frontend.
    @Test
    void shouldSerializeChangedProperty() throws Exception {

        Tenant tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn("NYW_SANDBOX");
        when(tenantContext.getSelectedTenant()).thenReturn(tenant);

        ConfigurationChangeView change = createChange(
            "NYW_SANDBOX",
            GridMode.OD,
            "EDGEWTR",
            "MIDTOWN",
            "ADULT_ONEWAY",
            "ADULT_ONEWAY",
            "13.25",
            "14.00"
        );

        when(stagingService.getConfigurationChanges("NYW_SANDBOX"))
            .thenReturn(List.of(change));

        mockMvc.perform(get("/api/publishing/pending-changes")
                .with(user("testoperator").roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].changed").value(true));
    }

    // Verify that unauthenticated users cannot access pending changes.
    @Test
    @WithAnonymousUser
    void shouldRequireAuthentication() throws Exception {

        mockMvc.perform(get("/api/publishing/pending-changes"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    // Verify that zonal configuration fields are mapped correctly.
    @Test
    void shouldMapZonalDraftCorrectly() throws Exception {

        Tenant tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn("METRA");
        when(tenantContext.getSelectedTenant()).thenReturn(tenant);

        ConfigurationChangeView change = createChange(
            "METRA",
            GridMode.ZONAL,
            "ADULT_ONEWAY",
            "ZONE_1",
            "ZONE_2",
            "ADULT_ONEWAY",
            "4.00",
            "4.50"
        );

        when(stagingService.getConfigurationChanges("METRA"))
            .thenReturn(List.of(change));

        mockMvc.perform(get("/api/publishing/pending-changes")
                .with(user("testoperator").roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].gridMode").value("ZONAL"))
            .andExpect(jsonPath("$[0].selectedContext")
                .value("ADULT_ONEWAY"))
            .andExpect(jsonPath("$[0].rowKey").value("ZONE_1"))
            .andExpect(jsonPath("$[0].colKey").value("ZONE_2"))
            .andExpect(jsonPath("$[0].publishedPrice").value(4.00))
            .andExpect(jsonPath("$[0].stagedPrice").value(4.50))
            .andExpect(jsonPath("$[0].changed").value(true));
    }

    // Verify that the API returns an empty list when no changes exist.
    @Test
    void shouldReturnEmptyListWhenNoChangesExist() throws Exception {

        Tenant tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn("NYW_SANDBOX");
        when(tenantContext.getSelectedTenant()).thenReturn(tenant);

        when(stagingService.getConfigurationChanges("NYW_SANDBOX"))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/publishing/pending-changes")
                .with(user("testoperator").roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));

        verify(stagingService)
            .getConfigurationChanges("NYW_SANDBOX");
    }
}





