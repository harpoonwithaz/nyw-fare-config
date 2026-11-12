package ca.sfu.cmpt276.nywfareconfig.availability.dto;

import java.util.List;
import java.util.Map;

public class ProductAvailabilityView {
    private String siteCode;
    private List<String> ticketProducts;
    private Map<String, Boolean> activeToggles;

    public ProductAvailabilityView() {}

    public ProductAvailabilityView(String siteCode, List<String> ticketProducts, Map<String, Boolean> activeToggles) {
        this.siteCode = siteCode;
        this.ticketProducts = ticketProducts;
        this.activeToggles = activeToggles;
    }

    public boolean isProductAvailable(String prod) {
        return activeToggles != null && activeToggles.getOrDefault(prod, true);
    }

    // Getters & Setters
    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public List<String> getTicketProducts() { return ticketProducts; }
    public void setTicketProducts(List<String> ticketProducts) { this.ticketProducts = ticketProducts; }

    public Map<String, Boolean> getActiveToggles() { return activeToggles; }
    public void setActiveToggles(Map<String, Boolean> activeToggles) { this.activeToggles = activeToggles; }
}