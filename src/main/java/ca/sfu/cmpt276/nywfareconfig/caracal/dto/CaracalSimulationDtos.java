package ca.sfu.cmpt276.nywfareconfig.caracal.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

public class CaracalSimulationDtos {

    // POST /functions/price-simulate request body
    public record PriceSimulateRequest(
            String tenantCode,
            String productCode,
            @JsonInclude(JsonInclude.Include.NON_NULL) String siteCode,
            Map<String, String> dimensions,
            @JsonInclude(JsonInclude.Include.NON_NULL) String at,
            @JsonInclude(JsonInclude.Include.NON_NULL) String channel) {

        public PriceSimulateRequest(
                String tenantCode,
                String productCode,
                String siteCode,
                Map<String, String> dimensions,
                String at) {
            this(tenantCode, productCode, siteCode, dimensions, at, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricingDetail(String priceRuleSetName, BigDecimal base, BigDecimal total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceSimulateResponse(PricingDetail pricing) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CaracalErrorResponse(String error) {
    }

    public record PriceSimulationResult(PriceSimulateResponse response, String errorMessage) {
    }
}