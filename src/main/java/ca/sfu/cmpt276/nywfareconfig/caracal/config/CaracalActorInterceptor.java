package ca.sfu.cmpt276.nywfareconfig.caracal.config;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import ca.sfu.cmpt276.nywfareconfig.security.ActorService;

// Sets X-Actor on every Caracal call. Requests marked with the useSystemActor attribute
// (like tenant-list, which runs before anyone picks a tenant) go out as "system";
// everything else is sent as the signed-in operator.
@Component
public class CaracalActorInterceptor implements ClientHttpRequestInterceptor {

    public static final String HEADER_NAME = "X-Actor";
    public static final String SYSTEM_ACTOR = "system";
    public static final String OPERATOR_PREFIX = "operator:";
    public static final String USE_SYSTEM_ACTOR = "useSystemActor";

    private final ActorService actorService;

    public CaracalActorInterceptor(ActorService actorService) {
        this.actorService = actorService;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (Boolean.TRUE.equals(request.getAttributes().get(USE_SYSTEM_ACTOR))) {
            request.getHeaders().set(HEADER_NAME, SYSTEM_ACTOR);
        } else {
            request.getHeaders().set(HEADER_NAME, OPERATOR_PREFIX + actorService.currentActorHeader());
        }
        return execution.execute(request, body);
    }
}
