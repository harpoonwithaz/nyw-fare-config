package ca.sfu.cmpt276.nywfareconfig.fare.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.RateTableGetRequest;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulateRequest;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalZonalDtos;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.CatalogOption;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareGrid;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareRule;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.DraftStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareCell;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.repository.FareDraftRepository;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

@Service
public class FareService {

    private static final Logger log = LoggerFactory.getLogger(FareService.class);

    public static final String DEFAULT_FARE_CLASS = "FULL";
    private static final String DEFAULT_CHANNEL = "UNATTENDED";

    // Price simulate can return 4xx.
    // Lowercased constants to match against lowercased Caracal error messages:
    private static final String NO_PRICE_RULE = "no pricerule matched the resolved dimension context";
    private static final String NOT_SOLD = "not sold";
    private static final String CONDITION = "no rate assignment's condition matches the requested moment or channel";

    private final FareDraftRepository fareDraftRepository;
    private final CaracalApiClient caracalApiClient;

    public FareService(FareDraftRepository fareDraftRepository, CaracalApiClient caracalApiClient) {
        this.fareDraftRepository = fareDraftRepository;
        this.caracalApiClient = caracalApiClient;
    }

    /**
     * Validates and normalizes an incoming UI pricingDate string (YYYY-MM-DD or ISO-8601 offset datetime)
     * into a YYYY-MM-DD date string compatible with HTML date inputs without making any external API calls.
     * Returns null for null/blank input.
     */
    public static String normalizePricingDate(String at) {
        if (at == null || at.isBlank()) {
            return null;
        }
        String trimmed = at.trim();
        try {
            return OffsetDateTime.parse(trimmed).toLocalDate().toString();
        } catch (DateTimeParseException ignored) {
            // Fall through and validate as YYYY-MM-DD
        }

        return parseLocalDate(trimmed, at).toString();
    }

    /**
     * Resolves a pricing date or offset timestamp into an ISO-8601 offset timestamp
     * using the origin terminal's timezone (fetching site metadata only when needed).
     */
    public String resolvePricingTimestamp(String tenantCode, String originSiteCode, String at) {
        if (isBlank(at)) {
            return null;
        }
        String trimmed = at.trim();
        String offsetTimestamp = tryFormatOffsetDateTime(trimmed);
        if (offsetTimestamp != null) {
            return offsetTimestamp;
        }

        // Validate date format before making any network call to Caracal
        LocalDate date = parseLocalDate(trimmed, at);
        ZoneId siteZone = findSiteZoneId(
                caracalApiClient.fetchSites(normalizeCode(tenantCode)),
                normalizeCode(originSiteCode));
        return formatLocalDateAtZone(date, siteZone);
    }

    /**
     * Resolves a pricing date or offset timestamp into an ISO-8601 offset timestamp
     * using the tenant's timezone (first non-null site timezone, e.g., America/Chicago for Metra).
     */
    public String resolveZonalPricingTimestamp(String tenantCode, String at) {
        if (isBlank(at)) {
            return null;
        }
        String trimmed = at.trim();
        String offsetTimestamp = tryFormatOffsetDateTime(trimmed);
        if (offsetTimestamp != null) {
            return offsetTimestamp;
        }

        // Validate date format before making any network call to Caracal
        LocalDate date = parseLocalDate(trimmed, at);
        ZoneId tenantZone = findTenantZoneId(caracalApiClient.fetchSites(normalizeCode(tenantCode)));
        return formatLocalDateAtZone(date, tenantZone);
    }

    /**
     * RAW BASELINE LOOKUP.
     * Never queries FareDraftRepository; returns hasPendingDraft = false.
     */
    public Optional<FareRule> getBaselineFareRule(Tenant tenant, String origin, String dest, String product) {
        return getBaselineFareRule(tenant, origin, dest, product, null, DEFAULT_FARE_CLASS);
    }

    public Optional<FareRule> getBaselineFareRule(
            Tenant tenant, String origin, String dest, String product, String at) {
        return getBaselineFareRule(tenant, origin, dest, product, at, DEFAULT_FARE_CLASS);
    }

