package ca.sfu.cmpt276.nywfareconfig.fare.model;

// Represents a fare product and its price/status, such as active, NOT_SOLD, or N/A.
public class FareProduct {
    public enum Status {
        ACTIVE,
        NOT_SOLD,
        NA
    }

    private Double price;
    private Status status;
    
    public FareProduct(Double price, Status status) {
        this.price = price;
        this.status = status;
    }

    public Double getPrice() {
        return price;
    }

    public Status getStatus() {
        return status;
    }

    
}
