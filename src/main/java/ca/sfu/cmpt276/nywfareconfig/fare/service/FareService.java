package ca.sfu.cmpt276.nywfareconfig.fare.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
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

    public FareService(FareDraftRepository fareDraftRepository, CaracalApiClient caracalApiClient) {
        this.fareDraftRepository = fareDraftRepository;
        this.caracalApiClient = caracalApiClient;
    }

    /**
     * RAW BASELINE LOOKUP (Used by FareDraftService to set originalPrice safely).
     * Never queries FareDraftRepository; returns hasPendingDraft = false.
     */
    public Optional<FareRule> getBaselineFareRule(String tenantCode, String origin, String dest, String product) {
        if (tenantCode == null || origin == null || dest == null || product == null) {
            return Optional.empty();
        }

        // TODO: Swap with Caracal API call (POST /functions/price-simulate or GET
        // /functions/product-get)
        List<FareRule> baselineRules = "METRA".equalsIgnoreCase(tenantCode)
                ? getBaselineZonalFareRules(tenantCode, product)
                : getBaselineOdFareRules(tenantCode, origin);

        return baselineRules.stream()
                .filter(rule -> origin.equalsIgnoreCase(rule.getOriginIdentifier())
                        && dest.equalsIgnoreCase(rule.getDestIdentifier())
                        && product.equalsIgnoreCase(rule.getProductCode()))
                .findFirst();
    }

    /**
     * EFFECTIVE LOOKUP (Used by Epic 3, Epic 5, and single-rule queries).
     * Returns baseline rule overlaid with any PENDING FareDraft from PostgreSQL.
     */
    public Optional<FareRule> getFareRule(String tenantCode, String origin, String dest, String product) {
        Optional<FareRule> ruleOpt = getBaselineFareRule(tenantCode, origin, dest, product);
        if (ruleOpt.isEmpty()) {
            return Optional.empty();
        }

        FareRule rule = ruleOpt.get();
        fareDraftRepository
                .findByTenantCodeAndOriginIdentifierAndDestIdentifierAndProductCodeAndDraftStatus(
                        tenantCode.toUpperCase(),
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
                List<CaracalCatalogDtos.SiteSummary> siteList = caracalApiClient.fetchSites(tenantCode);
                if (siteList == null) {
                    return List.of();
                }

                List<CatalogOption> filterList = new ArrayList<>();
                for (CaracalCatalogDtos.SiteSummary site : siteList) {
                    if (site.siteCode() == null || site.siteCode().isBlank()) {
                        continue;
                    }

                    filterList.add(new CatalogOption(site.siteCode(), site.name()));
                }

                return filterList;
            }

            // In ZONAL, the secondary filters are ticket products
            case GridMode.ZONAL: {
                List<CaracalCatalogDtos.ProductSummary> productList = caracalApiClient.fetchProducts(tenantCode);
                if (productList == null) {
                    return List.of();
                }

                List<CatalogOption> filterList = new ArrayList<>();
                for (CaracalCatalogDtos.ProductSummary product : productList) {
                    // Omit inactive products from the list
                    if (product.code() == null || product.code().isBlank() || !product.isActive()) {
                        continue;
                    }

                    filterList.add(new CatalogOption(product.code(), product.name()));
                }

                return filterList;
            }

            default:
                return List.of();
        }
    }

    /**
     * Builds the structured 2D FareGrid view-model based on the active mode.
     * Does not need to be modified when swapping mock data for live API calls.
     */
    public FareGrid buildMockGrid(Tenant tenant, String filterValue) {
        if (tenant == null) {
            return null;
        }

        String activeFilter = filterValue != null && !filterValue.isBlank()
                ? filterValue
                : tenant.getGridMode() == GridMode.ZONAL ? "ADULT_ONEWAY" : "EDGEWTR";

        if (tenant.getGridMode() == GridMode.ZONAL) {
            return buildZonalGrid(activeFilter, tenant);
        }
        return buildOdGrid(activeFilter, tenant);
    }

    /**
     * Builds Point-to-Point (O-D) matrix from normalized FareRule entities.
     * - Rows: Outbound Destination Terminals
     * - Columns: Ticket Products
     */
    private FareGrid buildOdGrid(String originSiteCode, Tenant tenant) {
        String normalizedOrigin = originSiteCode != null ? originSiteCode.toUpperCase() : "";

        List<String> colHeaders = getOdProducts(tenant.getCode());
        List<String> rowHeaders = getOutboundDestinations(tenant.getCode(), normalizedOrigin);
        Map<String, FareRule> ruleMap = indexRulesByCoordinates(getOdFareRules(tenant.getCode(), normalizedOrigin));

        List<List<FareCell>> rows = new ArrayList<>();

        for (String dest : rowHeaders) {
            List<FareCell> rowCells = new ArrayList<>();
            for (String prod : colHeaders) {
                FareRule rule = ruleMap.get(coordinateKey(normalizedOrigin, dest, prod));
                rowCells.add(toFareCell(rule, dest, prod));
            }
            rows.add(rowCells);
        }

        return new FareGrid(
                colHeaders,
                rowHeaders,
                rows);
    }

    /**
     * Builds Zone-to-Zone matrix from normalized FareRule entities.
     * - Rows: Origin Zones
     * - Columns: Destination Zones
     */
    private FareGrid buildZonalGrid(String productCode, Tenant tenant) {
        String normalizedProduct = productCode != null ? productCode.toUpperCase() : "";

        List<String> zones = getZones(tenant.getCode());
        Map<String, FareRule> ruleMap = indexRulesByCoordinates(getZonalFareRules(tenant.getCode(), normalizedProduct));

        List<List<FareCell>> rows = new ArrayList<>();

        for (String originZone : zones) {
            List<FareCell> rowCells = new ArrayList<>();
            for (String destZone : zones) {
                FareRule rule = ruleMap.get(coordinateKey(originZone, destZone, normalizedProduct));
                rowCells.add(toFareCell(rule, originZone, destZone));
            }
            rows.add(rowCells);
        }

        return new FareGrid(
                zones,
                zones,
                rows);
    }

    /**
     * Converts a normalized FareRule into a presentation FareCell for the 2D
     * matrix,
     * preserving whether the cell has a staged PENDING draft in PostgreSQL.
     */
    private FareCell toFareCell(FareRule rule, String rowKey, String colKey) {
        if (rule == null || rule.getStatus() == null || rule.getStatus() == FareStatus.NA) {
            return new FareCell(rule.getPrice(), FareStatus.NA, false, rowKey, colKey); // can be null if its not sold
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
        String normalizedTenant = tenantCode != null ? tenantCode.toUpperCase() : "NYW_SANDBOX";
        List<FareRule> rules = getBaselineOdFareRules(normalizedTenant, originSiteCode);
        applyPendingDraftsToRules(normalizedTenant, rules);
        return rules;
    }

    /**
     * Returns untouched baseline O-D FareRule entities (never overlaid with
     * drafts).
     */
    public List<FareRule> getBaselineOdFareRules(String tenantCode, String originSiteCode) {
        // TODO: Swap with POST /functions/price-simulate or GET
        // /functions/rate-table-get
        String normalizedTenant = tenantCode != null ? tenantCode.toUpperCase() : "NYW_SANDBOX";
        String normalizedOrigin = originSiteCode != null ? originSiteCode.toUpperCase() : "EDGEWTR";

        List<String> destinations = getOutboundDestinations(normalizedTenant, normalizedOrigin);
        List<String> products = getOdProducts(normalizedTenant);
        List<FareRule> rules = new ArrayList<>();

        for (String dest : destinations) {
            for (String prod : products) {
                FareStatus status;
                BigDecimal price = null;

                // NYW discontinued Senior tickets across all terminals -> NOT_SOLD
                if ("Senior 65+".equals(prod)) {
                    status = FareStatus.NOT_SOLD;
                }
                // Pier 11 is not directly served from Edgewater -> NA badge
                else if ("EDGEWTR".equals(normalizedOrigin) && "Pier 11 / Wall St".equals(dest)) {
                    status = FareStatus.NA;
                }
                // Standard active pricing
                else {
                    status = FareStatus.ACTIVE;
                    price = resolveOdPrice(dest, prod);
                }

                rules.add(FareRule.od(
                        normalizedTenant,
                        normalizedOrigin,
                        dest,
                        prod,
                        "OD-" + prod.toUpperCase().replace(" ", "_"),
                        price,
                        status));
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

        List<String> zones = getZones(normalizedTenant);
        BigDecimal[][] baseRates = resolveZonalBaseRates(normalizedProduct);
        List<FareRule> rules = new ArrayList<>();

        for (int r = 0; r < zones.size(); r++) {
            for (int c = 0; c < zones.size(); c++) {
                String originZone = zones.get(r);
                String destZone = zones.get(c);
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
    public List<String> getOdProducts(String tenantCode) {
        // TODO: Call GET /functions/product-list

        return List.of(
                "Adult One-Way",
                "Child One-Way",
                "Senior 65+",
                "10-Trip Pass",
                "Monthly Pass");
    }

    /**
     * Returns outbound destination terminals for a given origin terminal in O-D
     * mode.
     */
    public List<String> getOutboundDestinations(String tenantCode, String originSiteCode) {
        // TODO: Call GET /functions/site-list
        String normalizedOrigin = originSiteCode != null ? originSiteCode.toUpperCase() : "EDGEWTR";
        return switch (normalizedOrigin) {
            case "PORTIMP" -> List.of("Midtown / W. 39th St", "Brookfield Place", "Pier 11 / Wall St");
            case "MIDTOWN" -> List.of("Edgewater", "Port Imperial", "Hoboken 14th St");
            case "EDGEWTR" -> List.of("Midtown / W. 39th St", "Brookfield Place", "Pier 11 / Wall St");
            default -> List.of("Midtown / W. 39th St", "Brookfield Place");
        };
    }

    /**
     * Returns fare zones for Zonal tenants.
     */
    public List<String> getZones(String tenantCode) {
        // TODO: Call GET /functions/site-list
        return List.of("Zone 1", "Zone 2", "Zone 3");
    }

    // =========================================================================
    // INTERNAL MOCK HELPERS & COORDINATE INDEXING
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
     * Helper to mock realistic NYW ticket prices with exact 2-decimal BigDecimal
     * scale.
     */
    private BigDecimal resolveOdPrice(String dest, String prod) {
        BigDecimal base = dest.contains("Midtown")
                ? new BigDecimal("13.25")
                : new BigDecimal("15.00");

        BigDecimal rawPrice = switch (prod) {
            case "Child One-Way" -> base.subtract(new BigDecimal("4.25"));
            case "10-Trip Pass" -> base.multiply(new BigDecimal("8.5"));
            case "Monthly Pass" -> base.multiply(new BigDecimal("31.25"));
            default -> base; // Adult One-Way
        };

        return rawPrice.setScale(2, RoundingMode.HALF_UP);
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