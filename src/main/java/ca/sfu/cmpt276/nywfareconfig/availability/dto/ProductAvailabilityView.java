package ca.sfu.cmpt276.nywfareconfig.availability.dto;

import ca.sfu.cmpt276.nywfareconfig.availability.model.TemporalCondition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductAvailabilityView {
    private String siteCode;
    private String routeCode;
    private String rateTableName;
    private List<String> ticketProducts;
    private Map<String, String> productNames = new HashMap<>();
    private Map<String, Boolean> activeToggles = new HashMap<>();
    private Map<String, Boolean> phasedOutProducts = new HashMap<>();
    private Map<String, String> rateAssignmentIds = new HashMap<>();
    private Map<String, String> activeWhenConditions = new HashMap<>();
    private Map<String, String> caracalStatuses = new HashMap<>();
    private Map<String, Boolean> savedInDb = new HashMap<>();
    private Map<String, TemporalCondition> temporalConditions = new HashMap<>();

    public ProductAvailabilityView() {}

    public ProductAvailabilityView(String siteCode, List<String> ticketProducts, Map<String, Boolean> activeToggles) {
        this.siteCode = siteCode;
        this.routeCode = "ALL";
        this.ticketProducts = ticketProducts;
        this.activeToggles = activeToggles != null ? activeToggles : new HashMap<>();
    }

    public ProductAvailabilityView(String siteCode,
                                   List<String> ticketProducts,
                                   Map<String, Boolean> activeToggles,
                                   Map<String, TemporalCondition> temporalConditions) {
        this(siteCode, ticketProducts, activeToggles);
        this.temporalConditions = temporalConditions != null ? temporalConditions : new HashMap<>();
    }

    public boolean isProductAvailable(String prod) {
        if (isProductPhasedOut(prod)) {
            return false;
        }
        return activeToggles != null && activeToggles.getOrDefault(prod, true);
    }

    public boolean isProductPhasedOut(String prod) {
        return phasedOutProducts != null && phasedOutProducts.getOrDefault(prod, false);
    }

    public String getProductDisplayName(String prod) {
        if (productNames != null && productNames.containsKey(prod) && productNames.get(prod) != null) {
            return productNames.get(prod);
        }
        return prod;
    }

    public String getRateAssignmentId(String prod) {
        return rateAssignmentIds != null ? rateAssignmentIds.get(prod) : null;
    }

    public String getActiveWhenCondition(String prod) {
        return activeWhenConditions != null ? activeWhenConditions.get(prod) : null;
    }

    public String getCaracalStatus(String prod) {
        return caracalStatuses != null ? caracalStatuses.getOrDefault(prod, "SELLABLE") : "SELLABLE";
    }

    public boolean isSavedInDb(String prod) {
        return savedInDb != null && savedInDb.getOrDefault(prod, false);
    }

    public TemporalCondition getConditionForProduct(String prod) {
        return temporalConditions != null ? temporalConditions.get(prod) : null;
    }

    public String getConditionSummary(String prod) {
        TemporalCondition cond = getConditionForProduct(prod);
        if (cond == null) {
            String remoteCond = getActiveWhenCondition(prod);
            return (remoteCond != null && !remoteCond.isBlank()) ? "Caracal: " + remoteCond : "All Days Active";
        }

        String datePart = "";
        if (cond.getStartDate() != null || cond.getEndDate() != null) {
            datePart = (cond.getStartDate() != null ? cond.getStartDate() : "Start")
                    + " to " + (cond.getEndDate() != null ? cond.getEndDate() : "End");
        }

        String timePart = "";
        if (cond.getStartTime() != null || cond.getEndTime() != null) {
            String s = cond.getStartTime() != null ? cond.getStartTime().toString() : "00:00";
            String e = cond.getEndTime() != null ? cond.getEndTime().toString() : "23:59";
            if (s.length() > 5) s = s.substring(0, 5);
            if (e.length() > 5) e = e.substring(0, 5);
            timePart = s + "-" + e;
        }

        String labelPrefix = (cond.getDiscountLabel() != null && !cond.getDiscountLabel().isBlank())
                ? cond.getDiscountLabel() + ": "
                : "";

        if ("TEMPORARY_PROMO".equals(cond.getConditionType())) {
            String base = labelPrefix + "One-Time (" + (datePart.isEmpty() ? "Active Window" : datePart) + ")";
            return timePart.isEmpty() ? base : base + " [" + timePart + "]";
        }

        if ("RECURRING_MONTHLY".equals(cond.getConditionType())) {
            int domStart = cond.getDayOfMonthStart() != null ? cond.getDayOfMonthStart() : 1;
            int domEnd = cond.getDayOfMonthEnd() != null ? cond.getDayOfMonthEnd() : 31;
            String base = labelPrefix + "Monthly Days " + domStart + "-" + domEnd;
            if (!timePart.isEmpty()) base += " (" + timePart + ")";
            if (!datePart.isEmpty()) base += " [" + datePart + "]";
            return base;
        }

        int days = cond.getDaysOfWeek() != null ? cond.getDaysOfWeek().size() : 0;
        String base = labelPrefix + days + " Days (" + (timePart.isEmpty() ? "00:00-23:59" : timePart) + ")";
        return datePart.isEmpty() ? base : base + " [" + datePart + "]";
    }

    // Getters & Setters
    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }

    public String getRateTableName() { return rateTableName; }
    public void setRateTableName(String rateTableName) { this.rateTableName = rateTableName; }

    public List<String> getTicketProducts() { return ticketProducts; }
    public void setTicketProducts(List<String> ticketProducts) { this.ticketProducts = ticketProducts; }

    public Map<String, String> getProductNames() { return productNames; }
    public void setProductNames(Map<String, String> productNames) { this.productNames = productNames; }

    public Map<String, Boolean> getActiveToggles() { return activeToggles; }
    public void setActiveToggles(Map<String, Boolean> activeToggles) { this.activeToggles = activeToggles; }

    public Map<String, Boolean> getPhasedOutProducts() { return phasedOutProducts; }
    public void setPhasedOutProducts(Map<String, Boolean> phasedOutProducts) { this.phasedOutProducts = phasedOutProducts; }

    public Map<String, String> getRateAssignmentIds() { return rateAssignmentIds; }
    public void setRateAssignmentIds(Map<String, String> rateAssignmentIds) { this.rateAssignmentIds = rateAssignmentIds; }

    public Map<String, String> getActiveWhenConditions() { return activeWhenConditions; }
    public void setActiveWhenConditions(Map<String, String> activeWhenConditions) { this.activeWhenConditions = activeWhenConditions; }

    public Map<String, String> getCaracalStatuses() { return caracalStatuses; }
    public void setCaracalStatuses(Map<String, String> caracalStatuses) { this.caracalStatuses = caracalStatuses; }

    public Map<String, Boolean> getSavedInDb() { return savedInDb; }
    public void setSavedInDb(Map<String, Boolean> savedInDb) { this.savedInDb = savedInDb; }

    public Map<String, TemporalCondition> getTemporalConditions() { return temporalConditions; }
    public void setTemporalConditions(Map<String, TemporalCondition> temporalConditions) { this.temporalConditions = temporalConditions; }
}