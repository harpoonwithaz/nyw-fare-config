**Yes.** `metra.json` contains all three pieces of metadata that Caracal's read endpoints (`site-list`, `site-get`, `catalog-get`) do not expose:

* **Station-to-zone mappings (`sites[]`):** Every station entry includes `siteCode`, `zone`, and `dimensionValues.ORIGIN_ZONE` (e.g., `ST013` $\rightarrow$ `A`, `ST051` $\rightarrow$ `A`, `ST703` $\rightarrow$ `B`).


* **Ordered zone list (`dimensions[]` where `key == "ORIGIN_ZONE"` / `"DEST_ZONE"`):** Ten zones (`A`–`J`) with display names (`Zone A`–`Zone J`).


* **Fare classes (`dimensions[]` where `key == "FARE_CLASS"`):** `FULL` (`Full`), `REDUCED` (`Reduced — senior 65+, disabled, military, student`), and `SCPP` (`SCPP`).



Continue pulling tenants, live station lists/names/timezones, active products, and live fares from Caracal's HTTP endpoints—only extract the three items above from `metra.json`.

## DTOs for the Swappable Wrapper

Keep **public Caracal DTOs** (returned by `CaracalApiClient`) separate from the **raw file-parsing DTOs** (used only inside your JSON reader) so replacing `metra.json` with a future Caracal read API requires zero changes in `FareService` or controllers.

### 1. Public API-Layer DTOs (`caracal/dto/CaracalZonalDtos.java`)

```java
package ca.sfu.cmpt276.nywfareconfig.caracal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class CaracalZonalDtos {

    /**
     * Reusable for both Zones (A -> "Zone A") and Fare Classes (FULL -> "Full").
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DimensionValueSummary(String key, String name) {}

    /**
     * Matches Caracal's site-dimension-bind model (e.g., ST051 -> ORIGIN_ZONE = A).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SiteDimensionBinding(String siteCode, String dimensionKey, String valueKey) {}
}

```

### 2. Internal `metra.json` Parsing DTOs (`caracal/MetraJsonZonalMetadataProvider.java`)

