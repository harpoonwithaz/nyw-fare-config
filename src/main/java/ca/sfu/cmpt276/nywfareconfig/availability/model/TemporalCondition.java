package ca.sfu.cmpt276.nywfareconfig.availability.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "temporal_conditions")
public class TemporalCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String conditionType;

    @ElementCollection
    @CollectionTable(name = "temporal_condition_days", joinColumns = @JoinColumn(name = "condition_id"))
    @Column(name = "day_of_week")
    private List<Integer> daysOfWeek; // 1 = Mon, 7 = Sun

    private LocalTime startTime;
    private LocalTime endTime;

    private LocalDate startDate;
    private LocalDate endDate;

    public TemporalCondition() {}

    public boolean isActiveAt(LocalDateTime dt) {
        if ("TEMPORARY_PROMO".equals(conditionType)) {
            LocalDate date = dt.toLocalDate();
            if (startDate != null && date.isBefore(startDate)) return false;
            if (endDate != null && date.isAfter(endDate)) return false;
        }

        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            int currentDay = dt.getDayOfWeek().getValue();
            if (!daysOfWeek.contains(currentDay)) return false;
        }

        if (startTime != null && endTime != null) {
            LocalTime time = dt.toLocalTime();
            if (time.isBefore(startTime) || time.isAfter(endTime)) return false;
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

    public List<Integer> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<Integer> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}