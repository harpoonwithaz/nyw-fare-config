package ca.sfu.cmpt276.nywfareconfig.publishing.repository;

import ca.sfu.cmpt276.nywfareconfig.publishing.model.FareDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FareDraftRepository extends JpaRepository<FareDraft, Long> {
    List<FareDraft> findByStatus(String status);
    List<FareDraft> findByTenantCodeAndStatus(String tenantCode, String status);
}