package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import static org.mockito.Mockito.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.test.annotation.DirtiesContext;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.HttpCaracalApiClient;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareRule;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareDraftService;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareService;

import ca.sfu.cmpt276.nywfareconfig.publishing.service.PublishingService;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.FarePublicationService;

import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import ca.sfu.cmpt276.nywfareconfig.security.config.SecurityConfig;
import ca.sfu.cmpt276.nywfareconfig.security.controller.GlobalUserModelAdvice;

import ca.sfu.cmpt276.nywfareconfig.tenant.controller.GlobalTenantAdvice;
import ca.sfu.cmpt276.nywfareconfig.tenant.controller.TenantSwitcherAdvice;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

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
@Import({
    SecurityConfig.class,
    PublishingService.class,
    FarePublicationService.class,
    PublicationIntegrationTest.WireMockTestConfig.class
})
@DirtiesContext(
    classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD
)
class PublicationIntegrationTest {

    private static final String TENANT = "NYW_SANDBOX";

    private static final String ACTOR = "admin@example.com";
    private static final String ACTOR_HEADER = "operator:" + ACTOR;

    private static final String ORIGIN = "EDGEWTR";
    private static final String DESTINATION = "BROOKFD";
    private static final String PRODUCT = "CHILD_ONEWAY";
    private static final String RULE_SET = "OD-CHILD_ONEWAY";

    private static final String PUBLISH_PATH =
            "/functions/catalog-publish";

    private static final String HISTORY_PATH =
            "/functions/publication-list";

    private static final String MUTATION_PATH =
            "/functions/od-matrix-set";

    private static final String EMPTY_HISTORY =
            "{\"publications\":[]}";

    private static final String UPDATED_HISTORY = """
            {
                "publications": [
                    {"id":"publication-123","kind":"CATALOG"}
                ]
            }
            """;

    private static final String PUBLICATION_RESPONSE =
            "{\"publicationId\":\"publication-123\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WireMockServer wireMockServer;

    @MockitoBean
    private FareDraftService fareDraftService;

    @MockitoBean
    private FareService fareService;

    @MockitoBean
    private TenantContext tenantContext;

    @MockitoBean
    private ActorService actorService;

    private Tenant tenant;
    private FareDraft draft;

    @TestConfiguration
    static class WireMockTestConfig {

        @Bean(initMethod = "start", destroyMethod = "stop")
        WireMockServer wireMockServer() {

            return new WireMockServer(
                    wireMockConfig().dynamicPort()
            );
        }

        @Bean
        CaracalApiClient caracalApiClient(
                WireMockServer wireMockServer,
                ActorService actorService) {

            RestClient restClient = RestClient.builder()
                    .baseUrl(wireMockServer.baseUrl() + "/functions")
                    .defaultHeader("X-Actor", "system")
                    .build();

            return new HttpCaracalApiClient(
                    restClient,
                    actorService
            );
        }
    }

    @BeforeEach
    void setUp() {

        wireMockServer.resetAll();

        tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn(TENANT);
        when(tenant.getGridMode()).thenReturn(GridMode.OD);

        when(tenantContext.getSelectedTenant())
                .thenReturn(tenant);

        when(actorService.currentActorHeader())
                .thenReturn(ACTOR);

        draft = mock(FareDraft.class);

        when(draft.getId()).thenReturn(22L);
        when(draft.getTenantCode()).thenReturn(TENANT);
        when(draft.getGridMode()).thenReturn(GridMode.OD);
        when(draft.getOriginIdentifier()).thenReturn(ORIGIN);
        when(draft.getDestIdentifier()).thenReturn(DESTINATION);
        when(draft.getProductCode()).thenReturn(PRODUCT);
        when(draft.getTargetStatus()).thenReturn(FareStatus.ACTIVE);
        when(draft.getOriginalPrice()).thenReturn(new BigDecimal("9.00"));
        when(draft.getDraftPrice()).thenReturn(new BigDecimal("8.50"));

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(List.of(draft));

        FareRule originalRule = mock(FareRule.class);

        when(originalRule.getPriceRuleSetName()).thenReturn(RULE_SET);
        when(originalRule.getStatus()).thenReturn(FareStatus.ACTIVE);
        when(originalRule.getPrice()).thenReturn(new BigDecimal("9.00"));

        FareRule updatedRule = mock(FareRule.class);

        when(updatedRule.getPriceRuleSetName()).thenReturn(RULE_SET);
        when(updatedRule.getStatus()).thenReturn(FareStatus.ACTIVE);
        when(updatedRule.getPrice()).thenReturn(new BigDecimal("8.50"));

        when(fareService.getBaselineFareRule(
                tenant, ORIGIN, DESTINATION, PRODUCT))
                .thenReturn(
                        Optional.of(originalRule),
                        Optional.of(updatedRule)
                );
    }

    private void stubEmptyHistory() {

        wireMockServer.stubFor(
                get(urlPathEqualTo(HISTORY_PATH))
                    .withQueryParam("tenant", equalTo(TENANT))
                    .willReturn(okJson(EMPTY_HISTORY))
        );
    }

