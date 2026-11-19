package ca.sfu.cmpt276.nywfareconfig.caracal.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class CaracalCatalogDtos {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TenantSummary(String code, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TenantListResponse(List<TenantSummary> tenants) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TenantDetail(String code, String name, String defaultCurrency, String defaultLocale,
            List<String> supportedLocales) {
    }
}
