package ca.sfu.cmpt276.nywfareconfig.caracal.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import ca.sfu.cmpt276.nywfareconfig.caracal.HttpCaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.security.ActorService;

// Live Caracal answers 403 when read endpoints get operator:<email>, so every read must go out as system
class CaracalReadActorTest {

    private static final String BASE_URL = "http://caracal.test/functions";

    @Test
    void readCallsGoOutAsSystem() {
        ActorService actorService = mock(ActorService.class);
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL)
                .requestInterceptor(new CaracalActorInterceptor(actorService));
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpCaracalApiClient client = new HttpCaracalApiClient(builder.build());

        server.expect(requestTo(BASE_URL + "/tenant-get?tenantCode=NYW_SANDBOX"))
                .andExpect(header("X-Actor", "system"))
                .andRespond(withSuccess("{\"code\":\"NYW_SANDBOX\",\"name\":\"NYW\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/site-list?tenantCode=NYW_SANDBOX"))
                .andExpect(header("X-Actor", "system"))
                .andRespond(withSuccess("{\"sites\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/product-list?tenantCode=NYW_SANDBOX"))
                .andExpect(header("X-Actor", "system"))
                .andRespond(withSuccess("{\"products\":[]}", MediaType.APPLICATION_JSON));

        client.fetchTenant("NYW_SANDBOX");
        client.fetchSites("NYW_SANDBOX");
        client.fetchProducts("NYW_SANDBOX");

        server.verify();
        verify(actorService, never()).currentActorHeader();
    }
}
