package ca.sfu.cmpt276.nywfareconfig.fare.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;

public class FareRule {

    private String tenantCode;
    private GridMode gridMode;

    private String originIdentifier; // Terminal code ("EDGEWTR") or Origin Zone ("ZONE_1")
    private String destIdentifier; // Dest terminal ("MIDTOWN") or Dest Zone ("ZONE_2")
    private String productCode; // e.g., "ADULT_ONEWAY", "FULL_MONTHLY"

    // Caracal Rule Set & Assignment Metadata (used by Epic 3 & Epic 5)
    private String priceRuleSetName;
    private String matchedConditionName;

    // Zonal Rate Tier Abstraction (null in OD mode)
    private String tierCode;

    // Resolved Rate & Sellability State
    private BigDecimal price;
    private FareStatus status;
    private boolean hasPendingDraft;

    protected FareRule(
            String tenantCode,
            GridMode gridMode,
            String originIdentifier,
            String destIdentifier,
            String productCode,
            String priceRuleSetName,
            String matchedConditionName,
            String tierCode,
            BigDecimal price,
            FareStatus status,
            boolean hasPendingDraft) {
        this.tenantCode = tenantCode;
        this.gridMode = gridMode;
        this.originIdentifier = originIdentifier;
        this.destIdentifier = destIdentifier;
        this.productCode = productCode;
        this.priceRuleSetName = priceRuleSetName;
        this.matchedConditionName = matchedConditionName;
        this.tierCode = tierCode;
        this.price = price;
        this.status = status;
        this.hasPendingDraft = hasPendingDraft;
    }

    public static FareRule od(
            String tenantCode,
            String originSiteCode,
            String destSiteCode,
            String productCode,
            String priceRuleSetName,
            BigDecimal price,
            FareStatus status) {
        return od(
                tenantCode,
                originSiteCode,
                destSiteCode,
                productCode,
                priceRuleSetName,
                null,
                price,
                status);
    }

    public static FareRule od(
            String tenantCode,
            String originSiteCode,
            String destSiteCode,
            String productCode,
            String priceRuleSetName,
            String matchedConditionName,
            BigDecimal price,
            FareStatus status) {
        return new FareRule(
                tenantCode,
                GridMode.OD,
                originSiteCode,
                destSiteCode,
                productCode,
                priceRuleSetName,
                matchedConditionName,
                null,
                price,
                status,
                false);
    }

    public static FareRule zonal(
            String tenantCode,
            String originZone,
            String destZone,
            String productCode,
            String priceRuleSetName,
            String tierCode,
            BigDecimal price,
            FareStatus status) {
        return new FareRule(
                tenantCode,
                GridMode.ZONAL,
                originZone,
                destZone,
                productCode,
                priceRuleSetName,
                null,
                tierCode,
                price,
                status,
                false);
    }

    // =========================================================================
    // COMPUTED GETTERS (Derived from gridMode, tierCode, and price)
    // =========================================================================

    public String getOriginDimensionKey() {
        if (gridMode == null) {
            return null;
        }
        return gridMode == GridMode.ZONAL ? "ORIGIN_ZONE" : "ORIGIN";
    }

    public String getDestDimensionKey() {
        if (gridMode == null) {
            return null;
        }
        return gridMode == GridMode.ZONAL ? "DEST_ZONE" : "DEST";
    }

    public String getTierLabel() {
        if (tierCode == null || tierCode.isBlank()) {
            return null;
        }
        String tierSuffix = tierCode.replace("TIER_", "");
        if (price == null) {
            return "Tier " + tierSuffix;
        }
        BigDecimal scaled = price.setScale(2, RoundingMode.HALF_UP);
        return String.format("Tier %s ($%.2f)", tierSuffix, scaled);
    }

    // =========================================================================
    // STANDARD GETTERS & SETTERS
    // =========================================================================

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