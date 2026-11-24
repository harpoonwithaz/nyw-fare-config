package ca.sfu.cmpt276.nywfareconfig.fare.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.RateTableGetRequest;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulateRequest;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareGrid;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareRule;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.CatalogOption;
import ca.sfu.cmpt276.nywfareconfig.fare.model.DraftStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareCell;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.repository.FareDraftRepository;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

@Service
public class FareService {

    private final FareDraftRepository fareDraftRepository;
    private final CaracalApiClient caracalApiClient;

    // Price simulate can return 4xx.
    // When it is not sold you get a 4xx with a reason, which is represented as-is:
    private static final String NO_PRICE = "no PriceRule matched the resolved dimension context";
    private static final String NOT_SOLD = "not sold";
    private static final String NOT_SOLD_CURRENTLY = "no rate assignment's condition matches the requested moment or channel";

    public FareService(FareDraftRepository fareDraftRepository, CaracalApiClient caracalApiClient) {
        this.fareDraftRepository = fareDraftRepository;
        this.caracalApiClient = caracalApiClient;
    }

    /**
     * RAW BASELINE LOOKUP.
     * Never queries FareDraftRepository; returns hasPendingDraft = false.
     */
    public Optional<FareRule> getBaselineFareRule(Tenant tenant, String origin, String dest, String product) {
        if (tenant == null || tenant.getCode() == null || origin == null || dest == null || product == null) {
            return Optional.empty();
        }

        if (tenant.getGridMode() == GridMode.OD) {
            return simulateOdFareRule(tenant.getCode(), origin, dest, product);
        }

        // TODO: Swap with Caracal API call for ZONAL
        return Optional.empty();
    }

