package ca.sfu.cmpt276.nywfareconfig.fare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareChangeRequest;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareRule;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.DraftStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareAction;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.repository.FareDraftRepository;
import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.ConfigurationChangeTracker;

@ExtendWith(MockitoExtension.class)
class FareDraftServiceTests {

    @Mock
    private FareDraftRepository fareDraftRepository;

    @Mock
    private FareService fareService;

    @Mock
    private ActorService actorService;

    private FareDraftService fareDraftService;
    private Tenant nywTenant;

    @BeforeEach
    void setUp() {
        fareDraftService = new FareDraftService(fareDraftRepository, fareService, actorService, new ConfigurationChangeTracker());
        nywTenant = new Tenant("NYW_SANDBOX", "NY Waterway", "USD", "en-US", GridMode.OD, true, List.of());
        lenient().when(actorService.currentActorHeader()).thenReturn("operator:operator@nyw.com");
    }

    private FareRule createBaselineRule(String dest, String prod, BigDecimal price, FareStatus status) {
        return FareRule.od(
                "NYW_SANDBOX",
                "EDGEWTR",
                dest,
                prod,
                "OD-ADULT_ONEWAY",
                price,
                status);
    }

    private void stubBaselineLookup(String dest, String prod, FareRule rule) {
        lenient().when(fareService.getBaselineFareRule(nywTenant, "EDGEWTR", dest, prod))
                .thenReturn(Optional.ofNullable(rule));
        lenient().when(fareService.getFareRule(nywTenant, "EDGEWTR", dest, prod))
                .thenReturn(Optional.ofNullable(rule));
    }

    @Test
    void stageChangesSavesNewDraftWithServerResolvedOriginalPriceAndActor() {
        FareRule baseline = createBaselineRule(
                "Midtown / W. 39th St", "Adult One-Way", new BigDecimal("13.25"), FareStatus.ACTIVE);
        when(fareDraftRepository.findByTenantCodeAndDraftStatus("NYW_SANDBOX", DraftStatus.PENDING))
                .thenReturn(List.of());
        stubBaselineLookup("Midtown / W. 39th St", "Adult One-Way", baseline);

        FareChangeRequest request = new FareChangeRequest(
                "NYW_SANDBOX",
                GridMode.OD,
                "EDGEWTR",
                "Midtown / W. 39th St",
                "Adult One-Way",
                new BigDecimal("14.50"),
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                null);

        int savedCount = fareDraftService.stageChanges(nywTenant, List.of(request));
        assertEquals(1, savedCount);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FareDraft>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(fareDraftRepository).saveAll(saveCaptor.capture());

        FareDraft savedDraft = saveCaptor.getValue().get(0);
        assertEquals("NYW_SANDBOX", savedDraft.getTenantCode());
        assertEquals("operator:operator@nyw.com", savedDraft.getActorEmail());
        assertEquals(new BigDecimal("13.25"), savedDraft.getOriginalPrice());
        assertEquals(new BigDecimal("14.50"), savedDraft.getDraftPrice());
        assertEquals(DraftStatus.PENDING, savedDraft.getDraftStatus());
    }

    @Test
    void stageChangesDeduplicatesRequestsForSameCellCoordinate() {
        FareRule baseline = createBaselineRule(
                "Midtown / W. 39th St", "Adult One-Way", new BigDecimal("13.25"), FareStatus.ACTIVE);
        when(fareDraftRepository.findByTenantCodeAndDraftStatus("NYW_SANDBOX", DraftStatus.PENDING))
                .thenReturn(List.of());
        stubBaselineLookup("Midtown / W. 39th St", "Adult One-Way", baseline);

        FareChangeRequest firstEdit = new FareChangeRequest(
                "NYW_SANDBOX", GridMode.OD, "EDGEWTR", "Midtown / W. 39th St", "Adult One-Way",
                new BigDecimal("14.00"), FareStatus.ACTIVE, FareAction.INLINE_EDIT, null);
        FareChangeRequest secondEditSameCell = new FareChangeRequest(
                "NYW_SANDBOX", GridMode.OD, "EDGEWTR", "Midtown / W. 39th St", "Adult One-Way",
                new BigDecimal("15.25"), FareStatus.ACTIVE, FareAction.INLINE_EDIT, null);

        int savedCount = fareDraftService.stageChanges(nywTenant, List.of(firstEdit, secondEditSameCell));
        assertEquals(1, savedCount);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FareDraft>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(fareDraftRepository).saveAll(saveCaptor.capture());
        assertEquals(new BigDecimal("15.25"), saveCaptor.getValue().get(0).getDraftPrice());
    }

