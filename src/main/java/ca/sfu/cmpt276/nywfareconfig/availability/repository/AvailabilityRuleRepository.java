package ca.sfu.cmpt276.nywfareconfig.availability.repository;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {
    List<AvailabilityRule> findByTenantCode(String tenantCode);
    List<AvailabilityRule> findByTenantCodeAndPendingPublishTrue(String tenantCode);
    List<AvailabilityRule> findByTenantCodeAndSiteCode(String tenantCode, String siteCode);
    List<AvailabilityRule> findByTenantCodeAndSiteCodeAndRouteCode(String tenantCode, String siteCode, String routeCode);
    Optional<AvailabilityRule> findByTenantCodeAndSiteCodeAndRouteCodeAndProductCode(
            String tenantCode, String siteCode, String routeCode, String productCode);
}