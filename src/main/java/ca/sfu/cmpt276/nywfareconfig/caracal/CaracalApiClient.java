package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.util.List;
import java.util.Map;

import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;

// Decouples Epics 2–5 from Caracal’s HTTP routing, threading,
// and raw JSON schemas by exposing normalized domain methods.
public interface CaracalApiClient {

    // Catalog Discovery
    List<CaracalCatalogDtos.TenantSummary> fetchTenants();
    CaracalCatalogDtos.TenantDetail fetchTenant(String tenantCode);
    List<CaracalCatalogDtos.SiteSummary> fetchSites(String tenantCode);
    List<CaracalCatalogDtos.ProductSummary> fetchProducts(String tenantCode);

    // Epic 3: Rate Tables & Live Availability Resolution
    List<CaracalCatalogDtos.RateTableSummary> fetchRateTables(String tenantCode);
    CaracalCatalogDtos.RateTableGetResponse fetchRateTable(String tenantCode, String rateTableName);
    List<CaracalCatalogDtos.SimulatedProductAvailability> simulateAvailability(
            String tenantCode,
            String siteCode,
            Map<String, String> dimensions);

    // Epic 3: Authoring Mutations (Story 3.4)
    String updateRateAssignment(CaracalCatalogDtos.RateAssignmentUpdateRequest request, String actorEmail);
    String createWhenCondition(CaracalCatalogDtos.WhenConditionCreateRequest request, String actorEmail);
    String createRateAssignment(CaracalCatalogDtos.RateAssignmentCreateRequest request, String actorEmail);
}