    public Optional<FareRule> getBaselineFareRule(
            Tenant tenant, String origin, String dest, String product, String at, String fareClass) {
        if (tenant == null || isBlank(tenant.getCode())
                || isBlank(origin) || isBlank(dest) || isBlank(product)) {
            return Optional.empty();
        }

        String normalizedTenant = normalizeCode(tenant.getCode());
        String normalizedOrigin = normalizeCode(origin);
        String normalizedDest = normalizeCode(dest);
        String normalizedProduct = normalizeCode(product);

        if (tenant.getGridMode() == GridMode.OD) {
            String formattedAt = resolvePricingTimestamp(normalizedTenant, normalizedOrigin, at);
            return simulateOdFareRuleWithFormattedAt(
                    normalizedTenant, normalizedOrigin, normalizedDest, normalizedProduct, formattedAt);
        }

        String normalizedFareClass = normalizeFareClass(fareClass);
        String formattedAt = resolveZonalPricingTimestamp(normalizedTenant, at);
        return simulateZonalFareRuleWithFormattedAt(
                normalizedTenant,
                normalizedOrigin,
                normalizedDest,
                normalizedProduct,
                normalizedFareClass,
                formattedAt);
    }

    /**
     * EFFECTIVE LOOKUP.
     * Returns baseline rule overlaid with any PENDING FareDraft from PostgreSQL.
     */
    public Optional<FareRule> getFareRule(Tenant tenant, String origin, String dest, String product) {
        return getFareRule(tenant, origin, dest, product, null, DEFAULT_FARE_CLASS);
    }

    public Optional<FareRule> getFareRule(
            Tenant tenant, String origin, String dest, String product, String at) {
        return getFareRule(tenant, origin, dest, product, at, DEFAULT_FARE_CLASS);
    }

    public Optional<FareRule> getFareRule(
            Tenant tenant, String origin, String dest, String product, String at, String fareClass) {
        Optional<FareRule> ruleOpt = getBaselineFareRule(tenant, origin, dest, product, at, fareClass);
        if (ruleOpt.isEmpty()) {
            return Optional.empty();
        }

        FareRule rule = ruleOpt.get();
        fareDraftRepository
                .findByTenantCodeAndOriginIdentifierAndDestIdentifierAndProductCodeAndDraftStatus(
                        normalizeCode(tenant.getCode()),
                        rule.getOriginIdentifier(),
                        rule.getDestIdentifier(),
                        rule.getProductCode(),
                        DraftStatus.PENDING)
                .ifPresent(draft -> applyDraftOverlay(rule, draft));

        return Optional.of(rule);
    }

    /**
     * Returns secondary filter options depending on the tenant mode:
     * - O-D Mode: Origin Terminal codes
     * - Zonal Mode: Ticket Product codes
     */
    public List<CatalogOption> getSecondaryFilterOptions(String tenantCode, GridMode gridMode) {
        if (isBlank(tenantCode) || gridMode == null) {
            return List.of();
        }

        String normalizedTenant = normalizeCode(tenantCode);
        switch (gridMode) {
            case GridMode.OD:
                return getSitesCatalog(normalizedTenant);
            case GridMode.ZONAL:
                return getProductsCatalog(normalizedTenant);
            default:
                return List.of();
        }
    }

    /**
     * Builds the structured 2D FareGrid view-model based on the active mode.
     */
    public FareGrid buildGrid(Tenant tenant, String filterValue) {
        return buildGrid(tenant, filterValue, null, DEFAULT_FARE_CLASS);
    }

    public FareGrid buildGrid(Tenant tenant, String filterValue, String at) {
        return buildGrid(tenant, filterValue, at, DEFAULT_FARE_CLASS);
    }

    public FareGrid buildGrid(Tenant tenant, String filterValue, String at, String fareClass) {
        if (tenant == null || isBlank(tenant.getCode()) || isBlank(filterValue)) {
            return null;
        }
        // Fail fast on malformed pricingDate before making any Caracal API calls
        normalizePricingDate(at);

        String normalizedFilter = normalizeCode(filterValue);
        if (tenant.getGridMode() == GridMode.ZONAL) {
            return buildZonalGrid(tenant, normalizedFilter, at, normalizeFareClass(fareClass));
        }
        return buildOdGrid(tenant, normalizedFilter, at);
    }

