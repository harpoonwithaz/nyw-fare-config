package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.fare.model.FareDraft;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;

@Service
public class CaracalFareBaselineProvider implements FareBaselineProvider {

    private static final Map<String, String> LEGACY_SITES = Map.of(
            "midtown / w. 39th st", "MIDTOWN",
            "hoboken 14th st", "HOB14ST",
            "brookfield place", "BROOKFD",
            "pier 11 / wall st", "PIER11",
            "edgewater", "EDGEWTR",
            "port imperial", "PORTIMP"
    );

    private static final Map<String, String> LEGACY_PRODUCTS = Map.of(
            "adult one-way", "ADULT_ONEWAY",
            "child one-way", "CHILD_ONEWAY",
            "senior 65+", "SENIOR_ONEWAY",
            "10-trip pass", "FULL_10TRIP",
            "monthly pass", "FULL_MONTHLY"
    );

    private final CaracalApiClient caracalApiClient;

    public CaracalFareBaselineProvider(CaracalApiClient caracalApiClient) {
        this.caracalApiClient = caracalApiClient;
    }

    @Override
    public BigDecimal getBaselinePrice(FareDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("Fare draft cannot be null.");
        }

        if (draft.getGridMode() != GridMode.OD) {
            throw new UnsupportedOperationException(
                    "Live baseline pricing currently supports O-D fares only.");
        }

        String tenantCode = draft.getTenantCode();

        String originCode = resolveSiteCode(
                tenantCode, draft.getOriginIdentifier());

        String destCode = resolveSiteCode(
                tenantCode, draft.getDestIdentifier());

        String productCode = resolveProductCode(
                tenantCode, draft.getProductCode());

        return caracalApiClient.fetchBaselinePrice(
                tenantCode,
                originCode,
                destCode,
                productCode
        );
    }

    private String resolveSiteCode(String tenantCode, String identifier) {
        List<CaracalCatalogDtos.SiteSummary> sites =
                caracalApiClient.fetchSites(tenantCode);

        String resolvedIdentifier = LEGACY_SITES.getOrDefault(
                identifier.toLowerCase(), identifier);

        return sites.stream()
                .filter(site ->
                        site.siteCode().equalsIgnoreCase(resolvedIdentifier)
                        || site.name().equalsIgnoreCase(resolvedIdentifier))
                .map(CaracalCatalogDtos.SiteSummary::siteCode)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Caracal site: " + identifier));
    }

    private String resolveProductCode(String tenantCode, String identifier) {
        List<CaracalCatalogDtos.ProductSummary> products =
                caracalApiClient.fetchProducts(tenantCode);

        String resolvedIdentifier = LEGACY_PRODUCTS.getOrDefault(
                identifier.toLowerCase(), identifier);

        return products.stream()
                .filter(product ->
                        product.code().equalsIgnoreCase(resolvedIdentifier))
                .map(CaracalCatalogDtos.ProductSummary::code)
                .findFirst()
                .orElseGet(() ->
                        resolveProductByName(products, resolvedIdentifier));
    }

    private String resolveProductByName(
            List<CaracalCatalogDtos.ProductSummary> products,
            String identifier) {

        List<CaracalCatalogDtos.ProductSummary> matches =
                products.stream()
                        .filter(product ->
                                product.name().equalsIgnoreCase(identifier))
                        .toList();

        if (matches.size() != 1) {
            throw new IllegalArgumentException(
                    "Cannot uniquely resolve Caracal product: " + identifier);
        }

        return matches.get(0).code();
    }
}


