package ca.sfu.cmpt276.nywfareconfig.caracal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class CaracalZonalDtos {

    private CaracalZonalDtos() {
    }

    /**
     * Reusable for both Zones (A -> "Zone A") and Fare Classes (FULL -> "Full").
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DimensionValueSummary(String key, String name) {}

    /**
     * Matches Caracal's site-dimension-bind model (e.g., ST051 -> ORIGIN_ZONE = A).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SiteDimensionBinding(String siteCode, String dimensionKey, String valueKey) {}
}