package ca.sfu.cmpt276.nywfareconfig.alert.service;

import java.util.List;
import java.util.Map;

public final class AlertLimits {

    public static final int MESSAGE_MAX_LENGTH = 200;
    public static final int LABEL_MAX_LENGTH = 20;
    public static final int PRODUCT_CODE_MAX_LENGTH = 40;
    public static final int MAX_ROUTES = 500;

    // Languages an alert can be written in, matched against the tenant's locales by prefix (es, es-US, ...).
    // English is always on because the English message is required.
    public static final List<String> LANGUAGES = List.of("en", "es");

    // Used when a button label is left blank (Story 4.5), keyed by language
    public static final Map<String, String> DEFAULT_CONFIRM = Map.of("en", "Continue", "es", "Continuar");
    public static final Map<String, String> DEFAULT_DISMISS = Map.of("en", "Cancel", "es", "Cancelar");

    private AlertLimits() {
    }

    // Alert languages the tenant supports. A tenant with no locales configured keeps every language.
    public static List<String> languagesFor(List<String> locales) {
        if (locales == null || locales.isEmpty()) {
            return LANGUAGES;
        }
        return LANGUAGES.stream()
                .filter(lang -> lang.equals("en") || locales.stream()
                        .anyMatch(locale -> locale != null && locale.toLowerCase().startsWith(lang)))
                .toList();
    }
}
