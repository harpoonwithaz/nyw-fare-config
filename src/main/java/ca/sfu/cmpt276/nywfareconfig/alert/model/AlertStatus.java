package ca.sfu.cmpt276.nywfareconfig.alert.model;

public enum AlertStatus {
    DRAFT("Draft"),
    ACTIVE("Active"),
    ARCHIVED("Archived");

    private final String label;

    AlertStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}