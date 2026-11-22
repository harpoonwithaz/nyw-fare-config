package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.util.List;
import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;

import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;

// Decouples Epics 2–5 from Caracal’s HTTP routing, threading, 
// and raw JSON schemas by exposing normalized domain methods.
public interface CaracalApiClient {
    
    // Catalog Discovery
    public List<CaracalCatalogDtos.TenantSummary> fetchTenants();
    public CaracalCatalogDtos.TenantDetail fetchTenant(String tenantCode);
    public List<CaracalCatalogDtos.SiteSummary> fetchSites(String tenantCode);
    public List<CaracalCatalogDtos.ProductSummary> fetchProducts(String tenantCode);

    //Publication and audit history.
    JsonNode fetchPublicationHistory(String tenantCode);
    JsonNode fetchAuditHistory(String tenantCode, int limit);

    //comparison with the baseline.
    BigDecimal fetchBaselinePrice(String tenantCode, String originCode, String destCode, String productCode);

    // Sandbox pricing for validation against the NYW reference sheet.
    // Uses price-simulate, which can consider DRAFT rate tables.
    // Returns null when the ticket cannot be sold on the route.
    BigDecimal fetchSimulatedPrice(
            String tenantCode,
            String productCode,
            String originCode,
            String destCode,
            String at
    );
}