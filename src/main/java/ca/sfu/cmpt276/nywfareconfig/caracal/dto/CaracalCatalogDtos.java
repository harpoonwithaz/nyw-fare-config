package ca.sfu.cmpt276.nywfareconfig.caracal.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    public record SiteSummary(String id, String siteCode, String name, String kind, String timezone) {
        public SiteSummary(String siteCode, String name) {
            this(null, siteCode, name, null, null);
        }
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

    // POST /functions/rate-table-list
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record RateTableListRequest(String tenantCode, String siteCode, Integer limit) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RateTableSummary(String id, String name, String status, String siteCode, String createdAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RateTableListResponse(List<RateTableSummary> items) {
    }

    // POST /functions/rate-table-get
    public record RateTableGetRequest(String tenantCode, String rateTableName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RateAssignmentSummary(
            String id,
            String productCode,
            String cellType,
            int priority,
            int sortOrder,
            String rateScheduleName,
            String priceRuleSetName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RateTableGetResponse(
            String id,
            String name,
            String status,
            String siteId,
            List<RateAssignmentSummary> assignments) {
    }

    // POST /functions/availability-simulate
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record AvailabilitySimulateRequest(
            String tenantCode,
            String siteCode,
            String channel,
            String at,
            List<String> credentials,
            Map<String, String> dimensions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SimulatedProductAvailability(
            String productCode,
            String status,
            String matchedCondition,
            String suppressedByProductCode,
            Integer boundaryClampMinutes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AvailabilitySimulateResponse(
            Boolean dryRun,
            List<SimulatedProductAvailability> products) {
    }

    // POST /functions/rate-assignment-update
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record RateAssignmentUpdateRequest(
            String tenantCode,
            String assignmentId,
            String cellType,
            Integer priority,
            Integer sortOrder,
            String rateScheduleName,
            String priceRuleSetName,
            String channelConditionName) {
    }

    // POST /functions/when-condition-create
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record WhenConditionCreateRequest(
            String tenantCode,
            String name,
            String conditionType,
            List<String> daysOfWeek,
            String startTime,
            String endTime,
            String dateRangeStart,
            String dateRangeEnd,
            Integer seasonMonthStart,
            Integer seasonMonthEnd,
            Integer dayOfMonthStart,
            Integer dayOfMonthEnd) {
    }

    // POST /functions/rate-assignment-create
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record RateAssignmentCreateRequest(
            String tenantCode,
            String rateTableName,
            String productCode,
            String rateScheduleName,
            String priceRuleSetName,
            String cellType,
            Integer priority,
            String whenConditionName,
            String channelConditionName,
            Integer sortOrder) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CaracalIdResponse(String id) {
    }
}