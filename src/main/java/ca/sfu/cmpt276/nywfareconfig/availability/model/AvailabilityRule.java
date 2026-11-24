package ca.sfu.cmpt276.nywfareconfig.availability.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "availability_rules",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"tenant_code", "site_code", "route_code", "product_code"}
    )
)
public class AvailabilityRule {

    public static final String ALL_ROUTES = "ALL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", nullable = false)
    private String tenantCode;

    @Column(name = "site_code", nullable = false)
    private String siteCode;

    @Column(name = "route_code", nullable = false, columnDefinition = "varchar(255) default 'ALL'")
    private String routeCode = ALL_ROUTES;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "is_available", nullable = false, columnDefinition = "boolean default true")
    private boolean available = true;

    @Column(name = "phased_out", nullable = false, columnDefinition = "boolean default false")
    private boolean phasedOut = false;

    @Column(name = "rate_table_name")
    private String rateTableName;

    @Column(name = "rate_assignment_id")
    private String rateAssignmentId;

    @Column(name = "price_rule_set_name")
    private String priceRuleSetName;

    @Column(name = "rate_schedule_name")
    private String rateScheduleName;

    @Column(name = "matched_condition")
    private String matchedCondition;

    @Column(name = "synced_to_caracal", nullable = false, columnDefinition = "boolean default false")
    private boolean syncedToCaracal = false;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "temporal_condition_id")
    private TemporalCondition temporalCondition;

    public AvailabilityRule() {
    }

    public AvailabilityRule(String tenantCode, String siteCode, String productCode, boolean available) {
        this(tenantCode, siteCode, ALL_ROUTES, productCode, available);
    }

    public AvailabilityRule(
            String tenantCode,
            String siteCode,
            String routeCode,
            String productCode,
            boolean available) {
        this.tenantCode = tenantCode;
        this.siteCode = siteCode;
        this.routeCode = (routeCode == null || routeCode.isBlank()) ? ALL_ROUTES : routeCode;
        this.productCode = productCode;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getSiteCode() {
        return siteCode;
    }

    public void setSiteCode(String siteCode) {
        this.siteCode = siteCode;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public void setRouteCode(String routeCode) {
        this.routeCode = (routeCode == null || routeCode.isBlank()) ? ALL_ROUTES : routeCode;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isPhasedOut() {
        return phasedOut;
    }

    public void setPhasedOut(boolean phasedOut) {
        this.phasedOut = phasedOut;
    }

    public String getRateTableName() {
        return rateTableName;
    }

    public void setRateTableName(String rateTableName) {
        this.rateTableName = rateTableName;
    }

    public String getRateAssignmentId() {
        return rateAssignmentId;
    }

    public void setRateAssignmentId(String rateAssignmentId) {
        this.rateAssignmentId = rateAssignmentId;
    }

    public String getPriceRuleSetName() {
        return priceRuleSetName;
    }

    public void setPriceRuleSetName(String priceRuleSetName) {
        this.priceRuleSetName = priceRuleSetName;
    }

    public String getRateScheduleName() {
        return rateScheduleName;
    }

    public void setRateScheduleName(String rateScheduleName) {
        this.rateScheduleName = rateScheduleName;
    }

    public String getMatchedCondition() {
        return matchedCondition;
    }

    public void setMatchedCondition(String matchedCondition) {
        this.matchedCondition = matchedCondition;
    }

    public boolean isSyncedToCaracal() {
        return syncedToCaracal;
    }

    public void setSyncedToCaracal(boolean syncedToCaracal) {
        this.syncedToCaracal = syncedToCaracal;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public TemporalCondition getTemporalCondition() {
        return temporalCondition;
    }

    public void setTemporalCondition(TemporalCondition temporalCondition) {
        this.temporalCondition = temporalCondition;
    }
}