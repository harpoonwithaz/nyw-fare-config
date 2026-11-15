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

import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.StagingService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.TenantService;
import ca.sfu.cmpt276.nywfareconfig.security.controller.GlobalUserModelAdvice;
import ca.sfu.cmpt276.nywfareconfig.security.config.SecurityConfig;


// PR Comment #4: Test the real MVC endpoint without unrelated view-model advice.
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
    private StagingService stagingService;

    @MockitoBean
    private TenantContext tenantContext;

    @MockitoBean
    private TenantService tenantService;

    // PR COMMENT #4:
    // Verify tenant filtering through the real HTTP endpoint.
    @Test
    void shouldReturnOnlyChangesForSelectedTenant() throws Exception {

        Tenant tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn("NYW_SANDBOX");
        when(tenantContext.getSelectedTenant()).thenReturn(tenant);

        ConfigurationChangeView nywChange =
            new ConfigurationChangeView(
                "NYW_SANDBOX",
                GridMode.OD,
                "EDGEWTR",
                "Midtown / W. 39th St",
                "Adult One-Way",
                "Adult One-Way",
                new BigDecimal("13.25"),
                new BigDecimal("14.00")
            );

        ConfigurationChangeView metraChange =
            new ConfigurationChangeView(
                "METRA",
                GridMode.ZONAL,
                "ADULT_ONEWAY",
                "Zone 1",
                "Zone 2",
                "Adult One-Way",
                new BigDecimal("4.00"),
                new BigDecimal("4.50")
            );

        when(stagingService.getConfigurationChanges())
            .thenReturn(List.of(nywChange, metraChange));

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
            .andExpect(jsonPath("$[0].stagedPrice")
                .value(14.00));
    }

    // PR COMMENT #4:
    // Verify the endpoint returns an empty JSON array without a tenant.
    @Test
    void shouldReturnEmptyListWhenNoTenantSelected()
            throws Exception {

        when(tenantContext.getSelectedTenant()).thenReturn(null);

        mockMvc.perform(get("/api/publishing/pending-changes")
        .with(user("testoperator").roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));

        verifyNoInteractions(stagingService);
    }

    // PR COMMENT #4:
    // Verify Jackson serializes the change status as "changed".
    @Test
    void shouldSerializeChangedProperty() throws Exception {

        Tenant tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn("NYW_SANDBOX");
        when(tenantContext.getSelectedTenant()).thenReturn(tenant);

        ConfigurationChangeView change =
            new ConfigurationChangeView(
                "NYW_SANDBOX",
                GridMode.OD,
                "EDGEWTR",
                "MIDTOWN",
                "Adult One-Way",
                "Adult One-Way",
                new BigDecimal("13.25"),
                new BigDecimal("14.00")
            );

        when(stagingService.getConfigurationChanges())
            .thenReturn(List.of(change));

        mockMvc.perform(get("/api/publishing/pending-changes")
        .with(user("testoperator").roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].changed").value(true));
    }

    @Test
    @WithAnonymousUser
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/publishing/pending-changes"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }
}




