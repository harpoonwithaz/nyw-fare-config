package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareRule;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareDraftService;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

import tools.jackson.databind.ObjectMapper;

class FarePublicationServiceTest {

    private static final String TENANT = "NYW_SANDBOX";
    private static final String RULE_SET = "OD-CHILD_ONEWAY";
    private static final String PRODUCT = "CHILD_ONEWAY";

    private FareDraftService fareDraftService;
    private FareService fareService;
    private CaracalApiClient caracalApiClient;
    private FarePublicationService service;

    private Tenant tenant;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {

        fareDraftService = mock(FareDraftService.class);
        fareService = mock(FareService.class);
        caracalApiClient = mock(CaracalApiClient.class);

        tenant = mock(Tenant.class);

        when(tenant.getCode()).thenReturn(TENANT);
        when(tenant.getGridMode()).thenReturn(GridMode.OD);

        service = new FarePublicationService(
                fareDraftService,
                fareService,
                caracalApiClient
        );
    }

    private FareDraft draft(
            long id,
            String origin,
            String destination,
            String originalPrice,
            String draftPrice) {

        FareDraft draft = mock(FareDraft.class);

        when(draft.getId()).thenReturn(id);
        when(draft.getTenantCode()).thenReturn(TENANT);
        when(draft.getGridMode()).thenReturn(GridMode.OD);
        when(draft.getOriginIdentifier()).thenReturn(origin);
        when(draft.getDestIdentifier()).thenReturn(destination);
        when(draft.getProductCode()).thenReturn(PRODUCT);
        when(draft.getTargetStatus()).thenReturn(FareStatus.ACTIVE);

        when(draft.getOriginalPrice())
                .thenReturn(new BigDecimal(originalPrice));

        when(draft.getDraftPrice())
                .thenReturn(new BigDecimal(draftPrice));

        return draft;
    }

    private FareRule baseline(String price) {

        FareRule rule = mock(FareRule.class);

        when(rule.getPriceRuleSetName()).thenReturn(RULE_SET);
        when(rule.getStatus()).thenReturn(FareStatus.ACTIVE);
        when(rule.getPrice()).thenReturn(new BigDecimal(price));

        return rule;
    }

    private void mockBaseline(
            String origin,
            String destination,
            String originalPrice,
            String updatedPrice) {

        FareRule original = baseline(originalPrice);
        FareRule updated = baseline(updatedPrice);

        when(fareService.getBaselineFareRule(
                tenant, origin, destination, PRODUCT))
                .thenReturn(
                        Optional.of(original),
                        Optional.of(updated)
                );
    }

    private void mockSuccessfulMutation(
            String origin,
            String destination,
            String price) {

        when(caracalApiClient.updateOdMatrix(
                TENANT,
                RULE_SET,
                origin,
                destination,
                new BigDecimal(price)))
                .thenReturn(mapper.readTree(
                        "{\"rulesWritten\":1}"
                ));
    }

    @Test
    void multipleFareDraftsAreAppliedAndVerified() {

        FareDraft first = draft(
                1L, "EDGEWTR", "BROOKFD", "9.00", "8.50");

        FareDraft second = draft(
                2L, "BROOKFD", "EDGEWTR", "10.00", "9.50");

        List<FareDraft> drafts = List.of(first, second);

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(drafts);

        mockBaseline("EDGEWTR", "BROOKFD", "9.00", "8.50");
        mockBaseline("BROOKFD", "EDGEWTR", "10.00", "9.50");

        mockSuccessfulMutation("EDGEWTR", "BROOKFD", "8.50");
        mockSuccessfulMutation("BROOKFD", "EDGEWTR", "9.50");

        assertEquals(
                drafts,
                service.applyPendingFareChanges(tenant)
        );

        verify(caracalApiClient).updateOdMatrix(
                TENANT, RULE_SET, "EDGEWTR", "BROOKFD",
                new BigDecimal("8.50"));

        verify(caracalApiClient).updateOdMatrix(
                TENANT, RULE_SET, "BROOKFD", "EDGEWTR",
                new BigDecimal("9.50"));

        verify(fareDraftService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void conflictingPricesAreRejectedBeforeMutation() {

        FareDraft first = draft(
                1L, "EDGEWTR", "BROOKFD", "9.00", "8.50");

        FareDraft second = draft(
                2L, "EDGEWTR", "BROOKFD", "9.00", "7.50");

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(List.of(first, second));

        FareRule original = baseline("9.00");

        when(fareService.getBaselineFareRule(
                tenant, "EDGEWTR", "BROOKFD", PRODUCT))
                .thenReturn(Optional.of(original));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingFareChanges(tenant)
        );

        assertTrue(exception.getMessage()
                .contains("Conflicting prices"));

        verifyNoInteractions(caracalApiClient);
    }

    @Test
    void identicalPriceUpdatesAreDeduplicated() {

        FareDraft first = draft(
                1L, "EDGEWTR", "BROOKFD", "9.00", "8.50");

        FareDraft second = draft(
                2L, "EDGEWTR", "BROOKFD", "9.00", "8.50");

        List<FareDraft> drafts = List.of(first, second);

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(drafts);

        FareRule original = baseline("9.00");
        FareRule updated = baseline("8.50");

        when(fareService.getBaselineFareRule(
                tenant, "EDGEWTR", "BROOKFD", PRODUCT))
                .thenReturn(
                        Optional.of(original),
                        Optional.of(original),
                        Optional.of(updated),
                        Optional.of(updated)
                );

        mockSuccessfulMutation("EDGEWTR", "BROOKFD", "8.50");

        assertEquals(
                drafts,
                service.applyPendingFareChanges(tenant)
        );

        verify(caracalApiClient, times(1)).updateOdMatrix(
                TENANT, RULE_SET, "EDGEWTR", "BROOKFD",
                new BigDecimal("8.50"));
    }

    @Test
    void changedBaselinePricePreventsMutation() {

        FareDraft draft = draft(
                1L, "EDGEWTR", "BROOKFD", "9.00", "8.50");

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(List.of(draft));

        FareRule changed = baseline("10.00");

        when(fareService.getBaselineFareRule(
                tenant, "EDGEWTR", "BROOKFD", PRODUCT))
                .thenReturn(Optional.of(changed));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingFareChanges(tenant)
        );

        assertTrue(exception.getMessage()
                .contains("Baseline price has changed"));

        verifyNoInteractions(caracalApiClient);
    }