    private void stubSuccessfulFareMutation() {

        wireMockServer.stubFor(
                post(urlEqualTo(MUTATION_PATH))
                    .withHeader("X-Actor", equalTo(ACTOR_HEADER))
                    .withRequestBody(equalToJson("""
                        {
                            "tenantCode":"NYW_SANDBOX",
                            "priceRuleSetName":"OD-CHILD_ONEWAY",
                            "originDimensionKey":"ORIGIN",
                            "destDimensionKey":"DEST",
                            "rows":[
                                {
                                    "originSiteCode":"EDGEWTR",
                                    "destSiteCode":"BROOKFD",
                                    "price":8.50
                                }
                            ]
                        }
                        """))
                    .willReturn(okJson("{\"rulesWritten\":1}"))
        );
    }

    private void stubSuccessfulPublication() {

        String scenario = "Catalog Publication";

        wireMockServer.stubFor(
                get(urlPathEqualTo(HISTORY_PATH))
                    .withQueryParam("tenant", equalTo(TENANT))
                    .inScenario(scenario)
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(okJson(EMPTY_HISTORY))
        );

        wireMockServer.stubFor(
                post(urlEqualTo(PUBLISH_PATH))
                    .inScenario(scenario)
                    .whenScenarioStateIs(Scenario.STARTED)
                    .withHeader("X-Actor", equalTo(ACTOR_HEADER))
                    .withRequestBody(equalToJson("""
                        {"tenantCode":"NYW_SANDBOX"}
                        """))
                    .willSetStateTo("Published")
                    .willReturn(okJson(PUBLICATION_RESPONSE))
        );

        wireMockServer.stubFor(
                get(urlPathEqualTo(HISTORY_PATH))
                    .withQueryParam("tenant", equalTo(TENANT))
                    .inScenario(scenario)
                    .whenScenarioStateIs("Published")
                    .willReturn(okJson(UPDATED_HISTORY))
        );
    }

    @Test
    void authorizedUserCanPublishPendingFareChanges()
            throws Exception {

        stubSuccessfulFareMutation();
        stubSuccessfulPublication();

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
                    .with(csrf())
            )
            .andExpect(status().isOk())
            .andExpect(content().string(
                "Configuration published successfully. "
                + "Publication ID: publication-123"
            ));

        wireMockServer.verify(
                1,
                postRequestedFor(urlEqualTo(MUTATION_PATH))
                    .withHeader("X-Actor", equalTo(ACTOR_HEADER))
        );

        wireMockServer.verify(
                1,
                postRequestedFor(urlEqualTo(PUBLISH_PATH))
                    .withHeader("X-Actor", equalTo(ACTOR_HEADER))
        );

        wireMockServer.verify(
                2,
                getRequestedFor(urlPathEqualTo(HISTORY_PATH))
                    .withQueryParam("tenant", equalTo(TENANT))
        );

        verify(fareDraftService)
                .commitPublishedDrafts(List.of(draft));
    }

    @Test
    void operatorCannotPublish() throws Exception {

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/publishing/publish")
                    .with(user("test-operator").roles("OPERATOR"))
                    .with(csrf())
            )
            .andExpect(status().isForbidden());

        wireMockServer.verify(
                0,
                postRequestedFor(urlEqualTo(MUTATION_PATH))
        );

        wireMockServer.verify(
                0,
                postRequestedFor(urlEqualTo(PUBLISH_PATH))
        );

        verifyNoInteractions(fareDraftService);
    }

    @Test
    void noPendingDraftsPreventsPublication() throws Exception {

        stubEmptyHistory();

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(List.of());

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
                    .with(csrf())
            )
            .andExpect(status().isConflict());

        wireMockServer.verify(
                0,
                postRequestedFor(urlEqualTo(MUTATION_PATH))
        );

        wireMockServer.verify(
                0,
                postRequestedFor(urlEqualTo(PUBLISH_PATH))
        );

        verify(fareDraftService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void fareMutationFailurePreventsPublication() throws Exception {

        stubEmptyHistory();

        wireMockServer.stubFor(
                post(urlEqualTo(MUTATION_PATH))
                    .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Fare mutation failed\"}"))
        );

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
                    .with(csrf())
            )
            .andExpect(status().isBadGateway());

        wireMockServer.verify(
                0,
                postRequestedFor(urlEqualTo(PUBLISH_PATH))
        );

        verify(fareDraftService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void catalogPublicationFailurePreventsDraftCommit()
            throws Exception {

        stubEmptyHistory();
        stubSuccessfulFareMutation();

        wireMockServer.stubFor(
                post(urlEqualTo(PUBLISH_PATH))
                    .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Publication failed\"}"))
        );

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
                    .with(csrf())
            )
            .andExpect(status().isBadGateway());

        wireMockServer.verify(
                1,
                postRequestedFor(urlEqualTo(MUTATION_PATH))
        );

        wireMockServer.verify(
                1,
                postRequestedFor(urlEqualTo(PUBLISH_PATH))
        );

        verify(fareDraftService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void missingHistoryRecordPreventsDraftCommit()
            throws Exception {

        stubEmptyHistory();
        stubSuccessfulFareMutation();

        wireMockServer.stubFor(
                post(urlEqualTo(PUBLISH_PATH))
                    .willReturn(okJson(PUBLICATION_RESPONSE))
        );

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/publishing/publish")
                    .with(user("test-admin").roles("CLIENT_SERVICES"))
                    .with(csrf())
            )
            .andExpect(status().isConflict())
            .andExpect(content().string(
                "Publication result could not be verified."
            ));

        wireMockServer.verify(
                2,
                getRequestedFor(urlPathEqualTo(HISTORY_PATH))
        );

        verify(fareDraftService, never())
                .commitPublishedDrafts(anyList());
    }
}