package ca.sfu.cmpt276.nywfareconfig.fare.model;

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
