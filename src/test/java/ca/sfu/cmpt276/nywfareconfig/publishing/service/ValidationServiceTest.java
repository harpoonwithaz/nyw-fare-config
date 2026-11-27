package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulateRequest;

import tools.jackson.databind.ObjectMapper;

class ValidationServiceTest {

    private CaracalApiClient caracalApiClient;
    private ValidationService validationService;
    private Path referenceSheetPath;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {

        caracalApiClient = mock(CaracalApiClient.class);

        referenceSheetPath = tempDir.resolve("test-nyw-sheet.json");

        // Synthetic reference data for automated testing.
        // Does not depend on the employer-provided spreadsheet.
        Files.writeString(referenceSheetPath, """
                {
                    "machines": [
                        {
                            "originSiteCode": "EDGEWTR",
                            "routes": [
                                {
                                    "destSiteCode": "MIDTOWN",
                                    "fares": {
                                        "ADULT": 10.00,
                                        "CHILD": 5.00,
                                        "SENIOR": null,
                                        "BIKE": null,
                                        "TEN_TRIP": 90.00,
                                        "MONTHLY": 300.00,
                                        "WEEKEND": 8.00
                                    }
                                }
                            ]
                        }
                    ]
                }
                """);

        validationService = new ValidationService(
                new ObjectMapper(),
                caracalApiClient,
                referenceSheetPath.toString()
        );

        // Default: Caracal reports that a fare is unavailable.
        // Individual tests can override this behavior.
        when(caracalApiClient.simulatePrice(any(PriceSimulateRequest.class)))
                .thenReturn(unavailableFare());
    }

    private CaracalSimulationDtos.PriceSimulationResult unavailableFare() {
        return new CaracalSimulationDtos.PriceSimulationResult(
                null,
                "Fare unavailable",
                404
        );
    }

    private CaracalSimulationDtos.PriceSimulationResult successfulFare(
            String price) {

        var pricing = new CaracalSimulationDtos.PricingDetail(
                "OD-FULL_MONTHLY",
                new BigDecimal(price),
                new BigDecimal(price)
        );

        var response = new CaracalSimulationDtos.PriceSimulateResponse(
                pricing
        );

        return new CaracalSimulationDtos.PriceSimulationResult(
                response,
                "",
                null
        );
    }

    @Test
    void loadReferenceSheetShouldContainMachines() {
        var sheet = validationService.loadReferenceSheet();

        assertTrue(sheet.path("machines").isArray());
        assertFalse(sheet.path("machines").isEmpty());
    }

    @Test
    void missingReferenceSheetShouldThrowException() throws IOException {

        Files.delete(referenceSheetPath);

        var exception = assertThrows(
                IllegalStateException.class,
                () -> validationService.loadReferenceSheet()
        );

        assertTrue(exception.getMessage().contains(
                "NYW reference sheet not found"
        ));
    }

    @Test
    void malformedReferenceSheetShouldThrowException() throws IOException {

        Files.writeString(referenceSheetPath, "{ invalid json");

        assertThrows(
                IllegalStateException.class,
                () -> validationService.loadReferenceSheet()
        );
    }

    @Test
    void invalidReferenceSheetShouldThrowException() throws IOException {

        Files.writeString(referenceSheetPath, """
                {
                    "invalid": []
                }
                """);

        var exception = assertThrows(
                IllegalStateException.class,
                () -> validationService.loadReferenceSheet()
        );

        assertTrue(exception.getMessage().contains(
                "Invalid NYW reference sheet"
        ));
    }

    @Test
    void matchingFaresShouldHaveNoDiscrepancies() {
        List<ValidationService.Discrepancy> result =
                validationService.compareFare(
                        "EDGEWTR\u2192MIDTOWN",
                        "ADULT",
                        new BigDecimal("13.25"),
                        new BigDecimal("13.25")
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void differentPricesShouldProducePriceDiscrepancy() {
        var result = validationService.compareFare(
                "EDGEWTR\u2192MIDTOWN",
                "ADULT",
                new BigDecimal("13.25"),
                new BigDecimal("14.00")
        );

        assertEquals(1, result.size());
        assertEquals("PRICE DIFFERS", result.get(0).category());
        assertEquals("13.25", result.get(0).expected());
        assertEquals("14.00", result.get(0).actual());
    }

    @Test
    void platformSellingUnavailableTicketShouldBeDetected() {
        var result = validationService.compareFare(
                "PORTIMP\u2192MIDTOWN",
                "SENIOR",
                null,
                new BigDecimal("5.40")
        );

        assertEquals(1, result.size());
        assertEquals(
                "PLATFORM SELLS, SHEET DOES NOT",
                result.get(0).category()
        );
    }

    @Test
    void platformMissingExpectedTicketShouldBeDetected() {
        var result = validationService.compareFare(
                "PAULSHK\u2192BELFORD",
                "ADULT",
                new BigDecimal("23.00"),
                null
        );

        assertEquals(1, result.size());
        assertEquals(
                "SHEET SELLS, PLATFORM DOES NOT",
                result.get(0).category()
        );
    }

    @Test
    void bothUnavailableShouldHaveNoDiscrepancies() {
        var result = validationService.compareFare(
                "EDGEWTR\u2192MIDTOWN",
                "BIKE",
                null,
                null
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void groupedRoutesShouldContainExpectedRoute() {
        var routes = validationService.getGroupedReferenceRoutes();

        assertFalse(routes.isEmpty());
        assertTrue(routes.containsKey("EDGEWTR\u2192MIDTOWN"));
    }

    @Test
    void caracalFailureShouldPropagate() {
        when(caracalApiClient.simulatePrice(any(PriceSimulateRequest.class)))
                .thenThrow(new IllegalStateException("Caracal unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> validationService.validate("NYW_SANDBOX")
        );
    }

    @Test
    void monthlyTicketSoldOutsideSaleWindowShouldBeDetected() {

        when(caracalApiClient.simulatePrice(any(PriceSimulateRequest.class)))
                .thenAnswer(invocation -> {

                    PriceSimulateRequest request =
                            invocation.getArgument(0);

                    if (request.tenantCode().equals("NYW_SANDBOX")
                            && request.productCode().equals("FULL_MONTHLY")
                            && request.siteCode().equals("EDGEWTR")
                            && "MIDTOWN".equals(
                                    request.dimensions().get("DEST"))
                            && request.at().equals(
                                    "2026-10-12T12:00:00-04:00")) {

                        return successfulFare("414.50");
                    }

                    return unavailableFare();
                });

        var result = validationService.validate("NYW_SANDBOX");

        assertTrue(result.discrepancies().stream().anyMatch(
                discrepancy ->
                        discrepancy.category().equals("SALE WINDOW DIFFERS")
                        && discrepancy.ticketType().equals("MONTHLY")
                        && discrepancy.route().equals("EDGEWTR\u2192MIDTOWN")
        ));
    }
}


