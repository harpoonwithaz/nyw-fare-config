package ca.sfu.cmpt276.nywfareconfig.alert.service;

import ca.sfu.cmpt276.nywfareconfig.alert.dto.AlertCatalog;
import ca.sfu.cmpt276.nywfareconfig.alert.dto.AlertChange;
import ca.sfu.cmpt276.nywfareconfig.alert.dto.AlertForm;
import ca.sfu.cmpt276.nywfareconfig.alert.dto.RouteOption;
import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertStatus;
import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertText;
import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import ca.sfu.cmpt276.nywfareconfig.alert.model.TriggerType;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.RouteScheduleRepository;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.ServiceAlertRepository;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.CatalogOption;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AlertService {

    private static final Pattern ROUTE_KEY = Pattern.compile("^[A-Z0-9_-]+:[A-Z0-9_-]+$");
    // A suggestion like "Buy for [Day]" that still has its bracketed placeholder in it
    private static final Pattern PLACEHOLDER = Pattern.compile("\\[[^\\]]*\\]");

    private final ServiceAlertRepository alerts;
    private final RouteScheduleRepository schedules;
    private final FareService fares;

    public AlertService(ServiceAlertRepository alerts, RouteScheduleRepository schedules, FareService fares) {
        this.alerts = alerts;
        this.schedules = schedules;
        this.fares = fares;
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

    public Optional<ServiceAlert> findPendingReplacement(String tenantCode, Long activeId) {
        if (isBlank(tenantCode) || activeId == null) {
            return Optional.empty();
        }
        return alerts.findFirstByReplacesAlertIdAndTenantCodeAndStatus(activeId, tenantCode, AlertStatus.DRAFT);
    }

    public List<AlertChange> pendingChanges(String tenantCode) {
        List<AlertChange> changes = new ArrayList<>();
        for (ServiceAlert draft : list(tenantCode, null, AlertStatus.DRAFT, null)) {
            ServiceAlert current = null;
            if (draft.getReplacesAlertId() != null) {
                current = alerts.findByIdAndTenantCode(draft.getReplacesAlertId(), tenantCode)
                        .filter(a -> a.getStatus() == AlertStatus.ACTIVE)
                        .orElse(null);
            }
            changes.add(new AlertChange(draft, current));
        }
        return changes;
    }

    public boolean isDeletable(ServiceAlert alert) {
        return alert != null && alert.getStatus() == AlertStatus.DRAFT;
    }

    // languages: the tenant's alert languages (AlertLimits.languagesFor), main language first
    @Transactional
    public List<String> save(String tenantCode, Long id, AlertForm form, String actor, List<String> languages) {
        List<String> errors = new ArrayList<>();

        if (isBlank(tenantCode)) {
            errors.add("Select a client before saving an alert.");
            return errors;
        }
        if (form == null) {
            errors.add("The alert form is empty.");
            return errors;
        }

        ServiceAlert existing = null;
        if (id != null) {
            // Locked so two saves on the same live alert can't each create a draft copy
            existing = alerts.lockByIdAndTenantCode(id, tenantCode).orElse(null);
            if (existing == null) {
                errors.add("That alert no longer exists.");
                return errors;
            }
            if (existing.getStatus() == AlertStatus.ARCHIVED) {
                errors.add("Archived alerts cannot be edited.");
                return errors;
            }
        }

        Set<String> routeKeys = cleanRouteKeys(form.getRouteKeys(), errors);

        TriggerType trigger = form.getTriggerType();
        if (trigger == null) {
            errors.add("Choose when the warning should show.");
        }

        LocalDate startDate = null;
        if (trigger != null && trigger.isRequiresStartDate()) {
            startDate = parseStartDate(form.getStartDate(), errors);
        }

        String productCode = clean(form.getProductCode());
        if (productCode != null && productCode.length() > AlertLimits.PRODUCT_CODE_MAX_LENGTH) {
            errors.add("The ticket code must be " + AlertLimits.PRODUCT_CODE_MAX_LENGTH + " characters or fewer.");
        }

        // Wording per language. The tenant's first language must have a message; languages the tenant
        // no longer lists are kept as posted (the form sends them back as hidden fields).
        List<String> langs = languages == null || languages.isEmpty()
                ? List.of(AlertLimits.FALLBACK_LANGUAGE) : languages;
        String primaryLanguage = langs.get(0);
        Map<String, AlertText> texts = new LinkedHashMap<>();
        for (Map.Entry<String, AlertText> entry : form.getTexts().entrySet()) {
            AlertText posted = entry.getValue() == null ? new AlertText() : entry.getValue();
            texts.put(entry.getKey(), new AlertText(clean(posted.getMessage()),
                    clean(posted.getConfirmLabel()), clean(posted.getDismissLabel())));
        }
        String primaryMessage = texts.containsKey(primaryLanguage) ? texts.get(primaryLanguage).getMessage() : null;
        if (primaryMessage == null) {
            errors.add("Message in " + AlertLimits.displayName(primaryLanguage) + " is required.");
        }
        boolean messageTooLong = false;
        boolean labelTooLong = false;
        boolean placeholderLeft = false;
        for (AlertText text : texts.values()) {
            messageTooLong |= tooLong(text.getMessage(), AlertLimits.MESSAGE_MAX_LENGTH);
            labelTooLong |= tooLong(text.getConfirmLabel(), AlertLimits.LABEL_MAX_LENGTH)
                    || tooLong(text.getDismissLabel(), AlertLimits.LABEL_MAX_LENGTH);
            placeholderLeft |= hasPlaceholder(text.getConfirmLabel()) || hasPlaceholder(text.getDismissLabel());
        }
        if (messageTooLong) {
            errors.add("Message must be " + AlertLimits.MESSAGE_MAX_LENGTH + " characters or fewer.");
        }
        if (labelTooLong) {
            errors.add("Label must be " + AlertLimits.LABEL_MAX_LENGTH + " characters or fewer.");
        }
        if (placeholderLeft) {
            errors.add("Replace the placeholder in brackets, like [Day], in the button label before saving.");
        }

        // A ticket that has since been retired stays allowed when it is the one already saved on this alert
        String savedProduct = existing == null ? null : existing.getProductCode();
        if (!routeKeys.isEmpty() || productCode != null) {
            try {
                productCode = checkCatalog(tenantCode, routeKeys, productCode, savedProduct, errors);
            } catch (CaracalApiException ex) {
                errors.add("The Caracal catalog is not available right now. Try again in a moment.");
            }
        }

        if (!errors.isEmpty()) {
            return errors;
        }

        // Blank labels fall back to the defaults so the popup never shows an empty button
        // Blank labels get the default for each tenant language so the popup never shows an empty button
        for (String language : langs) {
            AlertText text = texts.computeIfAbsent(language, key -> new AlertText());
            if (text.getConfirmLabel() == null) {
                text.setConfirmLabel(AlertLimits.defaultConfirm(language));
            }
            if (text.getDismissLabel() == null) {
                text.setDismissLabel(AlertLimits.defaultDismiss(language));
            }
        }

        ServiceAlert target;
        if (existing == null) {
            target = new ServiceAlert(tenantCode);
            target.setCreatedBy(actor);
        } else if (existing.getStatus() == AlertStatus.ACTIVE) {
            target = alerts
                    .findFirstByReplacesAlertIdAndTenantCodeAndStatus(existing.getId(), tenantCode, AlertStatus.DRAFT)
                    .orElse(null);
            if (target == null) {
                target = new ServiceAlert(tenantCode);
                target.setReplacesAlertId(existing.getId());
                target.setCreatedBy(actor);
            }
        } else {
            target = existing;
        }

        target.setRouteKeys(routeKeys);
        target.setTriggerType(trigger);
        target.setStartDate(startDate);
        target.setProductCode(productCode);
        target.setPrimaryMessage(primaryMessage);
        target.setTexts(texts);
        target.setStatus(AlertStatus.DRAFT);
        target.setUpdatedBy(actor);

        alerts.save(target);
        return errors;
    }

    @Transactional
    public List<String> publish(String tenantCode, List<Long> ids, String actor) {
        List<String> errors = new ArrayList<>();

        if (isBlank(tenantCode)) {
            errors.add("Select a client before publishing.");
            return errors;
        }
        if (ids == null || ids.isEmpty()) {
            errors.add("Select at least one alert to publish.");
            return errors;
        }

        // Drafts are locked, so a second "Make active" on the same draft waits and then sees it is no longer a draft.
        // ponytail: publish locks draft then live alert, archive locks live alert then draft; Postgres aborts one
        // if both hit the same alert in the same instant. Lock in one fixed order if that ever shows up.
        List<ServiceAlert> drafts = new ArrayList<>();
        Map<Long, ServiceAlert> replaced = new LinkedHashMap<>();
        for (Long alertId : new LinkedHashSet<>(ids)) {
            Optional<ServiceAlert> found = alertId == null
                    ? Optional.empty()
                    : alerts.lockByIdAndTenantCode(alertId, tenantCode);
            if (found.isEmpty()) {
                errors.add("Alert " + alertId + " was not found.");
            } else if (found.get().getStatus() != AlertStatus.DRAFT) {
                errors.add("Alert " + alertId + " is not a draft.");
            } else {
                ServiceAlert draft = found.get();
                if (draft.getReplacesAlertId() != null) {
                    ServiceAlert old = alerts.lockByIdAndTenantCode(draft.getReplacesAlertId(), tenantCode)
                            .filter(a -> a.getStatus() == AlertStatus.ACTIVE)
                            .orElse(null);
                    if (old == null) {
                        errors.add("Alert " + alertId + " edits a live alert that is no longer active. Delete this draft.");
                        continue;
                    }
                    replaced.put(draft.getId(), old);
                }
                drafts.add(draft);
            }
        }
        if (!errors.isEmpty()) {
            return errors;
        }

        Instant now = Instant.now();
        for (ServiceAlert draft : drafts) {
            ServiceAlert old = replaced.get(draft.getId());
            if (old != null) {
                old.setStatus(AlertStatus.ARCHIVED);
                old.setArchivedBy(actor);
                old.setArchivedAt(now);
                alerts.save(old);
            }
            draft.setStatus(AlertStatus.ACTIVE);
            draft.setPublishedBy(actor);
            draft.setPublishedAt(now);
            alerts.save(draft);
        }
        return errors;
    }

    @Transactional
    public boolean archive(String tenantCode, Long id, String actor) {
        if (isBlank(tenantCode) || id == null) {
            return false;
        }
        Optional<ServiceAlert> found = alerts.lockByIdAndTenantCode(id, tenantCode);
        if (found.isEmpty() || found.get().getStatus() != AlertStatus.ACTIVE) {
            return false;
        }
        ServiceAlert alert = found.get();
        alert.setStatus(AlertStatus.ARCHIVED);
        alert.setArchivedBy(actor);
        alert.setArchivedAt(Instant.now());
        alerts.save(alert);
        // Unpublished edits of an archived alert have nothing left to replace, so they go too
        alerts.findFirstByReplacesAlertIdAndTenantCodeAndStatus(id, tenantCode, AlertStatus.DRAFT)
                .ifPresent(alerts::delete);
        return true;
    }

    @Transactional
    public boolean delete(String tenantCode, Long id) {
        if (isBlank(tenantCode) || id == null) {
            return false;
        }
        Optional<ServiceAlert> found = alerts.lockByIdAndTenantCode(id, tenantCode);
        if (found.isEmpty() || !isDeletable(found.get())) {
            return false;
        }
        alerts.delete(found.get());
        return true;
    }

    // Routes grouped by origin, keyed by the heading shown on the page ("Pier 11 (PIER11)").
    // Empty when Caracal can't be reached or the tenant has fewer than two sites.
    public Map<String, List<RouteOption>> routeOptionsByTerminal(String tenantCode) {
        if (isBlank(tenantCode)) {
            return new LinkedHashMap<>();
        }
        try {
            return buildRoutes(sites(tenantCode));
        } catch (CaracalApiException ex) {
            return new LinkedHashMap<>();
        }
    }

    // Routes and tickets for the alert form, with a flag for whether Caracal answered both
    public AlertCatalog formCatalog(String tenantCode) {
        if (isBlank(tenantCode)) {
            return new AlertCatalog(new LinkedHashMap<>(), List.of(), false);
        }
        boolean loaded = true;
        Map<String, List<RouteOption>> routes = new LinkedHashMap<>();
        List<CatalogOption> products = List.of();
        try {
            routes = buildRoutes(sites(tenantCode));
        } catch (CaracalApiException ex) {
            loaded = false;
        }
        try {
            products = fares.getOdProducts(tenantCode);
        } catch (CaracalApiException ex) {
            loaded = false;
        }
        return new AlertCatalog(routes, products, loaded);
    }

    private List<CatalogOption> sites(String tenantCode) {
        return fares.getSecondaryFilterOptions(tenantCode, GridMode.OD);
    }

    // ponytail: every pair of sites, not only routes Caracal actually serves; swap in a served-routes lookup when one exists
    private static Map<String, List<RouteOption>> buildRoutes(List<CatalogOption> siteList) {
        List<CatalogOption> sorted = siteList.stream()
                .sorted(Comparator.comparing(AlertService::nameOf, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<String, List<RouteOption>> options = new LinkedHashMap<>();
        for (CatalogOption origin : sorted) {
            List<RouteOption> routes = new ArrayList<>();
            for (CatalogOption destination : sorted) {
                if (!origin.code().equals(destination.code())) {
                    routes.add(new RouteOption(origin.code(), destination.code(),
                            nameOf(origin) + " to " + nameOf(destination)));
                }
            }
            if (!routes.isEmpty()) {
                options.put(nameOf(origin) + " (" + origin.code() + ")", routes);
            }
        }
        return options;
    }

    private static String nameOf(CatalogOption option) {
        return isBlank(option.displayName()) ? option.code() : option.displayName();
    }

    private String checkCatalog(String tenantCode, Set<String> routeKeys, String productCode, String savedProduct,
            List<String> errors) {
        if (!routeKeys.isEmpty()) {
            Set<String> siteCodes = new HashSet<>();
            for (CatalogOption site : sites(tenantCode)) {
                siteCodes.add(site.code());
            }
            Set<String> unknownSites = new LinkedHashSet<>();
            for (String key : routeKeys) {
                String[] parts = key.split(":");
                if (parts.length != 2) {
                    continue;
                }
                for (String part : parts) {
                    if (!siteCodes.contains(part)) {
                        unknownSites.add(part);
                    }
                }
            }
            for (String code : unknownSites) {
                errors.add("Unknown site code: " + code + ".");
            }
        }

        if (productCode == null || productCode.length() > AlertLimits.PRODUCT_CODE_MAX_LENGTH) {
            return productCode;
        }
        if (productCode.equalsIgnoreCase(savedProduct)) {
            return savedProduct;
        }
        for (CatalogOption product : fares.getOdProducts(tenantCode)) {
            if (product.code().equalsIgnoreCase(productCode)) {
                return product.code();
            }
        }
        errors.add("Unknown ticket: " + productCode + ".");
        return productCode;
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
            errors.add("Start date is required.");
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
        return contains(alert.getPrimaryMessage(), query)
                || alert.getTexts().values().stream().anyMatch(text -> contains(text.getMessage(), query))
                || contains(alert.getProductCode(), query)
                || alert.getRouteKeys().stream().anyMatch(key -> contains(key, query));
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private static boolean hasPlaceholder(String value) {
        return value != null && PLACEHOLDER.matcher(value).find();
    }

    private static boolean tooLong(String value, int max) {
        return value != null && value.length() > max;
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