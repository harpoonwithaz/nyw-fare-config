package ca.sfu.cmpt276.nywfareconfig.fare.model;

import java.math.BigDecimal;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;

// Encapsulates the pricing and status state of an individual matrix intersection
public class FareCell {

    private BigDecimal price;
    private FareStatus status;
    private boolean hasPendingDraft;

    private String rowKey;
    private String colKey;

    private String tierCode;

    public FareCell() {
    }

    public FareCell(
            BigDecimal price,
            FareStatus status,
            boolean hasPendingDraft,
            String rowKey,
            String colKey) {
        this(price, status, hasPendingDraft, rowKey, colKey, null);
    }

    // 6-arg constructor used by FareService.toFareCell(...)
    public FareCell(
            BigDecimal price,
            FareStatus status,
            boolean hasPendingDraft,
            String rowKey,
            String colKey,
            String tierCode) {
        this.price = price;
        this.status = status;
        this.hasPendingDraft = hasPendingDraft;
        this.rowKey = rowKey;
        this.colKey = colKey;
        this.tierCode = tierCode;
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

    public String getRowKey() {
        return rowKey;
    }

    public void setRowKey(String rowKey) {
        this.rowKey = rowKey;
    }

    public String getColKey() {
        return colKey;
    }

    public void setColKey(String colKey) {
        this.colKey = colKey;
    }

    // Methods used by thymeleaf
    public boolean isEditable() {
        return status == FareStatus.ACTIVE && price != null;
    }

    public String getFormattedPrice() {
        return price == null ? null : String.format("$%.2f", price);
    }

    public boolean isActive() {
        return status == FareStatus.ACTIVE;
    }
}