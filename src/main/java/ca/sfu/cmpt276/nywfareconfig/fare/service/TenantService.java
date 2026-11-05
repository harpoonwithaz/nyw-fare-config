package ca.sfu.cmpt276.nywfareconfig.fare.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import ca.sfu.cmpt276.nywfareconfig.fare.model.*;
import jakarta.transaction.Transactional;

@Service 
// @Transactional(readOnly = true) // only if querying a database 
public class TenantService {
    TenantService() {}

    public List<Tenant> getTenantsList() {
        List<Tenant> tenantList = new ArrayList<Tenant>();

        tenantList.add(new Tenant("NYW_SANDBOX", "NY Waterway", GridMode.OD, false));
        tenantList.add(new Tenant("METRA", "Metra", GridMode.ZONAL, false));

        return tenantList;
    }

    public Tenant getTenantById(String tenant) {
        switch (tenant) {
            case "NYW_SANDBOX":
                return new Tenant("NYW_SANDBOX", "NY Waterway", GridMode.OD, true);
            case "METRA":
                return new Tenant("METRA", "Metra", GridMode.ZONAL, false);
            default:
                return null;
        }
    }
}
