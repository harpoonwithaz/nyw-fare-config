package ca.sfu.cmpt276.nywfareconfig.publishing.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fare_drafts")
public class FareDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantCode;

    @Column(name = "change_type", nullable = false)
    private String changeType; // e.g., "AVAILABILITY_TOGGLE" or "PRICE_UPDATE"

    @Column(nullable = false)
    private String origin; // siteCode e.g., "MIDTOWN", "EDGEWTR"

    @Column(nullable = false)
    private String productCode;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private String status; // "STAGED", "PUBLISHED"

    private LocalDateTime createdAt;

    public FareDraft() {}

    public FareDraft(String tenantCode, String changeType, String origin, String productCode, String summary) {
        this.tenantCode = tenantCode;
        this.changeType = changeType;
        this.origin = origin;
        this.productCode = productCode;
        this.summary = summary;
        this.status = "STAGED";
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

