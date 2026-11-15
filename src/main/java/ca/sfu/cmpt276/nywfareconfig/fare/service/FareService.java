package ca.sfu.cmpt276.nywfareconfig.fare.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareGrid;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareRule;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareStatus;
import ca.sfu.cmpt276.nywfareconfig.fare.model.*;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

@Service
public class FareService {

    public FareService() {
    }

    public Optional<FareRule> getFareRule(String tenantCode, String origin, String dest, String product) {
        // TODO: Call GET /functions/product-get
        return Optional.empty();
    }

    /**
     * Returns secondary filter options depending on the tenant mode:
     * - O-D Mode: Origin Terminal codes
     * - Zonal Mode: Ticket Product codes
     */
    public List<String> getSecondaryFilterOptions(String tenantCode) {
        if (tenantCode == null) {
            return List.of();
        }

        // TODO: Call GET /functions/site-list
        return switch (tenantCode.toUpperCase()) {
            case "NYW_SANDBOX", "NYW" -> List.of("EDGEWTR", "PORTIMP", "MIDTOWN");
            case "METRA" -> List.of("ADULT_ONEWAY", "FULL_10TRIP", "FULL_MONTHLY");
            default -> List.of();
        };
    }

    /**
     * Builds the structured 2D FareGrid view-model based on the active mode.
     */
    public FareGrid buildMockGrid(Tenant tenant, String filterValue) {
        if (tenant == null) {
            return null;
        }
        if (tenant.getGridMode() == GridMode.ZONAL) {
            return buildZonalGrid(filterValue != null ? filterValue : "ADULT_ONEWAY", tenant);
        }
        return buildOdGrid(filterValue != null ? filterValue : "EDGEWTR", tenant);
    }

    /**
     * Builds Point-to-Point (O-D) matrix for NY Waterway.
     * - Rows: Outbound Destination Terminals
     * - Columns: Ticket Products
     */
    private FareGrid buildOdGrid(String originSiteCode, Tenant tenant) {
        String normalizedOrigin = originSiteCode != null ? originSiteCode.toUpperCase() : "EDGEWTR";

        // Columns: Products
        List<String> colHeaders = List.of(
            "Adult One-Way",
            "Child One-Way",
            "Senior 65+",
            "10-Trip Pass",
            "Monthly Pass"
        );

        // Rows: Outbound Destination sites
        List<String> rowHeaders = switch (normalizedOrigin) {
            case "PORTIMP" -> List.of("Midtown / W. 39th St", "Brookfield Place", "Pier 11 / Wall St");
            case "MIDTOWN" -> List.of("Edgewater", "Port Imperial", "Hoboken 14th St");
            case "EDGEWTR" -> List.of("Midtown / W. 39th St", "Brookfield Place", "Pier 11 / Wall St");
            default -> List.of("Midtown / W. 39th St", "Brookfield Place");
        };

        List<List<FareCell>> rows = new ArrayList<>();

        for (String dest : rowHeaders) {
            List<FareCell> rowCells = new ArrayList<>();
            for (String prod : colHeaders) {
                // NYW discontinued Senior tickets across all terminals -> NOT_SOLD
                if ("Senior 65+".equals(prod)) {
                    rowCells.add(new FareCell(FareStatus.NOT_SOLD, dest, prod));
                } 
                // Pier 11 is not directly served from Edgewater -> NA badge
                else if ("EDGEWTR".equals(normalizedOrigin) && "Pier 11 / Wall St".equals(dest)) {
                    rowCells.add(new FareCell(FareStatus.NA, dest, prod));
                } 
                // Standard active pricing
                else {
                    Double price = resolveOdPrice(dest, prod);
                    rowCells.add(new FareCell(
                        price,
                        String.format("$%.2f", price),
                        FareStatus.ACTIVE,
                        true,
                        dest,
                        prod
                    ));
                }
            }
            rows.add(rowCells);
        }

        return new FareGrid(
            tenant,
            "Origin Terminal",
            normalizedOrigin,
            colHeaders,
            rowHeaders,
            rows
        );
    }

    /**
     * Builds symmetric Zone-to-Zone matrix for Metra Rail.
     * - Rows: Origin Zones (Zone 1 to 3)
     * - Columns: Destination Zones (Zone 1 to 3)
     */
    private FareGrid buildZonalGrid(String productCode, Tenant tenant) {
        String normalizedProduct = productCode != null ? productCode.toUpperCase() : "ADULT_ONEWAY";

        List<String> colHeaders = List.of("Zone 1", "Zone 2", "Zone 3");
        List<String> rowHeaders = List.of("Zone 1", "Zone 2", "Zone 3");

        // 3x3 baseline rates matching the system rate schedule
        Double[][] baseRates = switch (normalizedProduct) {
            case "FULL_10TRIP" -> new Double[][] {
                { 22.50, 36.00, 58.50 },
                { 36.00, 22.50, 36.00 },
                { 58.50, 36.00, 22.50 }
            };
            case "FULL_MONTHLY" -> new Double[][] {
                { 75.00, 120.00, 195.00 },
                { 120.00, 75.00, 120.00 },
                { 195.00, 120.00, 75.00 }
            };
            case "ADULT_ONEWAY" -> new Double[][] {
                { 2.50, 4.00, 6.50 },
                { 4.00, 2.50, 4.00 },
                { 6.50, 4.00, 2.50 }
            };
            default -> new Double[][] {
                { 2.50, 4.00, 6.50 },
                { 4.00, 2.50, 4.00 },
                { 6.50, 4.00, 2.50 }
            };
        };

        List<List<FareCell>> rows = new ArrayList<>();

        for (int r = 0; r < rowHeaders.size(); r++) {
            List<FareCell> rowCells = new ArrayList<>();
            for (int c = 0; c < colHeaders.size(); c++) {
                Double price = baseRates[r][c];
                rowCells.add(new FareCell(
                    price,
                    String.format("$%.2f", price),
                    FareStatus.ACTIVE,
                    true,
                    rowHeaders.get(r),
                    colHeaders.get(c)
                ));
            }
            rows.add(rowCells);
        }

        return new FareGrid(
            tenant,
            "Ticket Product",
            normalizedProduct,
            colHeaders,
            rowHeaders,
            rows
        );
    }

    /**
     * Helper to mock realistic NYW ticket prices.
     */
    private Double resolveOdPrice(String dest, String prod) {
        double base = dest.contains("Midtown") ? 13.25 : 15.00;
        return switch (prod) {
            case "Child One-Way" -> base - 4.25;
            case "10-Trip Pass" -> base * 8.5;
            case "Monthly Pass" -> base * 31.25;
            default -> base; // Adult One-Way
        };
    }
}