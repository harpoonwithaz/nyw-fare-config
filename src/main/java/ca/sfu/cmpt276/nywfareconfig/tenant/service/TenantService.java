package ca.sfu.cmpt276.nywfareconfig.tenant.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.tenant.config.TenantProperties;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;

@Service
public class TenantService {

    @Value("${app.tenants.excluded-codes:}")
    private String excludedCodes = "";

    private final CaracalApiClient caracalApiClient;
    private final TenantProperties tenantProperties;

    @Autowired
    public TenantService(
            CaracalApiClient caracalApiClient,
            TenantProperties tenantProperties) {

        this.caracalApiClient = caracalApiClient;
        this.tenantProperties = tenantProperties != null
                ? tenantProperties
                : TenantProperties.defaults();
    }

    public TenantService(
            CaracalApiClient caracalApiClient,
            TenantContext tenantContext) {

        this(caracalApiClient, TenantProperties.defaults());
    }

    /**
     * Returns supported O-D and Zonal tenants.
     */
    public List<Tenant> getAvailableTenants() {

        List<CaracalCatalogDtos.TenantSummary> tenantsSummaryList =
                caracalApiClient.fetchTenants();

        if (tenantsSummaryList == null) {
            return List.of();
        }

        List<String> excluded = Arrays.stream(excludedCodes.split(","))
                .map(String::trim)
                .toList();

        List<Tenant> tenantList = new ArrayList<>();

        for (CaracalCatalogDtos.TenantSummary summary : tenantsSummaryList) {

            if (summary.code() == null || summary.code().isBlank()) {
                continue;
            }

            if (!tenantProperties.isSupported(summary.code())) {
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
     * Resolves a supported Tenant instance by its natural tenant code.
     */
    public Tenant getTenantByCode(String tenantCode) {

        if (tenantCode == null || tenantCode.isBlank()) {
            return null;
        }

        Optional<GridMode> configuredMode =
                tenantProperties.resolveGridMode(tenantCode);

        if (configuredMode.isEmpty()) {
            return null;
        }

        CaracalCatalogDtos.TenantDetail tenantDetails =
                caracalApiClient.fetchTenant(tenantCode.trim());

        if (tenantDetails == null) {
            return null;
        }

        GridMode gridMode =
                tenantProperties.resolveGridMode(tenantDetails.code())
                        .orElse(configuredMode.get());

        return new Tenant(
                tenantDetails.code(),
                tenantDetails.name(),
                tenantDetails.defaultCurrency(),
                tenantDetails.defaultLocale(),
                gridMode,
                true,
                tenantDetails.supportedLocales());
    }
}