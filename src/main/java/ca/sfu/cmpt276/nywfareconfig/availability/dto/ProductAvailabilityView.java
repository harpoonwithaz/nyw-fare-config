package ca.sfu.cmpt276.nywfareconfig.availability.dto;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.model.TemporalCondition;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductAvailabilityView {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private String siteCode;
    private String routeCode = AvailabilityRule.ALL_ROUTES;
    private List<String> allProducts;
    private Map<String, String> productNames = new LinkedHashMap<>();
    private Map<String, Boolean> productToggles = new LinkedHashMap<>();
    private Map<String, Boolean> phasedOutProducts = new LinkedHashMap<>();
    private Map<String, String> caracalStatuses = new LinkedHashMap<>();
    private Map<String, Boolean> savedInDb = new LinkedHashMap<>();
    private Map<String, TemporalCondition> temporalConditions = new LinkedHashMap<>();

    public ProductAvailabilityView(
            String siteCode,
            List<String> allProducts,
            Map<String, Boolean> productToggles,
            Map<String, TemporalCondition> temporalConditions) {
        this.siteCode = siteCode;
        this.allProducts = allProducts;
        if (productToggles != null) {
            this.productToggles = productToggles;
        }
        if (temporalConditions != null) {
            this.temporalConditions = temporalConditions;
        }
    }

    public boolean isProductAvailable(String productCode) {
        return Boolean.TRUE.equals(productToggles.get(productCode));
    }

    public boolean isProductPhasedOut(String productCode) {
        return Boolean.TRUE.equals(phasedOutProducts.get(productCode));
    }

    public boolean isSavedInDb(String productCode) {
        return Boolean.TRUE.equals(savedInDb.get(productCode));
    }

    public String getProductDisplayName(String productCode) {
        return productNames.getOrDefault(productCode, productCode);
    }

    public String getCaracalStatus(String productCode) {
        return caracalStatuses.getOrDefault(productCode, isProductAvailable(productCode) ? "SELLABLE" : "NOT_SOLD");
    }

    public TemporalCondition getConditionForProduct(String productCode) {
        return temporalConditions.get(productCode);
    }

    public String getConditionType(String productCode) {
        TemporalCondition tc = temporalConditions.get(productCode);
        return tc != null && tc.getConditionType() != null ? tc.getConditionType() : "RECURRING_WEEKLY";
    }

    public String getDaysCsv(String productCode) {
        TemporalCondition tc = temporalConditions.get(productCode);
        if (tc == null || tc.getDaysOfWeek() == null || tc.getDaysOfWeek().isEmpty()) {
            return "";
        }
        return tc.getDaysOfWeek().stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public String getStartTimeValue(String productCode) {
        TemporalCondition tc = temporalConditions.get(productCode);
        return (tc != null && tc.getStartTime() != null) ? tc.getStartTime().format(HH_MM) : "";
    }

    public String getEndTimeValue(String productCode) {
        TemporalCondition tc = temporalConditions.get(productCode);
        return (tc != null && tc.getEndTime() != null) ? tc.getEndTime().format(HH_MM) : "";
    }

    public String getStartDateValue(String productCode) {
        TemporalCondition tc = temporalConditions.get(productCode);
        return (tc != null && tc.getStartDate() != null) ? tc.getStartDate().toString() : "";
    }

    public String getEndDateValue(String productCode) {
        TemporalCondition tc = temporalConditions.get(productCode);
        return (tc != null && tc.getEndDate() != null) ? tc.getEndDate().toString() : "";
    }

    public String getConditionSummary(String productCode) {
        TemporalCondition tc = temporalConditions.get(productCode);
        if (tc == null) {
            return "All Days Active";
        }

        String timeSuffix = (tc.getStartTime() != null && tc.getEndTime() != null)
                ? " (" + tc.getStartTime().format(HH_MM) + "-" + tc.getEndTime().format(HH_MM) + ")"
                : "";

        if ("TEMPORARY_PROMO".equals(tc.getConditionType())) {
            String start = tc.getStartDate() != null ? tc.getStartDate().toString() : "Now";
            String end = tc.getEndDate() != null ? tc.getEndDate().toString() : "Ongoing";
            return "Promo (" + start + " to " + end + ")" + timeSuffix;
        }

        String daysText = "All Days";
        if (tc.getDaysOfWeek() != null && !tc.getDaysOfWeek().isEmpty()) {
            if (tc.getDaysOfWeek().size() < 7) {
                daysText = tc.getDaysOfWeek().stream()
                        .sorted()
                        .map(d -> DayOfWeek.of(d).getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                        .collect(Collectors.joining(", "));
            }
        }

        return daysText + timeSuffix;
    }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }

    public List<String> getAllProducts() { return allProducts; }
    public List<String> getTicketProducts() { return allProducts; }
    public void setAllProducts(List<String> allProducts) { this.allProducts = allProducts; }

    public Map<String, String> getProductNames() { return productNames; }
    public void setProductNames(Map<String, String> productNames) { this.productNames = productNames; }

    public Map<String, Boolean> getProductToggles() { return productToggles; }
    public void setProductToggles(Map<String, Boolean> productToggles) { this.productToggles = productToggles; }

    public Map<String, Boolean> getPhasedOutProducts() { return phasedOutProducts; }
    public void setPhasedOutProducts(Map<String, Boolean> phasedOutProducts) { this.phasedOutProducts = phasedOutProducts; }

    public Map<String, String> getCaracalStatuses() { return caracalStatuses; }
    public void setCaracalStatuses(Map<String, String> caracalStatuses) { this.caracalStatuses = caracalStatuses; }

    public Map<String, Boolean> getSavedInDb() { return savedInDb; }
    public void setSavedInDb(Map<String, Boolean> savedInDb) { this.savedInDb = savedInDb; }

    public Map<String, TemporalCondition> getTemporalConditions() { return temporalConditions; }
    public void setTemporalConditions(Map<String, TemporalCondition> temporalConditions) { this.temporalConditions = temporalConditions; }
}