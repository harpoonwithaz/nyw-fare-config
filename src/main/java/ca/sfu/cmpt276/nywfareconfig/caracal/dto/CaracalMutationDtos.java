package ca.sfu.cmpt276.nywfareconfig.caracal.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

public class CaracalMutationDtos {

    // POST /functions/rate-assignment-update
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record RateAssignmentUpdateRequest(
            String tenantCode,
            String assignmentId,
            String cellType,
            Integer priority,
            Integer sortOrder,
            String rateScheduleName,
            String priceRuleSetName,
            String channelConditionName) {
    }

    // POST /functions/when-condition-create
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record WhenConditionCreateRequest(
            String tenantCode,
            String name,
            String conditionType,
            List<String> daysOfWeek,
            String startTime,
            String endTime,
            String dateRangeStart,
            String dateRangeEnd,
            Integer seasonMonthStart,
            Integer seasonMonthEnd,
            Integer dayOfMonthStart,
            Integer dayOfMonthEnd) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CaracalIdResponse(String id) {
    }
}