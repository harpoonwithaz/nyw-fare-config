package ca.sfu.cmpt276.nywfareconfig.alert.repository;

import ca.sfu.cmpt276.nywfareconfig.alert.model.RouteSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteScheduleRepository extends JpaRepository<RouteSchedule, Long> {
    List<RouteSchedule> findByTenantCode(String tenantCode);

    Optional<RouteSchedule> findByTenantCodeAndOriginCodeAndDestinationCode(String tenantCode, String originCode,
            String destinationCode);
}