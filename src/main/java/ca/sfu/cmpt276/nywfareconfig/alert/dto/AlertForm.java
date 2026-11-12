package ca.sfu.cmpt276.nywfareconfig.alert.dto;

import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertStatus;

public class AlertForm {

    private String originCode;
    private String destinationCode;
    private String messageEn;
    private String messageEs;
    private String confirmLabelEn;
    private String confirmLabelEs;
    private String dismissLabelEn;
    private String dismissLabelEs;
    private AlertStatus status = AlertStatus.DRAFT;

    public String getOriginCode() {
        return originCode;
    }

    public void setOriginCode(String originCode) {
        this.originCode = originCode;
    }

    public String getDestinationCode() {
        return destinationCode;
    }

    public void setDestinationCode(String destinationCode) {
        this.destinationCode = destinationCode;
    }

    public String getMessageEn() {
        return messageEn;
    }

    public void setMessageEn(String messageEn) {
        this.messageEn = messageEn;
    }

    public String getMessageEs() {
        return messageEs;
    }

    public void setMessageEs(String messageEs) {
        this.messageEs = messageEs;
    }

    public String getConfirmLabelEn() {
        return confirmLabelEn;
    }

    public void setConfirmLabelEn(String confirmLabelEn) {
        this.confirmLabelEn = confirmLabelEn;
    }

    public String getConfirmLabelEs() {
        return confirmLabelEs;
    }

    public void setConfirmLabelEs(String confirmLabelEs) {
        this.confirmLabelEs = confirmLabelEs;
    }

    public String getDismissLabelEn() {
        return dismissLabelEn;
    }

    public void setDismissLabelEn(String dismissLabelEn) {
        this.dismissLabelEn = dismissLabelEn;
    }

    public String getDismissLabelEs() {
        return dismissLabelEs;
    }

    public void setDismissLabelEs(String dismissLabelEs) {
        this.dismissLabelEs = dismissLabelEs;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }
}