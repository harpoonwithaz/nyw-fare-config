package ca.sfu.cmpt276.nywfareconfig.alert.model;

import java.util.Map;

// Each trigger suggests a continue label per language (Story 4.5). [Day] / [día] is left for the operator to fill in.
public enum TriggerType {
    WEEKENDS("Weekends", Map.of("en", "Buy for [Day]", "es", "Comprar para [día]")),
    OUTSIDE_SERVICE_HOURS("Outside service hours", Map.of("en", "Buy for [Day]", "es", "Comprar para [día]")),
    BEFORE_START_DATE("Before the start date", Map.of("en", "Buy now", "es", "Comprar ahora"));

    private final String label;
    private final Map<String, String> suggestedConfirm;

    TriggerType(String label, Map<String, String> suggestedConfirm) {
        this.label = label;
        this.suggestedConfirm = suggestedConfirm;
    }

    public String getLabel() {
        return label;
    }

    public Map<String, String> getSuggestedConfirm() {
        return suggestedConfirm;
    }

    public String getSuggestedConfirm(String language) {
        return suggestedConfirm.get(language);
    }

    public boolean isRequiresStartDate() {
        return this == BEFORE_START_DATE;
    }
}
