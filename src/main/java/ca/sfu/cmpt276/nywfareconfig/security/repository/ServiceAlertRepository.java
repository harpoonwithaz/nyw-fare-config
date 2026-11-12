package ca.sfu.cmpt276.nywfareconfig.alert.repository;

import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceAlertRepository extends JpaRepository<ServiceAlert, Long> {
    List<ServiceAlert> findByTenantId(String tenantId, Sort sort);

    Optional<ServiceAlert> findByIdAndTenantId(Long id, String tenantId);
}