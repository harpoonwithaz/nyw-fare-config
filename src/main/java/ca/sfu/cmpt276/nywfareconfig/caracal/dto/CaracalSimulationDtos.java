package ca.sfu.cmpt276.nywfareconfig.caracal.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class CaracalSimulationDtos {

    // GET /functions/price-simulate request body
    // TODO: add String at for when implemnting prices at certain times
    public record PriceSimulateRequest(String tenantCode, String productCode, String siteCode,
            Map<String, String> dimensions) {
    }

    // {
    // "dryRun": true,
    // "pricing": {
    // "resolvedAssignmentId": "e460546c-090d-4e24-aaf5-ab2f6db906ea",
    // "priceRuleSetName": "OD-ADULT_ONEWAY",
    // "matchedPriceRuleId": "c7f18988-dc95-4440-a110-0b188174bfb5",
    // "durationMinutes": 0,
    // "base": 13.25,
    // "adjustments": [],
    // "total": 13.25
    // }
    // }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricingDetail(String priceRuleSetName, BigDecimal base, BigDecimal total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceSimulateResponse(PricingDetail pricing) {
    }

    public record CaracalErrorResponse(String error) {}

    public record PriceSimulationResult(PriceSimulateResponse response, String errorMessage) {
        public boolean isSuccess() {
            return response != null && response.pricing() != null;
        }
    }
}
