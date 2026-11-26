package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import tools.jackson.databind.JsonNode;

import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos;

// Decouples Epics 2–5 from Caracal's HTTP routing and raw JSON schemas.
public interface CaracalApiClient {

    // Catalog Discovery
    List<CaracalCatalogDtos.TenantSummary> fetchTenants();

    CaracalCatalogDtos.TenantDetail fetchTenant(String tenantCode);

    List<CaracalCatalogDtos.SiteSummary> fetchSites(String tenantCode);

    List<CaracalCatalogDtos.ProductSummary> fetchProducts(String tenantCode);

    Optional<List<CaracalCatalogDtos.RateAssignmentSummary>> fetchRateTable(
            CaracalCatalogDtos.RateTableGetRequest requestPayload);

    // Publication and audit history
    JsonNode fetchPublicationHistory(String tenantCode);

    JsonNode fetchAuditHistory(String tenantCode, int limit);

// Publish the validated sandbox configuration to Caracal
JsonNode publishCatalog(String tenantCode);

// Update an origin-destination fare in Caracal.
JsonNode updateOdMatrix(
        String tenantCode,
        String priceRuleSetName,
        String originCode,
        String destCode,
        BigDecimal price
);

// Update ticket availability at a terminal.
JsonNode updateRateAssignment(
        String tenantCode,
        String assignmentId,
        String cellType
);

// Active baseline pricing
    BigDecimal fetchBaselinePrice(
            String tenantCode,
            String originCode,
            String destCode,
            String productCode
    );

    // Shared Caracal price simulation method
    CaracalSimulationDtos.PriceSimulationResult simulatePrice(
            CaracalSimulationDtos.PriceSimulateRequest requestPayload
    );
}
