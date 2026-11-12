package ca.sfu.cmpt276.nywfareconfig.availability.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "availability_rules")
public class AvailabilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantCode;

    @Column(nullable = false)
    private String siteCode;

    @Column(nullable = false)
    private String productCode;

    @Column(nullable = false)
    private boolean isAvailable;

    private LocalDateTime lastUpdated;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "temporal_condition_id")
    private TemporalCondition temporalCondition;

    public AvailabilityRule() {}

    public AvailabilityRule(String tenantCode, String siteCode, String productCode, boolean isAvailable) {
        this.tenantCode = tenantCode;
        this.siteCode = siteCode;
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

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { 
        isAvailable = available; 
        this.lastUpdated = LocalDateTime.now();
    }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public TemporalCondition getTemporalCondition() { return temporalCondition; }
    public void setTemporalCondition(TemporalCondition temporalCondition) { this.temporalCondition = temporalCondition; }
}