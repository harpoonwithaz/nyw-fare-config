package ca.sfu.cmpt276.nywfareconfig.caracal.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class CaracalClientConfig {

    @Bean
    public RestClient caracalRestClient(
            RestClient.Builder builder,
            ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder,
            CaracalApiProperties props) {

        HttpClientSettings settings = HttpClientSettings.defaults()
                .withTimeouts(props.connectTimeout(), props.readTimeout());

        ClientHttpRequestFactory requestFactory = requestFactoryBuilder.build(settings);

        return builder
                .baseUrl(props.baseUrl() + "/functions")
                .defaultHeader("X-Actor", "system") // TODO: Change this to use CaracalActorInterceptor
                .requestFactory(requestFactory)
                .build();
    }
}
