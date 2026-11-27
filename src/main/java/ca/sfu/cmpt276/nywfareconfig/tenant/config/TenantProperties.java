package ca.sfu.cmpt276.nywfareconfig.tenant.config;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;

@ConfigurationProperties(prefix = "app.tenants")
public record TenantProperties(
        @DefaultValue({ "NYW", "NYW_SANDBOX" }) Set<String> odCodes,
        @DefaultValue({ "METRA" }) Set<String> zonalCodes) {

    public TenantProperties {
        odCodes = normalizeCodes(odCodes);
        zonalCodes = normalizeCodes(zonalCodes);
    }

    public static TenantProperties defaults() {
        return new TenantProperties(Set.of("NYW", "NYW_SANDBOX"), Set.of("METRA"));
    }

    public Optional<GridMode> resolveGridMode(String tenantCode) {
        String normalized = normalizeCode(tenantCode);
        if (normalized == null) {
            return Optional.empty();
        }
        if (zonalCodes.contains(normalized)) {
            return Optional.of(GridMode.ZONAL);
        }
        if (odCodes.contains(normalized)) {
            return Optional.of(GridMode.OD);
        }
        return Optional.empty();
    }

    public boolean isSupported(String tenantCode) {
        return resolveGridMode(tenantCode).isPresent();
    }

    private static Set<String> normalizeCodes(Set<String> codes) {
        if (codes == null) {
            return Set.of();
        }
        return codes.stream()
                .map(TenantProperties::normalizeCode)
                .filter(code -> code != null)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}