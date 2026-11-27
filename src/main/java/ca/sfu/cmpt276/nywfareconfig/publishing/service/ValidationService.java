package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulateRequest;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ValidationService {

    private final ObjectMapper objectMapper;
    private final CaracalApiClient caracalApiClient;
    private final String referenceSheetPath;

    private static final String WEEKDAY =
            "2026-10-20T12:00:00-04:00";

    private static final String SATURDAY =
            "2026-10-24T12:00:00-04:00";

    private static final String MONTHLY_ON_SALE =
            "2026-10-21T12:00:00-04:00";

    private static final String MONTHLY_OFF_SALE =
            "2026-10-12T12:00:00-04:00";

    private static final Map<String, FareCheck> CHECKS =
            new LinkedHashMap<>();

    static {
        CHECKS.put("ADULT", new FareCheck(
                List.of("ADULT_ONEWAY", "ADULT_ONEWAY_WD"),
                WEEKDAY));

        CHECKS.put("CHILD", new FareCheck(
                List.of("CHILD_ONEWAY", "CHILD_ONEWAY_WD"),
                WEEKDAY));

        CHECKS.put("SENIOR", new FareCheck(
                List.of("SENIOR_ONEWAY", "SENIOR_ONEWAY_WD"),
                WEEKDAY));

        CHECKS.put("BIKE", new FareCheck(
                List.of("BIKE_ONEWAY"),
                WEEKDAY));

        CHECKS.put("TEN_TRIP", new FareCheck(
                List.of("FULL_10TRIP"),
                WEEKDAY));

        CHECKS.put("MONTHLY", new FareCheck(
                List.of("FULL_MONTHLY"),
                MONTHLY_ON_SALE));

        CHECKS.put("WEEKEND", new FareCheck(
                List.of("WEEKEND_TICKET"),
                SATURDAY));
    }

    public ValidationService(
            ObjectMapper objectMapper,
            CaracalApiClient caracalApiClient,
            @Value("${caracal.api.nyw-sheet-path}")
            String referenceSheetPath) {

        this.objectMapper = objectMapper;
        this.caracalApiClient = caracalApiClient;
        this.referenceSheetPath = referenceSheetPath;
    }

    // ============================================================
    // Result records
    // ============================================================

    public record FareCheck(
            List<String> productCodes,
            String at) {
    }

    public record Discrepancy(
            String category,
            String route,
            String ticketType,
            String expected,
            String actual) {
    }

    public record ValidationResult(
            String tenantCode,
            int routesChecked,
            int fareChecks,
            int discrepancyCount,
            List<Discrepancy> discrepancies) {
    }

    // ============================================================
    // Load reference JSON
    // ============================================================

    public JsonNode loadReferenceSheet() {

        Path path = Path.of(referenceSheetPath);

        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(
                    "NYW reference sheet not found at: "
                            + path.toAbsolutePath());
        }

        try (InputStream input = Files.newInputStream(path)) {

            JsonNode sheet = objectMapper.readTree(input);

            if (sheet == null
                    || !sheet.path("machines").isArray()) {

                throw new IllegalStateException(
                        "Invalid NYW reference sheet.");
            }

            return sheet;

        } catch (IOException | JacksonException e) {
            throw new IllegalStateException(
                    "Failed to load NYW reference sheet from: "
                            + path.toAbsolutePath(), e);
        }
    }

    // ============================================================
    // Extract routes
    // ============================================================

    public List<JsonNode> getReferenceRoutes() {

        JsonNode sheet = loadReferenceSheet();
        List<JsonNode> routes = new ArrayList<>();

        for (JsonNode machine : sheet.path("machines")) {

            String origin =
                    machine.path("originSiteCode").asText();

            for (JsonNode route : machine.path("routes")) {

                String destination =
                        route.path("destSiteCode").asText();

                if (origin.isBlank()
                        || destination.isBlank()) {
                    continue;
                }

                routes.add(route);
            }
        }

        return routes;
    }

    // ============================================================
    // Group routes by origin and destination
    // ============================================================

    public Map<String, List<JsonNode>> getGroupedReferenceRoutes() {

        JsonNode sheet = loadReferenceSheet();

        Map<String, List<JsonNode>> groupedRoutes =
                new LinkedHashMap<>();

        for (JsonNode machine : sheet.path("machines")) {

            String origin =
                    machine.path("originSiteCode").asText();

            for (JsonNode route : machine.path("routes")) {

                String destination =
                        route.path("destSiteCode").asText();

                if (origin.isBlank()
                        || destination.isBlank()) {
                    continue;
                }

                String routeKey =
                        origin + "→" + destination;

                groupedRoutes
                        .computeIfAbsent(
                                routeKey,
                                key -> new ArrayList<>())
                        .add(route);
            }
        }

        return groupedRoutes;
    }

    // ============================================================
    // Compare expected and actual fares
    // ============================================================

    public List<Discrepancy> compareFare(
            String route,
            String ticketType,
            BigDecimal expected,
            BigDecimal actual) {

        if (expected == null && actual == null) {
            return List.of();
        }

        if (expected == null) {
            return List.of(new Discrepancy(
                    "PLATFORM SELLS, SHEET DOES NOT",
                    route,
                    ticketType,
                    "Not sold",
                    actual.toString()));
        }

        if (actual == null) {
            return List.of(new Discrepancy(
                    "SHEET SELLS, PLATFORM DOES NOT",
                    route,
                    ticketType,
                    expected.toString(),
                    "Not sold"));
        }

        if (expected.compareTo(actual) != 0) {
            return List.of(new Discrepancy(
                    "PRICE DIFFERS",
                    route,
                    ticketType,
                    expected.toString(),
                    actual.toString()));
        }

        return List.of();
    }

    // ============================================================
    // Resolve expected fare from the reference sheet
    // ============================================================

    private BigDecimal getExpectedFare(
            List<JsonNode> routes,
            String ticketType) {

        BigDecimal expected = null;

        for (JsonNode route : routes) {

            JsonNode fare =
                    route.path("fares").path(ticketType);

            if (!fare.isNumber()) {
                continue;
            }

            BigDecimal price = fare.decimalValue();

            if (expected == null) {
                expected = price;
            } else if (expected.compareTo(price) != 0) {
                throw new IllegalStateException(
                        "Reference sheet contains conflicting "
                        + ticketType + " prices for the same route.");
            }
        }

        return expected;
    }

    // ============================================================
    // Shared Caracal price simulation
    // ============================================================

    private BigDecimal getSimulatedPrice(
            String tenantCode,
            String productCode,
            String origin,
            String destination,
            String at) {

        PriceSimulateRequest request = new PriceSimulateRequest(
                tenantCode,
                productCode,
                origin,
                Map.of("DEST", destination),
                at,
                "UNATTENDED");

        CaracalSimulationDtos.PriceSimulationResult result =
                caracalApiClient.simulatePrice(request);

        if (result == null) {
            throw new IllegalStateException(
                    "Caracal returned no simulation result.");
        }

        if (!result.isSuccess()) {

            Integer status = result.statusCode();

            if (status != null &&
                    (status == 404 || status == 409 || status == 422)) {
                return null;
            }

            throw new IllegalStateException(
                    "Unexpected Caracal simulation error: "
                    + result.errorMessage());
        }

        BigDecimal total = result.response().pricing().total();

        if (total == null) {
            throw new IllegalStateException(
                    "Caracal returned a simulation without a total price.");
        }

        return total;
    }

    // ============================================================
    // Simulate pricing with product-code fallbacks
    // ============================================================

    private BigDecimal simulateFare(
            String tenantCode,
            String origin,
            String destination,
            FareCheck check) {

        for (String productCode : check.productCodes()) {

            BigDecimal price = getSimulatedPrice(
                    tenantCode,
                    productCode,
                    origin,
                    destination,
                    check.at());

            if (price != null) {
                return price;
            }
        }

        return null;
    }

    // ============================================================
    // Run validation
    // ============================================================

    public ValidationResult validate(String tenantCode) {

        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant code must not be blank.");
        }

        Map<String, List<JsonNode>> groupedRoutes =
                getGroupedReferenceRoutes();

        List<Discrepancy> discrepancies =
                new ArrayList<>();

        int fareChecks = 0;

        for (Map.Entry<String, List<JsonNode>> entry
                : groupedRoutes.entrySet()) {

            String routeKey = entry.getKey();

            String[] routeParts =
                    routeKey.split("→", 2);

            String origin = routeParts[0];
            String destination = routeParts[1];

            for (Map.Entry<String, FareCheck> checkEntry
                    : CHECKS.entrySet()) {

                String ticketType = checkEntry.getKey();
                FareCheck check = checkEntry.getValue();

                BigDecimal expected = getExpectedFare(
                        entry.getValue(),
                        ticketType);

                BigDecimal actual = simulateFare(
                        tenantCode,
                        origin,
                        destination,
                        check);

                discrepancies.addAll(compareFare(
                        routeKey,
                        ticketType,
                        expected,
                        actual));

                fareChecks++;
            }
        }

        // Monthly ticket should not be on sale on October 12.
        // Check the first route where the sheet sells monthly.
        for (Map.Entry<String, List<JsonNode>> entry
                : groupedRoutes.entrySet()) {

            BigDecimal monthlyExpected = getExpectedFare(
                    entry.getValue(),
                    "MONTHLY");

            if (monthlyExpected == null) {
                continue;
            }

            String routeKey = entry.getKey();
            String[] routeParts = routeKey.split("→", 2);

            BigDecimal offSalePrice = getSimulatedPrice(
                    tenantCode,
                    "FULL_MONTHLY",
                    routeParts[0],
                    routeParts[1],
                    MONTHLY_OFF_SALE);

            fareChecks++;

            if (offSalePrice != null) {
                discrepancies.add(new Discrepancy(
                        "SALE WINDOW DIFFERS",
                        routeKey,
                        "MONTHLY",
                        "Not on sale",
                        offSalePrice.toString()));
            }

            break;
        }

        return new ValidationResult(
                tenantCode,
                groupedRoutes.size(),
                fareChecks,
                discrepancies.size(),
                List.copyOf(discrepancies));
    }
}

