package ca.sfu.cmpt276.nywfareconfig.availability.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "temporal_conditions")
public class TemporalCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // RECURRING_WEEKLY | RECURRING_MONTHLY | TEMPORARY_PROMO
    private String conditionType;

    // Optional label for discount/window (e.g. "Holiday Special", "Senior Commuter")
    private String discountLabel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "temporal_condition_days", joinColumns = @JoinColumn(name = "condition_id"))
    @Column(name = "day_of_week")
    private List<Integer> daysOfWeek = new ArrayList<>(); // 1 = Mon, 7 = Sun

    private Integer dayOfMonthStart; // 1..31 for RECURRING_MONTHLY
    private Integer dayOfMonthEnd;   // 1..31 for RECURRING_MONTHLY

    private LocalTime startTime;
    private LocalTime endTime;

    private LocalDate startDate;
    private LocalDate endDate;

    public TemporalCondition() {}

    public boolean isActiveAt(LocalDateTime dt) {
        LocalDate date = dt.toLocalDate();

        // Enforce start/end date bounds whenever present (both one-time and recurring schedules)
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }

        if ("RECURRING_MONTHLY".equals(conditionType)) {
            if (dayOfMonthStart != null && dayOfMonthEnd != null) {
                int dom = date.getDayOfMonth();
                if (dayOfMonthStart <= dayOfMonthEnd) {
                    if (dom < dayOfMonthStart || dom > dayOfMonthEnd) return false;
                } else {
                    // Wrap-around window (e.g., 20th of month through 15th of next month)
                    if (dom < dayOfMonthStart && dom > dayOfMonthEnd) return false;
                }
            }
        } else if ("RECURRING_WEEKLY".equals(conditionType)
                || (daysOfWeek != null && !daysOfWeek.isEmpty())) {
            if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
                int currentDay = dt.getDayOfWeek().getValue();
                if (!daysOfWeek.contains(currentDay)) {
                    return false;
                }
            }
        }

        // Enforce start/end hours whenever present (applies to both recurring AND one-time discounts)
        if (startTime != null && endTime != null) {
            LocalTime time = dt.toLocalTime();
            if (!endTime.isBefore(startTime)) {
                if (time.isBefore(startTime) || time.isAfter(endTime)) return false;
            } else {
                // Overnight window (e.g., 22:00 to 04:00)
                if (time.isBefore(startTime) && time.isAfter(endTime)) return false;
            }
        } else if (startTime != null && dt.toLocalTime().isBefore(startTime)) {
            return false;
        } else if (endTime != null && dt.toLocalTime().isAfter(endTime)) {
            return false;
        }

        return true;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}