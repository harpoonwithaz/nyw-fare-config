
package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.repository.AvailabilityRuleRepository;
import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.RateAssignmentSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.RateTableGetRequest;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

class AvailabilityPublicationServiceTest {

    private static final String TENANT = "NYW_SANDBOX";
    private static final String SITE = "EDGEWTR";
    private static final String PRODUCT = "CHILD_ONEWAY";
    private static final String ASSIGNMENT_ID = "assignment-123";
    private static final String RULE_SET = "OD-CHILD_ONEWAY";

    private static final LocalDateTime LAST_UPDATED =
            LocalDateTime.of(2026, 11, 26, 12, 0);

    private AvailabilityRuleRepository repository;
    private CaracalApiClient caracalApiClient;
    private AvailabilityPublicationService service;

    private Tenant tenant;
    private AvailabilityRule rule;

    @BeforeEach
    void setUp() {

        repository = mock(AvailabilityRuleRepository.class);
        caracalApiClient = mock(CaracalApiClient.class);

        service = new AvailabilityPublicationService(
                repository,
                caracalApiClient
        );

        tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn(TENANT);
        when(tenant.getGridMode()).thenReturn(GridMode.OD);

        rule = createRule(1L, false);

        when(repository.findByTenantCodeAndPendingPublishTrue(TENANT))
                .thenReturn(List.of(rule));
    }

    private AvailabilityRule createRule(Long id, boolean available) {

        AvailabilityRule result = mock(AvailabilityRule.class);

        when(result.getId()).thenReturn(id);
        when(result.getTenantCode()).thenReturn(TENANT);
        when(result.getSiteCode()).thenReturn(SITE);
        when(result.getRouteCode())
                .thenReturn(AvailabilityRule.ALL_ROUTES);
        when(result.getProductCode()).thenReturn(PRODUCT);
        when(result.isAvailable()).thenReturn(available);
        when(result.isPendingPublish()).thenReturn(true);
        when(result.isPhasedOut()).thenReturn(false);
        when(result.getLastUpdated()).thenReturn(LAST_UPDATED);

        return result;
    }

    private RateAssignmentSummary assignment(String cellType) {

        return new RateAssignmentSummary(
                ASSIGNMENT_ID,
                PRODUCT,
                cellType,
                RULE_SET
        );
    }

    private RateTableGetRequest rateTableRequest() {

        return new RateTableGetRequest(
                TENANT,
                "RT-" + SITE
        );
    }

    @Test
    void disablingAvailabilityUpdatesAssignmentToNotSold() {

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(
                        Optional.of(List.of(assignment("VALUE"))),
                        Optional.of(List.of(assignment("NOT_SOLD")))
                );

        List<AvailabilityRule> result =
                service.applyPendingAvailabilityChanges(tenant);

        assertEquals(List.of(rule), result);

        var order = inOrder(caracalApiClient);

        order.verify(caracalApiClient)
                .fetchRateTable(rateTableRequest());

        order.verify(caracalApiClient)
                .updateRateAssignment(
                        TENANT,
                        ASSIGNMENT_ID,
                        "NOT_SOLD"
                );

        order.verify(caracalApiClient)
                .fetchRateTable(rateTableRequest());

        verify(rule, never()).setPendingPublish(false);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void enablingAvailabilityUpdatesAssignmentToValue() {

        when(rule.isAvailable()).thenReturn(true);

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(
                        Optional.of(List.of(assignment("NOT_SOLD"))),
                        Optional.of(List.of(assignment("VALUE")))
                );

        List<AvailabilityRule> result =
                service.applyPendingAvailabilityChanges(tenant);

        assertEquals(List.of(rule), result);

        verify(caracalApiClient)
                .updateRateAssignment(
                        TENANT,
                        ASSIGNMENT_ID,
                        "VALUE"
                );

        verify(caracalApiClient, times(2))
                .fetchRateTable(rateTableRequest());

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void alreadyAppliedAvailabilitySkipsMutation() {

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(
                        Optional.of(List.of(assignment("NOT_SOLD")))
                );

        List<AvailabilityRule> result =
                service.applyPendingAvailabilityChanges(tenant);

        assertEquals(List.of(rule), result);

        verify(caracalApiClient, never())
                .updateRateAssignment(
                        anyString(),
                        anyString(),
                        anyString()
                );

        verify(caracalApiClient, times(2))
                .fetchRateTable(rateTableRequest());
    }

    @Test
    void noPendingRulesReturnsEmptyList() {

        when(repository.findByTenantCodeAndPendingPublishTrue(TENANT))
                .thenReturn(List.of());

        assertTrue(
                service.applyPendingAvailabilityChanges(tenant)
                        .isEmpty()
        );

        verifyNoInteractions(caracalApiClient);
    }

    @Test
    void nullTenantIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.applyPendingAvailabilityChanges(null)
        );

        verifyNoInteractions(repository, caracalApiClient);
    }

