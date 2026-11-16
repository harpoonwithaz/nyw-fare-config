package ca.sfu.cmpt276.nywfareconfig.fare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.sfu.cmpt276.nywfareconfig.fare.model.DraftStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;

public interface FareDraftRepository extends JpaRepository<FareDraft, Long> {
    // Prefetches all active PENDING drafts for a tenant in a single SELECT query.
    List<FareDraft> findByTenantCodeAndDraftStatus(String tenantCode, DraftStatus draftStatus);

    Optional<FareDraft> findByTenantCodeAndOriginIdentifierAndDestIdentifierAndProductCodeAndDraftStatus(
            String tenantCode,
            String originIdentifier,
            String destIdentifier,
            String productCode,
            DraftStatus draftStatus);

    long countByTenantCodeAndDraftStatus(String tenantCode, DraftStatus draftStatus);
}
