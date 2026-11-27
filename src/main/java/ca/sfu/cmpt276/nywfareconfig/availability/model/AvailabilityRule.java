package ca.sfu.cmpt276.nywfareconfig.availability.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "availability_rules",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_availability_tenant_site_route_product",
            columnNames = {"tenant_code", "site_code", "route_code", "product_code"}
        )
    }
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

    @Column(name = "route_code", nullable = false, columnDefinition = "varchar(255) not null default 'ALL'")
    private String routeCode = ALL_ROUTES;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

    @Column(name = "phased_out", nullable = false, columnDefinition = "boolean not null default false")
    private boolean phasedOut = false;

    @Column(name = "pending_publish", nullable = false, columnDefinition = "boolean not null default true")
    private boolean pendingPublish = true;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "temporal_condition_id", referencedColumnName = "id")
    private TemporalCondition temporalCondition;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "updated_by")
    private String updatedBy;

    public AvailabilityRule() {
    }

    public AvailabilityRule(String tenantCode, String siteCode, String productCode, boolean isAvailable) {
        this(tenantCode, siteCode, ALL_ROUTES, productCode, isAvailable);
    }

    public AvailabilityRule(String tenantCode, String siteCode, String routeCode, String productCode, boolean isAvailable) {
        this.tenantCode = tenantCode;
        this.siteCode = siteCode;
        this.routeCode = (routeCode == null || routeCode.isBlank()) ? ALL_ROUTES : routeCode;
        this.productCode = productCode;
        this.isAvailable = isAvailable;
        this.pendingPublish = true;
    }

    @PrePersist
    @PreUpdate
    public void onSave() {
        if (this.routeCode == null || this.routeCode.isBlank()) {
            this.routeCode = ALL_ROUTES;
        }
        this.lastUpdated = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public boolean isPhasedOut() {
        return phasedOut;
    }

    public void setPhasedOut(boolean phasedOut) {
        this.phasedOut = phasedOut;
    }

    public boolean isPendingPublish() {
        return pendingPublish;
    }

    public void setPendingPublish(boolean pendingPublish) {
        this.pendingPublish = pendingPublish;
    }

    public TemporalCondition getTemporalCondition() {
        return temporalCondition;
    }

    public void setTemporalCondition(TemporalCondition temporalCondition) {
        this.temporalCondition = temporalCondition;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}