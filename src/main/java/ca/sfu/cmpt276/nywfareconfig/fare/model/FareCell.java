package ca.sfu.cmpt276.nywfareconfig.fare.model;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;

// Encapsulates the pricing and status state of an individual matrix intersection
public class FareCell {
    
    // TODO: Change this to a BigDecimal type to avoid floating point errors
    private Double price;
    private String formattedPrice;
    private FareStatus status;
    private boolean editable;

    private String rowKey;
    private String colKey;

    public FareCell() {
    }

    // Overloaded constructor for non-priced cells (NOT_SOLD, NA)
    public FareCell(FareStatus status, String rowKey, String colKey) {
        this.rowKey = rowKey;
        this.colKey = colKey;
        this.status = status;
        this.price = null;
        this.formattedPrice = null;
        this.editable = false;
    }

    public FareCell(Double price, String formattedPrice, FareStatus status, boolean editable, String rowKey,
            String colKey) {
        this.price = price;
        this.formattedPrice = formattedPrice;
        this.status = status;
        this.editable = editable;
        this.rowKey = rowKey;
        this.colKey = colKey;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
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

    public boolean isPriced() {
        return price != null && status == FareStatus.ACTIVE;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
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