    /**
     * Builds Point-to-Point (O-D) matrix from normalized FareRule entities.
     * - Rows: Outbound Destination Terminals
     * - Columns: Ticket Products
     */
    private FareGrid buildOdGrid(Tenant tenant, String originSiteCode, String at) {
        String normalizedTenant = normalizeCode(tenant.getCode());
        String normalizedOrigin = normalizeCode(originSiteCode);

        List<CatalogOption> colHeaders = getOdProducts(normalizedTenant).reversed();
        // Follow conventions of the NYW spreadsheet
        List<CaracalCatalogDtos.SiteSummary> sites = caracalApiClient.fetchSites(normalizedTenant);
        List<CatalogOption> outboundDestinations = toOutboundDestinations(sites, normalizedOrigin);
        String formattedAt = formatAt(at, findSiteZoneId(sites, normalizedOrigin));

        Map<String, FareRule> ruleMap = indexRulesByCoordinates(
                getOdFareRulesWithFormattedAt(
                        normalizedTenant, normalizedOrigin, outboundDestinations, colHeaders, formattedAt));

        List<CatalogOption> visibleRowHeaders = new ArrayList<>();
        List<List<FareCell>> rows = new ArrayList<>();

        for (CatalogOption dest : outboundDestinations) {
            List<FareCell> rowCells = new ArrayList<>();
            boolean hasVisibleCell = false;

            for (CatalogOption prod : colHeaders) {
                FareRule rule = ruleMap.get(coordinateKey(normalizedOrigin, dest.code(), prod.code()));
                FareCell cell = toFareCell(rule, dest.code(), prod.code());
                rowCells.add(cell);

                if (cell.getStatus() == FareStatus.ACTIVE || cell.isHasPendingDraft()) {
                    hasVisibleCell = true;
                }
            }

            if (hasVisibleCell) {
                visibleRowHeaders.add(dest);
                rows.add(rowCells);
            }
        }

        return new FareGrid(colHeaders, visibleRowHeaders, rows);
    }

    /**
     * Builds Zone-to-Zone matrix from normalized FareRule entities.
     * - Rows: Origin Zones
     * - Columns: Destination Zones
     */
    private FareGrid buildZonalGrid(Tenant tenant, String productCode, String at, String fareClass) {
        String normalizedTenant = normalizeCode(tenant.getCode());
        String normalizedProduct = normalizeCode(productCode);
        String normalizedFareClass = normalizeFareClass(fareClass);

        List<CatalogOption> zones = getZones(normalizedTenant);
        if (zones.isEmpty()) {
            return new FareGrid(List.of(), List.of(), List.of());
        }

        String formattedAt = resolveZonalPricingTimestamp(normalizedTenant, at);
        Map<String, FareRule> ruleMap = indexRulesByCoordinates(
                getZonalFareRulesWithFormattedAt(
                        normalizedTenant, normalizedProduct, zones, normalizedFareClass, formattedAt));

        List<List<FareCell>> rows = new ArrayList<>();

        for (CatalogOption originZone : zones) {
            List<FareCell> rowCells = new ArrayList<>();
            for (CatalogOption destZone : zones) {
                FareRule rule = ruleMap.get(coordinateKey(originZone.code(), destZone.code(), normalizedProduct));
                rowCells.add(toFareCell(rule, originZone.code(), destZone.code()));
            }
            rows.add(rowCells);
        }

        return new FareGrid(zones, zones, rows);
    }

    /**
     * Converts a normalized FareRule into a presentation FareCell for the 2D matrix,
     * preserving tierCode and whether the cell has a staged PENDING draft in PostgreSQL.
     */
    private FareCell toFareCell(FareRule rule, String rowKey, String colKey) {
        if (rule == null || rule.getStatus() == null || rule.getStatus() == FareStatus.NA) {
            return new FareCell(null, FareStatus.NA, false, rowKey, colKey, null);
        }
        if (rule.getStatus() == FareStatus.NOT_SOLD || rule.getPrice() == null) {
            return new FareCell(
                    rule.getPrice(),
                    rule.getStatus(),
                    rule.isHasPendingDraft(),
                    rowKey,
                    colKey,
                    rule.getTierCode());
        }

        BigDecimal price = rule.getPrice().setScale(2, RoundingMode.HALF_UP);
        return new FareCell(
                price,
                rule.getStatus(),
                rule.isHasPendingDraft(),
                rowKey,
                colKey,
                rule.getTierCode());
    }

