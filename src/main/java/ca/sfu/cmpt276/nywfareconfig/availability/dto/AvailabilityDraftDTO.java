package ca.sfu.cmpt276.nywfareconfig.availability.dto;

import java.time.LocalDate;
import java.util.List;

public class AvailabilityDraftDTO {
    private String siteCode;
    private String routeCode;
    private String productCode;
    private boolean available;
    private Boolean phasedOut;
    private boolean clearCondition;
    private String conditionType;
    private String discountLabel;
    private List<Integer> daysOfWeek;
    private Integer dayOfMonthStart;
    private Integer dayOfMonthEnd;
    private String startTime;
    private String endTime;
    private String startDate;
    private String endDate;

    public AvailabilityDraftDTO() {}

    // Getters & Setters
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

    public String getDiscountLabel() { return discountLabel; }
    public void setDiscountLabel(String discountLabel) { this.discountLabel = discountLabel; }

    public List<Integer> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<Integer> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public Integer getDayOfMonthStart() { return dayOfMonthStart; }
    public void setDayOfMonthStart(Integer dayOfMonthStart) { this.dayOfMonthStart = dayOfMonthStart; }

    public Integer getDayOfMonthEnd() { return dayOfMonthEnd; }
    public void setDayOfMonthEnd(Integer dayOfMonthEnd) { this.dayOfMonthEnd = dayOfMonthEnd; }

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