    @Test
    void stageChangesPreservesOriginalPriceWhenUpdatingExistingPendingDraft() {
        FareRule baseline = createBaselineRule(
                "Midtown / W. 39th St", "Adult One-Way", new BigDecimal("13.25"), FareStatus.ACTIVE);
        FareDraft existingDraft = new FareDraft(
                "NYW_SANDBOX",
                "operator:previous@nyw.com",
                GridMode.OD,
                "EDGEWTR",
                "Midtown / W. 39th St",
                "Adult One-Way",
                null,
                new BigDecimal("13.25"),
                new BigDecimal("14.00"),
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                DraftStatus.PENDING);

        when(fareDraftRepository.findByTenantCodeAndDraftStatus("NYW_SANDBOX", DraftStatus.PENDING))
                .thenReturn(List.of(existingDraft));
        stubBaselineLookup("Midtown / W. 39th St", "Adult One-Way", baseline);

        FareChangeRequest secondEdit = new FareChangeRequest(
                "NYW_SANDBOX",
                GridMode.OD,
                "EDGEWTR",
                "Midtown / W. 39th St",
                "Adult One-Way",
                new BigDecimal("15.00"),
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                null);

        int savedCount = fareDraftService.stageChanges(nywTenant, List.of(secondEdit));
        assertEquals(1, savedCount);
        assertEquals(new BigDecimal("13.25"), existingDraft.getOriginalPrice());
        assertEquals(new BigDecimal("15.00"), existingDraft.getDraftPrice());
        assertEquals("operator:operator@nyw.com", existingDraft.getActorEmail());
    }

    @Test
    void stageChangesDeletesExistingDraftWhenEditedBackToBaselinePrice() {
        FareRule baseline = createBaselineRule(
                "Midtown / W. 39th St", "Adult One-Way", new BigDecimal("13.25"), FareStatus.ACTIVE);
        FareDraft existingDraft = new FareDraft(
                "NYW_SANDBOX",
                "operator:operator@nyw.com",
                GridMode.OD,
                "EDGEWTR",
                "Midtown / W. 39th St",
                "Adult One-Way",
                null,
                new BigDecimal("13.25"),
                new BigDecimal("14.00"),
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                DraftStatus.PENDING);

        when(fareDraftRepository.findByTenantCodeAndDraftStatus("NYW_SANDBOX", DraftStatus.PENDING))
                .thenReturn(List.of(existingDraft));
        stubBaselineLookup("Midtown / W. 39th St", "Adult One-Way", baseline);

        FareChangeRequest revertToBaseline = new FareChangeRequest(
                "NYW_SANDBOX",
                GridMode.OD,
                "EDGEWTR",
                "Midtown / W. 39th St",
                "Adult One-Way",
                new BigDecimal("13.25"),
                FareStatus.ACTIVE,
                FareAction.INLINE_EDIT,
                null);

        int savedCount = fareDraftService.stageChanges(nywTenant, List.of(revertToBaseline));
        assertEquals(0, savedCount);
        verify(fareDraftRepository).deleteAllInBatch(List.of(existingDraft));
        verify(fareDraftRepository, never()).saveAll(anyList());
    }

    @Test
    void stageChangesRejectsNaAndLockedNotSoldCellsInOdMode() {
        FareRule naRule = createBaselineRule("Pier 11 / Wall St", "Adult One-Way", null, FareStatus.NA);
        FareRule lockedSeniorRule = createBaselineRule("Midtown / W. 39th St", "Senior 65+", null, FareStatus.NOT_SOLD);

        when(fareDraftRepository.findByTenantCodeAndDraftStatus("NYW_SANDBOX", DraftStatus.PENDING))
                .thenReturn(List.of());
        stubBaselineLookup("Pier 11 / Wall St", "Adult One-Way", naRule);
        stubBaselineLookup("Midtown / W. 39th St", "Senior 65+", lockedSeniorRule);

        FareChangeRequest naRequest = new FareChangeRequest(
                "NYW_SANDBOX", GridMode.OD, "EDGEWTR", "Pier 11 / Wall St", "Adult One-Way",
                new BigDecimal("10.00"), FareStatus.ACTIVE, FareAction.INLINE_EDIT, null);
        FareChangeRequest lockedRequest = new FareChangeRequest(
                "NYW_SANDBOX", GridMode.OD, "EDGEWTR", "Midtown / W. 39th St", "Senior 65+",
                new BigDecimal("10.00"), FareStatus.ACTIVE, FareAction.INLINE_EDIT, null);

        assertThrows(IllegalArgumentException.class,
                () -> fareDraftService.stageChanges(nywTenant, List.of(naRequest)));
        assertThrows(IllegalArgumentException.class,
                () -> fareDraftService.stageChanges(nywTenant, List.of(lockedRequest)));
        verify(fareDraftRepository, never()).saveAll(anyList());
    }

