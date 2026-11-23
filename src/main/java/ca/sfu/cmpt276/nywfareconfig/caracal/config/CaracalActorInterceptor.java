package ca.sfu.cmpt276.nywfareconfig.caracal.config;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import ca.sfu.cmpt276.nywfareconfig.security.ActorService;

@Component
public class CaracalActorInterceptor implements ClientHttpRequestInterceptor {

    private final ActorService actorService;

    public CaracalActorInterceptor(ActorService actorService) {
        this.actorService = actorService;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        Object useSystemAttr = request.getAttributes().get("useSystemActor");
        boolean useSystem = Boolean.TRUE.equals(useSystemAttr);

        String actorHeader;
        if (useSystem) {
            actorHeader = "system";
        } else {
            Object explicitActor = request.getAttributes().get("explicitActor");
            if (explicitActor instanceof String explicit && !explicit.isBlank()) {
                actorHeader = formatOperatorHeader(explicit);
            } else {
                try {
                    actorHeader = formatOperatorHeader(actorService.currentActorHeader());
                } catch (IllegalStateException ex) {
                    actorHeader = "system";
                }
            }
        }

        request.getHeaders().set("X-Actor", actorHeader);
        return execution.execute(request, body);
    }

    private String formatOperatorHeader(String rawActor) {
        if (rawActor == null || rawActor.isBlank()) {
            return "system";
        }
        if ("system".equalsIgnoreCase(rawActor) || rawActor.startsWith("operator:")) {
            return rawActor;
        }
        return "operator:" + rawActor;
    }
}