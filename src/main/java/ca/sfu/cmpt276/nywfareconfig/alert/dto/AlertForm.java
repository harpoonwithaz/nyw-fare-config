package ca.sfu.cmpt276.nywfareconfig.alert.dto;

import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertStatus;
import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import ca.sfu.cmpt276.nywfareconfig.alert.model.TriggerType;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class AlertForm {

    private List<String> routeKeys = new ArrayList<>();
    private TriggerType triggerType = TriggerType.WEEKENDS;
    private String startDate;
    private String productCode;
    private String messageEn;
    private String messageEs;
    private String confirmLabelEn;
    private String confirmLabelEs;
    private String dismissLabelEn;
    private String dismissLabelEs;
    private AlertStatus status = AlertStatus.DRAFT;

    public static AlertForm from(ServiceAlert alert) {
        AlertForm form = new AlertForm();
        form.routeKeys = new ArrayList<>(new TreeSet<>(alert.getRouteKeys()));
        form.triggerType = alert.getTriggerType();
        form.startDate = alert.getStartDate() == null ? null : alert.getStartDate().toString();
        form.productCode = alert.getProductCode();
        form.messageEn = alert.getMessageEn();
        form.messageEs = alert.getMessageEs();
        form.confirmLabelEn = alert.getConfirmLabelEn();
        form.confirmLabelEs = alert.getConfirmLabelEs();
        form.dismissLabelEn = alert.getDismissLabelEn();
        form.dismissLabelEs = alert.getDismissLabelEs();
        form.status = alert.getStatus();
        return form;
    }

    public List<String> getRouteKeys() {
        return routeKeys;
    }

    public void setRouteKeys(List<String> routeKeys) {
        this.routeKeys = routeKeys == null ? new ArrayList<>() : routeKeys;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
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