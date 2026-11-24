package ca.sfu.cmpt276.nywfareconfig.caracal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.RateAssignmentSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.RateTableGetRequest;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulateRequest;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.PriceSimulationResult;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;

class HttpCaracalApiClientTests {

    private static final String BASE_URL = "http://caracal.test/functions";

    private MockRestServiceServer mockServer;
    private HttpCaracalApiClient apiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        // If your HttpCaracalApiClient constructor also takes ActorService, pass mock(ActorService.class) as the 2nd arg.
        apiClient = new HttpCaracalApiClient(builder.build());
    }

    @Test
    void fetchRateTableRejectsInvalidPayload() {
        assertThrows(IllegalArgumentException.class, () -> apiClient.fetchRateTable(null));
        assertThrows(IllegalArgumentException.class,
                () -> apiClient.fetchRateTable(new RateTableGetRequest(" ", "RT-MIDTOWN")));
        assertThrows(IllegalArgumentException.class,
                () -> apiClient.fetchRateTable(new RateTableGetRequest("NYW_SANDBOX", "")));
    }

    @Test
    void fetchRateTableReturnsAssignmentsOnHttp200() {
        mockServer.expect(requestTo(BASE_URL + "/rate-table-get"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.tenantCode").value("NYW_SANDBOX"))
                .andExpect(jsonPath("$.rateTableName").value("RT-MIDTOWN"))
                .andRespond(withSuccess(
                        """
                        {
                          "assignments": [
                            { "productCode": "ADULT_ONEWAY", "cellType": "VALUE", "priceRuleSetName": "PRS_ADULT" },
                            { "productCode": "SENIOR_ONEWAY", "cellType": "NOT_SOLD", "priceRuleSetName": null }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        Optional<List<RateAssignmentSummary>> result = apiClient.fetchRateTable(
                new RateTableGetRequest("NYW_SANDBOX", "RT-MIDTOWN"));

        mockServer.verify();
        assertTrue(result.isPresent());
        assertEquals(2, result.get().size());
        assertEquals("ADULT_ONEWAY", result.get().get(0).productCode());
        assertEquals("VALUE", result.get().get(0).cellType());
    }

    @Test
    void fetchRateTableReturnsEmptyOptionalOnHttp404() {
        mockServer.expect(requestTo(BASE_URL + "/rate-table-get"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"rate table not found\"}"));

        Optional<List<RateAssignmentSummary>> result = apiClient.fetchRateTable(
                new RateTableGetRequest("NYW_SANDBOX", "RT-PAULSHK"));

        mockServer.verify();
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchRateTableThrowsCaracalApiExceptionOnAuthOrServerErrors() {
        mockServer.expect(requestTo(BASE_URL + "/rate-table-get"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(CaracalApiException.class, () -> apiClient.fetchRateTable(
                new RateTableGetRequest("NYW_SANDBOX", "RT-MIDTOWN")));

        mockServer.reset();
        mockServer.expect(requestTo(BASE_URL + "/rate-table-get"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(CaracalApiException.class, () -> apiClient.fetchRateTable(
                new RateTableGetRequest("NYW_SANDBOX", "RT-MIDTOWN")));
    }

    @Test
    void simulatePriceRejectsInvalidPayload() {
        assertThrows(IllegalArgumentException.class, () -> apiClient.simulatePrice(null));
        assertThrows(IllegalArgumentException.class, () -> apiClient.simulatePrice(
                new PriceSimulateRequest("NYW_SANDBOX", "ADULT_ONEWAY", "MIDTOWN", null, null)));
        assertThrows(IllegalArgumentException.class, () -> apiClient.simulatePrice(
                new PriceSimulateRequest("NYW_SANDBOX", " ", "MIDTOWN", Map.of("DEST", "EDGEWTR"), null)));
    }

    @Test
    void simulatePriceOmitsNullAtAndReturnsPricingOnHttp200() {
        mockServer.expect(requestTo(BASE_URL + "/price-simulate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(not(containsString("\"at\""))))
                .andExpect(jsonPath("$.tenantCode").value("NYW_SANDBOX"))
                .andExpect(jsonPath("$.productCode").value("ADULT_ONEWAY"))
                .andExpect(jsonPath("$.siteCode").value("MIDTOWN"))
                .andExpect(jsonPath("$.dimensions.DEST").value("EDGEWTR"))
                .andRespond(withSuccess(
                        """
                        {
                          "pricing": {
                            "priceRuleSetName": "PRS_ADULT",
                            "base": 13.25,
                            "total": 13.25
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        PriceSimulationResult result = apiClient.simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "ADULT_ONEWAY", "MIDTOWN", Map.of("DEST", "EDGEWTR"), null));

        mockServer.verify();
        assertNull(result.errorMessage());
        assertNotNull(result.response());
        assertEquals(new BigDecimal("13.25"), result.response().pricing().total());
        assertEquals("PRS_ADULT", result.response().pricing().priceRuleSetName());
    }

    @Test
    void simulatePriceIncludesAtWhenProvided() {
        mockServer.expect(requestTo(BASE_URL + "/price-simulate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.at").value("2026-10-24T10:00:00-04:00"))
                .andRespond(withSuccess(
                        """
                        {
                          "pricing": {
                            "priceRuleSetName": "PRS_WKND",
                            "base": 6.50,
                            "total": 6.50
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        PriceSimulationResult result = apiClient.simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "WEEKEND_TICKET", "HOBNJT",
                Map.of("DEST", "PIER11"), "2026-10-24T10:00:00-04:00"));

        mockServer.verify();
        assertEquals(new BigDecimal("6.50"), result.response().pricing().total());
    }

    @Test
    void simulatePriceReturnsErrorMessageForBusiness4xxJsonAndPlainTextBodies() {
        mockServer.expect(requestTo(BASE_URL + "/price-simulate"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"no PriceRule matched the resolved dimension context\"}"));

        PriceSimulationResult jsonResult = apiClient.simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "ADULT_ONEWAY", "MIDTOWN", Map.of("DEST", "PIER11"), null));

        assertNull(jsonResult.response());
        assertEquals("no PriceRule matched the resolved dimension context", jsonResult.errorMessage());

        mockServer.reset();
        mockServer.expect(requestTo(BASE_URL + "/price-simulate"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("not sold at this terminal"));

        PriceSimulationResult textResult = apiClient.simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "SENIOR_ONEWAY", "MIDTOWN", Map.of("DEST", "EDGEWTR"), null));

        assertNull(textResult.response());
        assertEquals("not sold at this terminal", textResult.errorMessage());
    }

    @Test
    void simulatePriceThrowsOnEmptyPricingAuthErrorsAndServerErrors() {
        // 200 with missing pricing block
        mockServer.expect(requestTo(BASE_URL + "/price-simulate"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(CaracalApiException.class, () -> apiClient.simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "ADULT_ONEWAY", "MIDTOWN", Map.of("DEST", "EDGEWTR"), null)));

        // 401 should NOT be swallowed as a business 4xx
        mockServer.reset();
        mockServer.expect(requestTo(BASE_URL + "/price-simulate"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"unauthorized\"}"));
        assertThrows(CaracalApiException.class, () -> apiClient.simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "ADULT_ONEWAY", "MIDTOWN", Map.of("DEST", "EDGEWTR"), null)));

        // 403 should NOT be swallowed as a business 4xx
        mockServer.reset();
        mockServer.expect(requestTo(BASE_URL + "/price-simulate"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"forbidden\"}"));
        assertThrows(CaracalApiException.class, () -> apiClient.simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "ADULT_ONEWAY", "MIDTOWN", Map.of("DEST", "EDGEWTR"), null)));

        // 500 should propagate as CaracalApiException
        mockServer.reset();
        mockServer.expect(requestTo(BASE_URL + "/price-simulate"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThrows(CaracalApiException.class, () -> apiClient.simulatePrice(new PriceSimulateRequest(
                "NYW_SANDBOX", "ADULT_ONEWAY", "MIDTOWN", Map.of("DEST", "EDGEWTR"), null)));
    }
}