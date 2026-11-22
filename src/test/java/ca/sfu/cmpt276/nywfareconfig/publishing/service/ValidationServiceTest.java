package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import tools.jackson.databind.ObjectMapper;

class ValidationServiceTest {

    private CaracalApiClient caracalApiClient;
    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        caracalApiClient = mock(CaracalApiClient.class);

        validationService = new ValidationService(
                new ObjectMapper(),
                caracalApiClient
        );
    }

    @Test
    void loadReferenceSheetShouldContainMachines() {
        var sheet = validationService.loadReferenceSheet();

        assertTrue(sheet.path("machines").isArray());
        assertFalse(sheet.path("machines").isEmpty());
    }

    @Test
    void matchingFaresShouldHaveNoDiscrepancies() {
        List<ValidationService.Discrepancy> result =
                validationService.compareFare(
                        "EDGEWTR→MIDTOWN",
                        "ADULT",
                        new BigDecimal("13.25"),
                        new BigDecimal("13.25")
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void differentPricesShouldProducePriceDiscrepancy() {
        var result = validationService.compareFare(
                "EDGEWTR→MIDTOWN",
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
                "PORTIMP→MIDTOWN",
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
                "PAULSHK→BELFORD",
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
                "EDGEWTR→MIDTOWN",
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
        assertTrue(routes.containsKey("EDGEWTR→MIDTOWN"));
    }

    @Test
    void caracalFailureShouldPropagate() {
        when(caracalApiClient.fetchSimulatedPrice(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenThrow(new IllegalStateException("Caracal unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> validationService.validate("NYW_SANDBOX")
        );
    }
    @Test
    void monthlyTicketSoldOutsideSaleWindowShouldBeDetected() {
        when(caracalApiClient.fetchSimulatedPrice(
                eq("NYW_SANDBOX"),
                eq("FULL_MONTHLY"),
                eq("EDGEWTR"),
                eq("MIDTOWN"),
                eq("2026-10-12T12:00:00-04:00")
        )).thenReturn(new BigDecimal("414.50"));

        var result = validationService.validate("NYW_SANDBOX");

        assertTrue(result.discrepancies().stream().anyMatch(
                discrepancy ->
                        discrepancy.category().equals("SALE WINDOW DIFFERS")
                        && discrepancy.ticketType().equals("MONTHLY")
                        && discrepancy.route().equals("EDGEWTR→MIDTOWN")
        ));
    }
}

