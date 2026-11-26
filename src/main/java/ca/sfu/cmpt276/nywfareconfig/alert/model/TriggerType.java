package ca.sfu.cmpt276.nywfareconfig.alert.model;

import java.util.Map;

// Each trigger suggests a continue label per language (Story 4.5); a language without one gets no suggestion.
// [Day] / [día] is left for the operator to fill in.
public enum TriggerType {
    WEEKENDS("Weekends", Map.of("en", "Buy for [Day]", "es", "Comprar para [día]", "fr", "Acheter pour [jour]")),
    OUTSIDE_SERVICE_HOURS("Outside service hours",
            Map.of("en", "Buy for [Day]", "es", "Comprar para [día]", "fr", "Acheter pour [jour]")),
    BEFORE_START_DATE("Before the start date", Map.of("en", "Buy now", "es", "Comprar ahora", "fr", "Acheter"));

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
