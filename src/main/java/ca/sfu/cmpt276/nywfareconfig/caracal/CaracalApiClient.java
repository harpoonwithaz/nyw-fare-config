package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.math.BigDecimal;
import java.util.List;

import tools.jackson.databind.JsonNode;

import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos;

// Decouples Epics 2–5 from Caracal's HTTP routing and raw JSON schemas.
public interface CaracalApiClient {

    List<CaracalCatalogDtos.SiteSummary> fetchSites(String tenantCode);

    List<CaracalCatalogDtos.ProductSummary> fetchProducts(String tenantCode);

    List<CaracalCatalogDtos.TenantSummary> fetchTenants();

    CaracalCatalogDtos.TenantDetail fetchTenant(String tenantCode);

    // Publication and audit history
    JsonNode fetchPublicationHistory(String tenantCode);

    JsonNode fetchAuditHistory(String tenantCode, int limit);

    //Active baseline pricing
    BigDecimal fetchBaselinePrice(
            String tenantCode,
            String originCode,
            String destCode,
            String productCode
    );

    // Existing validation pricing method (to be migrated)
    // BigDecimal fetchSimulatedPrice(
    //         String tenantCode,
    //         String productCode,
    //         String originCode,
    //         String destCode,
    //         String at
    // );

    //Shared Caracal price simulation method
    CaracalSimulationDtos.PriceSimulationResult simulatePrice(
            CaracalSimulationDtos.PriceSimulateRequest requestPayload
    );
}
