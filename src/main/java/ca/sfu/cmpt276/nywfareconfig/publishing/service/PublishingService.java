
package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

import tools.jackson.databind.JsonNode;

@Service
public class PublishingService {

    private static final String TENANT = "NYW_SANDBOX";

    private final CaracalApiClient caracalApiClient;
    private final FarePublicationService farePublicationService;
    private final AvailabilityPublicationService availabilityPublicationService;

    private final AtomicBoolean publishing = new AtomicBoolean(false);

    private boolean publicationOutcomeUncertain = false;

    public PublishingService(
            CaracalApiClient caracalApiClient,
            FarePublicationService farePublicationService,
            AvailabilityPublicationService availabilityPublicationService) {

        this.caracalApiClient = caracalApiClient;
        this.farePublicationService = farePublicationService;
        this.availabilityPublicationService = availabilityPublicationService;
    }

    public JsonNode publish(Tenant tenant) {

        if (tenant == null || !TENANT.equals(tenant.getCode())) {
            throw new IllegalArgumentException(
                    "Invalid publishing tenant.");
        }

        // Prevent concurrent publication requests.
        if (!publishing.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "A publication is already in progress.");
        }

        try {

            // Prevent retries when a previous outcome is uncertain.
            if (publicationOutcomeUncertain) {
                throw new IllegalStateException(
                        "Previous publication outcome is uncertain. "
                        + "Check publication history before retrying.");
            }

            // Retrieve existing publication history.
            Set<String> previousIds = getPublicationIds(
                    caracalApiClient.fetchPublicationHistory(TENANT));

            // Apply staged PostgreSQL fare changes to Caracal.
            List<FareDraft> appliedDrafts =
                    farePublicationService.applyPendingFareChanges(tenant);

            // Apply staged PostgreSQL availability changes to Caracal.
            List<AvailabilityRule> appliedAvailabilityRules =
                    availabilityPublicationService
                            .applyPendingAvailabilityChanges(tenant);

            if (appliedDrafts.isEmpty()
                    && appliedAvailabilityRules.isEmpty()) {

                throw new IllegalStateException(
                        "No pending configuration changes to publish.");
            }

            // Schedule publication will be integrated separately.

            // Publish the updated Caracal catalog.
            publicationOutcomeUncertain = true;

            JsonNode publishResponse =
                    caracalApiClient.publishCatalog(TENANT);

            if (publishResponse == null) {
                throw new IllegalStateException(
                        "Caracal returned no publication response.");
            }

            // Extract the exact publication ID returned by Caracal.
            JsonNode publicationIdNode =
                    publishResponse.path("publicationId");

            if (!publicationIdNode.isTextual()
                    || publicationIdNode.asText().isBlank()) {

                throw new IllegalStateException(
                        "Caracal returned no valid publication ID.");
            }

            String expectedPublicationId =
                    publicationIdNode.asText();

            // Retrieve updated publication history.
            JsonNode updatedHistory =
                    caracalApiClient.fetchPublicationHistory(TENANT);

            getPublicationIds(updatedHistory);

            // Verify that the expected publication is new.
            for (JsonNode publication :
                    updatedHistory.path("publications")) {

                String id = publication.path("id").asText();

                if (expectedPublicationId.equals(id)
                        && !previousIds.contains(id)
                        && "CATALOG".equals(
                                publication.path("kind").asText())) {

                    // Finalize PostgreSQL changes only after verification.
                    if (!appliedDrafts.isEmpty()) {
                        farePublicationService
                                .commitPublishedDrafts(appliedDrafts);
                    }

                    if (!appliedAvailabilityRules.isEmpty()) {
                        availabilityPublicationService
                                .commitPublishedRules(
                                        appliedAvailabilityRules);
                    }

                    publicationOutcomeUncertain = false;

                    return publication;
                }
            }

            throw new IllegalStateException(
                    "Publication result could not be verified.");

        } finally {

            publishing.set(false);
        }
    }

    // Extract and verify publication history IDs.
    private Set<String> getPublicationIds(JsonNode history) {

        if (history == null
                || !history.path("publications").isArray()) {

            throw new IllegalStateException(
                    "Unable to retrieve valid publication history.");
        }

        Set<String> ids = new HashSet<>();

        for (JsonNode publication : history.path("publications")) {

            String id = publication.path("id").asText("");

            if (id.isBlank() || !ids.add(id)) {
                throw new IllegalStateException(
                        "Invalid publication history record.");
            }
        }

        return ids;
    }
}


