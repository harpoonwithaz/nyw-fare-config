package ca.sfu.cmpt276.nywfareconfig.availability.dto;

import ca.sfu.cmpt276.nywfareconfig.availability.model.TemporalCondition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductAvailabilityView {
    private String siteCode;
    private List<String> ticketProducts;
    private Map<String, Boolean> activeToggles;
    private Map<String, TemporalCondition> temporalConditions = new HashMap<>();

    public ProductAvailabilityView() {}

    public ProductAvailabilityView(String siteCode, List<String> ticketProducts, Map<String, Boolean> activeToggles) {
        this.siteCode = siteCode;
        this.ticketProducts = ticketProducts;
        this.activeToggles = activeToggles;
    }

    public ProductAvailabilityView(String siteCode,
                                   List<String> ticketProducts,
                                   Map<String, Boolean> activeToggles,
                                   Map<String, TemporalCondition> temporalConditions) {
        this.siteCode = siteCode;
        this.ticketProducts = ticketProducts;
        this.activeToggles = activeToggles;
        this.temporalConditions = temporalConditions != null ? temporalConditions : new HashMap<>();
    }

    public boolean isProductAvailable(String prod) {
        return activeToggles != null && activeToggles.getOrDefault(prod, true);
    }

    public TemporalCondition getConditionForProduct(String prod) {
        return temporalConditions != null ? temporalConditions.get(prod) : null;
    }

    public String getConditionSummary(String prod) {
        TemporalCondition cond = getConditionForProduct(prod);
        if (cond == null) return "All Days Active";
        if ("TEMPORARY_PROMO".equals(cond.getConditionType())) {
            return "Promo (" + (cond.getStartDate() != null ? cond.getStartDate() : "Start")
                    + " to " + (cond.getEndDate() != null ? cond.getEndDate() : "End") + ")";
        }
        int days = cond.getDaysOfWeek() != null ? cond.getDaysOfWeek().size() : 0;
        String start = cond.getStartTime() != null ? cond.getStartTime().toString() : "00:00";
        String end = cond.getEndTime() != null ? cond.getEndTime().toString() : "23:59";
        return days + " Days (" + start + "-" + end + ")";
    }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public List<String> getTicketProducts() { return ticketProducts; }
    public void setTicketProducts(List<String> ticketProducts) { this.ticketProducts = ticketProducts; }

    public Map<String, Boolean> getActiveToggles() { return activeToggles; }
    public void setActiveToggles(Map<String, Boolean> activeToggles) { this.activeToggles = activeToggles; }

    public Map<String, TemporalCondition> getTemporalConditions() { return temporalConditions; }
    public void setTemporalConditions(Map<String, TemporalCondition> temporalConditions) { this.temporalConditions = temporalConditions; }
}