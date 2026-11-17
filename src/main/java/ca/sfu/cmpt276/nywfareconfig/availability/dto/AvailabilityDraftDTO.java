package ca.sfu.cmpt276.nywfareconfig.availability.dto;

import java.time.LocalDate;
import java.util.List;

public class AvailabilityDraftDTO {
    private String siteCode;
    private String productCode;
    private boolean available;
    private String conditionType;
    private List<Integer> daysOfWeek;
    private String startTime;
    private String endTime;
    private String startDate;
    private String endDate;

    public AvailabilityDraftDTO() {}

    // Getters & Setters
    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getConditionType() { return conditionType; }
    public void setConditionType(String conditionType) { this.conditionType = conditionType; }

    public List<Integer> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<Integer> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate != null ? endDate.toString() : null; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}