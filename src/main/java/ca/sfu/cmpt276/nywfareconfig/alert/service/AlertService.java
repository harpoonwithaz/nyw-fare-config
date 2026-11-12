package ca.sfu.cmpt276.nywfareconfig.alert.service;

import ca.sfu.cmpt276.nywfareconfig.alert.dto.AlertForm;
import ca.sfu.cmpt276.nywfareconfig.alert.dto.RouteOption;
import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertStatus;
import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import ca.sfu.cmpt276.nywfareconfig.alert.model.TriggerType;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.RouteScheduleRepository;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.ServiceAlertRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AlertService {

    private static final Pattern ROUTE_KEY = Pattern.compile("^[A-Z0-9]+:[A-Z0-9]+$");
    private static final Pattern PRODUCT_CODE = Pattern.compile("^[A-Z0-9_]{1,40}$");

    private final ServiceAlertRepository alerts;
    private final RouteScheduleRepository schedules;

    public AlertService(ServiceAlertRepository alerts, RouteScheduleRepository schedules) {
        this.alerts = alerts;
        this.schedules = schedules;
    }

    public List<ServiceAlert> list(String tenantCode, String search, AlertStatus status, String routeKey) {
        if (isBlank(tenantCode)) {
            return List.of();
        }
        String query = search == null ? "" : search.trim().toLowerCase();
        String route = routeKey == null ? "" : routeKey.trim().toUpperCase();

        return alerts.findByTenantCode(tenantCode, Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> route.isEmpty() || a.getRouteKeys().contains(route))
                .filter(a -> query.isEmpty() || matchesSearch(a, query))
                .toList();
    }

    public Optional<ServiceAlert> get(String tenantCode, Long id) {
        if (isBlank(tenantCode) || id == null) {
            return Optional.empty();
        }
        return alerts.findByIdAndTenantCode(id, tenantCode);
    }

    @Transactional
    public List<String> save(String tenantCode, Long id, AlertForm form, String actor) {
        List<String> errors = new ArrayList<>();

        if (isBlank(tenantCode)) {
            errors.add("Select a client before saving an alert.");
            return errors;
        }
        if (form == null) {
            errors.add("The alert form is empty.");
            return errors;
        }

        ServiceAlert alert;
        if (id == null) {
            alert = new ServiceAlert(tenantCode);
        } else {
            alert = alerts.findByIdAndTenantCode(id, tenantCode).orElse(null);
            if (alert == null) {
                errors.add("That alert no longer exists.");
                return errors;
            }
        }

        Set<String> routeKeys = cleanRouteKeys(form.getRouteKeys(), errors);

        TriggerType trigger = form.getTriggerType();
        if (trigger == null) {
            errors.add("Choose when the alert should appear.");
        }

        LocalDate startDate = null;
        if (trigger != null && trigger.isRequiresStartDate()) {
            startDate = parseStartDate(form.getStartDate(), errors);
        }

        String productCode = clean(form.getProductCode());
        if (productCode != null) {
            productCode = productCode.toUpperCase();
            if (!PRODUCT_CODE.matcher(productCode).matches()) {
                errors.add("The ticket code may only contain letters, numbers and underscores, up to "
                        + AlertLimits.PRODUCT_CODE_MAX_LENGTH + " characters.");
            }
        }

        String messageEn = clean(form.getMessageEn());
        if (messageEn == null) {
            errors.add("The English message is required.");
        }
        String messageEs = clean(form.getMessageEs());
        String confirmEn = clean(form.getConfirmLabelEn());
        String confirmEs = clean(form.getConfirmLabelEs());
        String dismissEn = clean(form.getDismissLabelEn());
        String dismissEs = clean(form.getDismissLabelEs());

        checkMax(messageEn, AlertLimits.MESSAGE_MAX_LENGTH, "The English message", errors);
        checkMax(messageEs, AlertLimits.MESSAGE_MAX_LENGTH, "The Spanish message", errors);
        checkMax(confirmEn, AlertLimits.LABEL_MAX_LENGTH, "The English confirm button label", errors);
        checkMax(confirmEs, AlertLimits.LABEL_MAX_LENGTH, "The Spanish confirm button label", errors);
        checkMax(dismissEn, AlertLimits.LABEL_MAX_LENGTH, "The English dismiss button label", errors);
        checkMax(dismissEs, AlertLimits.LABEL_MAX_LENGTH, "The Spanish dismiss button label", errors);

        AlertStatus status = form.getStatus();
        if (status == null) {
            errors.add("Choose a status.");
        }

        if (!errors.isEmpty()) {
            return errors;
        }

        alert.setRouteKeys(routeKeys);
        alert.setTriggerType(trigger);
        alert.setStartDate(startDate);
        alert.setProductCode(productCode);
        alert.setMessageEn(messageEn);
        alert.setMessageEs(messageEs);
        alert.setConfirmLabelEn(confirmEn);
        alert.setConfirmLabelEs(confirmEs);
        alert.setDismissLabelEn(dismissEn);
        alert.setDismissLabelEs(dismissEs);
        alert.setStatus(status);
        alert.setUpdatedBy(actor);
        if (id == null) {
            alert.setCreatedBy(actor);
        }

        alerts.save(alert);
        return errors;
    }

    @Transactional
    public boolean delete(String tenantCode, Long id) {
        if (isBlank(tenantCode) || id == null) {
            return false;
        }
        Optional<ServiceAlert> alert = alerts.findByIdAndTenantCode(id, tenantCode);
        if (alert.isEmpty()) {
            return false;
        }
        alerts.delete(alert.get());
        return true;
    }

    public Map<String, List<RouteOption>> routeOptionsByTerminal(String tenantCode) {
        Map<String, List<RouteOption>> options = new LinkedHashMap<>();
        options.put("PIER11", List.of(new RouteOption("PIER11", "HOB14ST", "PIER11 to HOB14ST")));
        options.put("HOB14ST", List.of(new RouteOption("HOB14ST", "PIER11", "HOB14ST to PIER11")));
        return options;
    }

    private Set<String> cleanRouteKeys(List<String> raw, List<String> errors) {
        Set<String> keys = new LinkedHashSet<>();
        if (raw != null) {
            for (String value : raw) {
                String key = clean(value);
                if (key != null) {
                    keys.add(key.toUpperCase());
                }
            }
        }

        if (keys.isEmpty()) {
            errors.add("Select at least one route.");
            return keys;
        }
        if (keys.size() > AlertLimits.MAX_ROUTES) {
            errors.add("An alert can cover at most " + AlertLimits.MAX_ROUTES + " routes.");
        }

        for (String key : keys) {
            if (!ROUTE_KEY.matcher(key).matches()) {
                errors.add("Invalid route: " + key);
                break;
            }
            String[] parts = key.split(":");
            if (parts[0].equals(parts[1])) {
                errors.add("A route must connect two different sites: " + key);
                break;
            }
        }
        return keys;
    }

    private LocalDate parseStartDate(String raw, List<String> errors) {
        String value = clean(raw);
        if (value == null) {
            errors.add("Enter a start date for this alert.");
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            errors.add("Enter a valid start date.");
            return null;
        }
    }

    private boolean matchesSearch(ServiceAlert alert, String query) {
        return contains(alert.getMessageEn(), query)
                || contains(alert.getMessageEs(), query)
                || contains(alert.getProductCode(), query)
                || alert.getRouteKeys().stream().anyMatch(key -> contains(key, query));
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private static void checkMax(String value, int max, String fieldName, List<String> errors) {
        if (value != null && value.length() > max) {
            errors.add(fieldName + " must be at most " + max + " characters.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}