    @Test
    void unsupportedTenantIsRejected() {

        when(tenant.getCode()).thenReturn("NYW");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        verifyNoInteractions(repository, caracalApiClient);
    }

    @Test
    void unsupportedGridModeIsRejected() {

        when(tenant.getGridMode()).thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        verifyNoInteractions(repository, caracalApiClient);
    }

    @Test
    void ruleFromDifferentTenantIsRejected() {

        when(rule.getTenantCode()).thenReturn("NYW");

        assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        verifyNoInteractions(caracalApiClient);
    }

    @Test
    void routeSpecificAvailabilityIsRejected() {

        when(rule.getRouteCode()).thenReturn("ROUTE-1");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        assertTrue(
                exception.getMessage().contains("Route-specific")
        );

        verifyNoInteractions(caracalApiClient);
    }

    @Test
    void phasedOutProductIsRejected() {

        when(rule.isPhasedOut()).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        assertTrue(
                exception.getMessage().contains("phase-out")
        );

        verifyNoInteractions(caracalApiClient);
    }

    @Test
    void missingRateTablePreventsMutation() {

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        assertTrue(
                exception.getMessage().contains("No rate table")
        );

        verify(caracalApiClient, never())
                .updateRateAssignment(
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void missingProductAssignmentPreventsMutation() {

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(Optional.of(List.of(
                        new RateAssignmentSummary(
                                "different-assignment",
                                "OTHER_PRODUCT",
                                "VALUE",
                                RULE_SET
                        )
                )));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        assertTrue(
                exception.getMessage().contains("No rate assignment")
        );

        verify(caracalApiClient, never())
                .updateRateAssignment(
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void multipleMatchingAssignmentsPreventMutation() {

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(Optional.of(List.of(
                        assignment("VALUE"),
                        new RateAssignmentSummary(
                                "assignment-456",
                                PRODUCT,
                                "NOT_SOLD",
                                RULE_SET
                        )
                )));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        assertTrue(
                exception.getMessage()
                        .contains("Multiple rate assignments")
        );

        verify(caracalApiClient, never())
                .updateRateAssignment(
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void missingAssignmentIdPreventsMutation() {

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(Optional.of(List.of(
                        new RateAssignmentSummary(
                                null,
                                PRODUCT,
                                "VALUE",
                                RULE_SET
                        )
                )));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        assertTrue(
                exception.getMessage().contains("No assignment ID")
        );

        verify(caracalApiClient, never())
                .updateRateAssignment(
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void unsupportedAssignmentStatePreventsMutation() {

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(Optional.of(List.of(
                        assignment("UNKNOWN")
                )));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        assertTrue(
                exception.getMessage()
                        .contains("Unsupported current assignment state")
        );

        verify(caracalApiClient, never())
                .updateRateAssignment(
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void enablingWithoutPriceRuleSetIsRejected() {

        when(rule.isAvailable()).thenReturn(true);

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(Optional.of(List.of(
                        new RateAssignmentSummary(
                                ASSIGNMENT_ID,
                                PRODUCT,
                                "NOT_SOLD",
                                null
                        )
                )));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        assertTrue(
                exception.getMessage().contains("No price rule set")
        );

        verify(caracalApiClient, never())
                .updateRateAssignment(
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void allRulesAreValidatedBeforeAnyMutation() {

        AvailabilityRule invalidRule = createRule(2L, false);

        when(invalidRule.getRouteCode()).thenReturn("ROUTE-1");

        when(repository.findByTenantCodeAndPendingPublishTrue(TENANT))
                .thenReturn(List.of(rule, invalidRule));

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(
                        Optional.of(List.of(assignment("VALUE")))
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        verify(caracalApiClient, never())
                .updateRateAssignment(
                        anyString(),
                        anyString(),
                        anyString()
                );

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void mutationFailurePreventsFinalization() {

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(
                        Optional.of(List.of(assignment("VALUE")))
                );

        when(caracalApiClient.updateRateAssignment(
                TENANT,
                ASSIGNMENT_ID,
                "NOT_SOLD"))
                .thenThrow(
                        new IllegalStateException("Mutation failed")
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        verify(rule, never()).setPendingPublish(false);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void incorrectUpdatedStateFailsVerification() {

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(
                        Optional.of(List.of(assignment("VALUE"))),
                        Optional.of(List.of(assignment("VALUE")))
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        assertTrue(
                exception.getMessage()
                        .contains("could not be verified")
        );

        verify(rule, never()).setPendingPublish(false);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void changedAssignmentIdFailsVerification() {

        when(caracalApiClient.fetchRateTable(rateTableRequest()))
                .thenReturn(
                        Optional.of(List.of(assignment("VALUE"))),
                        Optional.of(List.of(
                                new RateAssignmentSummary(
                                        "different-assignment",
                                        PRODUCT,
                                        "NOT_SOLD",
                                        RULE_SET
                                )
                        ))
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingAvailabilityChanges(tenant)
        );

        assertTrue(
                exception.getMessage()
                        .contains("could not be verified")
        );

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void successfulCommitClearsPendingPublishFlag() {

        AvailabilityRule currentRule = createRule(1L, false);

        when(repository.findById(1L))
                .thenReturn(Optional.of(currentRule));

        service.commitPublishedRules(List.of(rule));

        var order = inOrder(repository, currentRule);

        order.verify(repository).findById(1L);

        order.verify(currentRule).setPendingPublish(false);

        order.verify(repository).saveAll(List.of(currentRule));
    }

    @Test
    void deletedRulePreventsCommit() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.commitPublishedRules(List.of(rule))
        );

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void modifiedTimestampPreventsCommit() {

        AvailabilityRule currentRule = createRule(1L, false);

        when(currentRule.getLastUpdated())
                .thenReturn(LAST_UPDATED.plusMinutes(1));

        when(repository.findById(1L))
                .thenReturn(Optional.of(currentRule));

        assertThrows(
                IllegalStateException.class,
                () -> service.commitPublishedRules(List.of(rule))
        );

        verify(currentRule, never()).setPendingPublish(false);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void changedAvailabilityPreventsCommit() {

        AvailabilityRule currentRule = createRule(1L, true);

        when(repository.findById(1L))
                .thenReturn(Optional.of(currentRule));

        assertThrows(
                IllegalStateException.class,
                () -> service.commitPublishedRules(List.of(rule))
        );

        verify(currentRule, never()).setPendingPublish(false);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void alreadyCommittedRulePreventsCommit() {

        AvailabilityRule currentRule = createRule(1L, false);

        when(currentRule.isPendingPublish()).thenReturn(false);

        when(repository.findById(1L))
                .thenReturn(Optional.of(currentRule));

        assertThrows(
                IllegalStateException.class,
                () -> service.commitPublishedRules(List.of(rule))
        );

        verify(currentRule, never()).setPendingPublish(false);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void invalidSecondRulePreventsAllLocalCommits() {

        AvailabilityRule secondRule = createRule(2L, false);
        AvailabilityRule currentFirstRule = createRule(1L, false);
        AvailabilityRule currentSecondRule = createRule(2L, true);

        when(repository.findById(1L))
                .thenReturn(Optional.of(currentFirstRule));

        when(repository.findById(2L))
                .thenReturn(Optional.of(currentSecondRule));

        assertThrows(
                IllegalStateException.class,
                () -> service.commitPublishedRules(
                        List.of(rule, secondRule)
                )
        );

        verify(currentFirstRule, never()).setPendingPublish(false);
        verify(currentSecondRule, never()).setPendingPublish(false);

        verify(repository, never()).saveAll(anyList());
    }
}