    // =========================================================================
    // DATA PROVIDER METHODS (Baseline + Draft Overlay)
    // =========================================================================

    /**
     * Returns O-D FareRule entities overlaid with any PENDING drafts in PostgreSQL.
     */
    public List<FareRule> getOdFareRules(String tenantCode, String originSiteCode) {
        return getOdFareRules(tenantCode, originSiteCode, null);
    }

    public List<FareRule> getOdFareRules(String tenantCode, String originSiteCode, String at) {
        if (isBlank(tenantCode) || isBlank(originSiteCode)) {
            return List.of();
        }

        List<FareRule> rules = getBaselineOdFareRules(tenantCode, originSiteCode, at);
        applyPendingDraftsToRules(normalizeCode(tenantCode), rules);
        return rules;
    }

    private List<FareRule> getOdFareRulesWithFormattedAt(
            String normalizedTenant,
            String normalizedOrigin,
            List<CatalogOption> destinations,
            List<CatalogOption> products,
            String formattedAt) {
        List<FareRule> rules = getBaselineOdFareRulesWithFormattedAt(
                normalizedTenant, normalizedOrigin, destinations, products, formattedAt);
        applyPendingDraftsToRules(normalizedTenant, rules);
        return rules;
    }

    /**
     * Returns untouched baseline O-D FareRule entities (never overlaid with drafts).
     */
    public List<FareRule> getBaselineOdFareRules(String tenantCode, String originSiteCode) {
        return getBaselineOdFareRules(tenantCode, originSiteCode, null);
    }

    public List<FareRule> getBaselineOdFareRules(String tenantCode, String originSiteCode, String at) {
        if (isBlank(tenantCode) || isBlank(originSiteCode)) {
            return List.of();
        }
        String normalizedTenant = normalizeCode(tenantCode);
        String normalizedOrigin = normalizeCode(originSiteCode);
        List<CaracalCatalogDtos.SiteSummary> sites = caracalApiClient.fetchSites(normalizedTenant);
        List<CatalogOption> destinations = toOutboundDestinations(sites, normalizedOrigin);
        List<CatalogOption> products = getOdProducts(normalizedTenant);
        String formattedAt = formatAt(at, findSiteZoneId(sites, normalizedOrigin));

        return getBaselineOdFareRulesWithFormattedAt(
                normalizedTenant, normalizedOrigin, destinations, products, formattedAt);
    }

    private List<FareRule> getBaselineOdFareRulesWithFormattedAt(
            String normalizedTenant,
            String normalizedOrigin,
            List<CatalogOption> destinations,
            List<CatalogOption> products,
            String formattedAt) {
        Set<String> soldProductCodes = getRateTable(normalizedTenant, normalizedOrigin);
        if (soldProductCodes.isEmpty()) {
            return List.of();
        }

        List<FareRule> rules = new ArrayList<>();

        for (CatalogOption dest : destinations) {
            for (CatalogOption prod : products) {
                String normalizedProduct = normalizeCode(prod.code());
                if (!soldProductCodes.contains(normalizedProduct)) {
                    rules.add(FareRule.od(
                            normalizedTenant,
                            normalizedOrigin,
                            dest.code(),
                            normalizedProduct,
                            null,
                            null,
                            FareStatus.NOT_SOLD));
                    continue;
                }

                simulateOdFareRuleWithFormattedAt(
                        normalizedTenant, normalizedOrigin, dest.code(), normalizedProduct, formattedAt)
                        .ifPresent(rules::add);
            }
        }

        return rules;
    }

    /**
     * Returns Zonal FareRule entities overlaid with any PENDING drafts in PostgreSQL.
     */
    public List<FareRule> getZonalFareRules(String tenantCode, String productCode) {
        return getZonalFareRules(tenantCode, productCode, null, DEFAULT_FARE_CLASS);
    }

    public List<FareRule> getZonalFareRules(
            String tenantCode, String productCode, String at, String fareClass) {
        if (isBlank(tenantCode) || isBlank(productCode)) {
            return List.of();
        }

        String normalizedTenant = normalizeCode(tenantCode);
        List<FareRule> rules = getBaselineZonalFareRules(normalizedTenant, productCode, at, fareClass);
        applyPendingDraftsToRules(normalizedTenant, rules);
        return rules;
    }

