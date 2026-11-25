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

    @Column(nullable = false)
    private String name;

    @Column(name = "discount_label")
    private String discountLabel;

    @Column(name = "condition_type", nullable = false)
    private String conditionType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "temporal_condition_days",
        joinColumns = @JoinColumn(name = "condition_id")
    )
    @Column(name = "day_of_week")
    private List<Integer> daysOfWeek = new ArrayList<>();

    @Column(name = "day_of_month_start")
    private Integer dayOfMonthStart;

    @Column(name = "day_of_month_end")
    private Integer dayOfMonthEnd;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    public TemporalCondition() {
    }

    public boolean isActiveAt(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }

        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();

        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }

        if ("RECURRING_MONTHLY".equals(conditionType)) {
            if (dayOfMonthStart != null && dayOfMonthEnd != null) {
                int dom = date.getDayOfMonth();
                boolean inRange = (dayOfMonthStart <= dayOfMonthEnd)
                        ? (dom >= dayOfMonthStart && dom <= dayOfMonthEnd)
                        : (dom >= dayOfMonthStart || dom <= dayOfMonthEnd);
                if (!inRange) {
                    return false;
                }
            }
        } else if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            int isoDay = date.getDayOfWeek().getValue();
            if (!daysOfWeek.contains(isoDay)) {
                return false;
            }
        }

        if (startTime != null && endTime != null) {
            if (!startTime.isAfter(endTime)) {
                return !time.isBefore(startTime) && !time.isAfter(endTime);
            } else {
                return !time.isBefore(startTime) || !time.isAfter(endTime);
            }
        } else if (startTime != null) {
            return !time.isBefore(startTime);
        } else if (endTime != null) {
            return !time.isAfter(endTime);
        }

        return true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDiscountLabel() {
        return discountLabel;
    }

    public void setDiscountLabel(String discountLabel) {
        this.discountLabel = discountLabel;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    public List<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<Integer> daysOfWeek) {
        this.daysOfWeek = daysOfWeek != null ? daysOfWeek : new ArrayList<>();
    }

    public Integer getDayOfMonthStart() {
        return dayOfMonthStart;
    }

    public void setDayOfMonthStart(Integer dayOfMonthStart) {
        this.dayOfMonthStart = dayOfMonthStart;
    }

    public Integer getDayOfMonthEnd() {
        return dayOfMonthEnd;
    }

    public void setDayOfMonthEnd(Integer dayOfMonthEnd) {
        this.dayOfMonthEnd = dayOfMonthEnd;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}