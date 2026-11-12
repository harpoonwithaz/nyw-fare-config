package ca.sfu.cmpt276.nywfareconfig.fare.dto;

import java.math.BigDecimal;

import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;

public class FareRule {
    private String tenantCode;
    private GridMode gridMode;

    // Caracal Dimensional Pricing Keys (aligned with /functions/od-matrix-set)
    private String originDimensionKey;  // "ORIGIN" (OD) or "ORIGIN_ZONE" (ZONAL)
    private String destDimensionKey;    // "DEST" (OD) or "DEST_ZONE" (ZONAL)
    private String originIdentifier;    // Terminal code ("EDGEWTR") or Origin Zone ("ZONE_1")
    private String destIdentifier;      // Dest terminal ("MIDTOWN") or Dest Zone ("ZONE_2")
    private String productCode;         // e.g., "ADULT_ONEWAY", "FULL_MONTHLY"

    // Caracal Rule Set & Assignment Metadata (used by Epic 3 & Epic 5)
    private String priceRuleSetName;    // e.g., "OD-ADULT_ONEWAY" (from price-simulate / rate-table-get)
    private String rateAssignmentId;    // UUID from rate-table-get
    private String matchedConditionName;// Optional WhenCondition name (e.g., "MONTHLY_SALE_20_TO_10")

    // Zonal Rate Tier Abstraction (null in OD mode; populated in ZONAL mode)
    private String tierCode;            // e.g., "TIER_A", "TIER_B"
    private String tierLabel;           // e.g., "Tier A ($4.90)" for UI badges/chips

    // Resolved Rate & Sellability State
    private BigDecimal price;           // Effective price (staged draft if present, else Caracal baseline)
    private FareStatus status;              // "VALUE", "NOT_SOLD", "CONDITIONAL", or "NA"
    private boolean hasPendingDraft;    // True if overlaid by an uncommitted FareDraft
    public FareRule(String tenantCode, GridMode gridMode, String originDimensionKey, String destDimensionKey,
            String originIdentifier, String destIdentifier, String productCode, String priceRuleSetName,
            String rateAssignmentId, String matchedConditionName, String tierCode, String tierLabel, BigDecimal price,
            FareStatus status, boolean hasPendingDraft) {
        this.tenantCode = tenantCode;
        this.gridMode = gridMode;
        this.originDimensionKey = originDimensionKey;
        this.destDimensionKey = destDimensionKey;
        this.originIdentifier = originIdentifier;
        this.destIdentifier = destIdentifier;
        this.productCode = productCode;
        this.priceRuleSetName = priceRuleSetName;
        this.rateAssignmentId = rateAssignmentId;
        this.matchedConditionName = matchedConditionName;
        this.tierCode = tierCode;
        this.tierLabel = tierLabel;
        this.price = price;
        this.status = status;
        this.hasPendingDraft = hasPendingDraft;
    }
    public String getTenantCode() {
        return tenantCode;
    }
    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }
    public GridMode getGridMode() {
        return gridMode;
    }
    public void setGridMode(GridMode gridMode) {
        this.gridMode = gridMode;
    }
    public String getOriginDimensionKey() {
        return originDimensionKey;
    }
    public void setOriginDimensionKey(String originDimensionKey) {
        this.originDimensionKey = originDimensionKey;
    }
    public String getDestDimensionKey() {
        return destDimensionKey;
    }
    public void setDestDimensionKey(String destDimensionKey) {
        this.destDimensionKey = destDimensionKey;
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
    public String getPriceRuleSetName() {
        return priceRuleSetName;
    }
    public void setPriceRuleSetName(String priceRuleSetName) {
        this.priceRuleSetName = priceRuleSetName;
    }
    public String getRateAssignmentId() {
        return rateAssignmentId;
    }
    public void setRateAssignmentId(String rateAssignmentId) {
        this.rateAssignmentId = rateAssignmentId;
    }
    public String getMatchedConditionName() {
        return matchedConditionName;
    }
    public void setMatchedConditionName(String matchedConditionName) {
        this.matchedConditionName = matchedConditionName;
    }
    public String getTierCode() {
        return tierCode;
    }
    public void setTierCode(String tierCode) {
        this.tierCode = tierCode;
    }
    public String getTierLabel() {
        return tierLabel;
    }
    public void setTierLabel(String tierLabel) {
        this.tierLabel = tierLabel;
    }
    public BigDecimal getPrice() {
        return price;
    }
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    public FareStatus getStatus() {
        return status;
    }
    public void setStatus(FareStatus status) {
        this.status = status;
    }
    public boolean isHasPendingDraft() {
        return hasPendingDraft;
    }
    public void setHasPendingDraft(boolean hasPendingDraft) {
        this.hasPendingDraft = hasPendingDraft;
    }
}