    @Test
    void countPendingDraftsAndGetPendingDraftsValidateTenantInput() {
        assertEquals(0L, fareDraftService.countPendingDrafts(null));
        assertEquals(0L, fareDraftService.countPendingDrafts("   "));

        when(fareDraftRepository.countByTenantCodeAndDraftStatus("NYW_SANDBOX", DraftStatus.PENDING))
                .thenReturn(4L);
        assertEquals(4L, fareDraftService.countPendingDrafts("nyw_sandbox"));

        assertThrows(IllegalArgumentException.class, () -> fareDraftService.getPendingDrafts(""));
    }

    @Test
    void stageChangesWithPricingDateResolvesTimestampOncePerOriginAndValidatesTimeGatedFares() {
        String resolvedAt = "2026-10-24T10:00:00-04:00";

        when(actorService.currentActorHeader()).thenReturn("operator:operator@nyw.com");
        when(fareDraftRepository.findByTenantCodeAndDraftStatus("NYW_SANDBOX", DraftStatus.PENDING))
                .thenReturn(List.of());
        when(fareService.resolvePricingTimestamp("NYW_SANDBOX", "EDGEWTR", "2026-10-24"))
                .thenReturn(resolvedAt);

        FareRule midtownWeekend = FareRule.od(
                "NYW_SANDBOX", "EDGEWTR", "MIDTOWN", "WEEKEND_TICKET",
                "PRS_WKND", new BigDecimal("6.50"), FareStatus.ACTIVE);
        FareRule pier11Weekend = FareRule.od(
                "NYW_SANDBOX", "EDGEWTR", "PIER11", "WEEKEND_TICKET",
                "PRS_WKND", new BigDecimal("8.00"), FareStatus.ACTIVE);

        when(fareService.getBaselineFareRule(nywTenant, "EDGEWTR", "MIDTOWN", "WEEKEND_TICKET", resolvedAt))
                .thenReturn(Optional.of(midtownWeekend));
        when(fareService.getBaselineFareRule(nywTenant, "EDGEWTR", "PIER11", "WEEKEND_TICKET", resolvedAt))
                .thenReturn(Optional.of(pier11Weekend));

        List<FareChangeRequest> requests = List.of(
                new FareChangeRequest("NYW_SANDBOX", GridMode.OD, "EDGEWTR", "MIDTOWN",
                        "WEEKEND_TICKET", new BigDecimal("7.25"), FareStatus.ACTIVE, FareAction.INLINE_EDIT, null),
                new FareChangeRequest("NYW_SANDBOX", GridMode.OD, "EDGEWTR", "PIER11",
                        "WEEKEND_TICKET", new BigDecimal("8.75"), FareStatus.ACTIVE, FareAction.INLINE_EDIT, null));

        int savedCount = fareDraftService.stageChanges(nywTenant, requests, "2026-10-24");

        assertEquals(2, savedCount);
        verify(fareService, times(1)).resolvePricingTimestamp("NYW_SANDBOX", "EDGEWTR", "2026-10-24");
        verify(fareService).getBaselineFareRule(nywTenant, "EDGEWTR", "MIDTOWN", "WEEKEND_TICKET", resolvedAt);
        verify(fareService).getBaselineFareRule(nywTenant, "EDGEWTR", "PIER11", "WEEKEND_TICKET", resolvedAt);
        verify(fareDraftRepository).saveAll(anyList());
    }

    @Test
    void stageChangesWithBlankPricingDateUsesFourArgBaselineLookupWithoutResolvingTimestamp() {
        when(actorService.currentActorHeader()).thenReturn("operator:operator@nyw.com");
        when(fareDraftRepository.findByTenantCodeAndDraftStatus("NYW_SANDBOX", DraftStatus.PENDING))
                .thenReturn(List.of());

        FareRule baseline = FareRule.od(
                "NYW_SANDBOX", "EDGEWTR", "MIDTOWN", "ADULT_ONEWAY",
                "PRS_ADULT", new BigDecimal("13.25"), FareStatus.ACTIVE);
        when(fareService.getBaselineFareRule(nywTenant, "EDGEWTR", "MIDTOWN", "ADULT_ONEWAY"))
                .thenReturn(Optional.of(baseline));

        FareChangeRequest request = new FareChangeRequest(
                "NYW_SANDBOX", GridMode.OD, "EDGEWTR", "MIDTOWN",
                "ADULT_ONEWAY", new BigDecimal("14.00"), FareStatus.ACTIVE, FareAction.INLINE_EDIT, null);

        int savedCount = fareDraftService.stageChanges(nywTenant, List.of(request), "   ");

        assertEquals(1, savedCount);
        verify(fareService, never()).resolvePricingTimestamp(any(), any(), any());
        verify(fareService).getBaselineFareRule(nywTenant, "EDGEWTR", "MIDTOWN", "ADULT_ONEWAY");
    }
}