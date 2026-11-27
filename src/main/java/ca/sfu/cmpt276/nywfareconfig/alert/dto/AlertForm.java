package ca.sfu.cmpt276.nywfareconfig.alert.dto;

import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertStatus;
import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertText;
import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import ca.sfu.cmpt276.nywfareconfig.alert.model.TriggerType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class AlertForm {

    private List<String> routeKeys = new ArrayList<>();
    private TriggerType triggerType = TriggerType.WEEKENDS;
    private String startDate;
    private String productCode;
    // Wording per language code, posted as texts[es].message, texts[es].confirmLabel, ...
    private Map<String, AlertText> texts = new LinkedHashMap<>();
    private AlertStatus status = AlertStatus.DRAFT;

    public static AlertForm from(ServiceAlert alert) {
        AlertForm form = new AlertForm();
        form.routeKeys = new ArrayList<>(new TreeSet<>(alert.getRouteKeys()));
        form.triggerType = alert.getTriggerType();
        form.startDate = alert.getStartDate() == null ? null : alert.getStartDate().toString();
        form.productCode = alert.getProductCode();
        alert.getTexts().forEach((language, text) -> form.texts.put(language,
                new AlertText(text.getMessage(), text.getConfirmLabel(), text.getDismissLabel())));
        // Alerts saved before per-language wording only have the English message column
        if (form.texts.isEmpty() && alert.getPrimaryMessage() != null) {
            form.texts.put("en", new AlertText(alert.getPrimaryMessage(), null, null));
        }
        form.status = alert.getStatus();
        return form;
    }

    // Makes sure every tenant language has a field group to render
    public void ensureLanguages(List<String> languages) {
        languages.forEach(language -> texts.computeIfAbsent(language, key -> new AlertText()));
    }

    // Shortcut for code and tests that set one language at a time
    public AlertText text(String language) {
        return texts.computeIfAbsent(language, key -> new AlertText());
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

    public Map<String, AlertText> getTexts() {
        return texts;
    }

    public void setTexts(Map<String, AlertText> texts) {
        this.texts = texts == null ? new LinkedHashMap<>() : texts;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }
}
