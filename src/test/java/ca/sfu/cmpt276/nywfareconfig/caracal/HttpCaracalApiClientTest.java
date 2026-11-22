package ca.sfu.cmpt276.nywfareconfig.caracal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;

class HttpCaracalApiClientTest {

    private MockRestServiceServer server;
    private HttpCaracalApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8080/functions");

        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpCaracalApiClient(builder.build(), null);
    }

    @Test
    void fetchBaselinePriceUsesQuoteCreate() {
        server.expect(requestTo("http://localhost:8080/functions/quote-create"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tenantCode").value("NYW_SANDBOX"))
                .andExpect(jsonPath("$.productCode").value("ADULT_ONEWAY"))
                .andExpect(jsonPath("$.siteCode").value("MIDTOWN"))
                .andExpect(jsonPath("$.dimensions.ORIGIN").value("MIDTOWN"))
                .andExpect(jsonPath("$.dimensions.DEST").value("HOB14ST"))
                .andRespond(withSuccess(
                        """
                        {
                          "pricing": {
                            "base": 10.75
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        BigDecimal price = client.fetchBaselinePrice(
                "NYW_SANDBOX", "MIDTOWN", "HOB14ST", "ADULT_ONEWAY");

        assertEquals(0, new BigDecimal("10.75").compareTo(price));
        server.verify();
    }

    @Test
    void fetchBaselinePriceRejectsInvalidResponse() {
        server.expect(requestTo("http://localhost:8080/functions/quote-create"))
                .andRespond(withSuccess(
                        """
                        {
                          "pricing": {}
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        assertThrows(CaracalApiException.class, () ->
                client.fetchBaselinePrice(
                        "NYW_SANDBOX", "MIDTOWN", "HOB14ST", "ADULT_ONEWAY"));

        server.verify();
    }
}