    @Test
    void alreadyAppliedFareDoesNotRequireAnotherMutation() {

        FareDraft draft = draft(
                1L, "EDGEWTR", "BROOKFD", "9.00", "8.50");

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(List.of(draft));

        FareRule updated = baseline("8.50");

        when(fareService.getBaselineFareRule(
                tenant, "EDGEWTR", "BROOKFD", PRODUCT))
                .thenReturn(Optional.of(updated));

        assertEquals(
                List.of(draft),
                service.applyPendingFareChanges(tenant)
        );

        verifyNoInteractions(caracalApiClient);
    }

    @Test
    void negativeDraftPriceIsRejected() {

        FareDraft draft = draft(
                1L, "EDGEWTR", "BROOKFD", "9.00", "-1.00");

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(List.of(draft));

        assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingFareChanges(tenant)
        );

        verifyNoInteractions(caracalApiClient);
    }

    @Test
    void missingBaselinePreventsMutation() {

        FareDraft draft = draft(
                1L, "EDGEWTR", "BROOKFD", "9.00", "8.50");

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(List.of(draft));

        when(fareService.getBaselineFareRule(
                tenant, "EDGEWTR", "BROOKFD", PRODUCT))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingFareChanges(tenant)
        );

        verifyNoInteractions(caracalApiClient);
    }

    @Test
    void unconfirmedMutationIsRejected() {

        FareDraft draft = draft(
                1L, "EDGEWTR", "BROOKFD", "9.00", "8.50");

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(List.of(draft));

        mockBaseline("EDGEWTR", "BROOKFD", "9.00", "8.50");

        when(caracalApiClient.updateOdMatrix(
                TENANT, RULE_SET, "EDGEWTR", "BROOKFD",
                new BigDecimal("8.50")))
                .thenReturn(mapper.readTree(
                        "{\"rulesWritten\":0}"
                ));

        assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingFareChanges(tenant)
        );

        verify(fareDraftService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void incorrectUpdatedPricePreventsVerification() {

        FareDraft draft = draft(
                1L, "EDGEWTR", "BROOKFD", "9.00", "8.50");

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(List.of(draft));

        mockBaseline("EDGEWTR", "BROOKFD", "9.00", "9.00");

        mockSuccessfulMutation("EDGEWTR", "BROOKFD", "8.50");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingFareChanges(tenant)
        );

        assertTrue(exception.getMessage()
                .contains("Updated fare could not be verified"));

        verify(fareDraftService, never())
                .commitPublishedDrafts(anyList());
    }

    @Test
    void nonSandboxTenantIsRejected() {

        when(tenant.getCode()).thenReturn("NYW");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.applyPendingFareChanges(tenant)
        );

        verifyNoInteractions(
                fareDraftService, fareService, caracalApiClient);
    }

    @Test
    void emptyDraftListRequiresNoMutations() {

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(List.of());

        assertTrue(
                service.applyPendingFareChanges(tenant).isEmpty()
        );

        verifyNoInteractions(fareService, caracalApiClient);
    }

    @Test
    void availabilityDraftRequiresSeparateHandling() {

        FareDraft draft = draft(
                1L, "EDGEWTR", "BROOKFD", "9.00", "8.50");

        when(draft.getTargetStatus())
                .thenReturn(FareStatus.NOT_SOLD);

        when(fareDraftService.getPendingDrafts(TENANT))
                .thenReturn(List.of(draft));

        assertThrows(
                IllegalStateException.class,
                () -> service.applyPendingFareChanges(tenant)
        );

        verifyNoInteractions(caracalApiClient);
    }

    @Test
    void commitPublishedDraftsDelegatesToDraftService() {

        FareDraft draft = mock(FareDraft.class);

        List<FareDraft> drafts = List.of(draft);

        service.commitPublishedDrafts(drafts);

        verify(fareDraftService).commitPublishedDrafts(drafts);
    }
}