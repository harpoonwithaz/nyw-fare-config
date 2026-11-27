package ca.sfu.cmpt276.nywfareconfig.availability.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

public class AvailabilityDraftDTO {

    @NotBlank(message = "siteCode is required.")
    private String siteCode;

    private String routeCode;

    @NotBlank(message = "productCode is required.")
    private String productCode;

    private boolean available;
    private Boolean phasedOut;
    private boolean clearCondition;
    private String conditionType;
    private List<Integer> daysOfWeek;
    private String startTime;
    private String endTime;
    private String startDate;
    private String endDate;

    public AvailabilityDraftDTO() {}

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public Boolean getPhasedOut() { return phasedOut; }
    public void setPhasedOut(Boolean phasedOut) { this.phasedOut = phasedOut; }

    public boolean isClearCondition() { return clearCondition; }
    public void setClearCondition(boolean clearCondition) { this.clearCondition = clearCondition; }

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