package ca.sfu.cmpt276.nywfareconfig.tenant.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

@Service 
public class TenantService {
    /**
     * Returns the list of demo tenants supported by the platform.
     */
    public List<Tenant> getAvailableTenants() {
        List<Tenant> tenantList = new ArrayList<>();
        tenantList.add(new Tenant("NYW_SANDBOX", "NY Waterway", GridMode.OD, true));
        tenantList.add(new Tenant("METRA", "Metra Rail", GridMode.ZONAL, true));
        return tenantList;
    }

    /**
     * Resolves a Tenant instance by its natural tenant code.
     */
    public Tenant getTenantByCode(String tenantCode) {
        if (tenantCode == null) {
            return null;
        }
        return switch (tenantCode.toUpperCase()) {
            case "NYW_SANDBOX", "NYW" -> new Tenant("NYW_SANDBOX", "NY Waterway", GridMode.OD, true);
            case "METRA" -> new Tenant("METRA", "Metra Rail", GridMode.ZONAL, true);
            default -> null;
        };
    }
}
