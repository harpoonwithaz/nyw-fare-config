package ca.sfu.cmpt276.nywfareconfig.availability.dto;

import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.model.TemporalCondition;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductAvailabilityView {

    private String siteCode;
    private String routeCode = AvailabilityRule.ALL_ROUTES;
    private String rateTableName;
    private List<String> allProducts;
    private Map<String, String> productNames = new LinkedHashMap<>();
    private Map<String, Boolean> productToggles = new LinkedHashMap<>();
    private Map<String, Boolean> phasedOutProducts = new LinkedHashMap<>();
    private Map<String, String> rateAssignmentIds = new LinkedHashMap<>();
    private Map<String, String> activeWhenConditions = new LinkedHashMap<>();
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

    public TemporalCondition getConditionForProduct(String productCode) {
        return temporalConditions.get(productCode);
    }

    public String getConditionSummary(String productCode) {
        TemporalCondition tc = temporalConditions.get(productCode);
        if (tc == null) {
            String activeWhen = activeWhenConditions.get(productCode);
            return (activeWhen != null && !activeWhen.isBlank()) ? activeWhen : "All Days Active";
        }

        String timeSuffix = (tc.getStartTime() != null && tc.getEndTime() != null)
                ? " (" + tc.getStartTime() + "-" + tc.getEndTime() + ")"
                : "";

        if ("TEMPORARY_PROMO".equals(tc.getConditionType())) {
            String start = tc.getStartDate() != null ? tc.getStartDate().toString() : "Now";
            String end = tc.getEndDate() != null ? tc.getEndDate().toString() : "Ongoing";
            return "Promo (" + start + " to " + end + ")" + timeSuffix;
        }

        if ("RECURRING_MONTHLY".equals(tc.getConditionType())
                && tc.getDayOfMonthStart() != null
                && tc.getDayOfMonthEnd() != null) {
            return "Days " + tc.getDayOfMonthStart() + "-" + tc.getDayOfMonthEnd() + " of Month" + timeSuffix;
        }

        String daysText = "All Days";
        if (tc.getDaysOfWeek() != null && !tc.getDaysOfWeek().isEmpty() && tc.getDaysOfWeek().size() < 7) {
            daysText = tc.getDaysOfWeek().stream()
                    .sorted()
                    .map(d -> DayOfWeek.of(d).getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .collect(Collectors.joining(", "));
        }

        return daysText + timeSuffix;
    }

    public String getSiteCode() {
        return siteCode;
    }

    public void setSiteCode(String siteCode) {
        this.siteCode = siteCode;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public void setRouteCode(String routeCode) {
        this.routeCode = routeCode;
    }

    public String getRateTableName() {
        return rateTableName;
    }

    public void setRateTableName(String rateTableName) {
        this.rateTableName = rateTableName;
    }

    public List<String> getAllProducts() {
        return allProducts;
    }

    public List<String> getTicketProducts() {
        return allProducts;
    }

    public void setAllProducts(List<String> allProducts) {
        this.allProducts = allProducts;
    }

    public Map<String, String> getProductNames() {
        return productNames;
    }

    public void setProductNames(Map<String, String> productNames) {
        this.productNames = productNames;
    }

    public Map<String, Boolean> getProductToggles() {
        return productToggles;
    }

    public void setProductToggles(Map<String, Boolean> productToggles) {
        this.productToggles = productToggles;
    }

    public Map<String, Boolean> getPhasedOutProducts() {
        return phasedOutProducts;
    }

    public void setPhasedOutProducts(Map<String, Boolean> phasedOutProducts) {
        this.phasedOutProducts = phasedOutProducts;
    }

    public Map<String, String> getRateAssignmentIds() {
        return rateAssignmentIds;
    }

    public void setRateAssignmentIds(Map<String, String> rateAssignmentIds) {
        this.rateAssignmentIds = rateAssignmentIds;
    }

    public Map<String, String> getActiveWhenConditions() {
        return activeWhenConditions;
    }

    public void setActiveWhenConditions(Map<String, String> activeWhenConditions) {
        this.activeWhenConditions = activeWhenConditions;
    }

    public Map<String, String> getCaracalStatuses() {
        return caracalStatuses;
    }

    public void setCaracalStatuses(Map<String, String> caracalStatuses) {
        this.caracalStatuses = caracalStatuses;
    }

    public Map<String, Boolean> getSavedInDb() {
        return savedInDb;
    }

    public void setSavedInDb(Map<String, Boolean> savedInDb) {
        this.savedInDb = savedInDb;
    }

    public Map<String, TemporalCondition> getTemporalConditions() {
        return temporalConditions;
    }

    public void setTemporalConditions(Map<String, TemporalCondition> temporalConditions) {
        this.temporalConditions = temporalConditions;
    }
}