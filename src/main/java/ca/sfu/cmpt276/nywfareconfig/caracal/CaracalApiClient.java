package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.util.List;
import java.util.Optional;

import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos.*;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.*;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalZonalDtos.*;

// Decouples Epics 2–5 from Caracal’s HTTP routing, threading, 
// and raw JSON schemas by exposing normalized domain methods.
public interface CaracalApiClient {
    
    // Catalog Discovery
    public List<TenantSummary> fetchTenants();
    public TenantDetail fetchTenant(String tenantCode);
    public List<SiteSummary> fetchSites(String tenantCode);
    public List<ProductSummary> fetchProducts(String tenantCode);
    public Optional<List<RateAssignmentSummary>> fetchRateTable(RateTableGetRequest requestPayload);

    // Zonal discovery
    public List<DimensionValueSummary> fetchZones(String tenantCode);
    public List<DimensionValueSummary> fetchFareClasses(String tenantCode);
    public List<SiteDimensionBinding> fetchStationZoneBindings(String tenantCode);

    // Matrix & Availability Hydration
    public PriceSimulationResult simulatePrice(PriceSimulateRequest requestPayload);
}