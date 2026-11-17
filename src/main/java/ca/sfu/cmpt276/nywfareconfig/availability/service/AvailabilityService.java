package ca.sfu.cmpt276.nywfareconfig.availability.service;

import ca.sfu.cmpt276.nywfareconfig.availability.dto.AvailabilityDraftDTO;
import ca.sfu.cmpt276.nywfareconfig.availability.dto.ProductAvailabilityView;
import ca.sfu.cmpt276.nywfareconfig.availability.model.AvailabilityRule;
import ca.sfu.cmpt276.nywfareconfig.availability.model.TemporalCondition;
import ca.sfu.cmpt276.nywfareconfig.availability.repository.AvailabilityRuleRepository;
import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class AvailabilityService {

    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final ActorService actorService;

    public static final List<String> CANONICAL_PRODUCTS = List.of(
        "ADULT_ONEWAY",
        "CHILD_ONEWAY",
        "SENIOR_ONEWAY",
        "FULL_10TRIP",
        "FULL_MONTHLY"
    );

    public AvailabilityService(AvailabilityRuleRepository availabilityRuleRepository, ActorService actorService) {
        this.availabilityRuleRepository = availabilityRuleRepository;
        this.actorService = actorService;
    }

    public ProductAvailabilityView getProductAvailability(String tenantCode, String siteCode) {
        Map<String, Boolean> toggles = new LinkedHashMap<>();
        Map<String, TemporalCondition> conditions = new HashMap<>();

        for (String prod : CANONICAL_PRODUCTS) {
            toggles.put(prod, true);
        }

        if (tenantCode != null && !tenantCode.isBlank()) {
            List<AvailabilityRule> savedRules = availabilityRuleRepository.findByTenantCodeAndSiteCode(tenantCode, siteCode);
            for (AvailabilityRule rule : savedRules) {
                toggles.put(rule.getProductCode(), rule.isAvailable());
                if (rule.getTemporalCondition() != null) {
                    conditions.put(rule.getProductCode(), rule.getTemporalCondition());
                }
            }
        }

        return new ProductAvailabilityView(siteCode, CANONICAL_PRODUCTS, toggles, conditions);
    }

    @Transactional
    public void stageAvailabilityDrafts(String tenantCode, List<AvailabilityDraftDTO> drafts) {
        String actor = actorService.currentActorHeader();

        for (AvailabilityDraftDTO draft : drafts) {
            AvailabilityRule rule = availabilityRuleRepository
                .findByTenantCodeAndSiteCodeAndProductCode(tenantCode, draft.getSiteCode(), draft.getProductCode())
                .orElseGet(() -> new AvailabilityRule(
                    tenantCode,
                    draft.getSiteCode(),
                    draft.getProductCode(),
                    draft.isAvailable()
                ));

            rule.setAvailable(draft.isAvailable());
            rule.setUpdatedBy(actor);
            rule.setLastUpdated(LocalDateTime.now());

            if (draft.getConditionType() != null && !draft.getConditionType().isEmpty()) {
                TemporalCondition condition = rule.getTemporalCondition() != null
                    ? rule.getTemporalCondition()
                    : new TemporalCondition();

                condition.setName(draft.getProductCode() + " Availability Rule");
                condition.setConditionType(draft.getConditionType());
                condition.setDaysOfWeek(draft.getDaysOfWeek());

                condition.setStartTime(draft.getStartTime() != null && !draft.getStartTime().isEmpty()
                    ? LocalTime.parse(draft.getStartTime()) : null);
                condition.setEndTime(draft.getEndTime() != null && !draft.getEndTime().isEmpty()
                    ? LocalTime.parse(draft.getEndTime()) : null);
                condition.setStartDate(draft.getStartDate() != null && !draft.getStartDate().isEmpty()
                    ? LocalDate.parse(draft.getStartDate()) : null);
                condition.setEndDate(draft.getEndDate() != null && !draft.getEndDate().isEmpty()
                    ? LocalDate.parse(draft.getEndDate()) : null);

                rule.setTemporalCondition(condition);
            }

            availabilityRuleRepository.save(rule);
        }
    }
}