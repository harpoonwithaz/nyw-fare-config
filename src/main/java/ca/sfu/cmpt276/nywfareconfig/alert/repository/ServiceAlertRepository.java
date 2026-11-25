package ca.sfu.cmpt276.nywfareconfig.alert.repository;

import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertStatus;
import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ServiceAlertRepository extends JpaRepository<ServiceAlert, Long> {
    List<ServiceAlert> findByTenantCode(String tenantCode, Sort sort);

    Optional<ServiceAlert> findByIdAndTenantCode(Long id, String tenantCode);

    // Row lock (SELECT ... FOR UPDATE) so two people can't save, activate or archive the same alert at once
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ServiceAlert a where a.id = :id and a.tenantCode = :tenantCode")
    Optional<ServiceAlert> lockByIdAndTenantCode(Long id, String tenantCode);

    Optional<ServiceAlert> findFirstByReplacesAlertIdAndTenantCodeAndStatus(Long replacesAlertId, String tenantCode,
            AlertStatus status);
}