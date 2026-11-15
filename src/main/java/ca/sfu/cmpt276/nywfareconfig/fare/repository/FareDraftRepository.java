package ca.sfu.cmpt276.nywfareconfig.fare.repository;

import ca.sfu.cmpt276.nywfareconfig.fare.model.DraftStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FareDraftRepository extends JpaRepository<FareDraft, Long> {
    List<FareDraft> findByDraftStatus(DraftStatus draftStatus);
    List<FareDraft> findByTenantCodeAndDraftStatus(String tenantCode, DraftStatus draftStatus);
}