    /**
     * EFFECTIVE LOOKUP.
     * Returns baseline rule overlaid with any PENDING FareDraft from PostgreSQL.
     */
    public Optional<FareRule> getFareRule(Tenant tenant, String origin, String dest, String product) {
        Optional<FareRule> ruleOpt = getBaselineFareRule(tenant, origin, dest, product);
        if (ruleOpt.isEmpty()) {
            return Optional.empty();
        }

        FareRule rule = ruleOpt.get();
        fareDraftRepository
                .findByTenantCodeAndOriginIdentifierAndDestIdentifierAndProductCodeAndDraftStatus(
                        tenant.getCode().toUpperCase(),
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
        if (tenantCode == null || tenantCode.isBlank()) {
            return List.of();
        }

        switch (gridMode) {

            // In OD, the secondary filters are the origin terminals/sites
            case GridMode.OD: {
                return getSitesCatalog(tenantCode);
            }

            // In ZONAL, the secondary filters are ticket products
            case GridMode.ZONAL: {
                return getProductsCatalog(tenantCode);
            }

            default:
                return List.of();
        }
    }

    /**
     * Builds the structured 2D FareGrid view-model based on the active mode.
     */

    public FareGrid buildGrid(Tenant tenant, String filterValue) {
        if (tenant == null || tenant.getCode() == null || tenant.getCode().isBlank()
                || filterValue == null || filterValue.isBlank()) {
            return null;
        }

        if (tenant.getGridMode() == GridMode.ZONAL) {
            return buildZonalGrid(tenant, filterValue);
        }
        return buildOdGrid(tenant, filterValue);
    }

    /**
     * Builds Point-to-Point (O-D) matrix from normalized FareRule entities.
     * - Rows: Outbound Destination Terminals
     * - Columns: Ticket Products
     */
    private FareGrid buildOdGrid(Tenant tenant, String originSiteCode) {
        List<CatalogOption> colHeaders = getOdProducts(tenant.getCode()).reversed();
        // follow conventions of the NYW spreadsheet
        List<CatalogOption> outboundDestinations = getOutboundDestinations(tenant.getCode(), originSiteCode);
        Map<String, FareRule> ruleMap = indexRulesByCoordinates(
                getOdFareRules(tenant.getCode(), originSiteCode, outboundDestinations, colHeaders));

        List<CatalogOption> visibleRowHeaders = new ArrayList<>();
        List<List<FareCell>> rows = new ArrayList<>();

        for (CatalogOption dest : outboundDestinations) {
            List<FareCell> rowCells = new ArrayList<>();
            boolean hasVisibleCell = false;

            for (CatalogOption prod : colHeaders) {
                FareRule rule = ruleMap.get(coordinateKey(originSiteCode, dest.code(), prod.code()));
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
        List<CatalogOption> zones = getZones(tenant.getCode());
        Map<String, FareRule> ruleMap = indexRulesByCoordinates(getZonalFareRules(tenant.getCode(), productCode));

        List<List<FareCell>> rows = new ArrayList<>();

        for (CatalogOption originZone : zones) {
            List<FareCell> rowCells = new ArrayList<>();
            for (CatalogOption destZone : zones) {
                FareRule rule = ruleMap.get(coordinateKey(originZone.code(), destZone.code(), productCode));
                rowCells.add(toFareCell(rule, originZone.code(), destZone.code()));
            }
            rows.add(rowCells);
        }

        return new FareGrid(zones, zones, rows);
    }

    /**
     * Converts a normalized FareRule into a presentation FareCell for the 2D
     * matrix,
     * preserving whether the cell has a staged PENDING draft in PostgreSQL.
     */
    private FareCell toFareCell(FareRule rule, String rowKey, String colKey) {
        if (rule == null || rule.getStatus() == null || rule.getStatus() == FareStatus.NA) {
            return new FareCell(null, FareStatus.NA, false, rowKey, colKey); // can be null if its not sold
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
        if (tenantCode == null || tenantCode.isBlank()
                || originSiteCode == null || originSiteCode.isBlank()) {
            return List.of();
        }

        List<CatalogOption> destinations = getOutboundDestinations(tenantCode, originSiteCode);
        List<CatalogOption> products = getOdProducts(tenantCode);
        return getOdFareRules(tenantCode, originSiteCode, destinations, products);
    }

    private List<FareRule> getOdFareRules(
            String tenantCode,
            String originSiteCode,
            List<CatalogOption> destinations,
            List<CatalogOption> products) {
        List<FareRule> rules = getBaselineOdFareRules(tenantCode, originSiteCode, destinations, products);
        applyPendingDraftsToRules(tenantCode.toUpperCase(), rules);
        return rules;
    }

    /**
     * Returns untouched baseline O-D FareRule entities (never overlaid with
     * drafts).
     */
    public List<FareRule> getBaselineOdFareRules(String tenantCode, String originSiteCode) {
        if (tenantCode == null || tenantCode.isBlank()
                || originSiteCode == null || originSiteCode.isBlank()) {
            return List.of();
        }

        List<CatalogOption> destinations = getOutboundDestinations(tenantCode, originSiteCode);
        List<CatalogOption> products = getOdProducts(tenantCode);
        return getBaselineOdFareRules(tenantCode, originSiteCode, destinations, products);
    }

    private List<FareRule> getBaselineOdFareRules(
            String tenantCode,
            String originSiteCode,
            List<CatalogOption> destinations,
            List<CatalogOption> products) {
        Set<String> soldProductCodes = getRateTable(tenantCode, originSiteCode);
        if (soldProductCodes.isEmpty()) {
            return List.of();
        }

        List<FareRule> rules = new ArrayList<>();

        for (CatalogOption dest : destinations) {
            for (CatalogOption prod : products) {
                if (!soldProductCodes.contains(prod.code().trim().toUpperCase())) {
                    rules.add(FareRule.od(
                            tenantCode,
                            originSiteCode,
                            dest.code(),
                            prod.code(),
                            null,
                            null,
                            FareStatus.NOT_SOLD));
                    continue;
                }

                simulateOdFareRule(tenantCode, originSiteCode, dest.code(), prod.code())
                        .ifPresent(rules::add);
            }
        }

        return rules;
    }

    /**
     * Returns Zonal FareRule entities overlaid with any PENDING drafts in
     * PostgreSQL.
     */
    public List<FareRule> getZonalFareRules(String tenantCode, String productCode) {
        String normalizedTenant = tenantCode != null ? tenantCode.toUpperCase() : "METRA";
        List<FareRule> rules = getBaselineZonalFareRules(normalizedTenant, productCode);
        applyPendingDraftsToRules(normalizedTenant, rules);
        return rules;
    }

    /**
     * Returns untouched baseline Zonal FareRule entities (never overlaid with
     * drafts).
     */
    public List<FareRule> getBaselineZonalFareRules(String tenantCode, String productCode) {
        // TODO: Swap with GET /functions/rate-table-get or POST
        // /functions/price-simulate
        String normalizedTenant = tenantCode != null ? tenantCode.toUpperCase() : "METRA";
        String normalizedProduct = productCode != null ? productCode.toUpperCase() : "ADULT_ONEWAY";

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
     * Returns ticket products codes for O-D tenants.
     */
    public List<CatalogOption> getOdProducts(String tenantCode) {
        return getProductsCatalog(tenantCode);
    }

    /**
     * Returns outbound destination terminals for a given origin terminal in O-D
     * mode.
     */
    public List<CatalogOption> getOutboundDestinations(String tenantCode, String originSiteCode) {
        return getSitesCatalog(tenantCode).stream()
                .filter(destination -> !destination.code().equalsIgnoreCase(originSiteCode))
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

    private List<CatalogOption> getProductsCatalog(String tenantCode) {
        List<CaracalCatalogDtos.ProductSummary> productList = caracalApiClient.fetchProducts(tenantCode);
        if (productList == null) {
            return List.of();
        }

        List<CatalogOption> products = new ArrayList<>();
        for (CaracalCatalogDtos.ProductSummary product : productList) {
            if (product.code() == null || product.code().isBlank() || !product.isActive()) {
                continue;
            }

            products.add(new CatalogOption(product.code(), product.name()));
        }

        return products;
    }

    private List<CatalogOption> getSitesCatalog(String tenantCode) {
        List<CaracalCatalogDtos.SiteSummary> siteList = caracalApiClient.fetchSites(tenantCode);
        if (siteList == null) {
            return List.of();
        }

        List<CatalogOption> sites = new ArrayList<>();
        for (CaracalCatalogDtos.SiteSummary site : siteList) {
            if (site.siteCode() == null || site.siteCode().isBlank()) {
                continue;
            }

            sites.add(new CatalogOption(site.siteCode(), site.name()));
        }

        return sites;
    }

    private Set<String> getRateTable(String tenantCode, String originTerminal) {
        Optional<List<CaracalCatalogDtos.RateAssignmentSummary>> rateAssignmentList = caracalApiClient.fetchRateTable(
                new RateTableGetRequest(tenantCode, "RT-" + originTerminal));
        if (rateAssignmentList.isEmpty()) {
            return Set.of();
        }

        Set<String> soldProductCodes = new HashSet<>();
        for (CaracalCatalogDtos.RateAssignmentSummary assignment : rateAssignmentList.get()) {
            if (assignment.productCode() == null || assignment.productCode().isBlank()) {
                continue;
            }
            if (!"VALUE".equalsIgnoreCase(assignment.cellType())) {
                continue;
            }

            soldProductCodes.add(assignment.productCode().trim().toUpperCase());
        }

        return soldProductCodes;
    }

    private Optional<FareRule> simulateOdFareRule(
            String tenantCode,
            String originSiteCode,
            String destSiteCode,
            String productCode) {
        Map<String, String> dimensions = Map.of("DEST", destSiteCode);

        CaracalSimulationDtos.PriceSimulationResult simulatedPrice = caracalApiClient.simulatePrice(
                new PriceSimulateRequest(tenantCode, productCode, originSiteCode, dimensions, "")); // for now omitted
        if (simulatedPrice == null) {
            return Optional.empty();
        }

        if (simulatedPrice.response() == null || simulatedPrice.response().pricing() == null) {
            String msg = simulatedPrice.errorMessage() != null
                    ? simulatedPrice.errorMessage().toLowerCase()
                    : "";

            if (msg.contains(NOT_SOLD)) {
                return Optional.of(FareRule.od(
                        tenantCode, originSiteCode, destSiteCode, productCode, null, null, FareStatus.NOT_SOLD));
            }

            // TODO: Add distinct handling for NOT_SOLD_CURRENTLY / temporal conditions
            return Optional.of(FareRule.od(
                    tenantCode, originSiteCode, destSiteCode, productCode, null, null, FareStatus.NA));
        }

        CaracalSimulationDtos.PricingDetail fare = simulatedPrice.response().pricing();
        BigDecimal total = fare.total() != null ? fare.total().setScale(2, RoundingMode.HALF_UP) : null;

        return Optional.of(FareRule.od(
                tenantCode,
                originSiteCode,
                destSiteCode,
                productCode,
                fare.priceRuleSetName(),
                null,
                total,
                FareStatus.ACTIVE));
    }

    // =========================================================================
    // INTERNAL HELPERS & COORDINATE INDEXING
    // =========================================================================

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
        return origin.trim().toUpperCase()
                + "::" + dest.trim().toUpperCase()
                + "::" + product.trim().toUpperCase();
    }

    /**
     * Batch-fetches PENDING drafts for the tenant in 1 query and overlays matching
     * rules.
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
     * Helper to mock 3x3 symmetric zone baseline rates matching the system rate
     * schedule.
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