    private List<FareRule> getZonalFareRulesWithFormattedAt(
            String normalizedTenant,
            String normalizedProduct,
            List<CatalogOption> zones,
            String normalizedFareClass,
            String formattedAt) {
        List<FareRule> rules = getBaselineZonalFareRulesWithFormattedAt(
                normalizedTenant, normalizedProduct, zones, normalizedFareClass, formattedAt);
        applyPendingDraftsToRules(normalizedTenant, rules);
        return rules;
    }

    /**
     * Returns untouched baseline Zonal FareRule entities (never overlaid with drafts),
     * with rate tiers synthesized from distinct ACTIVE prices in ascending order.
     */
    public List<FareRule> getBaselineZonalFareRules(String tenantCode, String productCode) {
        return getBaselineZonalFareRules(tenantCode, productCode, null, DEFAULT_FARE_CLASS);
    }

    public List<FareRule> getBaselineZonalFareRules(
            String tenantCode, String productCode, String at, String fareClass) {
        if (isBlank(tenantCode) || isBlank(productCode)) {
            return List.of();
        }

        String normalizedTenant = normalizeCode(tenantCode);
        String normalizedProduct = normalizeCode(productCode);
        String normalizedFareClass = normalizeFareClass(fareClass);

        List<CatalogOption> zones = getZones(normalizedTenant);
        if (zones.isEmpty()) {
            return List.of();
        }

        String formattedAt = resolveZonalPricingTimestamp(normalizedTenant, at);
        return getBaselineZonalFareRulesWithFormattedAt(
                normalizedTenant, normalizedProduct, zones, normalizedFareClass, formattedAt);
    }

    private List<FareRule> getBaselineZonalFareRulesWithFormattedAt(
            String normalizedTenant,
            String normalizedProduct,
            List<CatalogOption> zones,
            String normalizedFareClass,
            String formattedAt) {
        if (zones == null || zones.isEmpty()) {
            return List.of();
        }

        List<FareRule> rules = new ArrayList<>();

        for (CatalogOption originZone : zones) {
            for (CatalogOption destZone : zones) {
                simulateZonalFareRuleWithFormattedAt(
                        normalizedTenant,
                        originZone.code(),
                        destZone.code(),
                        normalizedProduct,
                        normalizedFareClass,
                        formattedAt)
                        .ifPresent(rules::add);
            }
        }

        assignBaselineZonalTiers(rules);
        return rules;
    }

    /**
     * Returns ticket product codes for O-D tenants.
     */
    public List<CatalogOption> getOdProducts(String tenantCode) {
        if (isBlank(tenantCode)) {
            return List.of();
        }
        return getProductsCatalog(normalizeCode(tenantCode));
    }

    /**
     * Returns outbound destination terminals for a given origin terminal in O-D mode.
     */
    public List<CatalogOption> getOutboundDestinations(String tenantCode, String originSiteCode) {
        if (isBlank(tenantCode)) {
            return List.of();
        }
        String normalizedOrigin = normalizeCode(originSiteCode);
        return getSitesCatalog(normalizeCode(tenantCode)).stream()
                .filter(destination -> normalizedOrigin == null
                        || !destination.code().equalsIgnoreCase(normalizedOrigin))
                .toList();
    }

    /**
     * Returns ordered fare zones for Zonal tenants (e.g., Zone A - Zone J for Metra).
     */
    public List<CatalogOption> getZones(String tenantCode) {
        if (isBlank(tenantCode)) {
            return List.of();
        }
        return toCatalogOptions(caracalApiClient.fetchZones(normalizeCode(tenantCode)));
    }

    /**
     * Returns fare class options for Zonal tenants (e.g., FULL, REDUCED, SCPP for Metra).
     */
    public List<CatalogOption> getFareClasses(String tenantCode) {
        if (isBlank(tenantCode)) {
            return List.of();
        }
        return toCatalogOptions(caracalApiClient.fetchFareClasses(normalizeCode(tenantCode)));
    }

    // =========================================================================
    // CARACAL API CLIENT HELPERS
    // =========================================================================

