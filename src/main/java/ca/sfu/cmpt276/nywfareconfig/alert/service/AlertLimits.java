package ca.sfu.cmpt276.nywfareconfig.alert.service;

public final class AlertLimits {

    public static final int MESSAGE_MAX_LENGTH = 200;
    public static final int LABEL_MAX_LENGTH = 20;
    public static final int PRODUCT_CODE_MAX_LENGTH = 40;
    public static final int MAX_ROUTES = 50;

    // Used when a button label is left blank (Story 4.5)
    public static final String DEFAULT_CONFIRM_EN = "Continue";
    public static final String DEFAULT_CONFIRM_ES = "Continuar";
    public static final String DEFAULT_DISMISS_EN = "Cancel";
    public static final String DEFAULT_DISMISS_ES = "Cancelar";

    private AlertLimits() {
    }
}