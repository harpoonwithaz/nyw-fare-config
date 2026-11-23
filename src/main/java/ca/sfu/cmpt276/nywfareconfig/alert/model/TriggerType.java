package ca.sfu.cmpt276.nywfareconfig.alert.model;

// Each trigger suggests a continue label (Story 4.5). [Day] / [día] is left for the operator to fill in.
public enum TriggerType {
    WEEKENDS("Weekends", "Buy for [Day]", "Comprar para [día]"),
    OUTSIDE_SERVICE_HOURS("Outside service hours", "Buy for [Day]", "Comprar para [día]"),
    BEFORE_START_DATE("Before the start date", "Buy now", "Comprar ahora");

    private final String label;
    private final String suggestedConfirmEn;
    private final String suggestedConfirmEs;

    TriggerType(String label, String suggestedConfirmEn, String suggestedConfirmEs) {
        this.label = label;
        this.suggestedConfirmEn = suggestedConfirmEn;
        this.suggestedConfirmEs = suggestedConfirmEs;
    }

    public String getLabel() {
        return label;
    }

    public String getSuggestedConfirmEn() {
        return suggestedConfirmEn;
    }

    public String getSuggestedConfirmEs() {
        return suggestedConfirmEs;
    }

    public boolean isRequiresStartDate() {
        return this == BEFORE_START_DATE;
    }
}
