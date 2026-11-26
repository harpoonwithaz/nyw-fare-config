package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ca.sfu.cmpt276.nywfareconfig.caracal.config.CaracalApiProperties;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalZonalDtos.DimensionValueSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalZonalDtos.SiteDimensionBinding;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Temporary swappable metadata provider that reads Metra's zones, fare classes,
 * and initial station-to-zone bindings from metra.json until Caracal exposes
 * dimensional read endpoints.
 */
@Component
public class MetraJsonZonalMetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(MetraJsonZonalMetadataProvider.class);

    private static final String ORIGIN_ZONE_DIMENSION = "ORIGIN_ZONE";
    private static final String FARE_CLASS_DIMENSION = "FARE_CLASS";

    private final String configuredTenantCode;
    private final List<DimensionValueSummary> zones;
    private final List<DimensionValueSummary> fareClasses;
    private final List<SiteDimensionBinding> stationZoneBindings;

    public MetraJsonZonalMetadataProvider(CaracalApiProperties properties, ObjectMapper objectMapper) {
        String rawPath = properties != null ? properties.metraConfigPath() : null;
        LoadedMetadata loaded = loadMetadata(rawPath, objectMapper);
        this.configuredTenantCode = loaded.tenantCode();
        this.zones = loaded.zones();
        this.fareClasses = loaded.fareClasses();
        this.stationZoneBindings = loaded.stationZoneBindings();
    }

    public List<DimensionValueSummary> getZones(String tenantCode) {
        return matchesConfiguredTenant(tenantCode) ? zones : List.of();
    }

    public List<DimensionValueSummary> getFareClasses(String tenantCode) {
        return matchesConfiguredTenant(tenantCode) ? fareClasses : List.of();
    }

    public List<SiteDimensionBinding> getStationZoneBindings(String tenantCode) {
        return matchesConfiguredTenant(tenantCode) ? stationZoneBindings : List.of();
    }

    private boolean matchesConfiguredTenant(String tenantCode) {
        if (configuredTenantCode == null || isBlank(tenantCode)) {
            return false;
        }
        return configuredTenantCode.equalsIgnoreCase(tenantCode.trim());
    }


    // =========================================================================
    // Internal parsing records (confined to this file-backed provider)
    // =========================================================================

    private record LoadedMetadata(
            String tenantCode,
            List<DimensionValueSummary> zones,
            List<DimensionValueSummary> fareClasses,
            List<SiteDimensionBinding> stationZoneBindings) {

        static LoadedMetadata empty() {
            return new LoadedMetadata(null, List.of(), List.of(), List.of());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MetraConfigDocument(
            TenantRef tenant,
            List<SiteEntry> sites,
            List<DimensionEntry> dimensions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TenantRef(String code) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SiteEntry(
            String siteCode,
            String zone,
            Map<String, String> dimensionValues) {

        String resolvedOriginZone() {
            if (dimensionValues != null) {
                String fromDim = dimensionValues.get(ORIGIN_ZONE_DIMENSION);
                if (fromDim != null && !fromDim.isBlank()) {
                    return fromDim;
                }
            }
            return zone;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DimensionEntry(
            String key,
            String name,
            List<DimensionValueEntry> values) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DimensionValueEntry(String key, String name) {
    }

    private LoadedMetadata loadMetadata(String rawPath, ObjectMapper objectMapper) {
        if (isBlank(rawPath)) {
            log.warn("caracal.api.metra-config-path (CARACAL_METRA_CONFIG_PATH) is not configured; "
                    + "zonal zones, fare classes, and station-zone mappings will be empty.");
            return LoadedMetadata.empty();
        }

        Path path;
        try {
            path = Path.of(rawPath.trim());
        } catch (InvalidPathException ex) {
            log.warn("Invalid caracal.api.metra-config-path '{}': {}; zonal metadata will be empty.",
                    rawPath, ex.getMessage());
            return LoadedMetadata.empty();
        }

        if (!Files.isRegularFile(path)) {
            log.warn("Metra config file not found at '{}'; zonal zones, fare classes, "
                    + "and station-zone mappings will be empty.", path.toAbsolutePath());
            return LoadedMetadata.empty();
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            MetraConfigDocument doc = objectMapper.readValue(inputStream, MetraConfigDocument.class);
            if (doc == null || doc.tenant() == null || isBlank(doc.tenant().code())) {
                log.warn("Metra config file '{}' is missing tenant.code; zonal metadata will be empty.",
                        path.toAbsolutePath());
                return LoadedMetadata.empty();
            }

            String tenantCode = normalizeCode(doc.tenant().code());
            List<DimensionValueSummary> extractedZones = extractDimensionValues(doc.dimensions(), ORIGIN_ZONE_DIMENSION);
            List<DimensionValueSummary> extractedFareClasses = extractDimensionValues(doc.dimensions(), FARE_CLASS_DIMENSION);
            List<SiteDimensionBinding> extractedBindings = extractStationBindings(doc.sites());

            log.info("Loaded zonal metadata for tenant {} from '{}': {} zones, {} fare classes, {} station bindings.",
                    tenantCode, path.toAbsolutePath(),
                    extractedZones.size(), extractedFareClasses.size(), extractedBindings.size());

            return new LoadedMetadata(tenantCode, extractedZones, extractedFareClasses, extractedBindings);
        } catch (IOException | JacksonException ex) {
            log.warn("Failed to read or parse Metra config file '{}': {}; zonal metadata will be empty.",
                    path.toAbsolutePath(), ex.getMessage());
            return LoadedMetadata.empty();
        }
    }

    private List<DimensionValueSummary> extractDimensionValues(
            List<DimensionEntry> dimensions,
            String targetDimensionKey) {
        if (dimensions == null) {
            return List.of();
        }

        for (DimensionEntry dimension : dimensions) {
            if (dimension == null || isBlank(dimension.key())) {
                continue;
            }
            if (!targetDimensionKey.equalsIgnoreCase(dimension.key().trim())) {
                continue;
            }
            if (dimension.values() == null) {
                return List.of();
            }

            List<DimensionValueSummary> results = new ArrayList<>();
            for (DimensionValueEntry entry : dimension.values()) {
                if (entry == null || isBlank(entry.key())) {
                    continue;
                }
                String normalizedKey = normalizeCode(entry.key());
                String displayName = isBlank(entry.name()) ? normalizedKey : entry.name().trim();
                results.add(new DimensionValueSummary(normalizedKey, displayName));
            }
            return List.copyOf(results);
        }

        return List.of();
    }

    private List<SiteDimensionBinding> extractStationBindings(List<SiteEntry> sites) {
        if (sites == null) {
            return List.of();
        }

        List<SiteDimensionBinding> bindings = new ArrayList<>();
        for (SiteEntry site : sites) {
            if (site == null || isBlank(site.siteCode())) {
                continue;
            }
            String originZone = site.resolvedOriginZone();
            if (isBlank(originZone)) {
                continue;
            }

            bindings.add(new SiteDimensionBinding(
                    normalizeCode(site.siteCode()),
                    ORIGIN_ZONE_DIMENSION,
                    normalizeCode(originZone)));
        }

        return List.copyOf(bindings);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

}