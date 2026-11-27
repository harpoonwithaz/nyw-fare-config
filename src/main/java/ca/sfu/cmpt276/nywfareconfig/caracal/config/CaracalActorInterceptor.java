package ca.sfu.cmpt276.nywfareconfig.caracal.config;

import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CaracalActorInterceptor implements ClientHttpRequestInterceptor {

    public static final String HEADER_NAME = "X-Actor";
    public static final String SYSTEM_ATTRIBUTE = "useSystemActor";
    public static final String SYSTEM_ACTOR = "system";
    public static final String PREFIX = "operator:";

    private static final Logger log = LoggerFactory.getLogger(CaracalActorInterceptor.class);

    private final ActorService actorService;

    public CaracalActorInterceptor(ActorService actorService) {
        this.actorService = actorService;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        boolean useSystem = Boolean.TRUE.equals(request.getAttributes().get(SYSTEM_ATTRIBUTE));

        String value;
        if (useSystem) {
            value = SYSTEM_ACTOR;
        } else {
            String actor = actorService.currentActorHeader();
            if (actor == null || actor.isBlank()) {
                throw new IllegalStateException("No authenticated operator");
            }
            value = actor.startsWith(PREFIX) ? actor : PREFIX + actor;
        }

        request.getHeaders().set(HEADER_NAME, value);
        log.debug("Caracal request {} {} as {}", request.getMethod(), request.getURI().getPath(), value);
        return execution.execute(request, body);
    }
}