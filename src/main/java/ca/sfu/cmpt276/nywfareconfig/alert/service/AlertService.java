package ca.sfu.cmpt276.nywfareconfig.alert.service;

import ca.sfu.cmpt276.nywfareconfig.alert.dto.AlertForm;
import ca.sfu.cmpt276.nywfareconfig.alert.dto.RouteOption;
import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertStatus;
import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.RouteScheduleRepository;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.ServiceAlertRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AlertService {

    private final ServiceAlertRepository alerts;
    private final RouteScheduleRepository schedules;

    public AlertService(ServiceAlertRepository alerts, RouteScheduleRepository schedules) {
        this.alerts = alerts;
        this.schedules = schedules;
    }

    public List<ServiceAlert> list(String tenantId, String search, AlertStatus status, String routeKey) {
        return List.of();
    }

    public Optional<ServiceAlert> get(String tenantId, Long id) {
        return Optional.empty();
    }

    public List<String> save(String tenantId, Long id, AlertForm form, String actor) {
        return List.of("Saving alerts is not implemented yet.");
    }

    public boolean delete(String tenantId, Long id) {
        return false;
    }

    public Map<String, List<RouteOption>> routeOptionsByTerminal(String tenantId) {
        Map<String, List<RouteOption>> options = new LinkedHashMap<>();
        options.put("PIER11", List.of(new RouteOption("PIER11", "HOB14ST", "PIER11 to HOB14ST")));
        options.put("HOB14ST", List.of(new RouteOption("HOB14ST", "PIER11", "HOB14ST to PIER11")));
        return options;
    }
}