    private List<CatalogOption> getProductsCatalog(String normalizedTenant) {
        List<CaracalCatalogDtos.ProductSummary> productList = caracalApiClient.fetchProducts(normalizedTenant);
        if (productList == null) {
            return List.of();
        }

        List<CatalogOption> products = new ArrayList<>();
        for (CaracalCatalogDtos.ProductSummary product : productList) {
            if (isBlank(product.code()) || !product.isActive()) {
                continue;
            }

            products.add(new CatalogOption(normalizeCode(product.code()), product.name()));
        }

        return products;
    }

    private List<CatalogOption> getSitesCatalog(String normalizedTenant) {
        List<CaracalCatalogDtos.SiteSummary> siteList = caracalApiClient.fetchSites(normalizedTenant);
        if (siteList == null) {
            return List.of();
        }

        List<CatalogOption> sites = new ArrayList<>();
        for (CaracalCatalogDtos.SiteSummary site : siteList) {
            if (isBlank(site.siteCode())) {
                continue;
            }

            sites.add(new CatalogOption(normalizeCode(site.siteCode()), site.name()));
        }

        return sites;
    }

    private List<CatalogOption> toCatalogOptions(List<CaracalZonalDtos.DimensionValueSummary> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<CatalogOption> options = new ArrayList<>();
        for (CaracalZonalDtos.DimensionValueSummary value : values) {
            if (value == null || isBlank(value.key())) {
                continue;
            }
            String normalizedKey = normalizeCode(value.key());
            String displayName = isBlank(value.name()) ? normalizedKey : value.name().trim();
            options.add(new CatalogOption(normalizedKey, displayName));
        }

        return options;
    }

    private Set<String> getRateTable(String normalizedTenant, String normalizedOrigin) {
        Optional<List<CaracalCatalogDtos.RateAssignmentSummary>> rateAssignmentList = caracalApiClient.fetchRateTable(
                new RateTableGetRequest(normalizedTenant, "RT-" + normalizedOrigin));
        if (rateAssignmentList == null || rateAssignmentList.isEmpty()) {
            return Set.of();
        }

        Set<String> soldProductCodes = new HashSet<>();
        for (CaracalCatalogDtos.RateAssignmentSummary assignment : rateAssignmentList.get()) {
            if (isBlank(assignment.productCode())) {
                continue;
            }
            if (!"VALUE".equalsIgnoreCase(assignment.cellType())) {
                continue;
            }

            soldProductCodes.add(normalizeCode(assignment.productCode()));
        }

        return soldProductCodes;
    }

    private Optional<FareRule> simulateOdFareRuleWithFormattedAt(
            String normalizedTenant,
            String normalizedOrigin,
            String normalizedDest,
            String normalizedProduct,
            String formattedAt) {
        Map<String, String> dimensions = Map.of("DEST", normalizedDest);

        CaracalSimulationDtos.PriceSimulationResult simulatedPrice = caracalApiClient.simulatePrice(
                new PriceSimulateRequest(
                        normalizedTenant, normalizedProduct, normalizedOrigin, dimensions, formattedAt));

        return classifySimulationResult(
                simulatedPrice, normalizedTenant, normalizedOrigin, normalizedDest, normalizedProduct)
                .map(result -> FareRule.od(
                        normalizedTenant,
                        normalizedOrigin,
                        normalizedDest,
                        normalizedProduct,
                        result.priceRuleSetName(),
                        null,
                        result.price(),
                        result.status()));
    }

    private Optional<FareRule> simulateZonalFareRuleWithFormattedAt(
            String normalizedTenant,
            String normalizedOriginZone,
            String normalizedDestZone,
            String normalizedProduct,
            String normalizedFareClass,
            String formattedAt) {
        Map<String, String> dimensions = Map.of(
                "ORIGIN_ZONE", normalizedOriginZone,
                "DEST_ZONE", normalizedDestZone,
                "FARE_CLASS", normalizedFareClass);

        CaracalSimulationDtos.PriceSimulationResult simulatedPrice = caracalApiClient.simulatePrice(
                new PriceSimulateRequest(
                        normalizedTenant,
                        normalizedProduct,
                        null,
                        dimensions,
                        formattedAt,
                        DEFAULT_CHANNEL));

        return classifySimulationResult(
                simulatedPrice, normalizedTenant, normalizedOriginZone, normalizedDestZone, normalizedProduct)
                .map(result -> FareRule.zonal(
                        normalizedTenant,
                        normalizedOriginZone,
                        normalizedDestZone,
                        normalizedProduct,
                        result.priceRuleSetName(),
                        null,
                        result.price(),
                        result.status()));
    }

