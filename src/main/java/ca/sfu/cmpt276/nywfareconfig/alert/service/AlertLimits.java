package ca.sfu.cmpt276.nywfareconfig.alert.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AlertLimits {

    public static final int MESSAGE_MAX_LENGTH = 200;
    public static final int LABEL_MAX_LENGTH = 20;
    public static final int PRODUCT_CODE_MAX_LENGTH = 40;
    public static final int MAX_ROUTES = 500;

    // Used when the tenant has no locales configured
    public static final String FALLBACK_LANGUAGE = "en";

    // Button text used when a label is left blank (Story 4.5), by language code.
    // ponytail: a language missing here falls back to English; add its words when a tenant needs it.
    public static final Map<String, String> DEFAULT_CONFIRM = Map.of(
            "en", "Continue", "es", "Continuar", "fr", "Continuer", "zh", "继续");
    public static final Map<String, String> DEFAULT_DISMISS = Map.of(
            "en", "Cancel", "es", "Cancelar", "fr", "Annuler", "zh", "取消");

    private AlertLimits() {
    }

    // Alert languages for a tenant, from Caracal's defaultLocale and supportedLocales (en-US -> en).
    // The default locale's language comes first; it is the one whose message is required.
    public static List<String> languagesFor(String defaultLocale, List<String> supportedLocales) {
        Set<String> languages = new LinkedHashSet<>();
        addLanguage(languages, defaultLocale);
        if (supportedLocales != null) {
            supportedLocales.forEach(locale -> addLanguage(languages, locale));
        }
        if (languages.isEmpty()) {
            languages.add(FALLBACK_LANGUAGE);
        }
        return new ArrayList<>(languages);
    }

    public static String defaultConfirm(String language) {
        return DEFAULT_CONFIRM.getOrDefault(language, DEFAULT_CONFIRM.get(FALLBACK_LANGUAGE));
    }

    public static String defaultDismiss(String language) {
        return DEFAULT_DISMISS.getOrDefault(language, DEFAULT_DISMISS.get(FALLBACK_LANGUAGE));
    }

    // "es" -> "Spanish", for labels on the form
    public static String displayName(String language) {
        String name = Locale.forLanguageTag(language).getDisplayLanguage(Locale.ENGLISH);
        return name.isBlank() ? language.toUpperCase(Locale.ROOT) : name;
    }

    private static void addLanguage(Set<String> languages, String locale) {
        if (locale == null || locale.isBlank()) {
            return;
        }
        String language = Locale.forLanguageTag(locale.trim().replace('_', '-')).getLanguage();
        if (!language.isBlank()) {
            languages.add(language.toLowerCase(Locale.ROOT));
        }
    }
}
