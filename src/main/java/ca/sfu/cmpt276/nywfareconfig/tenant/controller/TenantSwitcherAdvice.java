package ca.sfu.cmpt276.nywfareconfig.tenant.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ca.sfu.cmpt276.nywfareconfig.alert.controller.AlertController;
import ca.sfu.cmpt276.nywfareconfig.availability.controller.AvailabilityController;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.fare.controller.FareController;
import ca.sfu.cmpt276.nywfareconfig.publishing.controller.PublishingController;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.TenantService;

@ControllerAdvice(assignableTypes = {
        FareController.class,
        AvailabilityController.class,
        AlertController.class,
        PublishingController.class
})
public class TenantSwitcherAdvice {

    private static final Logger log = LoggerFactory.getLogger(TenantSwitcherAdvice.class);

    private final TenantService tenantService;

    public TenantSwitcherAdvice(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @ModelAttribute("availableTenants")
    public List<Tenant> getAvailableTenants(Authentication authentication, Model model) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return List.of();
        }

        try {
            return tenantService.getAvailableTenants();
        } catch (CaracalApiException ex) {
            log.warn("Unable to load available tenants from Caracal API: {}", ex.getMessage(), ex);
            model.addAttribute("caracalTenantErrorMessage", ex.getMessage());
            return List.of();
        }
    }
}