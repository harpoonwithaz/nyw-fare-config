package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.util.List;

import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;

// Decouples Epics 2–5 from Caracal’s HTTP routing, threading, 
// and raw JSON schemas by exposing normalized domain methods.
public interface CaracalApiClient {
    
    // Catalog Discovery
    public List<CaracalCatalogDtos.TenantSummary> fetchTenants();
    public CaracalCatalogDtos.TenantDetail fetchTenant(String tenantCode);
}