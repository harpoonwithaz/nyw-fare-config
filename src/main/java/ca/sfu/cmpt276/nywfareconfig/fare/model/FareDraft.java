package ca.sfu.cmpt276.nywfareconfig.fare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;


// Maps to the PostgreSQL fare_drafts staging table.
// Supports both O-D inline edits and Zonal rate-tier operations (tierCode, targetStatus, actionType),
// while keeping actorEmail and originalPrice server-controlled for review diffs and Caracal's X-Actor audit header.
@Entity
@Table(name = "fare_drafts")
public class FareDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", nullable = false, length = 50)
    private String tenantCode;

    @Column(name = "actor_email", nullable = false, length = 100)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "grid_mode", nullable = false, length = 20)
    private GridMode gridMode;

    @Column(name = "origin_identifier", nullable = false, length = 50)
    private String originIdentifier; // Terminal code (e.g., "EDGEWTR") or Origin Zone (e.g., "ZONE_1")

    @Column(name = "dest_identifier", nullable = false, length = 50)
    private String destIdentifier; // Dest terminal (e.g., "MIDTOWN") or Dest Zone (e.g., "ZONE_2")

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode; // e.g., "ADULT_ONEWAY", "FULL_10TRIP"

    @Column(name = "tier_code", length = 50)
    private String tierCode; // Null in OD mode; e.g., "TIER_A" in ZONAL mode

    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice; // Null if cell was previously NOT_SOLD

    @Column(name = "draft_price", precision = 10, scale = 2)
    private BigDecimal draftPrice; // Null if targetStatus is NOT_SOLD

    @Enumerated(EnumType.STRING)
    @Column(name = "target_status", nullable = false, length = 30)
    private FareStatus targetStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private FareAction actionType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public FareDraft() {
    }

    public FareDraft(
            String tenantCode,
            String actorEmail,
            GridMode gridMode,
            String originIdentifier,
            String destIdentifier,
            String productCode,
            String tierCode,
            BigDecimal originalPrice,
            BigDecimal draftPrice,
            FareStatus targetStatus,
            FareAction actionType) {
        this.tenantCode = tenantCode;
        this.actorEmail = actorEmail;
        this.gridMode = gridMode;
        this.originIdentifier = originIdentifier;
        this.destIdentifier = destIdentifier;
        this.productCode = productCode;
        this.tierCode = tierCode;
        this.originalPrice = originalPrice;
        this.draftPrice = draftPrice;
        this.targetStatus = targetStatus;
        this.actionType = actionType;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Convenience helper for Epic 5 diff views (draftPrice - originalPrice).
     */
    public BigDecimal getPriceDelta() {
        if (originalPrice == null || draftPrice == null) {
            return null;
        }
        return draftPrice.subtract(originalPrice);
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

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public GridMode getMatrixMode() {
        return gridMode;
    }

    public void setMatrixMode(GridMode gridMode) {
        this.gridMode = gridMode;
    }

    public String getOriginIdentifier() {
        return originIdentifier;
    }

    public void setOriginIdentifier(String originIdentifier) {
        this.originIdentifier = originIdentifier;
    }

    public String getDestIdentifier() {
        return destIdentifier;
    }

    public void setDestIdentifier(String destIdentifier) {
        this.destIdentifier = destIdentifier;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getTierCode() {
        return tierCode;
    }

    public void setTierCode(String tierCode) {
        this.tierCode = tierCode;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getDraftPrice() {
        return draftPrice;
    }

    public void setDraftPrice(BigDecimal draftPrice) {
        this.draftPrice = draftPrice;
    }

    public FareStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(FareStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public FareAction getActionType() {
        return actionType;
    }

    public void setActionType(FareAction actionType) {
        this.actionType = actionType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}