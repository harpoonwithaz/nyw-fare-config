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
    private String rateAssignmentId;    // UUID from rate-table-get (required by Epic 3's rate-assignment-update)
    private String matchedConditionName;// Optional WhenCondition name (e.g., "MONTHLY_SALE_20_TO_10")

    // Zonal Rate Tier Abstraction (null in OD mode; populated in ZONAL mode)
    private String tierCode;            // e.g., "TIER_A", "TIER_B"
    private String tierLabel;           // e.g., "Tier A ($4.90)" for UI badges/chips

    // Resolved Rate & Sellability State
    private BigDecimal price;           // Effective price (staged draft if present, else Caracal baseline)
    private String status;              // "VALUE", "NOT_SOLD", "CONDITIONAL", or "NA"
    private boolean hasPendingDraft;    // True if overlaid by an uncommitted FareDraft

    // Constructors, Getters, and Setters
}
