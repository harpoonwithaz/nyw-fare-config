package ca.sfu.cmpt276.nywfareconfig.caracal;

import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ActorHeaderInterceptor implements ClientHttpRequestInterceptor {

    public static final String HEADER_NAME = "X-Actor";
    public static final String PREFIX = "operator:";

    private final ActorService actorService;

    public ActorHeaderInterceptor(ActorService actorService) {
        this.actorService = actorService;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String email = actorService.currentActorHeader();
        request.getHeaders().set(HEADER_NAME, PREFIX + email);
        return execution.execute(request, body);
    }
}