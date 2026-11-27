
package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.repository.AvailabilityRuleRepository;
import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.RateAssignmentSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.RateTableGetRequest;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

@Service
public class AvailabilityPublicationService {

    private static final String TENANT = "NYW_SANDBOX";

    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final CaracalApiClient caracalApiClient;

    public AvailabilityPublicationService(
            AvailabilityRuleRepository availabilityRuleRepository,
            CaracalApiClient caracalApiClient) {

        this.availabilityRuleRepository = availabilityRuleRepository;
        this.caracalApiClient = caracalApiClient;
    }

    public List<AvailabilityRule> applyPendingAvailabilityChanges(
            Tenant tenant) {

        if (tenant == null || !TENANT.equals(tenant.getCode())) {
            throw new IllegalArgumentException(
                    "Availability publication is restricted to NYW_SANDBOX.");
        }

        if (tenant.getGridMode() != GridMode.OD) {
            throw new IllegalArgumentException(
                    "Only OD availability publication is currently supported.");
        }

        List<AvailabilityRule> pendingRules =
                availabilityRuleRepository
                        .findByTenantCodeAndPendingPublishTrue(TENANT);

        List<AvailabilityUpdate> updates = new ArrayList<>();

        // Validate all pending rules before applying any mutations.
        for (AvailabilityRule rule : pendingRules) {

            if (!TENANT.equals(rule.getTenantCode())) {
                throw new IllegalStateException(
                        "Unsupported availability rule: " + rule.getId());
            }

            if (!AvailabilityRule.ALL_ROUTES.equals(rule.getRouteCode())) {
                throw new IllegalStateException(
                        "Route-specific availability publication is not "
                                + "currently supported for rule "
                                + rule.getId());
            }

            if (rule.getTemporalCondition() != null) {
                throw new IllegalStateException(
                        "Temporal availability publication is not "
                                + "currently supported for rule "
                                + rule.getId());
            }

            if (rule.isPhasedOut()) {
                throw new IllegalStateException(
                        "Product phase-out publication is not "
                                + "currently supported for rule "
                                + rule.getId());
            }

            RateAssignmentSummary assignment = getAssignment(rule);

            String targetCellType =
                    rule.isAvailable() ? "VALUE" : "NOT_SOLD";

            if (!"VALUE".equals(assignment.cellType())
                    && !"NOT_SOLD".equals(assignment.cellType())) {

                throw new IllegalStateException(
                        "Unsupported current assignment state for rule "
                                + rule.getId());
            }

            if ("VALUE".equals(targetCellType)
                    && (assignment.priceRuleSetName() == null
                    || assignment.priceRuleSetName().isBlank())) {

                throw new IllegalStateException(
                        "No price rule set found for availability rule "
                                + rule.getId());
            }

            System.out.println(
                    "Availability publication target: "
                            + rule.getSiteCode()
                            + " | Product: "
                            + rule.getProductCode()
                            + " | Assignment: "
                            + assignment.id()
                            + " | Target state: "
                            + targetCellType);

            updates.add(new AvailabilityUpdate(
                    rule,
                    assignment.id(),
                    assignment.cellType(),
                    targetCellType));
        }

        // Apply validated availability changes to Caracal.
        for (AvailabilityUpdate update : updates) {

            // Skip mutations when the requested state already exists.
            if (update.targetCellType().equals(
                    update.currentCellType())) {
                continue;
            }

            caracalApiClient.updateRateAssignment(
                    TENANT,
                    update.assignmentId(),
                    update.targetCellType());
        }

        // Verify every pending rule against Caracal.
        for (AvailabilityUpdate update : updates) {

            AvailabilityRule rule = update.rule();

            RateAssignmentSummary updatedAssignment =
                    getAssignment(rule);

            if (!update.assignmentId().equals(updatedAssignment.id())
                    || !update.targetCellType().equals(
                            updatedAssignment.cellType())) {

                throw new IllegalStateException(
                        "Updated availability could not be verified "
                                + "for rule " + rule.getId());
            }

            System.out.println(
                    "Availability update verified for rule "
                            + rule.getId()
                            + " | Updated state: "
                            + updatedAssignment.cellType());
        }

        return List.copyOf(pendingRules);
    }

    // Finalize rules only after successful catalog publication.
    @Transactional
    public void commitPublishedRules(
            List<AvailabilityRule> publishedRules) {

        List<AvailabilityRule> rulesToCommit = new ArrayList<>();

        for (AvailabilityRule publishedRule : publishedRules) {

            AvailabilityRule currentRule =
                    availabilityRuleRepository
                            .findById(publishedRule.getId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Availability rule no longer exists: "
                                            + publishedRule.getId()));

            if (!TENANT.equals(currentRule.getTenantCode())
                    || !currentRule.isPendingPublish()
                    || !Objects.equals(
                            currentRule.getLastUpdated(),
                            publishedRule.getLastUpdated())
                    || !Objects.equals(
                            currentRule.getSiteCode(),
                            publishedRule.getSiteCode())
                    || !Objects.equals(
                            currentRule.getRouteCode(),
                            publishedRule.getRouteCode())
                    || !Objects.equals(
                            currentRule.getProductCode(),
                            publishedRule.getProductCode())
                    || currentRule.isAvailable()
                            != publishedRule.isAvailable()
                    || currentRule.isPhasedOut()
                    || currentRule.getTemporalCondition() != null) {

                throw new IllegalStateException(
                        "Availability rule changed during publication: "
                                + publishedRule.getId());
            }

            rulesToCommit.add(currentRule);
        }

        for (AvailabilityRule rule : rulesToCommit) {
            rule.setPendingPublish(false);
        }

        availabilityRuleRepository.saveAll(rulesToCommit);
    }

    private RateAssignmentSummary getAssignment(
            AvailabilityRule rule) {

        String rateTableName = "RT-" + rule.getSiteCode();

        List<RateAssignmentSummary> assignments =
                caracalApiClient.fetchRateTable(
                        new RateTableGetRequest(TENANT, rateTableName))
                        .orElseThrow(() -> new IllegalStateException(
                                "No rate table found for site "
                                        + rule.getSiteCode()));

        List<RateAssignmentSummary> matchingAssignments =
                assignments.stream()
                        .filter(assignment ->
                                rule.getProductCode().equals(
                                        assignment.productCode()))
                        .toList();

        if (matchingAssignments.isEmpty()) {
            throw new IllegalStateException(
                    "No rate assignment found for rule "
                            + rule.getId());
        }

        if (matchingAssignments.size() != 1) {
            throw new IllegalStateException(
                    "Multiple rate assignments found for rule "
                            + rule.getId()
                            + ". Automatic publication is unsafe.");
        }

        RateAssignmentSummary assignment =
                matchingAssignments.get(0);

        if (assignment.id() == null
                || assignment.id().isBlank()) {

            throw new IllegalStateException(
                    "No assignment ID found for rule "
                            + rule.getId());
        }

        return assignment;
    }

    private record AvailabilityUpdate(
            AvailabilityRule rule,
            String assignmentId,
            String currentCellType,
            String targetCellType) {
    }
}
