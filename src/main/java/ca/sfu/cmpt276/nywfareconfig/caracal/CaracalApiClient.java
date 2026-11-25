package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalMutationDtos;

// Decouples Epics 2–5 from Caracal’s HTTP routing, threading, 
// and raw JSON schemas by exposing normalized domain methods.
public interface CaracalApiClient {
    
    // Catalog Discovery
    public List<CaracalCatalogDtos.TenantSummary> fetchTenants();
    public CaracalCatalogDtos.TenantDetail fetchTenant(String tenantCode);
    public List<CaracalCatalogDtos.SiteSummary> fetchSites(String tenantCode);
    public List<CaracalCatalogDtos.ProductSummary> fetchProducts(String tenantCode);
    public List<CaracalCatalogDtos.RateTableSummary> fetchRateTables(String tenantCode);
    public Optional<List<CaracalCatalogDtos.RateAssignmentSummary>> fetchRateTable(CaracalCatalogDtos.RateTableGetRequest requestPayload);

    // Matrix & Availability Hydration
    public CaracalSimulationDtos.PriceSimulationResult simulatePrice(CaracalSimulationDtos.PriceSimulateRequest requestPayload);
    public List<CaracalSimulationDtos.SimulatedProductAvailability> simulateAvailability(
            String tenantCode, String siteCode, Map<String, String> dimensions);

    // Availability & Rule Mutations (Epic 3)
    public String createWhenCondition(CaracalMutationDtos.WhenConditionCreateRequest requestPayload);
    public void updateRateAssignment(CaracalMutationDtos.RateAssignmentUpdateRequest requestPayload);
}