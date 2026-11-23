package ca.sfu.cmpt276.nywfareconfig.availability.service;

import ca.sfu.cmpt276.nywfareconfig.availability.dto.AvailabilityDraftDTO;
import ca.sfu.cmpt276.nywfareconfig.availability.dto.ProductAvailabilityView;
import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.model.TemporalCondition;
import ca.sfu.cmpt276.nywfareconfig.availability.repository.AvailabilityRuleRepository;
import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalUnavailableException;
import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityService.class);

    public static final List<String> FALLBACK_PRODUCTS = List.of(
        "ADULT_ONEWAY",
        "CHILD_ONEWAY",
        "SENIOR_ONEWAY",
        "FULL_10TRIP",
        "FULL_MONTHLY"
    );

    public record StationOption(String code, String name, String kind) {}
    public record RouteOption(String code, String label, String destSiteCode) {}

    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final CaracalApiClient caracalApiClient;
    private final ActorService actorService;

    public AvailabilityService(
            AvailabilityRuleRepository availabilityRuleRepository,
            CaracalApiClient caracalApiClient,
            ActorService actorService) {
        this.availabilityRuleRepository = availabilityRuleRepository;
        this.caracalApiClient = caracalApiClient;
        this.actorService = actorService;
    }

    /**
     * Fetches every station/terminal for the active tenant from Caracal GET /functions/site-list.
     */
    public List<StationOption> getStationsForTenant(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return List.of();
        }
        try {
            List<CaracalCatalogDtos.SiteSummary> sites = caracalApiClient.fetchSites(tenantCode);
            if (sites != null && !sites.isEmpty()) {
                return sites.stream()
                        .filter(s -> s.siteCode() != null && !s.siteCode().isBlank())
                        .sorted(Comparator.comparing(s -> s.name() != null ? s.name() : s.siteCode()))
                        .map(s -> new StationOption(
                                s.siteCode(),
                                (s.name() != null && !s.name().isBlank()) ? s.name() : s.siteCode(),
                                s.kind() != null ? s.kind() : "TERMINAL"))
                        .toList();
            }
        } catch (CaracalApiException | CaracalUnavailableException ex) {
            log.warn("Unable to load live sites from Caracal for tenant {}: {}", tenantCode, ex.getMessage());
        }
        return List.of();
    }

    /**
     * Builds the route dropdown options scoped to the selected origin station.
     * Includes a terminal-wide default ("ALL") plus each destination station from Caracal.
     */
    public List<RouteOption> getRoutesForStation(String tenantCode, String originSiteCode, List<StationOption> stations) {
        List<RouteOption> routes = new ArrayList<>();
        String originLabel = stations.stream()
                .filter(s -> s.code().equals(originSiteCode))
                .map(StationOption::name)
                .findFirst()
                .orElse(originSiteCode);

        routes.add(new RouteOption(
                AvailabilityRule.ALL_ROUTES,
                "All Routes from " + originLabel + " (Terminal-Wide)",
                null));

        for (StationOption dest : stations) {
            if (!dest.code().equals(originSiteCode)) {
                String routeKey = originSiteCode + "->" + dest.code();
                routes.add(new RouteOption(
                        routeKey,
                        originLabel + " \u2192 " + dest.name() + " (" + dest.code() + ")",
                        dest.code()));
            }
        }
        return routes;
    }

    public ProductAvailabilityView getProductAvailability(String tenantCode, String siteCode) {
        return getProductAvailability(tenantCode, siteCode, AvailabilityRule.ALL_ROUTES);
    }

    /**
     * Merges live Caracal rate tables (/functions/rate-table-list, /functions/rate-table-get)
     * and live availability simulations (/functions/availability-simulate) with staged
     * PostgreSQL AvailabilityRule rows for the selected station and route.
     */
    public ProductAvailabilityView getProductAvailability(String tenantCode, String siteCode, String routeCode) {
        String normalizedRoute = (routeCode == null || routeCode.isBlank())
                ? AvailabilityRule.ALL_ROUTES
                : routeCode;

        LinkedHashSet<String> orderedProducts = new LinkedHashSet<>();
        Map<String, String> productNames = new LinkedHashMap<>();
        Map<String, Boolean> toggles = new LinkedHashMap<>();
        Map<String, Boolean> phasedOut = new LinkedHashMap<>();
        Map<String, String> rateAssignmentIds = new LinkedHashMap<>();
        Map<String, String> activeWhenConditions = new LinkedHashMap<>();
        Map<String, String> caracalStatuses = new LinkedHashMap<>();
        Map<String, Boolean> savedInDb = new LinkedHashMap<>();
        Map<String, TemporalCondition> conditions = new HashMap<>();
        String resolvedRateTableName = "RT-" + siteCode;

        if (tenantCode != null && !tenantCode.isBlank()) {
            // 1. Load live product catalog from Caracal (/functions/product-list)
            try {
                List<CaracalCatalogDtos.ProductSummary> liveProducts = caracalApiClient.fetchProducts(tenantCode);
                if (liveProducts != null) {
                    for (CaracalCatalogDtos.ProductSummary p : liveProducts) {
                        if (p.code() != null && !p.code().isBlank() && p.isActive()) {
                            orderedProducts.add(p.code());
                            productNames.put(p.code(), p.name() != null ? p.name() : p.code());
                            toggles.put(p.code(), true);
                        }
                    }
                }
            } catch (CaracalApiException | CaracalUnavailableException ex) {
                log.warn("Caracal product-list unavailable for {}: {}", tenantCode, ex.getMessage());
            }

            // 2. Fetch live rate tables (/functions/rate-table-list) & assignments (/functions/rate-table-get)
            try {
                List<CaracalCatalogDtos.RateTableSummary> tables = caracalApiClient.fetchRateTables(tenantCode);
                Optional<CaracalCatalogDtos.RateTableSummary> matchedTable = tables.stream()
                        .filter(rt -> siteCode.equalsIgnoreCase(rt.siteCode())
                                || ("RT-" + siteCode).equalsIgnoreCase(rt.name()))
                        .findFirst();

                if (matchedTable.isPresent()) {
                    resolvedRateTableName = matchedTable.get().name();
                    CaracalCatalogDtos.RateTableGetResponse tableDetail =
                            caracalApiClient.fetchRateTable(tenantCode, resolvedRateTableName);

                    if (tableDetail != null && tableDetail.assignments() != null) {
                        for (CaracalCatalogDtos.RateAssignmentSummary assignment : tableDetail.assignments()) {
                            String code = assignment.productCode();
                            if (code == null || code.isBlank()) continue;

                            orderedProducts.add(code);
                            productNames.putIfAbsent(code, code);
                            if (assignment.id() != null) {
                                rateAssignmentIds.put(code, assignment.id());
                            }
                            boolean isSellable = !"NOT_SOLD".equalsIgnoreCase(assignment.cellType());
                            toggles.put(code, isSellable);
                            caracalStatuses.put(code, assignment.cellType() != null ? assignment.cellType() : "VALUE");
                        }
                    }
                }
            } catch (CaracalApiException | CaracalUnavailableException ex) {
                log.warn("Caracal rate-table lookup unavailable for {}/{}: {}", tenantCode, siteCode, ex.getMessage());
            }

            // 3. Simulate live availability (/functions/availability-simulate) for active whenConditionName rules
            try {
                Map<String, String> dimensions = extractRouteDimensions(normalizedRoute);
                List<CaracalCatalogDtos.SimulatedProductAvailability> simulated =
                        caracalApiClient.simulateAvailability(tenantCode, siteCode, dimensions);

                for (CaracalCatalogDtos.SimulatedProductAvailability sim : simulated) {
                    String code = sim.productCode();
                    if (code == null || code.isBlank()) continue;

                    orderedProducts.add(code);
                    productNames.putIfAbsent(code, code);
                    if (sim.status() != null) {
                        caracalStatuses.put(code, sim.status());
                        if ("NOT_SOLD".equalsIgnoreCase(sim.status())) {
                            toggles.put(code, false);
                        } else if ("SELLABLE".equalsIgnoreCase(sim.status()) && !toggles.containsKey(code)) {
                            toggles.put(code, true);
                        }
                    }
                    if (sim.matchedCondition() != null && !sim.matchedCondition().isBlank()) {
                        activeWhenConditions.put(code, sim.matchedCondition());
                    }
                }
            } catch (CaracalApiException | CaracalUnavailableException ex) {
                log.warn("Caracal availability-simulate unavailable for {}/{}: {}", tenantCode, siteCode, ex.getMessage());
            }
        }

        // Fallback if Caracal returned nothing (e.g., isolated MockMvc test without stubbed catalog)
        if (orderedProducts.isEmpty()) {
            for (String prod : FALLBACK_PRODUCTS) {
                orderedProducts.add(prod);
                productNames.put(prod, prod);
                toggles.put(prod, true);
            }
        }

        // 4. Overlay staged PostgreSQL AvailabilityRule rows (terminal-wide first, then route-specific override)
        if (tenantCode != null && !tenantCode.isBlank()) {
            // Check tenant-wide phase-outs first (Story 3.2)
            List<AvailabilityRule> tenantRules = availabilityRuleRepository.findByTenantCode(tenantCode);
            for (AvailabilityRule tr : tenantRules) {
                if (tr.isPhasedOut()) {
                    orderedProducts.add(tr.getProductCode());
                    productNames.putIfAbsent(tr.getProductCode(), tr.getProductCode());
                    phasedOut.put(tr.getProductCode(), true);
                    toggles.put(tr.getProductCode(), false);
                }
            }

            // Apply terminal-wide ("ALL") saved rules if viewing a specific route
            if (!AvailabilityRule.ALL_ROUTES.equals(normalizedRoute)) {
                List<AvailabilityRule> terminalRules = availabilityRuleRepository
                        .findByTenantCodeAndSiteCodeAndRouteCode(tenantCode, siteCode, AvailabilityRule.ALL_ROUTES);
                applyDbOverlay(terminalRules, orderedProducts, productNames, toggles, phasedOut,
                        rateAssignmentIds, activeWhenConditions, savedInDb, conditions, false);
            }

            // Apply route-specific saved rules
            List<AvailabilityRule> routeRules = availabilityRuleRepository
                    .findByTenantCodeAndSiteCodeAndRouteCode(tenantCode, siteCode, normalizedRoute);
            applyDbOverlay(routeRules, orderedProducts, productNames, toggles, phasedOut,
                    rateAssignmentIds, activeWhenConditions, savedInDb, conditions, true);
        }

        ProductAvailabilityView view = new ProductAvailabilityView(
                siteCode,
                new ArrayList<>(orderedProducts),
                toggles,
                conditions);
        view.setRouteCode(normalizedRoute);
        view.setRateTableName(resolvedRateTableName);
        view.setProductNames(productNames);
        view.setPhasedOutProducts(phasedOut);
        view.setRateAssignmentIds(rateAssignmentIds);
        view.setActiveWhenConditions(activeWhenConditions);
        view.setCaracalStatuses(caracalStatuses);
        view.setSavedInDb(savedInDb);
        return view;
    }

    private void applyDbOverlay(
            List<AvailabilityRule> rules,
            Set<String> orderedProducts,
            Map<String, String> productNames,
            Map<String, Boolean> toggles,
            Map<String, Boolean> phasedOut,
            Map<String, String> rateAssignmentIds,
            Map<String, String> activeWhenConditions,
            Map<String, Boolean> savedInDb,
            Map<String, TemporalCondition> conditions,
            boolean markAsSavedForCurrentRoute) {

        for (AvailabilityRule rule : rules) {
            String code = rule.getProductCode();
            orderedProducts.add(code);
            productNames.putIfAbsent(code, code);

            if (rule.isPhasedOut()) {
                phasedOut.put(code, true);
                toggles.put(code, false);
            } else if (!phasedOut.getOrDefault(code, false)) {
                toggles.put(code, rule.isAvailable());
            }

            if (rule.getRateAssignmentId() != null && !rule.getRateAssignmentId().isBlank()) {
                rateAssignmentIds.putIfAbsent(code, rule.getRateAssignmentId());
            }
            if (rule.getMatchedCondition() != null && !rule.getMatchedCondition().isBlank()) {
                activeWhenConditions.put(code, rule.getMatchedCondition());
            }
            if (markAsSavedForCurrentRoute) {
                savedInDb.put(code, true);
            }
            if (rule.getTemporalCondition() != null) {
                conditions.put(code, rule.getTemporalCondition());
            }
        }
    }

    @Transactional
    public void stageAvailabilityDrafts(String tenantCode, List<AvailabilityDraftDTO> drafts) {
        String actor = resolveActor();
        boolean isClientServices = hasClientServicesRole();

        for (AvailabilityDraftDTO draft : drafts) {
            String routeCode = (draft.getRouteCode() == null || draft.getRouteCode().isBlank())
                    ? AvailabilityRule.ALL_ROUTES
                    : draft.getRouteCode();

            AvailabilityRule rule = availabilityRuleRepository
                .findByTenantCodeAndSiteCodeAndRouteCodeAndProductCode(
                        tenantCode, draft.getSiteCode(), routeCode, draft.getProductCode())
                .orElseGet(() -> new AvailabilityRule(
                    tenantCode,
                    draft.getSiteCode(),
                    routeCode,
                    draft.getProductCode(),
                    draft.isAvailable()
                ));

            // Story 3.2: Permanent phase-out guardrail
            boolean currentlyPhasedOut = rule.isPhasedOut()
                    || availabilityRuleRepository.findByTenantCodeAndProductCode(tenantCode, draft.getProductCode())
                            .stream().anyMatch(AvailabilityRule::isPhasedOut);

            if (currentlyPhasedOut && (draft.isAvailable() || Boolean.FALSE.equals(draft.getPhasedOut()))) {
                if (!isClientServices) {
                    throw new AccessDeniedException(
                            "Only CLIENT_SERVICES administrators can reactivate permanently phased-out product: "
                                    + draft.getProductCode());
                }
                // Re-enable across tenant if CLIENT_SERVICES explicitly lifts phase-out
                List<AvailabilityRule> phasedRows = availabilityRuleRepository
                        .findByTenantCodeAndProductCode(tenantCode, draft.getProductCode());
                for (AvailabilityRule r : phasedRows) {
                    r.setPhasedOut(false);
                    r.setAvailable(draft.isAvailable());
                    r.setSyncedToCaracal(false);
                    r.setUpdatedBy(actor);
                    availabilityRuleRepository.save(r);
                }
                rule.setPhasedOut(false);
            }

            if (Boolean.TRUE.equals(draft.getPhasedOut())) {
                // Permanently phase out across all existing rules for this tenant + product
                List<AvailabilityRule> existingForProduct = availabilityRuleRepository
                        .findByTenantCodeAndProductCode(tenantCode, draft.getProductCode());
                for (AvailabilityRule r : existingForProduct) {
                    r.setPhasedOut(true);
                    r.setAvailable(false);
                    r.setSyncedToCaracal(false);
                    r.setUpdatedBy(actor);
                    availabilityRuleRepository.save(r);
                }
                rule.setPhasedOut(true);
                rule.setAvailable(false);
            } else {
                rule.setAvailable(draft.isAvailable());
            }

            // Capture Caracal rateAssignmentId if available so Story 3.4 sync can update it
            attachCaracalAssignmentMetadata(tenantCode, draft.getSiteCode(), rule);

            rule.setSyncedToCaracal(false);
            rule.setUpdatedBy(actor);
            rule.setLastUpdated(LocalDateTime.now());

            if (draft.isClearCondition()) {
                rule.setTemporalCondition(null);
            } else if (draft.getConditionType() != null && !draft.getConditionType().isBlank()) {
                TemporalCondition condition = rule.getTemporalCondition() != null
                    ? rule.getTemporalCondition()
                    : new TemporalCondition();

                String condName = (draft.getDiscountLabel() != null && !draft.getDiscountLabel().isBlank())
                        ? draft.getDiscountLabel().trim()
                        : draft.getProductCode() + "_" + draft.getSiteCode() + "_RULE";

                condition.setName(condName);
                condition.setDiscountLabel(draft.getDiscountLabel());
                condition.setConditionType(draft.getConditionType());
                condition.setDaysOfWeek(draft.getDaysOfWeek() != null ? draft.getDaysOfWeek() : List.of());
                condition.setDayOfMonthStart(draft.getDayOfMonthStart());
                condition.setDayOfMonthEnd(draft.getDayOfMonthEnd());

                condition.setStartTime(parseLocalTime(draft.getStartTime()));
                condition.setEndTime(parseLocalTime(draft.getEndTime()));
                condition.setStartDate(parseLocalDate(draft.getStartDate()));
                condition.setEndDate(parseLocalDate(draft.getEndDate()));

                rule.setTemporalCondition(condition);
            }

            availabilityRuleRepository.save(rule);
        }
    }

    /**
     * Story 3.4: Synchronizes staged PostgreSQL availability rules & temporal conditions
     * to Caracal via /functions/when-condition-create and /functions/rate-assignment-update.
     */
    @Transactional
    public int syncAvailabilityRulesToCaracal(String tenantCode) {
        List<AvailabilityRule> pendingRules = availabilityRuleRepository.findByTenantCodeAndSyncedToCaracalFalse(tenantCode);
        if (pendingRules.isEmpty()) {
            return 0;
        }

        int syncedCount = 0;
        for (AvailabilityRule rule : pendingRules) {
            String actor = (rule.getUpdatedBy() != null && !rule.getUpdatedBy().isBlank())
                    ? rule.getUpdatedBy()
                    : resolveActor();

            attachCaracalAssignmentMetadata(tenantCode, rule.getSiteCode(), rule);

            if (rule.getTemporalCondition() != null) {
                TemporalCondition tc = rule.getTemporalCondition();
                String caracalCondType = mapToCaracalConditionType(tc);
                String uniqueConditionName = sanitizeConditionName(
                        rule.getProductCode() + "_" + rule.getSiteCode() + "_" + System.currentTimeMillis());

                CaracalCatalogDtos.WhenConditionCreateRequest wcReq =
                        new CaracalCatalogDtos.WhenConditionCreateRequest(
                                tenantCode,
                                uniqueConditionName,
                                caracalCondType,
                                mapDaysOfWeek(tc.getDaysOfWeek()),
                                tc.getStartTime() != null ? tc.getStartTime().format(DateTimeFormatter.ISO_LOCAL_TIME) : null,
                                tc.getEndTime() != null ? tc.getEndTime().format(DateTimeFormatter.ISO_LOCAL_TIME) : null,
                                tc.getStartDate() != null ? tc.getStartDate().atStartOfDay().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null,
                                tc.getEndDate() != null ? tc.getEndDate().atTime(23, 59, 59).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null,
                                null,
                                null,
                                tc.getDayOfMonthStart(),
                                tc.getDayOfMonthEnd());

                caracalApiClient.createWhenCondition(wcReq, actor);
                rule.setMatchedCondition(uniqueConditionName);
            }

            if (rule.getRateAssignmentId() != null && !rule.getRateAssignmentId().isBlank()) {
                String cellType = rule.isAvailable() ? "VALUE" : "NOT_SOLD";
                CaracalCatalogDtos.RateAssignmentUpdateRequest updateReq =
                        new CaracalCatalogDtos.RateAssignmentUpdateRequest(
                                tenantCode,
                                rule.getRateAssignmentId(),
                                cellType,
                                null,
                                null,
                                null,
                                null,
                                null);
                caracalApiClient.updateRateAssignment(updateReq, actor);
            }

            rule.setSyncedToCaracal(true);
            availabilityRuleRepository.save(rule);
            syncedCount++;
        }
        return syncedCount;
    }

    private void attachCaracalAssignmentMetadata(String tenantCode, String siteCode, AvailabilityRule rule) {
        if (rule.getRateAssignmentId() != null && !rule.getRateAssignmentId().isBlank()) {
            return;
        }
        try {
            List<CaracalCatalogDtos.RateTableSummary> tables = caracalApiClient.fetchRateTables(tenantCode);
            Optional<CaracalCatalogDtos.RateTableSummary> matched = tables.stream()
                    .filter(rt -> siteCode.equalsIgnoreCase(rt.siteCode())
                            || ("RT-" + siteCode).equalsIgnoreCase(rt.name()))
                    .findFirst();
            if (matched.isPresent()) {
                rule.setRateTableName(matched.get().name());
                CaracalCatalogDtos.RateTableGetResponse detail =
                        caracalApiClient.fetchRateTable(tenantCode, matched.get().name());
                if (detail != null && detail.assignments() != null) {
                    detail.assignments().stream()
                            .filter(a -> rule.getProductCode().equalsIgnoreCase(a.productCode()))
                            .findFirst()
                            .ifPresent(a -> {
                                rule.setRateAssignmentId(a.id());
                                rule.setPriceRuleSetName(a.priceRuleSetName());
                                rule.setRateScheduleName(a.rateScheduleName());
                            });
                }
            }
        } catch (CaracalApiException | CaracalUnavailableException ex) {
            log.debug("Skipping live assignment metadata attachment for {}/{}: {}", tenantCode, siteCode, ex.getMessage());
        }
    }

    private Map<String, String> extractRouteDimensions(String routeCode) {
        if (routeCode == null || AvailabilityRule.ALL_ROUTES.equals(routeCode) || !routeCode.contains("->")) {
            return Map.of();
        }
        String[] parts = routeCode.split("->", 2);
        return Map.of("ORIGIN", parts[0], "DEST", parts[1]);
    }

    private String mapToCaracalConditionType(TemporalCondition tc) {
        if ("RECURRING_MONTHLY".equals(tc.getConditionType())) {
            return "DAY_OF_MONTH";
        }
        if ("TEMPORARY_PROMO".equals(tc.getConditionType())) {
            return "DATE_RANGE";
        }
        if (tc.getDaysOfWeek() != null && !tc.getDaysOfWeek().isEmpty()) {
            return "WEEKLY";
        }
        return "TIME_WINDOW";
    }

    private List<String> mapDaysOfWeek(List<Integer> days) {
        if (days == null || days.isEmpty()) {
            return List.of();
        }
        return days.stream()
                .filter(d -> d >= 1 && d <= 7)
                .map(d -> switch (DayOfWeek.of(d)) {
                    case MONDAY -> "MON";
                    case TUESDAY -> "TUE";
                    case WEDNESDAY -> "WED";
                    case THURSDAY -> "THU";
                    case FRIDAY -> "FRI";
                    case SATURDAY -> "SAT";
                    case SUNDAY -> "SUN";
                })
                .toList();
    }

    private String sanitizeConditionName(String raw) {
        return raw.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private LocalTime parseLocalTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LocalTime.parse(raw.length() == 5 ? raw + ":00" : raw);
    }

    private LocalDate parseLocalDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LocalDate.parse(raw);
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