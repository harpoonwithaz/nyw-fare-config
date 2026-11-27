package ca.sfu.cmpt276.nywfareconfig.availability.service;

import ca.sfu.cmpt276.nywfareconfig.availability.dto.AvailabilityDraftDTO;
import ca.sfu.cmpt276.nywfareconfig.availability.dto.ProductAvailabilityView;
import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.model.TemporalCondition;
import ca.sfu.cmpt276.nywfareconfig.availability.repository.AvailabilityRuleRepository;
import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalZonalDtos;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.DraftStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.repository.FareDraftRepository;
import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class AvailabilityService {

    private static final String DEFAULT_FARE_CLASS = "FULL";
    private static final String DEFAULT_CHANNEL = "UNATTENDED";
    private static final String NO_PRICE_RULE = "no pricerule matched the resolved dimension context";
    private static final String NOT_SOLD = "not sold";
    private static final String CONDITION = "no rate assignment's condition matches the requested moment or channel";

    public record StationOption(String code, String name, String kind) {}

    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final FareDraftRepository fareDraftRepository;
    private final CaracalApiClient caracalApiClient;
    private final ActorService actorService;

    public AvailabilityService(
            AvailabilityRuleRepository availabilityRuleRepository,
            FareDraftRepository fareDraftRepository,
            CaracalApiClient caracalApiClient,
            ActorService actorService) {
        this.availabilityRuleRepository = availabilityRuleRepository;
        this.fareDraftRepository = fareDraftRepository;
        this.caracalApiClient = caracalApiClient;
        this.actorService = actorService;
    }

    public List<StationOption> getStationsForTenant(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return List.of();
        }
        String normalizedTenant = normalizeCode(tenantCode);
        List<CaracalCatalogDtos.SiteSummary> sites = caracalApiClient.fetchSites(normalizedTenant);
        if (sites == null || sites.isEmpty()) {
            return List.of();
        }
        return sites.stream()
                .filter(s -> s.siteCode() != null && !s.siteCode().isBlank())
                .sorted(Comparator.comparing(s -> s.name() != null ? s.name() : s.siteCode()))
                .map(s -> new StationOption(
                        normalizeCode(s.siteCode()),
                        (s.name() != null && !s.name().isBlank()) ? s.name().trim() : normalizeCode(s.siteCode()),
                        "STATION"))
                .toList();
    }

    public ProductAvailabilityView getProductAvailability(Tenant tenant, String siteCode, String routeCode) {
        String normalizedTenant = normalizeCode(tenant.getCode());
        String normalizedSite = normalizeCode(siteCode);
        String normalizedRoute = normalizeRouteCode(routeCode);

        List<CaracalCatalogDtos.ProductSummary> liveProducts = caracalApiClient.fetchProducts(normalizedTenant);
        LinkedHashSet<String> orderedProducts = new LinkedHashSet<>();
        Map<String, String> productNames = new LinkedHashMap<>();
        Map<String, Boolean> toggles = new LinkedHashMap<>();
        Map<String, Boolean> phasedOut = new LinkedHashMap<>();
        Map<String, String> caracalStatuses = new LinkedHashMap<>();
        Map<String, Boolean> savedInDb = new LinkedHashMap<>();
        Map<String, TemporalCondition> conditions = new LinkedHashMap<>();

        if (liveProducts != null) {
            for (CaracalCatalogDtos.ProductSummary p : liveProducts) {
                if (p.code() == null || p.code().isBlank() || !p.isActive()) {
                    continue;
                }
                String code = normalizeCode(p.code());
                orderedProducts.add(code);
                productNames.put(code, (p.name() != null && !p.name().isBlank()) ? p.name().trim() : code);
                // Default to false until verified against Caracal rate-table / price-simulate (matches FareGrid)
                toggles.put(code, false);
                caracalStatuses.put(code, "NOT_SOLD");
            }
        }

        if (tenant.getGridMode() == GridMode.ZONAL) {
            populateZonalAvailability(normalizedTenant, normalizedSite, orderedProducts, toggles, caracalStatuses);
        } else {
            populateOdAvailability(normalizedTenant, normalizedSite, normalizedRoute, orderedProducts, toggles, caracalStatuses);
        }

        // Overlay pending Fare Grid drafts (so FareGrid <-> Availability statuses stay synchronized)
        if (fareDraftRepository != null) {
            List<FareDraft> fareDrafts = fareDraftRepository.findByTenantCodeAndDraftStatus(
                    normalizedTenant, DraftStatus.PENDING);
            String destSite = extractDestCode(normalizedRoute);
            for (FareDraft fd : fareDrafts) {
                if (fd.getGridMode() == GridMode.OD && normalizedSite.equalsIgnoreCase(fd.getOriginIdentifier())) {
                    if (AvailabilityRule.ALL_ROUTES.equals(normalizedRoute)
                            || (destSite != null && destSite.equalsIgnoreCase(fd.getDestIdentifier()))) {
                        String code = normalizeCode(fd.getProductCode());
                        if (orderedProducts.contains(code)) {
                            boolean active = fd.getTargetStatus() == FareStatus.ACTIVE;
                            toggles.put(code, active);
                            caracalStatuses.put(code, active ? "SELLABLE" : fd.getTargetStatus().name());
                        }
                    }
                }
            }
        }

        // Overlay persisted PostgreSQL AvailabilityRule rows (Terminal-wide first, then specific O-D route)
        if (!AvailabilityRule.ALL_ROUTES.equals(normalizedRoute)) {
            List<AvailabilityRule> terminalRules = availabilityRuleRepository
                    .findByTenantCodeAndSiteCodeAndRouteCode(normalizedTenant, normalizedSite, AvailabilityRule.ALL_ROUTES);
            applyDbOverlay(terminalRules, orderedProducts, productNames, toggles, phasedOut, caracalStatuses, savedInDb, conditions, false);
        }

        List<AvailabilityRule> routeRules = availabilityRuleRepository
                .findByTenantCodeAndSiteCodeAndRouteCode(normalizedTenant, normalizedSite, normalizedRoute);
        applyDbOverlay(routeRules, orderedProducts, productNames, toggles, phasedOut, caracalStatuses, savedInDb, conditions, true);

        ProductAvailabilityView view = new ProductAvailabilityView(
                normalizedSite,
                new ArrayList<>(orderedProducts),
                toggles,
                conditions);
        view.setRouteCode(normalizedRoute);
        view.setProductNames(productNames);
        view.setPhasedOutProducts(phasedOut);
        view.setCaracalStatuses(caracalStatuses);
        view.setSavedInDb(savedInDb);
        return view;
    }

    private void populateOdAvailability(
            String tenantCode,
            String originSiteCode,
            String routeCode,
            Set<String> orderedProducts,
            Map<String, Boolean> toggles,
            Map<String, String> caracalStatuses) {

        Optional<List<CaracalCatalogDtos.RateAssignmentSummary>> rateTableOpt =
                caracalApiClient.fetchRateTable(
                        new CaracalCatalogDtos.RateTableGetRequest(tenantCode, "RT-" + originSiteCode));

        Set<String> soldAtOrigin = new HashSet<>();
        if (rateTableOpt != null && rateTableOpt.isPresent()) {
            for (CaracalCatalogDtos.RateAssignmentSummary assignment : rateTableOpt.get()) {
                if (assignment.productCode() == null || assignment.productCode().isBlank()) {
                    continue;
                }
                String code = normalizeCode(assignment.productCode());
                if ("VALUE".equalsIgnoreCase(assignment.cellType())) {
                    soldAtOrigin.add(code);
                }
            }
        }

        String destSiteCode = extractDestCode(routeCode);
        for (String productCode : orderedProducts) {
            if (!soldAtOrigin.contains(productCode)) {
                toggles.put(productCode, false);
                caracalStatuses.put(productCode, "NOT_SOLD");
                continue;
            }

            if (destSiteCode == null) {
                toggles.put(productCode, true);
                caracalStatuses.put(productCode, "SELLABLE");
                continue;
            }

            // Specific O-D route: evaluate with price-simulate (exact same call & classification as FareService)
            CaracalSimulationDtos.PriceSimulationResult sim = caracalApiClient.simulatePrice(
                    new CaracalSimulationDtos.PriceSimulateRequest(
                            tenantCode, productCode, originSiteCode, Map.of("DEST", destSiteCode), null));

            String status = classifyStatus(sim);
            boolean sellable = "SELLABLE".equals(status);
            toggles.put(productCode, sellable);
            caracalStatuses.put(productCode, status);
        }
    }

    private void populateZonalAvailability(
            String tenantCode,
            String stationCode,
            Set<String> orderedProducts,
            Map<String, Boolean> toggles,
            Map<String, String> caracalStatuses) {

        List<CaracalZonalDtos.SiteDimensionBinding> bindings = caracalApiClient.fetchStationZoneBindings(tenantCode);
        String originZone = bindings == null ? null : bindings.stream()
                .filter(b -> b != null && stationCode.equalsIgnoreCase(b.siteCode())
                        && "ORIGIN_ZONE".equalsIgnoreCase(b.dimensionKey()))
                .map(CaracalZonalDtos.SiteDimensionBinding::valueKey)
                .filter(Objects::nonNull)
                .map(this::normalizeCode)
                .findFirst()
                .orElse(null);

        if (originZone == null) {
            for (String productCode : orderedProducts) {
                toggles.put(productCode, true);
                caracalStatuses.put(productCode, "SELLABLE");
            }
            return;
        }

        for (String productCode : orderedProducts) {
            Map<String, String> dimensions = Map.of(
                    "ORIGIN_ZONE", originZone,
                    "DEST_ZONE", originZone,
                    "FARE_CLASS", DEFAULT_FARE_CLASS);

            CaracalSimulationDtos.PriceSimulationResult sim = caracalApiClient.simulatePrice(
                    new CaracalSimulationDtos.PriceSimulateRequest(
                            tenantCode, productCode, null, dimensions, null, DEFAULT_CHANNEL));

            String status = classifyStatus(sim);
            boolean sellable = "SELLABLE".equals(status);
            toggles.put(productCode, sellable);
            caracalStatuses.put(productCode, status);
        }
    }

    private String classifyStatus(CaracalSimulationDtos.PriceSimulationResult sim) {
        if (sim == null || sim.response() == null || sim.response().pricing() == null
                || sim.response().pricing().total() == null) {
            String msg = (sim != null && sim.errorMessage() != null)
                    ? sim.errorMessage().toLowerCase(Locale.ROOT)
                    : "";
            if (msg.contains(NOT_SOLD) || msg.contains(CONDITION)) {
                return "NOT_SOLD";
            }
            if (msg.contains(NO_PRICE_RULE)) {
                return "NA";
            }
            return "NOT_SOLD";
        }
        return "SELLABLE";
    }

    private void applyDbOverlay(
            List<AvailabilityRule> rules,
            Set<String> orderedProducts,
            Map<String, String> productNames,
            Map<String, Boolean> toggles,
            Map<String, Boolean> phasedOut,
            Map<String, String> caracalStatuses,
            Map<String, Boolean> savedInDb,
            Map<String, TemporalCondition> conditions,
            boolean markSaved) {

        for (AvailabilityRule rule : rules) {
            String code = normalizeCode(rule.getProductCode());
            orderedProducts.add(code);
            productNames.putIfAbsent(code, code);

            if (rule.isPhasedOut()) {
                phasedOut.put(code, true);
                toggles.put(code, false);
                caracalStatuses.put(code, "PHASED_OUT");
            } else if (!phasedOut.getOrDefault(code, false)) {
                toggles.put(code, rule.isAvailable());
                caracalStatuses.put(code, rule.isAvailable() ? "SELLABLE" : "NOT_SOLD");
            }

            if (markSaved) {
                savedInDb.put(code, true);
            }
            if (rule.getTemporalCondition() != null) {
                conditions.put(code, rule.getTemporalCondition());
            }
        }
    }

    @Transactional
    public void stageAvailabilityDrafts(String tenantCode, List<AvailabilityDraftDTO> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            throw new IllegalArgumentException("At least one availability rule change is required.");
        }

        String normalizedTenant = normalizeCode(tenantCode);
        String actor = resolveActor();
        boolean isClientServices = hasClientServicesRole();

        for (AvailabilityDraftDTO draft : drafts) {
            String normalizedSite = normalizeCode(draft.getSiteCode());
            String normalizedRoute = normalizeRouteCode(draft.getRouteCode());
            String normalizedProduct = normalizeCode(draft.getProductCode());

            AvailabilityRule rule = availabilityRuleRepository
                    .findByTenantCodeAndSiteCodeAndRouteCodeAndProductCode(
                            normalizedTenant, normalizedSite, normalizedRoute, normalizedProduct)
                    .orElseGet(() -> new AvailabilityRule(
                            normalizedTenant, normalizedSite, normalizedRoute, normalizedProduct, draft.isAvailable()));

            boolean requestedPhasedOut = Boolean.TRUE.equals(draft.getPhasedOut());
            boolean changingPhasedOutState = (requestedPhasedOut != rule.isPhasedOut())
                    || (rule.isPhasedOut() && draft.isAvailable());

            if (changingPhasedOutState && !isClientServices) {
                throw new AccessDeniedException(
                        "Only CLIENT_SERVICES administrators can phase out or reactivate permanently phased-out product: "
                                + normalizedProduct);
            }

            if (requestedPhasedOut) {
                rule.setPhasedOut(true);
                rule.setAvailable(false);
            } else {
                rule.setPhasedOut(false);
                rule.setAvailable(draft.isAvailable());
            }

            rule.setPendingPublish(true);
            rule.setUpdatedBy(actor);
            rule.setLastUpdated(LocalDateTime.now());

            if (draft.isClearCondition()) {
                rule.setTemporalCondition(null);
            } else if (draft.getConditionType() != null && !draft.getConditionType().isBlank()) {
                String condType = normalizeCode(draft.getConditionType());
                TemporalCondition condition = rule.getTemporalCondition() != null
                        ? rule.getTemporalCondition()
                        : new TemporalCondition();

                condition.setName(normalizedProduct + "_" + normalizedSite + "_RULE");
                condition.setConditionType(condType);
                condition.setStartTime(parseLocalTime(draft.getStartTime()));
                condition.setEndTime(parseLocalTime(draft.getEndTime()));

                if ("TEMPORARY_PROMO".equals(condType)) {
                    LocalDate start = parseLocalDate(draft.getStartDate());
                    LocalDate end = parseLocalDate(draft.getEndDate());
                    if (start != null && end != null && end.isBefore(start)) {
                        throw new IllegalArgumentException("End date cannot be before start date.");
                    }
                    condition.setDaysOfWeek(List.of());
                    condition.setStartDate(start);
                    condition.setEndDate(end);
                } else {
                    List<Integer> validDays = draft.getDaysOfWeek() == null ? List.of()
                            : draft.getDaysOfWeek().stream()
                                    .filter(d -> d != null && d >= 1 && d <= 7)
                                    .distinct()
                                    .sorted()
                                    .toList();
                    condition.setDaysOfWeek(validDays);
                    condition.setStartDate(null);
                    condition.setEndDate(null);
                }

                rule.setTemporalCondition(condition);
            }

            availabilityRuleRepository.save(rule);
        }
    }

    @Transactional
    public void discardSavedDrafts(String tenantCode, String siteCode, String routeCode) {
        String normalizedTenant = normalizeCode(tenantCode);
        String normalizedSite = normalizeCode(siteCode);
        String normalizedRoute = normalizeRouteCode(routeCode);
        List<AvailabilityRule> saved = availabilityRuleRepository
                .findByTenantCodeAndSiteCodeAndRouteCode(normalizedTenant, normalizedSite, normalizedRoute);
        if (!saved.isEmpty()) {
            availabilityRuleRepository.deleteAllInBatch(saved);
        }
    }

    @Transactional(readOnly = true)
    public List<AvailabilityRule> getPendingAvailabilityRules(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return List.of();
        }
        return availabilityRuleRepository.findByTenantCodeAndPendingPublishTrue(normalizeCode(tenantCode));
    }

    private String extractDestCode(String routeCode) {
        if (routeCode == null || AvailabilityRule.ALL_ROUTES.equals(routeCode) || !routeCode.contains("->")) {
            return null;
        }
        String[] parts = routeCode.split("->", 2);
        return parts.length == 2 && !parts[1].isBlank() ? normalizeCode(parts[1]) : null;
    }

    private String normalizeRouteCode(String raw) {
        if (raw == null || raw.isBlank() || AvailabilityRule.ALL_ROUTES.equalsIgnoreCase(raw.trim())) {
            return AvailabilityRule.ALL_ROUTES;
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String raw) {
        return raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
    }

    private LocalTime parseLocalTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();
        try {
            return LocalTime.parse(trimmed.length() == 5 ? trimmed + ":00" : trimmed);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid time format (expected HH:mm or HH:mm:ss): " + raw, ex);
        }
    }

    private LocalDate parseLocalDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date format (expected YYYY-MM-DD): " + raw, ex);
        }
    }

    private String resolveActor() {
        try {
            return actorService.currentActorHeader();
        } catch (IllegalStateException ex) {
            return "system";
        }
    }

    private boolean hasClientServicesRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CLIENT_SERVICES".equals(a.getAuthority())
                        || "CLIENT_SERVICES".equals(a.getAuthority()));
    }
}