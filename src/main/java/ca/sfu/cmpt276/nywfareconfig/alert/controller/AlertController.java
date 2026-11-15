package ca.sfu.cmpt276.nywfareconfig.alert.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ca.sfu.cmpt276.nywfareconfig.alert.dto.AlertForm;
import ca.sfu.cmpt276.nywfareconfig.alert.model.AlertStatus;
import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import ca.sfu.cmpt276.nywfareconfig.alert.model.TriggerType;
import ca.sfu.cmpt276.nywfareconfig.alert.service.AlertLimits;
import ca.sfu.cmpt276.nywfareconfig.alert.service.AlertService;
import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

@Controller
public class AlertController {

    private static final String LIST = "redirect:/fares/alerts";

    private final TenantContext tenantContext;
    private final AlertService alertService;

    public AlertController(TenantContext tenantContext, AlertService alertService) {
        this.tenantContext = tenantContext;
        this.alertService = alertService;
    }

    // Service alerts for the selected tenant, with optional filters (Story 4.1)
    @GetMapping("/fares/alerts")
    public String list(@RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String route,
            Model model) {
        // No tenant picked yet: the page shows the "select a tenant" prompt, like the fare grid
        String tenantCode = tenantCode();
        if (tenantCode == null) {
            return "alerts/alerts";
        }

        model.addAttribute("alerts", alertService.list(tenantCode, search, parseStatus(status), route));
        model.addAttribute("routeOptions", alertService.routeOptionsByTerminal(tenantCode));

        // Keep the filter values so the form shows what was searched
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("route", route);
        return "alerts/alerts";
    }

    // Blank form for a new alert (Story 4.2)
    @GetMapping("/fares/alerts/new")
    public String newForm(Model model) {
        String tenantCode = tenantCode();
        if (tenantCode == null) {
            return LIST;
        }
        return showForm(model, tenantCode, null, new AlertForm(), List.of());
    }

    @GetMapping("/fares/alerts/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        String tenantCode = tenantCode();
        if (tenantCode == null) {
            return LIST;
        }
        Optional<ServiceAlert> found = alertService.get(tenantCode, id);
        if (found.isEmpty()) {
            redirect.addFlashAttribute("error", "That alert no longer exists.");
            return LIST;
        }
        ServiceAlert alert = found.get();
        if (alert.getStatus() == AlertStatus.ARCHIVED) {
            redirect.addFlashAttribute("error", "Archived alerts cannot be edited.");
            return LIST;
        }
        // An active alert that already has a pending draft: edit the draft, not the stale live values
        if (alert.getStatus() == AlertStatus.ACTIVE) {
            Optional<ServiceAlert> draft = alertService.findPendingReplacement(tenantCode, id);
            if (draft.isPresent()) {
                return "redirect:/fares/alerts/" + draft.get().getId() + "/edit";
            }
        }
        return showForm(model, tenantCode, alert, AlertForm.from(alert), List.of());
    }

    // Create (id absent) or update (id present); always saves a DRAFT
    @PostMapping({ "/fares/alerts", "/fares/alerts/{id}" })
    public String save(@PathVariable(required = false) Long id,
            @ModelAttribute("form") AlertForm form,
            @AuthenticationPrincipal AppUserDetails user,
            Model model, RedirectAttributes redirect) {
        String tenantCode = tenantCode();
        if (tenantCode == null) {
            return LIST;
        }
        List<String> errors = alertService.save(tenantCode, id, form, user.getEmail());
        if (!errors.isEmpty()) {
            ServiceAlert alert = id == null ? null : alertService.get(tenantCode, id).orElse(null);
            return showForm(model, tenantCode, alert, form, errors);
        }
        redirect.addFlashAttribute("success", "Alert saved as a draft. It goes live once it is published.");
        return LIST;
    }

    // Drafts only; restricted to CLIENT_SERVICES in SecurityConfig
    @PostMapping("/fares/alerts/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        String tenantCode = tenantCode();
        if (tenantCode != null && alertService.delete(tenantCode, id)) {
            redirect.addFlashAttribute("success", "Draft deleted.");
        } else {
            redirect.addFlashAttribute("error", "Only draft alerts can be deleted.");
        }
        return LIST;
    }

    // Active alerts only; restricted to CLIENT_SERVICES in SecurityConfig
    @PostMapping("/fares/alerts/{id}/archive")
    public String archive(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user,
            RedirectAttributes redirect) {
        String tenantCode = tenantCode();
        if (tenantCode != null && alertService.archive(tenantCode, id, user.getEmail())) {
            redirect.addFlashAttribute("success", "Alert archived. Riders will no longer see it.");
        } else {
            redirect.addFlashAttribute("error", "Only active alerts can be archived.");
        }
        return LIST;
    }

    private String showForm(Model model, String tenantCode, ServiceAlert alert, AlertForm form,
            List<String> errors) {
        Map<String, ?> routeOptions = alertService.routeOptionsByTerminal(tenantCode);
        model.addAttribute("alert", alert);
        model.addAttribute("form", form);
        model.addAttribute("errors", errors);
        model.addAttribute("routeOptions", routeOptions);
        model.addAttribute("triggerTypes", TriggerType.values());
        model.addAttribute("messageMax", AlertLimits.MESSAGE_MAX_LENGTH);
        model.addAttribute("labelMax", AlertLimits.LABEL_MAX_LENGTH);
        return "alerts/form";
    }

    private String tenantCode() {
        return tenantContext.getSelectedTenant() == null ? null : tenantContext.getSelectedTenant().getCode();
    }

    // Unknown values from the URL just mean "all statuses"
    private static AlertStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return AlertStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
