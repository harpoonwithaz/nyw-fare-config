package ca.sfu.cmpt276.nywfareconfig.caracal.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class CaracalSimulationDtos {

    // POST /functions/price-simulate request body
    // TODO: add String at for when implemnting prices at certain times
    public record PriceSimulateRequest(String tenantCode, String productCode, String siteCode,
            Map<String, String> dimensions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricingDetail(String priceRuleSetName, BigDecimal base, BigDecimal total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceSimulateResponse(PricingDetail pricing) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CaracalErrorResponse(String error) {}

    public record PriceSimulationResult(PriceSimulateResponse response, String errorMessage) {
    }
}
