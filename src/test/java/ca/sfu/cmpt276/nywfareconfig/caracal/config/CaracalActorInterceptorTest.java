package ca.sfu.cmpt276.nywfareconfig.caracal.config;

import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaracalActorInterceptorTest {

    private MockClientHttpRequest newRequest() {
        return new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/site-list"));
    }

    @Test
    void addsOperatorPrefixedHeader() throws Exception {
        ActorService actorService = mock(ActorService.class);
        when(actorService.currentActorHeader()).thenReturn("admin@caracal.com");
        CaracalActorInterceptor interceptor = new CaracalActorInterceptor(actorService);
        MockClientHttpRequest request = newRequest();
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        byte[] body = new byte[0];

        interceptor.intercept(request, body, execution);

        assertEquals("operator:admin@caracal.com", request.getHeaders().getFirst("X-Actor"));
        verify(execution).execute(request, body);
    }

    @Test
    void overwritesAHeaderThatWasAlreadySet() throws Exception {
        ActorService actorService = mock(ActorService.class);
        when(actorService.currentActorHeader()).thenReturn("admin@caracal.com");
        CaracalActorInterceptor interceptor = new CaracalActorInterceptor(actorService);
        MockClientHttpRequest request = newRequest();
        request.getHeaders().set("X-Actor", "operator:someone.else@example.com");
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        interceptor.intercept(request, new byte[0], execution);

        assertEquals("operator:admin@caracal.com", request.getHeaders().getFirst("X-Actor"));
    }

    @Test
    void refusesToSendWhenNobodyIsSignedIn() throws Exception {
        ActorService actorService = mock(ActorService.class);
        when(actorService.currentActorHeader()).thenThrow(new IllegalStateException("No authenticated operator"));
        CaracalActorInterceptor interceptor = new CaracalActorInterceptor(actorService);
        MockClientHttpRequest request = newRequest();
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        // Surfaces as a Caracal error so callers show their normal "Caracal unavailable" message
        assertThrows(CaracalApiException.class, () -> interceptor.intercept(request, new byte[0], execution));
        verify(execution, never()).execute(request, new byte[0]);
    }

    @Test
    void sendsSystemWhenTheRequestAsksForIt() throws Exception {
        ActorService actorService = mock(ActorService.class);
        CaracalActorInterceptor interceptor = new CaracalActorInterceptor(actorService);
        MockClientHttpRequest request = newRequest();
        request.getAttributes().put(CaracalActorInterceptor.USE_SYSTEM_ACTOR, true);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        interceptor.intercept(request, new byte[0], execution);

        assertEquals("system", request.getHeaders().getFirst("X-Actor"));
        verify(actorService, never()).currentActorHeader();
    }
}
