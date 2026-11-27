package ca.sfu.cmpt276.nywfareconfig.alert.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

// The rider-facing wording of an alert in one language: the warning and its two button labels.
// Also the form's per-language field group, bound as texts[<lang>].message etc.
@Embeddable
public class AlertText {

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "confirm_label", length = 40)
    private String confirmLabel;

    @Column(name = "dismiss_label", length = 40)
    private String dismissLabel;

    public AlertText() {
    }

    public AlertText(String message, String confirmLabel, String dismissLabel) {
        this.message = message;
        this.confirmLabel = confirmLabel;
        this.dismissLabel = dismissLabel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConfirmLabel() {
        return confirmLabel;
    }

    public void setConfirmLabel(String confirmLabel) {
        this.confirmLabel = confirmLabel;
    }

    public String getDismissLabel() {
        return dismissLabel;
    }

    public void setDismissLabel(String dismissLabel) {
        this.dismissLabel = dismissLabel;
    }
}
