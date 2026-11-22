package ca.sfu.cmpt276.nywfareconfig.fare.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareChangeRequest;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareRule;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.DraftStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.repository.FareDraftRepository;
import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

// Manages the persistence lifecycle of FareDraft entities in PostgreSQL,
// including upserting modified cells, computing old-vs-new price deltas,
// reverting individual/all drafts, and transitioning PENDING rows to COMMITTED.
@Service
public class FareDraftService {

    private final FareDraftRepository fareDraftRepository;
    private final FareService fareService;
    private final ActorService actorService;

    public FareDraftService(
            FareDraftRepository fareDraftRepository,
            FareService fareService,
            ActorService actorService) {
        this.fareDraftRepository = fareDraftRepository;
        this.fareService = fareService;
        this.actorService = actorService;
    }

    @Transactional(readOnly = true)
    public List<FareDraft> getPendingDrafts(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException("tenantCode is required.");
        }
        return fareDraftRepository.findByTenantCodeAndDraftStatus(
            tenantCode.toUpperCase(), DraftStatus.PENDING);
    }

    // Validates against baseline rules, resolves server-side originalPrice and
    // actorEmail,
    // and batch-upserts (or self-cancels) FareDraft rows in PostgreSQL.
    @Transactional
    public int stageChanges(Tenant activeTenant, List<FareChangeRequest> requests) {
        if (activeTenant == null || activeTenant.getCode() == null || activeTenant.getCode().isBlank()) {
            throw new IllegalArgumentException("An active tenant must be selected before staging fare changes.");
        }
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one fare change is required.");
        }

        String tenantCode = activeTenant.getCode();
        GridMode gridMode = activeTenant.getGridMode();
        String actorEmail = actorService.currentActorHeader();

        // Deduplicates incoming request list by (origin, dest, product)
        Map<String, FareChangeRequest> deduplicatedRequests = new LinkedHashMap<>();
        for (FareChangeRequest req : requests) {
            String key = coordinateKey(req.getOriginIdentifier(), req.getDestIdentifier(), req.getProductCode());
            deduplicatedRequests.put(key, req);
        }

        // loads all PENDING FareDrafts for tenant into HashMap
        List<FareDraft> existingPending = fareDraftRepository.findByTenantCodeAndDraftStatus(
                tenantCode, DraftStatus.PENDING);
        Map<String, FareDraft> existingByCoord = new HashMap<>();
        for (FareDraft draft : existingPending) {
            String key = coordinateKey(
                    draft.getOriginIdentifier(),
                    draft.getDestIdentifier(),
                    draft.getProductCode());
            existingByCoord.put(key, draft);
        }

        List<FareDraft> toSave = new ArrayList<>();
        List<FareDraft> toDelete = new ArrayList<>();

        // Resolve baseline prices in memory and build batch upsert/delete lists
        for (FareChangeRequest req : deduplicatedRequests.values()) {
            FareRule baselineRule = fareService.getBaselineFareRule(
                    activeTenant,
                    req.getOriginIdentifier(),
                    req.getDestIdentifier(),
                    req.getProductCode())
                    .orElseThrow(() -> new IllegalArgumentException(String.format(
                            "Invalid fare coordinate: %s -> %s (%s)",
                            req.getOriginIdentifier(), req.getDestIdentifier(), req.getProductCode())));

            if (baselineRule.getStatus() == FareStatus.NA) {
                throw new IllegalArgumentException(String.format(
                        "Route %s -> %s (%s) is N/A and cannot be priced.",
                        baselineRule.getOriginIdentifier(),
                        baselineRule.getDestIdentifier(),
                        baselineRule.getProductCode()));
            }

            if (gridMode == GridMode.OD && baselineRule.getStatus() == FareStatus.NOT_SOLD) {
                throw new IllegalArgumentException(String.format(
                        "Product %s at %s is NOT_SOLD and cannot be edited in O-D mode.",
                        baselineRule.getProductCode(), baselineRule.getOriginIdentifier()));
            }

            String coordKey = coordinateKey(
                    baselineRule.getOriginIdentifier(),
                    baselineRule.getDestIdentifier(),
                    baselineRule.getProductCode());
            FareDraft existingDraft = existingByCoord.get(coordKey);

            // Preserve originalPrice if a PENDING draft already exists; otherwise use
            // baseline
            BigDecimal originalPrice = (existingDraft != null)
                    ? existingDraft.getOriginalPrice()
                    : baselineRule.getPrice();

            BigDecimal normalizedNewPrice = (req.getTargetStatus() == FareStatus.NOT_SOLD || req.getNewPrice() == null)
                    ? null
                    : req.getNewPrice().setScale(2, RoundingMode.HALF_UP);

            String resolvedTierCode = (req.getTierCode() != null && !req.getTierCode().isBlank())
                    ? req.getTierCode().trim()
                    : baselineRule.getTierCode();

            // Self-canceling edit guard: if edited back to baseline price and status,
            // delete existing draft
            if (isSamePrice(normalizedNewPrice, originalPrice)
                    && req.getTargetStatus() == baselineRule.getStatus()) {
                if (existingDraft != null) {
                    toDelete.add(existingDraft);
                }
                continue;
            }

            if (existingDraft != null) {
                existingDraft.setActorEmail(actorEmail);
                existingDraft.setGridMode(gridMode);
                existingDraft.setTierCode(resolvedTierCode);
                existingDraft.setTargetStatus(req.getTargetStatus());
                existingDraft.setDraftPrice(normalizedNewPrice);
                existingDraft.setActionType(req.getActionType());
                existingDraft.setDraftStatus(DraftStatus.PENDING);
                toSave.add(existingDraft);
            } else {
                FareDraft newDraft = new FareDraft(
                        tenantCode,
                        actorEmail,
                        gridMode,
                        baselineRule.getOriginIdentifier(),
                        baselineRule.getDestIdentifier(),
                        baselineRule.getProductCode(),
                        resolvedTierCode,
                        originalPrice,
                        normalizedNewPrice,
                        req.getTargetStatus(),
                        req.getActionType(),
                        DraftStatus.PENDING);
                toSave.add(newDraft);
            }
        }

        if (!toDelete.isEmpty()) {
            fareDraftRepository.deleteAllInBatch(toDelete);
        }
        if (!toSave.isEmpty()) {
            fareDraftRepository.saveAll(toSave);
        }

        return toSave.size();
    }

    @Transactional(readOnly = true)
    public long countPendingDrafts(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return 0L;
        }
        return fareDraftRepository.countByTenantCodeAndDraftStatus(
                tenantCode.toUpperCase(), DraftStatus.PENDING);
    }

    private boolean isSamePrice(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    private String coordinateKey(String origin, String dest, String product) {
        return origin.trim().toUpperCase()
                + "::" + dest.trim().toUpperCase()
                + "::" + product.trim().toUpperCase();
    }
}
