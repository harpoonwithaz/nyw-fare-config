package ca.sfu.cmpt276.nywfareconfig.availability.repository;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {

    List<AvailabilityRule> findByTenantCode(String tenantCode);

    List<AvailabilityRule> findByTenantCodeAndSiteCode(String tenantCode, String siteCode);

    List<AvailabilityRule> findByTenantCodeAndSiteCodeAndRouteCode(
            String tenantCode, String siteCode, String routeCode);

    Optional<AvailabilityRule> findByTenantCodeAndSiteCodeAndProductCode(
            String tenantCode, String siteCode, String productCode);

    Optional<AvailabilityRule> findByTenantCodeAndSiteCodeAndRouteCodeAndProductCode(
            String tenantCode, String siteCode, String routeCode, String productCode);

    List<AvailabilityRule> findByTenantCodeAndProductCode(String tenantCode, String productCode);

    List<AvailabilityRule> findByTenantCodeAndSyncedToCaracalFalse(String tenantCode);
}