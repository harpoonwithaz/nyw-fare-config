package ca.sfu.cmpt276.nywfareconfig.caracal;

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

class ActorHeaderInterceptorTest {

    private MockClientHttpRequest newRequest() {
        return new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/site-list"));
    }

    @Test
    void addsOperatorPrefixedHeader() throws Exception {
        ActorService actorService = mock(ActorService.class);
        when(actorService.currentActorHeader()).thenReturn("admin@caracal.com");
        ActorHeaderInterceptor interceptor = new ActorHeaderInterceptor(actorService);
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
        ActorHeaderInterceptor interceptor = new ActorHeaderInterceptor(actorService);
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
        ActorHeaderInterceptor interceptor = new ActorHeaderInterceptor(actorService);
        MockClientHttpRequest request = newRequest();
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        assertThrows(IllegalStateException.class, () -> interceptor.intercept(request, new byte[0], execution));
        verify(execution, never()).execute(request, new byte[0]);
    }
}