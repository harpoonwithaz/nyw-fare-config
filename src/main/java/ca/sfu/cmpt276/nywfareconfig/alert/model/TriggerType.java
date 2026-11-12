package ca.sfu.cmpt276.nywfareconfig.alert.model;

public enum TriggerType {
    WEEKENDS("Weekends"),
    OUTSIDE_SERVICE_HOURS("Outside service hours"),
    BEFORE_START_DATE("Before the start date");

    private final String label;

    TriggerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isRequiresStartDate() {
        return this == BEFORE_START_DATE;
    }
}