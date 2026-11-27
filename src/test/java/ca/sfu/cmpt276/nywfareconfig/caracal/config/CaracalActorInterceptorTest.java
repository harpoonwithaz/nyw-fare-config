package ca.sfu.cmpt276.nywfareconfig.caracal.config;

import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CaracalActorInterceptorTest {

    private CaracalActorInterceptor interceptorFor(String actor) {
        ActorService actorService = mock(ActorService.class);
        when(actorService.currentActorHeader()).thenReturn(actor);
        return new CaracalActorInterceptor(actorService);
    }

    private HttpRequest fakeRequest(HttpHeaders headers, Map<String, Object> attributes) {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getAttributes()).thenReturn(attributes);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost/site-list"));
        return request;
    }

    @Test
    void addsOperatorPrefixedHeader() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        HttpRequest request = fakeRequest(headers, new HashMap<>());

        interceptorFor("admin@caracal.com")
                .intercept(request, new byte[0], mock(ClientHttpRequestExecution.class));

        assertEquals("operator:admin@caracal.com", headers.getFirst("X-Actor"));
    }

    @Test
    void usesSystemWhenTheFlagIsTrue() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        HttpRequest request = fakeRequest(headers, new HashMap<>(Map.of("useSystemActor", true)));

        interceptorFor("admin@caracal.com")
                .intercept(request, new byte[0], mock(ClientHttpRequestExecution.class));

        assertEquals("system", headers.getFirst("X-Actor"));
    }

    @Test
    void overwritesAHeaderThatWasAlreadySet() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Actor", "operator:someone.else@example.com");
        HttpRequest request = fakeRequest(headers, new HashMap<>());

        interceptorFor("admin@caracal.com")
                .intercept(request, new byte[0], mock(ClientHttpRequestExecution.class));

        assertEquals("operator:admin@caracal.com", headers.getFirst("X-Actor"));
    }

    @Test
    void doesNotDoubleThePrefix() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        HttpRequest request = fakeRequest(headers, new HashMap<>());

        interceptorFor("operator:admin@caracal.com")
                .intercept(request, new byte[0], mock(ClientHttpRequestExecution.class));

        assertEquals("operator:admin@caracal.com", headers.getFirst("X-Actor"));
    }

    @Test
    void refusesToSendWhenTheActorIsBlank() throws Exception {
        HttpRequest request = fakeRequest(new HttpHeaders(), new HashMap<>());
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        assertThrows(IllegalStateException.class,
                () -> interceptorFor("   ").intercept(request, new byte[0], execution));
        verify(execution, never()).execute(request, new byte[0]);
    }

    @Test
    void realRestClientSendsOperatorOrSystemDependingOnTheAttribute() {
        RestClient.Builder builder = RestClient.builder().requestInterceptor(interceptorFor("admin@caracal.com"));
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.baseUrl("http://caracal.test").build();

        server.expect(requestTo("http://caracal.test/rate-table-get"))
                .andExpect(header("X-Actor", "operator:admin@caracal.com"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://caracal.test/tenant-list"))
                .andExpect(header("X-Actor", "system"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.get().uri("/rate-table-get").attribute("useSystemActor", false).retrieve().body(String.class);
        client.get().uri("/tenant-list").attribute("useSystemActor", true).retrieve().body(String.class);

        server.verify();
    }
}