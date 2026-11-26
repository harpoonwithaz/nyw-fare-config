package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class PublishingServiceTest {

    private static final String TENANT = "NYW_SANDBOX";

    private CaracalApiClient caracalApiClient;
    private FarePublicationService farePublicationService;
    private PublishingService publishingService;

    private Tenant tenant;
    private List<FareDraft> drafts;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {

        caracalApiClient = mock(CaracalApiClient.class);
        farePublicationService = mock(FarePublicationService.class);

        tenant = mock(Tenant.class);
        when(tenant.getCode()).thenReturn(TENANT);

        drafts = List.of(mock(FareDraft.class));

        when(farePublicationService.applyPendingFareChanges(tenant))
                .thenReturn(drafts);

        publishingService = new PublishingService(
                caracalApiClient,
                farePublicationService
        );
    }

    private JsonNode json(String value) {
        return objectMapper.readTree(value);
    }

    private JsonNode emptyHistory() {
        return json("{\"publications\":[]}");
    }

    private JsonNode historyWithPublication(String id) {

        return json("""
                {
                    "publications": [
                        {"id": "%s", "kind": "CATALOG"}
                    ]
                }
                """.formatted(id));
    }

    private JsonNode publicationResponse(String id) {

        return json("""
                {"publicationId": "%s"}
                """.formatted(id));
    }

    private void mockSuccessfulPublication() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(
                        emptyHistory(),
                        historyWithPublication("new-1")
                );

        when(caracalApiClient.publishCatalog(TENANT))
                .thenReturn(publicationResponse("new-1"));
    }

    @Test
    void invalidTenantIsRejected() {

        when(tenant.getCode()).thenReturn("NYW");

        assertThrows(
                IllegalArgumentException.class,
                () -> publishingService.publish(tenant)
        );

        verifyNoInteractions(caracalApiClient, farePublicationService);
    }

    @Test
    void nullTenantIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> publishingService.publish(null)
        );

        verifyNoInteractions(caracalApiClient, farePublicationService);
    }

    @Test
    void successfulPublicationCommitsDraftsAfterVerification() {

        mockSuccessfulPublication();

        JsonNode publication = publishingService.publish(tenant);

        assertEquals("new-1", publication.path("id").asText());

        var order = inOrder(caracalApiClient, farePublicationService);

        order.verify(caracalApiClient)
                .fetchPublicationHistory(TENANT);

        order.verify(farePublicationService)
                .applyPendingFareChanges(tenant);

        order.verify(caracalApiClient)
                .publishCatalog(TENANT);

        order.verify(caracalApiClient)
                .fetchPublicationHistory(TENANT);

        order.verify(farePublicationService)
                .commitPublishedDrafts(drafts);
    }

    @Test
    void noPendingDraftsPreventsPublication() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(emptyHistory());

        when(farePublicationService.applyPendingFareChanges(tenant))
                .thenReturn(List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        assertEquals(
                "No pending fare changes to publish.",
                exception.getMessage()
        );

        verify(caracalApiClient, never()).publishCatalog(anyString());

        verify(farePublicationService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void fareMutationFailurePreventsCatalogPublication() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(emptyHistory());

        when(farePublicationService.applyPendingFareChanges(tenant))
                .thenThrow(new IllegalStateException("Fare update failed"));

        assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        verify(caracalApiClient, never()).publishCatalog(anyString());

        verify(farePublicationService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void invalidPreviousHistoryPreventsPublication() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(json("{}"));

        assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        verify(caracalApiClient, never()).publishCatalog(anyString());

        verifyNoInteractions(farePublicationService);
    }

    @Test
    void missingHistoryRecordPreventsDraftCommit() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(emptyHistory());

        when(caracalApiClient.publishCatalog(TENANT))
                .thenReturn(publicationResponse("new-1"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        assertEquals(
                "Publication result could not be verified.",
                exception.getMessage()
        );

        verify(farePublicationService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void nullPublicationResponsePreventsDraftCommit() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(emptyHistory());

        when(caracalApiClient.publishCatalog(TENANT))
                .thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        verify(farePublicationService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void missingPublicationIdPreventsDraftCommit() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(emptyHistory());

        when(caracalApiClient.publishCatalog(TENANT))
                .thenReturn(json("{\"success\":true}"));

        assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        verify(farePublicationService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void blankPublicationIdPreventsDraftCommit() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(emptyHistory());

        when(caracalApiClient.publishCatalog(TENANT))
                .thenReturn(publicationResponse(" "));

        assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        verify(farePublicationService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void mismatchedPublicationIdPreventsDraftCommit() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(
                        emptyHistory(),
                        historyWithPublication("another-publication")
                );

        when(caracalApiClient.publishCatalog(TENANT))
                .thenReturn(publicationResponse("expected-publication"));

        assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        verify(farePublicationService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void existingPublicationIdCannotVerifyNewPublication() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(historyWithPublication("old-1"));

        when(caracalApiClient.publishCatalog(TENANT))
                .thenReturn(publicationResponse("old-1"));

        assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        verify(farePublicationService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void incorrectPublicationKindPreventsDraftCommit() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(
                        emptyHistory(),
                        json("""
                            {
                                "publications": [
                                    {"id":"new-1","kind":"OTHER"}
                                ]
                            }
                            """)
                );

        when(caracalApiClient.publishCatalog(TENANT))
                .thenReturn(publicationResponse("new-1"));

        assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        verify(farePublicationService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void invalidUpdatedHistoryPreventsDraftCommit() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(emptyHistory(), json("{}"));

        when(caracalApiClient.publishCatalog(TENANT))
                .thenReturn(publicationResponse("new-1"));

        assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        verify(farePublicationService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void uncertainPublicationOutcomeBlocksRetry() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(emptyHistory());

        when(caracalApiClient.publishCatalog(TENANT))
                .thenThrow(new RuntimeException("Connection timed out"));

        assertThrows(
                RuntimeException.class,
                () -> publishingService.publish(tenant)
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        assertTrue(
                exception.getMessage().contains("outcome is uncertain")
        );

        verify(caracalApiClient, times(1)).publishCatalog(TENANT);

        verify(farePublicationService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void sequentialPublicationWithoutNewDraftsIsRejected() {

        mockSuccessfulPublication();

        publishingService.publish(tenant);

        when(farePublicationService.applyPendingFareChanges(tenant))
                .thenReturn(List.of());

        assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        verify(caracalApiClient, times(1)).publishCatalog(TENANT);

        verify(farePublicationService, times(1))
                .commitPublishedDrafts(drafts);
    }

    @Test
    void newDraftsCanBePublishedAfterSuccessfulPublication() {

        when(caracalApiClient.fetchPublicationHistory(TENANT))
                .thenReturn(
                        emptyHistory(),
                        historyWithPublication("new-1"),
                        historyWithPublication("new-1"),
                        historyWithPublication("new-2")
                );

        when(caracalApiClient.publishCatalog(TENANT))
                .thenReturn(
                        publicationResponse("new-1"),
                        publicationResponse("new-2")
                );

        List<FareDraft> newDrafts = List.of(mock(FareDraft.class));

        when(farePublicationService.applyPendingFareChanges(tenant))
                .thenReturn(drafts, newDrafts);

        assertEquals(
                "new-1",
                publishingService.publish(tenant).path("id").asText()
        );

        assertEquals(
                "new-2",
                publishingService.publish(tenant).path("id").asText()
        );

        verify(farePublicationService).commitPublishedDrafts(drafts);
        verify(farePublicationService).commitPublishedDrafts(newDrafts);

        verify(caracalApiClient, times(2)).publishCatalog(TENANT);
    }

    @Test
    void concurrentPublicationIsRejected() throws Exception {

        mockSuccessfulPublication();

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        when(farePublicationService.applyPendingFareChanges(tenant))
                .thenAnswer(invocation -> {

                    started.countDown();

                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Test timed out");
                    }

                    return drafts;
                });

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {

            Future<JsonNode> firstPublication = executor.submit(
                    () -> publishingService.publish(tenant)
            );

            assertTrue(started.await(5, TimeUnit.SECONDS));

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> publishingService.publish(tenant)
            );

            assertEquals(
                    "A publication is already in progress.",
                    exception.getMessage()
            );

            release.countDown();

            assertEquals(
                    "new-1",
                    firstPublication.get(5, TimeUnit.SECONDS)
                            .path("id").asText()
            );

            verify(caracalApiClient, times(1)).publishCatalog(TENANT);

        } finally {

            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void failedDraftCommitBlocksAutomaticRetry() {

        mockSuccessfulPublication();

        doThrow(new IllegalStateException("Draft changed"))
                .when(farePublicationService)
                .commitPublishedDrafts(drafts);

        assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> publishingService.publish(tenant)
        );

        assertTrue(
                exception.getMessage().contains("outcome is uncertain")
        );

        verify(caracalApiClient, times(1)).publishCatalog(TENANT);
    }
}


