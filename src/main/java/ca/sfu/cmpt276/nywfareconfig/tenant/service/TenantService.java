package ca.sfu.cmpt276.nywfareconfig.tenant.service;

import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

@Service
public class TenantService {
    @Value("${app.tenants.excluded-codes:}")
    private String excludedCodes;
    
    private final TenantContext tenantContext;
    private final CaracalApiClient caracalApiClient;

    public TenantService(CaracalApiClient caracalApiClient, TenantContext tenantContext) {
        this.caracalApiClient = caracalApiClient;
        this.tenantContext = tenantContext;
    }

    /**
     * Returns the list of demo tenants supported by the platform.
     */
    public List<Tenant> getAvailableTenants() {
    List<CaracalCatalogDtos.TenantSummary> tenantsSummaryList =
            caracalApiClient.fetchTenants();

    if (tenantsSummaryList == null) {
        return List.of();
    }

    List<String> excluded = java.util.Arrays.stream(excludedCodes.split(","))
            .map(String::trim)
            .toList();

    List<Tenant> tenantList = new ArrayList<>();

    for (CaracalCatalogDtos.TenantSummary summary : tenantsSummaryList) {
        if (summary.code() == null || summary.code().isBlank()) {
            continue;
        }

        if (excluded.contains(summary.code())) {
            continue;
        }

        tenantList.add(new Tenant(summary.code(), summary.name()));
    }

    return tenantList;
}

    /**
     * Resolves a Tenant instance by its natural tenant code.
     */
    public Tenant getTenantByCode(String tenantCode) {
        // TODO: Call GET /functions/site-list?tenantCode=<tenant>
        if (tenantCode == null) {
            return null;
        }

        CaracalCatalogDtos.TenantDetail tenantDetails = caracalApiClient.fetchTenant(tenantCode);

        switch (tenantDetails.code()) {
            // HARD CODED FOR NOW
            case "METRA":
                return new Tenant(
                        tenantDetails.code(),
                        tenantDetails.name(),
                        tenantDetails.defaultCurrency(),
                        tenantDetails.defaultLocale(),
                        GridMode.ZONAL,
                        true,
                        tenantDetails.supportedLocales());
            default:
                return new Tenant(
                        tenantDetails.code(),
                        tenantDetails.name(),
                        tenantDetails.defaultCurrency(),
                        tenantDetails.defaultLocale(),
                        GridMode.OD,
                        true,
                        tenantDetails.supportedLocales());
        }
    }
}
