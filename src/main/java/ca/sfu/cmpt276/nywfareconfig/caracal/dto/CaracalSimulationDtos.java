package ca.sfu.cmpt276.nywfareconfig.caracal.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

public class CaracalSimulationDtos {

    // POST /functions/price-simulate request body
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PriceSimulateRequest(
            String tenantCode,
            String productCode,
            String siteCode,
            Map<String, String> dimensions,
            String at,
            String channel) {

        // Four-argument constructor
        public PriceSimulateRequest(
                String tenantCode,
                String productCode,
                String siteCode,
                Map<String, String> dimensions) {

            this(tenantCode, productCode, siteCode,
                    dimensions, null, null);
        }

        // Five-argument constructor
        public PriceSimulateRequest(
                String tenantCode,
                String productCode,
                String siteCode,
                Map<String, String> dimensions,
                String at) {

            this(tenantCode, productCode, siteCode,
                    dimensions, at, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricingDetail(
            String priceRuleSetName,
            BigDecimal base,
            BigDecimal total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceSimulateResponse(
            PricingDetail pricing) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CaracalErrorResponse(
            String error) {
    }

    // Shared result for fare grid and validation
    public record PriceSimulationResult(
            PriceSimulateResponse response,
            String errorMessage,
            Integer statusCode) {

        public PriceSimulationResult(
                PriceSimulateResponse response,
                String errorMessage) {

            this(response, errorMessage, null);
        }

        public boolean isSuccess() {
            return response != null
                    && response.pricing() != null;
        }
    }
}