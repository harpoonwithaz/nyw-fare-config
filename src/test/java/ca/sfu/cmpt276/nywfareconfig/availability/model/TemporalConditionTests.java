package ca.sfu.cmpt276.nywfareconfig.availability.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TemporalConditionTests {

    @Test
    void recurringWeeklyRespectsDaysDatesAndHours() {
        TemporalCondition cond = new TemporalCondition();
        cond.setConditionType("RECURRING_WEEKLY");
        cond.setDaysOfWeek(List.of(1, 2, 3, 4, 5)); // Mon-Fri
        cond.setStartDate(LocalDate.of(2026, 11, 1));
        cond.setEndDate(LocalDate.of(2026, 11, 30));
        cond.setStartTime(LocalTime.of(6, 0));
        cond.setEndTime(LocalTime.of(22, 0));

        // Monday Nov 2, 2026 at 10:00 -> active
        assertThat(cond.isActiveAt(LocalDateTime.of(2026, 11, 2, 10, 0))).isTrue();
        // Saturday Nov 7, 2026 at 10:00 -> inactive (weekend)
        assertThat(cond.isActiveAt(LocalDateTime.of(2026, 11, 7, 10, 0))).isFalse();
        // Monday Nov 2, 2026 at 23:00 -> inactive (outside hours)
        assertThat(cond.isActiveAt(LocalDateTime.of(2026, 11, 2, 23, 0))).isFalse();
        // Monday Dec 7, 2026 at 10:00 -> inactive (after endDate)
        assertThat(cond.isActiveAt(LocalDateTime.of(2026, 12, 7, 10, 0))).isFalse();
    }

    @Test
    void oneTimeDiscountRespectsStartEndDatesAndHours() {
        TemporalCondition promo = new TemporalCondition();
        promo.setConditionType("TEMPORARY_PROMO");
        promo.setStartDate(LocalDate.of(2026, 12, 24));
        promo.setEndDate(LocalDate.of(2026, 12, 26));
        promo.setStartTime(LocalTime.of(9, 0));
        promo.setEndTime(LocalTime.of(17, 0));

        assertThat(promo.isActiveAt(LocalDateTime.of(2026, 12, 25, 12, 0))).isTrue();
        assertThat(promo.isActiveAt(LocalDateTime.of(2026, 12, 25, 18, 30))).isFalse();
        assertThat(promo.isActiveAt(LocalDateTime.of(2026, 12, 27, 12, 0))).isFalse();
    }

    @Test
    void recurringMonthlyWrapAroundWindowEvaluatesCorrectly() {
        TemporalCondition monthly = new TemporalCondition();
        monthly.setConditionType("RECURRING_MONTHLY");
        monthly.setDayOfMonthStart(20);
        monthly.setDayOfMonthEnd(15);
        monthly.setStartTime(LocalTime.of(0, 0));
        monthly.setEndTime(LocalTime.of(23, 59));

        assertThat(monthly.isActiveAt(LocalDateTime.of(2026, 11, 25, 12, 0))).isTrue();
        assertThat(monthly.isActiveAt(LocalDateTime.of(2026, 11, 10, 12, 0))).isTrue();
        assertThat(monthly.isActiveAt(LocalDateTime.of(2026, 11, 18, 12, 0))).isFalse();
    }
}
