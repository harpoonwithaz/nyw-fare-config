package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ca.sfu.cmpt276.nywfareconfig.publishing.service.PublishingService;
import ca.sfu.cmpt276.nywfareconfig.security.config.SecurityConfig;
import ca.sfu.cmpt276.nywfareconfig.security.controller.GlobalUserModelAdvice;
import ca.sfu.cmpt276.nywfareconfig.tenant.controller.GlobalTenantAdvice;
import ca.sfu.cmpt276.nywfareconfig.tenant.controller.TenantSwitcherAdvice;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(
    controllers = PublicationActionController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            GlobalTenantAdvice.class,
            TenantSwitcherAdvice.class,
            GlobalUserModelAdvice.class
        }
    )
)
@Import(SecurityConfig.class)
class PublicationActionControllerTest {

    private static final String TENANT = "NYW_SANDBOX";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublishingService publishingService;

    @MockitoBean
    private TenantContext tenantContext;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn(TENANT);
        when(tenantContext.getSelectedTenant()).thenReturn(tenant);
    }

    @Test
    void authorizedUserCanPublishConfiguration() throws Exception {

        when(publishingService.publish(tenant))
                .thenReturn(new ObjectMapper().readTree(
                    "{\"id\":\"publication-123\",\"kind\":\"CATALOG\"}"
                ));

        mockMvc.perform(
                post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
                    .with(csrf())
            )
            .andExpect(status().isOk())
            .andExpect(content().string(
                "Configuration published successfully. Publication ID: publication-123"
            ));

        verify(publishingService).publish(tenant);
    }

    @Test
    void operatorCannotPublish() throws Exception {

        mockMvc.perform(
                post("/api/publishing/publish")
                    .with(user("test-operator").roles("OPERATOR"))
                    .with(csrf())
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(publishingService);
    }

    @Test
    void publishWithoutCsrfTokenIsForbidden() throws Exception {

        mockMvc.perform(
                post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(publishingService);
    }

    @Test
    void missingTenantPreventsPublication() throws Exception {

        when(tenantContext.getSelectedTenant()).thenReturn(null);

        mockMvc.perform(
                post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
                    .with(csrf())
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(publishingService);
    }

    @Test
    void nonSandboxTenantPreventsPublication() throws Exception {

        when(tenant.getCode()).thenReturn("NYW");

        mockMvc.perform(
                post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
                    .with(csrf())
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(publishingService);
    }

    @Test
    void noPendingChangesReturnsConflict() throws Exception {

        when(publishingService.publish(tenant))
                .thenThrow(new IllegalStateException(
                    "No pending fare changes to publish."
                ));

        mockMvc.perform(
                post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
                    .with(csrf())
            )
            .andExpect(status().isConflict())
            .andExpect(content().string(
                "No pending fare changes to publish."
            ));
    }

    @Test
    void concurrentPublicationReturnsConflict() throws Exception {

        when(publishingService.publish(tenant))
                .thenThrow(new IllegalStateException(
                    "A publication is already in progress."
                ));

        mockMvc.perform(
                post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
                    .with(csrf())
            )
            .andExpect(status().isConflict())
            .andExpect(content().string(
                "A publication is already in progress."
            ));
    }

    @Test
    void invalidPublicationRequestReturnsBadRequest() throws Exception {

        when(publishingService.publish(tenant))
                .thenThrow(new IllegalArgumentException(
                    "Only OD fare publication is currently supported."
                ));

        mockMvc.perform(
                post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
                    .with(csrf())
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void unexpectedPublicationFailureReturnsBadGateway()
            throws Exception {

        when(publishingService.publish(tenant))
                .thenThrow(new RuntimeException("Caracal unavailable"));

        mockMvc.perform(
                post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
                    .with(csrf())
            )
            .andExpect(status().isBadGateway())
            .andExpect(content().string(
                "Publication could not be completed or verified. "
                + "Some changes may have been applied to Caracal."
            ));
    }
}