    private Optional<ClassifiedSimulation> classifySimulationResult(
            CaracalSimulationDtos.PriceSimulationResult simulatedPrice,
            String normalizedTenant,
            String origin,
            String dest,
            String normalizedProduct) {
        if (simulatedPrice == null) {
            return Optional.empty();
        }

        if (simulatedPrice.response() == null || simulatedPrice.response().pricing() == null) {
            String msg = simulatedPrice.errorMessage() != null
                    ? simulatedPrice.errorMessage().toLowerCase(Locale.ROOT)
                    : "";

            if (msg.contains(NOT_SOLD) || msg.contains(CONDITION)) {
                return Optional.of(new ClassifiedSimulation(null, null, FareStatus.NOT_SOLD));
            }

            if (!msg.contains(NO_PRICE_RULE)) {
                log.warn("Unrecognized Caracal 4xx response for {} {}->{} ({}): {}",
                        normalizedTenant, origin, dest, normalizedProduct,
                        simulatedPrice.errorMessage());
            }

            return Optional.of(new ClassifiedSimulation(null, null, FareStatus.NA));
        }

        CaracalSimulationDtos.PricingDetail fare = simulatedPrice.response().pricing();
        if (fare.total() == null) {
            log.warn("Caracal returned null total for {} {}->{} ({}); treating as N/A.",
                    normalizedTenant, origin, dest, normalizedProduct);
            return Optional.of(new ClassifiedSimulation(fare.priceRuleSetName(), null, FareStatus.NA));
        }

        BigDecimal total = fare.total().setScale(2, RoundingMode.HALF_UP);
        return Optional.of(new ClassifiedSimulation(fare.priceRuleSetName(), total, FareStatus.ACTIVE));
    }

    // =========================================================================
    // INTERNAL HELPERS & COORDINATE INDEXING
    // =========================================================================

    /**
     * Synthesizes sequential rate tier codes (TIER_A, TIER_B, ...) by grouping
     * distinct ACTIVE baseline prices in ascending order before draft overlay.
     */
    private void assignBaselineZonalTiers(List<FareRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }

        TreeSet<BigDecimal> distinctPrices = new TreeSet<>();
        for (FareRule rule : rules) {
            if (rule.getStatus() == FareStatus.ACTIVE && rule.getPrice() != null) {
                distinctPrices.add(rule.getPrice().setScale(2, RoundingMode.HALF_UP));
            }
        }

        if (distinctPrices.isEmpty()) {
            return;
        }

        Map<BigDecimal, String> priceToTierCode = new HashMap<>();
        int index = 0;
        for (BigDecimal price : distinctPrices) {
            priceToTierCode.put(price, toTierCode(index++));
        }

