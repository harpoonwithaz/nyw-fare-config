package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareRule;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareDraftService;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

import tools.jackson.databind.JsonNode;

@Service
public class FarePublicationService {

    private static final String TENANT = "NYW_SANDBOX";

    private final FareDraftService fareDraftService;
    private final FareService fareService;
    private final CaracalApiClient caracalApiClient;

    public FarePublicationService(
            FareDraftService fareDraftService,
            FareService fareService,
            CaracalApiClient caracalApiClient) {

        this.fareDraftService = fareDraftService;
        this.fareService = fareService;
        this.caracalApiClient = caracalApiClient;
    }

    public List<FareDraft> applyPendingFareChanges(Tenant tenant) {

        if (tenant == null || !TENANT.equals(tenant.getCode())) {
            throw new IllegalArgumentException(
                    "Fare publication is restricted to NYW_SANDBOX.");
        }

        if (tenant.getGridMode() != GridMode.OD) {
            throw new IllegalArgumentException(
                    "Only OD fare publication is currently supported.");
        }

        List<FareDraft> drafts =
                fareDraftService.getPendingDrafts(TENANT);

        List<FareUpdate> updates = new ArrayList<>();

        Map<PriceRuleKey, BigDecimal> requestedPrices =
                new HashMap<>();

        // Validate all pending drafts before applying any mutations.
        for (FareDraft draft : drafts) {

            if (!TENANT.equals(draft.getTenantCode())
                    || draft.getGridMode() != GridMode.OD) {

                throw new IllegalStateException(
                        "Unsupported fare draft: " + draft.getId());
            }

            if (draft.getTargetStatus() != FareStatus.ACTIVE) {
                throw new IllegalStateException(
                        "Availability changes require separate handling.");
            }

            BigDecimal newPrice = draft.getDraftPrice();

            if (newPrice == null || newPrice.signum() < 0) {
                throw new IllegalStateException(
                        "Invalid draft price: " + draft.getId());
            }

            FareRule baseline = getBaseline(tenant, draft);

            String priceRuleSetName =
                    baseline.getPriceRuleSetName();

            if (priceRuleSetName == null
                    || priceRuleSetName.isBlank()) {

                throw new IllegalStateException(
                        "No price rule set found for draft "
                                + draft.getId());
            }

            if (baseline.getStatus() != FareStatus.ACTIVE
                    || baseline.getPrice() == null) {

                throw new IllegalStateException(
                        "Fare is not currently active: "
                                + draft.getId());
            }

            // Detect conflicting drafts targeting the same price rule.
            PriceRuleKey key = new PriceRuleKey(
                    priceRuleSetName,
                    draft.getOriginIdentifier(),
                    draft.getDestIdentifier());

            BigDecimal previousPrice =
                    requestedPrices.putIfAbsent(key, newPrice);

            if (previousPrice != null
                    && previousPrice.compareTo(newPrice) != 0) {

                throw new IllegalStateException(
                        "Conflicting prices for the same price rule: "
                                + priceRuleSetName + " | "
                                + draft.getOriginIdentifier() + " -> "
                                + draft.getDestIdentifier());
            }

            // The requested price is already present on Caracal.
            if (baseline.getPrice().compareTo(newPrice) == 0) {
                continue;
            }

            // Reject stale drafts whose original baseline has changed.
            if (draft.getOriginalPrice() == null
                    || baseline.getPrice().compareTo(
                            draft.getOriginalPrice()) != 0) {

                throw new IllegalStateException(
                        "Baseline price has changed for draft "
                                + draft.getId());
            }

            // Identical updates to the same rule only need one mutation.
            if (previousPrice == null) {
                updates.add(
                        new FareUpdate(draft, priceRuleSetName));
            }
        }

        // Apply validated fare changes to Caracal.
        for (FareUpdate update : updates) {

            FareDraft draft = update.draft();

            JsonNode response = caracalApiClient.updateOdMatrix(
                    TENANT,
                    update.priceRuleSetName(),
                    draft.getOriginIdentifier(),
                    draft.getDestIdentifier(),
                    draft.getDraftPrice());

            if (response == null
                    || response.path("rulesWritten").asInt(0) != 1) {

                throw new IllegalStateException(
                        "Caracal fare update could not be confirmed "
                                + "for draft " + draft.getId());
            }
        }

        // Verify every draft against the updated Caracal configuration.
        for (FareDraft draft : drafts) {

            FareRule updatedBaseline = getBaseline(tenant, draft);

            if (updatedBaseline.getStatus() != FareStatus.ACTIVE
                    || updatedBaseline.getPrice() == null
                    || updatedBaseline.getPrice().compareTo(
                            draft.getDraftPrice()) != 0) {

                throw new IllegalStateException(
                        "Updated fare could not be verified for draft "
                                + draft.getId());
            }

        }

        return List.copyOf(drafts);
    }

    // Finalize drafts only after successful catalog publication.
    public void commitPublishedDrafts(
            List<FareDraft> publishedDrafts) {

        fareDraftService.commitPublishedDrafts(publishedDrafts);
    }

    private FareRule getBaseline(Tenant tenant, FareDraft draft) {

        return fareService.getBaselineFareRule(
                        tenant,
                        draft.getOriginIdentifier(),
                        draft.getDestIdentifier(),
                        draft.getProductCode())
                .orElseThrow(() -> new IllegalStateException(
                        "No baseline fare found for draft "
                                + draft.getId()));
    }

    private record FareUpdate(
            FareDraft draft,
            String priceRuleSetName) {
    }

    private record PriceRuleKey(
            String priceRuleSetName,
            String originCode,
            String destinationCode) {
    }
}