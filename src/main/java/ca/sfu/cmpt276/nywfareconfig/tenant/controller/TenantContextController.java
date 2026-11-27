package ca.sfu.cmpt276.nywfareconfig.tenant.controller;

import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;

import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.*;

@Controller
@RequestMapping("/context")
public class TenantContextController {

    private static final String DEFAULT_RETURN_URL = "/fares";
    private static final Logger log = LoggerFactory.getLogger(TenantContextController.class);

    private static final List<String> ALLOWED_PATH_PREFIXES = List.of(
            "/fares",
            "/availability",
            "/fares/alerts",
            "/review",
            "/publishing",
            "/history",
            "/dashboard",
            "/admin",
            "/account");

    private final TenantContext tenantContext;
    private final TenantService tenantService;

    public TenantContextController(TenantContext tenantContext, TenantService tenantService) {
        this.tenantContext = tenantContext;
        this.tenantService = tenantService;
    }

    @PostMapping("/tenant")
    public String switchTenant(
            @RequestParam(name = "tenant", required = false) String tenantCode,
            @RequestParam(name = "returnUrl", required = false) String returnUrl,
            RedirectAttributes redirect) {

        if (tenantCode == null || tenantCode.isBlank()) {
            tenantContext.setSelectedTenant(null);
            tenantContext.setSecondaryFilter(null);
            tenantContext.setSelectedDate(null);
        } else {
            boolean isNewTenant = tenantContext.getSelectedTenant() == null ||
                    !tenantContext.getSelectedTenant().getCode().equalsIgnoreCase(tenantCode);

            if (isNewTenant) {
                Tenant resolvedTenant;
                try {
                    resolvedTenant = tenantService.getTenantByCode(tenantCode);
                } catch (CaracalApiException ex) {
                    // Caracal down or rejected tenant-get: keep the current tenant and say why in the navbar banner
                    log.warn("Could not switch to tenant {}", tenantCode, ex);
                    redirect.addFlashAttribute("caracalTenantErrorMessage",
                            "Could not switch to " + tenantCode + ". " + ex.getMessage());
                    return "redirect:" + resolveSafeReturnUrl(returnUrl);
                }
                tenantContext.setSelectedTenant(resolvedTenant);

                // Clear secondary filter and selectedDate state when switching tenants
                tenantContext.setSecondaryFilter(null);
                tenantContext.setSelectedDate(null);
            }
        }

        return "redirect:" + resolveSafeReturnUrl(returnUrl);
    }

    private String resolveSafeReturnUrl(String returnUrl) {
        if (returnUrl == null || returnUrl.isBlank()) {
            return DEFAULT_RETURN_URL;
        }

        // Reject backslashes, protocol-relative prefixes, and percent-encoded authority
        // tricks
        if (returnUrl.contains("\\") || returnUrl.startsWith("//") || returnUrl.contains("%")) {
            return DEFAULT_RETURN_URL;
        }

        try {
            URI uri = new URI(returnUrl);
            if (uri.isAbsolute()
                    || uri.getScheme() != null
                    || uri.getRawAuthority() != null
                    || uri.getAuthority() != null
                    || uri.getHost() != null
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) {
                return DEFAULT_RETURN_URL;
            }

            String path = uri.normalize().getPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//") || path.contains("\\")) {
                return DEFAULT_RETURN_URL;
            }

            boolean isAllowed = ALLOWED_PATH_PREFIXES.stream()
                    .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));

            return isAllowed ? path : DEFAULT_RETURN_URL;
        } catch (URISyntaxException ex) {
            return DEFAULT_RETURN_URL;
        }
    }
}