Because `@JsonIgnoreProperties(ignoreUnknown = true)` drops `products`, `priceRules`, `calendars`, `devices`, and `demoQueries` automatically, Jackson only parses the three sections you need:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
record MetraConfigDocument(
        TenantRef tenant,
        List<SiteEntry> sites,
        List<DimensionEntry> dimensions) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record TenantRef(String code) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record SiteEntry(
        String siteCode,
        String zone,
        Map<String, String> dimensionValues) {

    /** Prefer dimensionValues.ORIGIN_ZONE, falling back to the top-level zone field. */
    String resolvedOriginZone() {
        if (dimensionValues != null && dimensionValues.get("ORIGIN_ZONE") != null) {
            return dimensionValues.get("ORIGIN_ZONE");
        }
        return zone;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record DimensionEntry(
        String key,
        String name,
        List<DimensionValueEntry> values) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record DimensionValueEntry(String key, String name) {}

```

### 3. Station Mapping View DTO (`fare/dto/StationZoneMappingView.java`)

```java
package ca.sfu.cmpt276.nywfareconfig.fare.dto;

public record StationZoneMappingView(
        String siteCode,
        String stationName,
        String baselineZone,
        String effectiveZone,
        boolean hasPendingDraft) {}

```

## Condensed Action Plan

### Step 1: Wire the Swappable `metra.json` Wrapper (`caracal` package)

1. **Gitignore & config:** Add `metra.json` to `.gitignore` and bind `caracal.metra-config-path=${CARACAL_METRA_CONFIG_PATH:}` in `application.properties` and `CaracalApiProperties`.


2. **Create `CaracalZonalDtos.java` and `MetraJsonZonalMetadataProvider.java`:**
* Parse `metra.json` once via Jackson into the internal records above (matching `tenant.code` case-insensitively).


* Extract `ORIGIN_ZONE` values $\rightarrow$ `List<DimensionValueSummary>`, `FARE_CLASS` values $\rightarrow$ `List<DimensionValueSummary>`, and `sites[]` $\rightarrow$ `List<SiteDimensionBinding>` using `resolvedOriginZone()`.


* If the path is blank or missing on disk, log a warning and return empty lists instead of failing startup.




3. **Extend `CaracalApiClient` & `HttpCaracalApiClient`:**
* Add `fetchZones(String tenantCode)`, `fetchFareClasses(String tenantCode)`, and `fetchStationZoneBindings(String tenantCode)` to `CaracalApiClient` and delegate them to `MetraJsonZonalMetadataProvider` inside `HttpCaracalApiClient`.





### Step 2: Update `PriceSimulateRequest` & `HttpCaracalApiClient`

1. **Update `CaracalSimulationDtos.PriceSimulateRequest`:** Annotate `siteCode` with `@JsonInclude(JsonInclude.Include.NON_NULL)` and add `@JsonInclude(JsonInclude.Include.NON_NULL) String channel` (keeping a 5-arg delegating constructor if needed).


2. **Relax `siteCode` validation in `HttpCaracalApiClient.simulatePrice(...)`:** Allow `siteCode == null` whenever `dimensions` is non-empty, and guard `actionDescription` against null `siteCode`.


3. **Verify `fetchSites(...)` limit:** Check `GET /functions/site-list?tenantCode=METRA` against Docker to ensure all Metra stations return without truncation; add a `limit` query param if Caracal caps the default page size.



### Step 3: Implement Live Zonal Hydration in `FareService`

1. **Replace mocks:**
* Delegate `getZones(tenantCode)` to `caracalApiClient.fetchZones(...)` (returning `Zone A`–`Zone J`) and add `getFareClasses(tenantCode)` delegating to `caracalApiClient.fetchFareClasses(...)`.


* Delete `resolveZonalBaseRates(...)`.


2. **Add `fareClass` & `at` support with backward-compatible overloads:**
* Default `fareClass` to `"FULL"` and `channel` to `"UNATTENDED"`.


* Keep existing `buildGrid` / `getBaselineFareRule` signatures as delegating overloads so cross-epic callers and existing tests keep compiling.


* Add `findTenantZoneId(List<SiteSummary>)` (first non-null site timezone, e.g., `America/Chicago`) because Zonal `origin` is a zone code (`A`–`J`), not a `siteCode`.




3. **Simulate live Zonal cells:**
* Extract the shared 4xx response classifier (`NOT_SOLD`, `CONDITION`, `NO_PRICE_RULE`, null total) from `simulateOdFareRuleWithFormattedAt(...)`.
* Add `simulateZonalFareRuleWithFormattedAt(...)` sending `siteCode = null`, `channel = "UNATTENDED"`, and `dimensions = Map.of("ORIGIN_ZONE", originZone, "DEST_ZONE", destZone, "FARE_CLASS", fareClass)`. (Note: `DAYPASS`/`WEEKEND`/`SPECIAL` only define `FULL`, and `SCPP` omits Zones `I`/`J`—your existing `NO_PRICE_RULE` handler maps those to `FareStatus.NA` automatically.)


* Replace both `getBaselineZonalFareRules(...)` (10 $\times$ 10 zone loop) and the `// TODO` Zonal branch in `getBaselineFareRule(...)` with this helper.


4. **Synthesize rate tiers before draft overlay:**
* Group distinct `ACTIVE` baseline prices in ascending order (`TreeSet<BigDecimal>`) into `TIER_A`, `TIER_B`, `TIER_C`, ... and assign `rule.setTierCode(...)` **before** `applyPendingDraftsToRules(...)`. Pass `tierCode` into `FareCell` inside `toFareCell(...)`.





### Step 4: Hydrate Station-to-Zone Mappings & Wire UI/Drafts

1. **Add `getStationZoneMappings(String tenantCode)` (in `FareService` or a dedicated `StationZoneService`):**
* Join live stations from `caracalApiClient.fetchSites(...)` with baseline bindings from `caracalApiClient.fetchStationZoneBindings(...)` (overlaid with any locally persisted committed `site-dimension-bind` overrides) and `PENDING` rows from `station_zone_drafts`, returning `List<StationZoneMappingView>` sorted by station name.




2. **Wire `TenantContext`, `FareController`, `FareDraftService`, and Thymeleaf:**
* Store `selectedFareClass` (default `"FULL"`) alongside `pricingDate` in `TenantContext`; pass both from `FareController` into `FareService.buildGrid(...)` and from `FareDraftService` into `getBaselineFareRule(...)` so off-day/time-gated edits (`WEEKEND`, `DAYPASS`, `MONTHLY`, `SPECIAL`) don't fail validation.


* Expose `fareClasses`, `zones`, and `stationZoneMappings` to the Zonal view/drawer (keeping Pricing Date visible in both modes and showing a warning banner if `zones` is empty due to a missing `metra.json`).




3. **Verify:**
* Unit tests: `MetraJsonZonalMetadataProvider` (valid parse + missing-file fallback), `HttpCaracalApiClient.simulatePrice` (null `siteCode`), and `FareService` (live Zonal grid, tier auto-grouping before draft overlay, station mapping join).
* Docker smoke test: `METRA` $\rightarrow$ `ONEWAY` (`A` $\rightarrow$ `H`: `$8.25` `FULL`, `$4.00` `REDUCED`), `WEEKEND` on Saturday or `2026-09-07` (`$7.00`), `MONTHLY` on the 25th (`$100.00` `FULL` / `$70.00` `REDUCED`) vs. 15th (`NOT_SOLD`), and the Station Zone Mapping drawer showing live stations with their `A`–`J` zones.