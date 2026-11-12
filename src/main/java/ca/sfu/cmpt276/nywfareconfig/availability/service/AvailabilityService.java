package ca.sfu.cmpt276.nywfareconfig.availability.service;

import ca.sfu.cmpt276.nywfareconfig.availability.dto.AvailabilityDraftDTO;
import ca.sfu.cmpt276.nywfareconfig.availability.dto.ProductAvailabilityView;
import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.model.TemporalCondition;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.StagingService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class AvailabilityService {

    private final StagingService stagingService;

    // Default products for prototype fallback
    private static final List<String> DEFAULT_PRODUCTS = List.of("ADULT", "SENIOR", "YOUTH", "CHILD");

    public AvailabilityService(StagingService stagingService) {
        this.stagingService = stagingService;
    }

    public ProductAvailabilityView getProductAvailability(String siteCode) {
        Map<String, Boolean> toggles = new HashMap<>();
        for (String prod : DEFAULT_PRODUCTS) {
            toggles.put(prod, true);
        }
        return new ProductAvailabilityView(siteCode, DEFAULT_PRODUCTS, toggles);
    }

    public void stageAvailabilityDrafts(String tenantCode, List<AvailabilityDraftDTO> drafts) {
        for (AvailabilityDraftDTO draft : drafts) {
            AvailabilityRule rule = new AvailabilityRule(
                tenantCode,
                draft.getSiteCode(),
                draft.getProductCode(),
                draft.isAvailable()
            );

            if (draft.getConditionType() != null && !draft.getConditionType().isEmpty()) {
                TemporalCondition condition = new TemporalCondition();
                condition.setName(draft.getProductCode() + " Availability Rule");
                condition.setConditionType(draft.getConditionType());
                condition.setDaysOfWeek(draft.getDaysOfWeek());

                if (draft.getStartTime() != null && !draft.getStartTime().isEmpty()) {
                    condition.setStartTime(LocalTime.parse(draft.getStartTime()));
                }
                if (draft.getEndTime() != null && !draft.getEndTime().isEmpty()) {
                    condition.setEndTime(LocalTime.parse(draft.getEndTime()));
                }
                if (draft.getStartDate() != null && !draft.getStartDate().isEmpty()) {
                    condition.setStartDate(LocalDate.parse(draft.getStartDate()));
                }
                if (draft.getEndDate() != null && !draft.getEndDate().isEmpty()) {
                    condition.setEndDate(LocalDate.parse(draft.getEndDate()));
                }

                rule.setTemporalCondition(condition);
            }

            // Persist to shared staging PostgreSQL table with change_type = 'AVAILABILITY_TOGGLE'
            stagingService.stageAvailabilityChange(tenantCode, rule);
        }
    }
}