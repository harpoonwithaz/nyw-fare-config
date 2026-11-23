package ca.sfu.cmpt276.nywfareconfig.availability.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "availability_rules")
public class AvailabilityRule {

    public static final String ALL_ROUTES = "ALL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantCode;

    @Column(nullable = false)
    private String siteCode;

    @Column(nullable = false)
    private String routeCode = ALL_ROUTES;

    @Column(nullable = false)
    private String productCode;

    @Column(nullable = false)
    private boolean isAvailable;

    @Column(nullable = false)
    private boolean isPhasedOut = false;

    private String rateAssignmentId;

    private String rateTableName;

    private String priceRuleSetName;

    private String rateScheduleName;

    private String matchedCondition;

    private boolean syncedToCaracal = false;

    private String updatedBy;

    private LocalDateTime lastUpdated;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "temporal_condition_id")
    private TemporalCondition temporalCondition;

    public AvailabilityRule() {}

    public AvailabilityRule(String tenantCode, String siteCode, String productCode, boolean isAvailable) {
        this(tenantCode, siteCode, ALL_ROUTES, productCode, isAvailable);
    }

    public AvailabilityRule(String tenantCode, String siteCode, String routeCode, String productCode, boolean isAvailable) {
        this.tenantCode = tenantCode;
        this.siteCode = siteCode;
        this.routeCode = (routeCode == null || routeCode.isBlank()) ? ALL_ROUTES : routeCode;
        this.productCode = productCode;
        this.isAvailable = isAvailable;
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public String getRouteCode() { return routeCode != null && !routeCode.isBlank() ? routeCode : ALL_ROUTES; }
    public void setRouteCode(String routeCode) {
        this.routeCode = (routeCode == null || routeCode.isBlank()) ? ALL_ROUTES : routeCode;
    }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public boolean isAvailable() { return !isPhasedOut && isAvailable; }
    public void setAvailable(boolean available) {
        isAvailable = available;
        this.lastUpdated = LocalDateTime.now();
    }

    public boolean isPhasedOut() { return isPhasedOut; }
    public void setPhasedOut(boolean phasedOut) {
        isPhasedOut = phasedOut;
        if (phasedOut) {
            this.isAvailable = false;
        }
        this.lastUpdated = LocalDateTime.now();
    }

    public String getRateAssignmentId() { return rateAssignmentId; }
    public void setRateAssignmentId(String rateAssignmentId) { this.rateAssignmentId = rateAssignmentId; }

    public String getRateTableName() { return rateTableName; }
    public void setRateTableName(String rateTableName) { this.rateTableName = rateTableName; }

    public String getPriceRuleSetName() { return priceRuleSetName; }
    public void setPriceRuleSetName(String priceRuleSetName) { this.priceRuleSetName = priceRuleSetName; }

    public String getRateScheduleName() { return rateScheduleName; }
    public void setRateScheduleName(String rateScheduleName) { this.rateScheduleName = rateScheduleName; }

    public String getMatchedCondition() { return matchedCondition; }
    public void setMatchedCondition(String matchedCondition) { this.matchedCondition = matchedCondition; }

    public boolean isSyncedToCaracal() { return syncedToCaracal; }
    public void setSyncedToCaracal(boolean syncedToCaracal) { this.syncedToCaracal = syncedToCaracal; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public TemporalCondition getTemporalCondition() { return temporalCondition; }
    public void setTemporalCondition(TemporalCondition temporalCondition) { this.temporalCondition = temporalCondition; }
}