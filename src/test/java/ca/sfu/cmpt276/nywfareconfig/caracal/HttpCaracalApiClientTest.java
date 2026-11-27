package ca.sfu.cmpt276.nywfareconfig.caracal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import tools.jackson.databind.JsonNode;

class HttpCaracalApiClientTest {

    private MockRestServiceServer server;
    private HttpCaracalApiClient client;
    private ActorService actorService;

    private static final String ACTOR = "operator@example.com";
    private static final String ACTOR_HEADER = "operator:" + ACTOR;

    private static final String PUBLICATION_ID = "publication-123";

    private static final String PUBLICATION_RESPONSE = """
            {
                "publicationId": "publication-123"
            }
            """;

    @BeforeEach
    void setUp() {

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8080/functions")
                .defaultHeader("X-Actor", "system");

        actorService = mock(ActorService.class);

        when(actorService.currentActorHeader())
                .thenReturn(ACTOR);

        server = MockRestServiceServer.bindTo(builder).build();

        client = new HttpCaracalApiClient(
                builder.build(),
                actorService
        );
    }

    @Test
    void fetchBaselinePriceUsesQuoteCreate() {

        server.expect(requestTo(
                    "http://localhost:8080/functions/quote-create"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
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

        server.expect(requestTo(
                    "http://localhost:8080/functions/quote-create"))
                .andRespond(withSuccess(
                        """
                        {
                          "pricing": {}
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        assertThrows(CaracalApiException.class, () ->
                client.fetchBaselinePrice(
                        "NYW_SANDBOX", "MIDTOWN",
                        "HOB14ST", "ADULT_ONEWAY"));

        server.verify();
    }

    // ============================================================
    // Story 5.3 - Publication API tests
    // ============================================================

    @Test
    void publishCatalogUsesCorrectEndpointAndPayload() {

        server.expect(requestTo(
                    "http://localhost:8080/functions/catalog-publish"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tenantCode").value("NYW_SANDBOX"))
                .andRespond(withSuccess(
                        PUBLICATION_RESPONSE,
                        MediaType.APPLICATION_JSON));

        JsonNode response = client.publishCatalog("NYW_SANDBOX");

        assertNotNull(response);

        assertEquals(
                PUBLICATION_ID,
                response.path("publicationId").asText()
        );

        server.verify();
    }

    @Test
    void publishCatalogHandlesServerError() {

        server.expect(requestTo(
                    "http://localhost:8080/functions/catalog-publish"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThrows(CaracalApiException.class, () ->
                client.publishCatalog("NYW_SANDBOX"));

        server.verify();
    }

    @Test
    void publishCatalogHandlesUnauthorizedResponse() {

        server.expect(requestTo(
                    "http://localhost:8080/functions/catalog-publish"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest());

        assertThrows(CaracalApiException.class, () ->
                client.publishCatalog("NYW_SANDBOX"));

        server.verify();
    }

    // ============================================================
    // Story 5.3 - Authenticated actor tests
    // ============================================================

    @Test
    void publishCatalogUsesAuthenticatedActor() {

        server.expect(requestTo(
                    "http://localhost:8080/functions/catalog-publish"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertEquals(
                        java.util.List.of(ACTOR_HEADER),
                        request.getHeaders().get("X-Actor")))
                .andRespond(withSuccess(
                        PUBLICATION_RESPONSE,
                        MediaType.APPLICATION_JSON));

        client.publishCatalog("NYW_SANDBOX");

        verify(actorService).currentActorHeader();

        server.verify();
    }

    @Test
    void publishCatalogRejectsUnauthenticatedActor() {

        when(actorService.currentActorHeader())
                .thenThrow(new IllegalStateException(
                        "No authenticated operator"));

        assertThrows(IllegalStateException.class, () ->
                client.publishCatalog("NYW_SANDBOX"));

        server.verify();
    }

    @Test
    void publishCatalogRejectsSystemActor() {

        when(actorService.currentActorHeader())
                .thenReturn("system");

        assertThrows(IllegalStateException.class, () ->
                client.publishCatalog("NYW_SANDBOX"));

        server.verify();
    }

    @Test
    void publishCatalogRejectsBlankActor() {

        when(actorService.currentActorHeader())
                .thenReturn(" ");

        assertThrows(IllegalStateException.class, () ->
                client.publishCatalog("NYW_SANDBOX"));

        server.verify();
    }

    @Test
    void publishCatalogRejectsNullActor() {

        when(actorService.currentActorHeader())
                .thenReturn(null);

        assertThrows(IllegalStateException.class, () ->
                client.publishCatalog("NYW_SANDBOX"));

        server.verify();
    }
}


