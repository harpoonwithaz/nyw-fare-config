package ca.sfu.cmpt276.nywfareconfig.caracal.dto;

import java.time.ZoneId;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CaracalCatalogDtos {

    // GET /functions/tenant-list
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TenantSummary(String code, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TenantList(List<TenantSummary> tenants) {
    }

    // GET /functions/tenant-get?tenantCode=
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TenantDetail(String code, String name, String defaultCurrency, String defaultLocale,
            List<String> supportedLocales) {
    }

    // GET /functions/site-list?tenantCode=
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SiteSummary(String siteCode, String name, ZoneId timezone) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SiteList(List<SiteSummary> sites) {
    }

    // GET /functions/product-list?tenantCode=
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductSummary(String code, String name, @JsonProperty("is_active") boolean isActive) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductList(List<ProductSummary> products) {
    }

    // POST /functions/rate-table-get
    public record RateTableGetRequest(String tenantCode, String rateTableName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RateAssignmentSummary(String productCode, String cellType, String priceRuleSetName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RateTableGetResponse(List<RateAssignmentSummary> assignments) {}
}
