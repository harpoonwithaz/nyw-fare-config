package ca.sfu.cmpt276.nywfareconfig.caracal.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "caracal.api")
public record CaracalApiProperties(
        @NotBlank String baseUrl,
        @NotNull @DefaultValue("3s") Duration connectTimeout,
        @NotNull @DefaultValue("5s") Duration readTimeout,
        @DefaultValue("") String metraConfigPath) {

    @ConstructorBinding
    public CaracalApiProperties {
    }

    public CaracalApiProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        this(baseUrl, connectTimeout, readTimeout, "");
    }
}