        for (FareRule rule : rules) {
            if (rule.getStatus() == FareStatus.ACTIVE && rule.getPrice() != null) {
                BigDecimal scaledPrice = rule.getPrice().setScale(2, RoundingMode.HALF_UP);
                rule.setTierCode(priceToTierCode.get(scaledPrice));
            }
        }
    }

    private static String toTierCode(int index) {
        if (index < 26) {
            return "TIER_" + (char) ('A' + index);
        }
        return "TIER_" + (index + 1);
    }

    private List<CatalogOption> toOutboundDestinations(
            List<CaracalCatalogDtos.SiteSummary> siteList,
            String normalizedOrigin) {
        if (siteList == null) {
            return List.of();
        }

        List<CatalogOption> destinations = new ArrayList<>();
        for (CaracalCatalogDtos.SiteSummary site : siteList) {
            if (isBlank(site.siteCode())) {
                continue;
            }
            String normalizedSiteCode = normalizeCode(site.siteCode());
            if (normalizedOrigin != null && normalizedSiteCode.equalsIgnoreCase(normalizedOrigin)) {
                continue;
            }
            destinations.add(new CatalogOption(normalizedSiteCode, site.name()));
        }
        return destinations;
    }

    private ZoneId findSiteZoneId(List<CaracalCatalogDtos.SiteSummary> sites, String normalizedSiteCode) {
        if (sites == null || isBlank(normalizedSiteCode)) {
            return ZoneOffset.UTC;
        }
        for (CaracalCatalogDtos.SiteSummary site : sites) {
            if (site.siteCode() != null
                    && site.siteCode().trim().equalsIgnoreCase(normalizedSiteCode)
                    && site.timezone() != null) {
                return site.timezone();
            }
        }
        return ZoneOffset.UTC;
    }

    private ZoneId findTenantZoneId(List<CaracalCatalogDtos.SiteSummary> sites) {
        if (sites == null) {
            return ZoneOffset.UTC;
        }
        for (CaracalCatalogDtos.SiteSummary site : sites) {
            if (site != null && site.timezone() != null) {
                return site.timezone();
            }
        }
        return ZoneOffset.UTC;
    }

    private String formatAt(String at, ZoneId zoneId) {
        if (isBlank(at)) {
            return null;
        }
        String trimmed = at.trim();
        String offsetTimestamp = tryFormatOffsetDateTime(trimmed);
        if (offsetTimestamp != null) {
            return offsetTimestamp;
        }

        LocalDate date = parseLocalDate(trimmed, at);
        return formatLocalDateAtZone(date, zoneId);
    }

    private static String tryFormatOffsetDateTime(String trimmed) {
        try {
            return OffsetDateTime.parse(trimmed).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static LocalDate parseLocalDate(String trimmed, String rawInput) {
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid pricing date format (expected YYYY-MM-DD or ISO-8601 offset datetime): " + rawInput, ex);
        }
    }

    private static String formatLocalDateAtZone(LocalDate date, ZoneId zoneId) {
        ZoneId effectiveZone = zoneId != null ? zoneId : ZoneOffset.UTC;
        return date.atTime(LocalTime.of(10, 0))
                .atZone(effectiveZone)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeFareClass(String fareClass) {
        return isBlank(fareClass) ? DEFAULT_FARE_CLASS : normalizeCode(fareClass);
    }

    private Map<String, FareRule> indexRulesByCoordinates(List<FareRule> rules) {
        Map<String, FareRule> map = new LinkedHashMap<>();
        for (FareRule rule : rules) {
            String key = coordinateKey(
                    rule.getOriginIdentifier(),
                    rule.getDestIdentifier(),
                    rule.getProductCode());
            map.put(key, rule);
        }
        return map;
    }

    private String coordinateKey(String origin, String dest, String product) {
        return origin.trim().toUpperCase(Locale.ROOT)
                + "::" + dest.trim().toUpperCase(Locale.ROOT)
                + "::" + product.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Batch-fetches PENDING drafts for the tenant in 1 query and overlays matching rules.
     */
    private void applyPendingDraftsToRules(String normalizedTenant, List<FareRule> rules) {
        if (fareDraftRepository == null || rules.isEmpty()) {
            return;
        }

        List<FareDraft> pendingDrafts = fareDraftRepository.findByTenantCodeAndDraftStatus(
                normalizedTenant, DraftStatus.PENDING);
        if (pendingDrafts.isEmpty()) {
            return;
        }

        Map<String, FareDraft> draftMap = new HashMap<>();
        for (FareDraft draft : pendingDrafts) {
            String key = coordinateKey(
                    draft.getOriginIdentifier(),
                    draft.getDestIdentifier(),
                    draft.getProductCode());
            draftMap.put(key, draft);
        }

        for (FareRule rule : rules) {
            String key = coordinateKey(
                    rule.getOriginIdentifier(),
                    rule.getDestIdentifier(),
                    rule.getProductCode());
            FareDraft matchingDraft = draftMap.get(key);
            if (matchingDraft != null) {
                applyDraftOverlay(rule, matchingDraft);
            }
        }
    }

    /**
     * OVERLAY HELPER (Mutates FareRule in-place when a PENDING FareDraft matches).
     */
    private void applyDraftOverlay(FareRule rule, FareDraft draft) {
        rule.setPrice(draft.getDraftPrice());
        rule.setStatus(draft.getTargetStatus());
        if (draft.getTierCode() != null && !draft.getTierCode().isBlank()) {
            rule.setTierCode(draft.getTierCode());
        }
        rule.setHasPendingDraft(true);
    }

    private record ClassifiedSimulation(String priceRuleSetName, BigDecimal price, FareStatus status) {
    }
}