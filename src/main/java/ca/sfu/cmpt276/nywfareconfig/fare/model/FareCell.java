package ca.sfu.cmpt276.nywfareconfig.fare.model;

import java.math.BigDecimal;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;

// Encapsulates the pricing and status state of an individual matrix intersection
public class FareCell {

    private BigDecimal price;
    private String formattedPrice;
    private FareStatus status;
    private boolean editable;
    private boolean hasPendingDraft;

    private String rowKey;
    private String colKey;

    public FareCell() {
    }

    // Overloaded constructor for non-priced baseline cells (NOT_SOLD, NA)
    public FareCell(FareStatus status, String rowKey, String colKey) {
        this(status, false, rowKey, colKey);
    }

    // Overloaded constructor for non-priced cells with draft state tracking
    public FareCell(FareStatus status, boolean hasPendingDraft, String rowKey, String colKey) {
        this.rowKey = rowKey;
        this.colKey = colKey;
        this.status = status;
        this.price = null;
        this.formattedPrice = null;
        this.editable = false;
        this.hasPendingDraft = hasPendingDraft;
    }

    // Existing constructor preserved for backward compatibility with tests
    public FareCell(
            BigDecimal price,
            String formattedPrice,
            FareStatus status,
            boolean editable,
            String rowKey,
            String colKey) {
        this(price, formattedPrice, status, editable, false, rowKey, colKey);
    }

    // Full constructor used by FareService.toFareCell(...)
    public FareCell(
            BigDecimal price,
            String formattedPrice,
            FareStatus status,
            boolean editable,
            boolean hasPendingDraft,
            String rowKey,
            String colKey) {
        this.price = price;
        this.formattedPrice = formattedPrice;
        this.status = status;
        this.editable = editable;
        this.hasPendingDraft = hasPendingDraft;
        this.rowKey = rowKey;
        this.colKey = colKey;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getFormattedPrice() {
        return formattedPrice;
    }

    public void setFormattedPrice(String formattedPrice) {
        this.formattedPrice = formattedPrice;
    }

    public FareStatus getStatus() {
        return status;
    }

    public void setStatus(FareStatus status) {
        this.status = status;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isHasPendingDraft() {
        return hasPendingDraft;
    }

    public void setHasPendingDraft(boolean hasPendingDraft) {
        this.hasPendingDraft = hasPendingDraft;
    }

    public boolean isPriced() {
        return price != null && status == FareStatus.ACTIVE;
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
}