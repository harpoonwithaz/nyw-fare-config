
package ca.sfu.cmpt276.nywfareconfig.caracal.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

public class CaracalSimulationDtos {

    // POST /functions/price-simulate request body
    // Supports optional timestamp and channel for validation.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PriceSimulateRequest(
            String tenantCode,
            String productCode,
            String siteCode,
            Map<String, String> dimensions,
            String at,
            String channel) {

        // Backward-compatible constructor for existing FareService calls.
        public PriceSimulateRequest(
                String tenantCode,
                String productCode,
                String siteCode,
                Map<String, String> dimensions) {

            this(tenantCode, productCode, siteCode, dimensions, null, null);
        }
    }

    // Response from Caracal price simulation.
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

    // Error response returned by Caracal.
    public record CaracalErrorResponse(
            String error) {
    }

    // Shared result for fare grid and validation.
    public record PriceSimulationResult(
            PriceSimulateResponse response,
            String errorMessage,
            Integer statusCode) {

        // Backward-compatible constructor for existing callers.
        public PriceSimulationResult(
                PriceSimulateResponse response,
                String errorMessage) {

            this(response, errorMessage, null);
        }

        public boolean isSuccess() {
            return response != null && response.pricing() != null;
        }
    }
}


