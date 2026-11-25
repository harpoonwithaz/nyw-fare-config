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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.RateTableGetRequest;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulateRequest;
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
     * RAW BASELINE LOOKUP.
     * Never queries FareDraftRepository; returns hasPendingDraft = false.
     */
    public Optional<FareRule> getBaselineFareRule(Tenant tenant, String origin, String dest, String product) {
        return getBaselineFareRule(tenant, origin, dest, product, null);
    }

    public Optional<FareRule> getBaselineFareRule(
            Tenant tenant, String origin, String dest, String product, String at) {
        if (tenant == null || isBlank(tenant.getCode())
                || isBlank(origin) || isBlank(dest) || isBlank(product)) {
            return Optional.empty();
        }

        String normalizedTenant = normalizeCode(tenant.getCode());
        String normalizedProduct = normalizeCode(product);

        if (tenant.getGridMode() == GridMode.OD) {
            String normalizedOrigin = normalizeCode(origin);
            String normalizedDest = normalizeCode(dest);
            String formattedAt = resolvePricingTimestamp(normalizedTenant, normalizedOrigin, at);
            return simulateOdFareRuleWithFormattedAt(
                    normalizedTenant, normalizedOrigin, normalizedDest, normalizedProduct, formattedAt);
        }

        // TODO: Swap with Caracal API call for ZONAL
        String trimmedOrigin = origin.trim();
        String trimmedDest = dest.trim();
        return getBaselineZonalFareRules(normalizedTenant, normalizedProduct).stream()
                .filter(rule -> trimmedOrigin.equalsIgnoreCase(rule.getOriginIdentifier())
                        && trimmedDest.equalsIgnoreCase(rule.getDestIdentifier())
                        && normalizedProduct.equalsIgnoreCase(rule.getProductCode()))
                .findFirst();
    }

    /**
     * EFFECTIVE LOOKUP.
     * Returns baseline rule overlaid with any PENDING FareDraft from PostgreSQL.
     */
    public Optional<FareRule> getFareRule(Tenant tenant, String origin, String dest, String product) {
        return getFareRule(tenant, origin, dest, product, null);
    }

    public Optional<FareRule> getFareRule(
            Tenant tenant, String origin, String dest, String product, String at) {
        Optional<FareRule> ruleOpt = getBaselineFareRule(tenant, origin, dest, product, at);
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
        return buildGrid(tenant, filterValue, null);
    }

    public FareGrid buildGrid(Tenant tenant, String filterValue, String at) {
        if (tenant == null || isBlank(tenant.getCode()) || isBlank(filterValue)) {
            return null;
        }

        String normalizedFilter = normalizeCode(filterValue);
        if (tenant.getGridMode() == GridMode.ZONAL) {
            return buildZonalGrid(tenant, normalizedFilter);
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
    private FareGrid buildZonalGrid(Tenant tenant, String productCode) {
        String normalizedTenant = normalizeCode(tenant.getCode());
        String normalizedProduct = normalizeCode(productCode);

        List<CatalogOption> zones = getZones(normalizedTenant);
        Map<String, FareRule> ruleMap = indexRulesByCoordinates(
                getZonalFareRules(normalizedTenant, normalizedProduct));

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
     * preserving whether the cell has a staged PENDING draft in PostgreSQL.
     */
    private FareCell toFareCell(FareRule rule, String rowKey, String colKey) {
        if (rule == null || rule.getStatus() == null || rule.getStatus() == FareStatus.NA) {
            return new FareCell(null, FareStatus.NA, false, rowKey, colKey);
        }
        if (rule.getStatus() == FareStatus.NOT_SOLD || rule.getPrice() == null) {
            return new FareCell(rule.getPrice(), rule.getStatus(), rule.isHasPendingDraft(), rowKey, colKey);
        }

        BigDecimal price = rule.getPrice().setScale(2, RoundingMode.HALF_UP);
        return new FareCell(
                price,
                rule.getStatus(),
                rule.isHasPendingDraft(),
                rowKey,
                colKey);
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
        String normalizedTenant = !isBlank(tenantCode) ? normalizeCode(tenantCode) : "METRA";
        String normalizedProduct = !isBlank(productCode) ? normalizeCode(productCode) : "ADULT_ONEWAY";
        List<FareRule> rules = getBaselineZonalFareRules(normalizedTenant, normalizedProduct);
        applyPendingDraftsToRules(normalizedTenant, rules);
        return rules;
    }

    /**
     * Returns untouched baseline Zonal FareRule entities (never overlaid with drafts).
     */
    public List<FareRule> getBaselineZonalFareRules(String tenantCode, String productCode) {
        // TODO: Swap with GET /functions/rate-table-get or POST /functions/price-simulate
        String normalizedTenant = !isBlank(tenantCode) ? normalizeCode(tenantCode) : "METRA";
        String normalizedProduct = !isBlank(productCode) ? normalizeCode(productCode) : "ADULT_ONEWAY";

        List<CatalogOption> zones = getZones(normalizedTenant);
        BigDecimal[][] baseRates = resolveZonalBaseRates(normalizedProduct);
        List<FareRule> rules = new ArrayList<>();

        for (int r = 0; r < zones.size(); r++) {
            for (int c = 0; c < zones.size(); c++) {
                String originZone = zones.get(r).code();
                String destZone = zones.get(c).code();
                BigDecimal price = baseRates[r][c].setScale(2, RoundingMode.HALF_UP);

                int zoneDistance = Math.abs(r - c);
                String tierCode = switch (zoneDistance) {
                    case 0 -> "TIER_A";
                    case 1 -> "TIER_B";
                    default -> "TIER_C";
                };

                rules.add(FareRule.zonal(
                        normalizedTenant,
                        originZone,
                        destZone,
                        normalizedProduct,
                        "ZONAL-" + normalizedProduct,
                        tierCode,
                        price,
                        FareStatus.ACTIVE));
            }
        }

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
     * Returns fare zones for Zonal tenants.
     */
    public List<CatalogOption> getZones(String tenantCode) {
        // TODO: Call GET /functions/site-list
        return List.of(
                new CatalogOption("Zone 1", "Zone 1"),
                new CatalogOption("Zone 2", "Zone 2"),
                new CatalogOption("Zone 3", "Zone 3"));
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
        if (simulatedPrice == null) {
            return Optional.empty();
        }

        if (simulatedPrice.response() == null || simulatedPrice.response().pricing() == null) {
            String msg = simulatedPrice.errorMessage() != null
                    ? simulatedPrice.errorMessage().toLowerCase(Locale.ROOT)
                    : "";

            if (msg.contains(NOT_SOLD) || msg.contains(CONDITION)) {
                return Optional.of(FareRule.od(
                        normalizedTenant, normalizedOrigin, normalizedDest, normalizedProduct,
                        null, null, FareStatus.NOT_SOLD));
            }

            if (!msg.contains(NO_PRICE_RULE)) {
                log.warn("Unrecognized Caracal 4xx response for {} {}->{} ({}): {}",
                        normalizedTenant, normalizedOrigin, normalizedDest, normalizedProduct,
                        simulatedPrice.errorMessage());
            }

            return Optional.of(FareRule.od(
                    normalizedTenant, normalizedOrigin, normalizedDest, normalizedProduct,
                    null, null, FareStatus.NA));
        }

        CaracalSimulationDtos.PricingDetail fare = simulatedPrice.response().pricing();
        if (fare.total() == null) {
            log.warn("Caracal returned null total for {} {}->{} ({}); treating as N/A.",
                    normalizedTenant, normalizedOrigin, normalizedDest, normalizedProduct);
            return Optional.of(FareRule.od(
                    normalizedTenant, normalizedOrigin, normalizedDest, normalizedProduct,
                    fare.priceRuleSetName(), null, FareStatus.NA));
        }

        BigDecimal total = fare.total().setScale(2, RoundingMode.HALF_UP);
        return Optional.of(FareRule.od(
                normalizedTenant,
                normalizedOrigin,
                normalizedDest,
                normalizedProduct,
                fare.priceRuleSetName(),
                null,
                total,
                FareStatus.ACTIVE));
    }

    // =========================================================================
    // INTERNAL HELPERS & COORDINATE INDEXING
    // =========================================================================

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

    /**
     * Helper to mock 3x3 symmetric zone baseline rates matching the system rate schedule.
     */
    private BigDecimal[][] resolveZonalBaseRates(String normalizedProduct) {
        return switch (normalizedProduct) {
            case "FULL_10TRIP" -> new BigDecimal[][] {
                    { new BigDecimal("22.50"), new BigDecimal("36.00"), new BigDecimal("58.50") },
                    { new BigDecimal("36.00"), new BigDecimal("22.50"), new BigDecimal("36.00") },
                    { new BigDecimal("58.50"), new BigDecimal("36.00"), new BigDecimal("22.50") }
            };
            case "FULL_MONTHLY" -> new BigDecimal[][] {
                    { new BigDecimal("75.00"), new BigDecimal("120.00"), new BigDecimal("195.00") },
                    { new BigDecimal("120.00"), new BigDecimal("75.00"), new BigDecimal("120.00") },
                    { new BigDecimal("195.00"), new BigDecimal("120.00"), new BigDecimal("75.00") }
            };
            case "ADULT_ONEWAY" -> new BigDecimal[][] {
                    { new BigDecimal("2.50"), new BigDecimal("4.00"), new BigDecimal("6.50") },
                    { new BigDecimal("4.00"), new BigDecimal("2.50"), new BigDecimal("4.00") },
                    { new BigDecimal("6.50"), new BigDecimal("4.00"), new BigDecimal("2.50") }
            };
            default -> new BigDecimal[][] {
                    { new BigDecimal("2.50"), new BigDecimal("4.00"), new BigDecimal("6.50") },
                    { new BigDecimal("4.00"), new BigDecimal("2.50"), new BigDecimal("4.00") },
                    { new BigDecimal("6.50"), new BigDecimal("4.00"), new BigDecimal("2.50") }
            };
        };
    }
}