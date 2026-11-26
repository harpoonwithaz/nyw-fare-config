package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalZonalDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.CaracalErrorResponse;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalZonalDtos.DimensionValueSummary;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalZonalDtos.SiteDimensionBinding;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalUnavailableException;

@Service
public class HttpCaracalApiClient implements CaracalApiClient {

    private final RestClient caracalRestClient;
    private final MetraJsonZonalMetadataProvider zonalMetadataProvider;

    private static final int SITE_LIST_LIMIT = 500;

    public HttpCaracalApiClient(RestClient caracalRestClient, MetraJsonZonalMetadataProvider zonalMetadataProvider) {
        this.caracalRestClient = caracalRestClient;
        this.zonalMetadataProvider = zonalMetadataProvider;
    }

    // TODO: Remove the X-Actor: system header from each api request except
    // fetchTenants()

    // ==============================================================================
    // Catalog Discovery
    // ==============================================================================

    // Queries GET /functions/tenant-list using X-Actor: system
    // to discover active platform tenants.
    @Override
    public List<CaracalCatalogDtos.TenantSummary> fetchTenants() {
        CaracalCatalogDtos.TenantList response = get(
                "/tenant-list",
                true,
                CaracalCatalogDtos.TenantList.class,
                "loading tenants");

        if (response == null || response.tenants() == null) {
            return List.of();
        }
        return response.tenants();
    }

    // Queries GET /functions/tenant-get?tenantCode={tenantCode} using X-Actor:
    // operator:<email>
    // to discover specific tenant details.
    @Override
    public CaracalCatalogDtos.TenantDetail fetchTenant(String tenantCode) {
        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException("Tenant code must not be blank.");
        }

        CaracalCatalogDtos.TenantDetail response = get(
                "/tenant-get?tenantCode={tenantCode}",
                false,
                CaracalCatalogDtos.TenantDetail.class,
                "loading details for tenant " + tenantCode,
                tenantCode);

        if (response == null) {
            throw new CaracalApiException("Caracal returned an empty response for tenant " + tenantCode + ".");
        }
        return response;
    }

    // Queries GET /functions/site-list to populate O-D terminals and Zonal station
    // directories.
    @Override
    public List<CaracalCatalogDtos.SiteSummary> fetchSites(String tenantCode) {
        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException("Tenant code must not be blank.");
        }

        CaracalCatalogDtos.SiteList response = get(
                "/site-list?tenantCode={tenantCode}&limit={limit}",
                true,
                CaracalCatalogDtos.SiteList.class,
                "loading site list for tenant " + tenantCode,
                tenantCode,
                SITE_LIST_LIMIT);

        if (response == null) {
            throw new CaracalApiException("Caracal returned an empty response for tenant " + tenantCode + ".");
        }
        return response.sites() != null ? response.sites() : List.of();
    }

    @Override
    public List<CaracalCatalogDtos.ProductSummary> fetchProducts(String tenantCode) {
        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException("Tenant code must not be blank.");
        }

        CaracalCatalogDtos.ProductList response = get(
                "/product-list?tenantCode={tenantCode}",
                true,
                CaracalCatalogDtos.ProductList.class,
                "loading product list for tenant " + tenantCode,
                tenantCode);

        if (response == null) {
            throw new CaracalApiException("Caracal returned an empty response for tenant " + tenantCode + ".");
        }
        return response.products();
    }

    // Queries POST /functions/rate-table-get to populate O-D tenants
    @Override
    public Optional<List<CaracalCatalogDtos.RateAssignmentSummary>> fetchRateTable(
            CaracalCatalogDtos.RateTableGetRequest requestPayload) {
        if (requestPayload == null
                || isNullOrBlank(requestPayload.tenantCode())
                || isNullOrBlank(requestPayload.rateTableName())) {
            throw new IllegalArgumentException("Invalid request payload for rate table get.");
        }

        String actionDescription = "loading rate table " + requestPayload.rateTableName();

        return execute(actionDescription, () -> {
            try {
                CaracalCatalogDtos.RateTableGetResponse response = caracalRestClient
                        .post()
                        .uri("/rate-table-get")
                        .attribute("useSystemActor", false)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestPayload)
                        .retrieve()
                        .body(CaracalCatalogDtos.RateTableGetResponse.class);

                if (response == null) {
                    throw new CaracalApiException(
                            "Caracal returned an empty response for rate table "
                                    + requestPayload.rateTableName() + ".");
                }
                List<CaracalCatalogDtos.RateAssignmentSummary> assignments = response.assignments() != null
                        ? response.assignments()
                        : List.of();
                return Optional.of(assignments);
            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                    return Optional.empty();
                }
                throw ex;
            }
        });
    }

    @Override
    public List<CaracalZonalDtos.DimensionValueSummary> fetchZones(String tenantCode) {
        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException("Tenant code must not be blank.");
        }

        return zonalMetadataProvider.getZones(tenantCode);
    }

    @Override
    public List<DimensionValueSummary> fetchFareClasses(String tenantCode) {
        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException("Tenant code must not be blank.");
        }

        return zonalMetadataProvider.getFareClasses(tenantCode);
    }

    @Override
    public List<SiteDimensionBinding> fetchStationZoneBindings(String tenantCode) {
        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException("Tenant code must not be blank.");
        }

        return zonalMetadataProvider.getStationZoneBindings(tenantCode);
    }

    // ==============================================================================
    // Matrix & Availability Hydration
    // ==============================================================================
    @Override
    public CaracalSimulationDtos.PriceSimulationResult simulatePrice(
            CaracalSimulationDtos.PriceSimulateRequest requestPayload) {
        if (requestPayload == null
                || isNullOrBlank(requestPayload.tenantCode())
                || isNullOrBlank(requestPayload.productCode())
                || requestPayload.dimensions() == null
                || (requestPayload.siteCode() != null && requestPayload.siteCode().isBlank())
                || (requestPayload.siteCode() == null && requestPayload.dimensions().isEmpty())) {
            throw new IllegalArgumentException("Invalid request payload for cell price simulate.");
        }

        String targetDescription = requestPayload.siteCode() != null
                ? "at site " + requestPayload.siteCode()
                : "with dimensions " + requestPayload.dimensions();
        String actionDescription = "simulating price for product "
                + requestPayload.productCode() + " " + targetDescription;

        return execute(actionDescription, () -> {
            try {
                CaracalSimulationDtos.PriceSimulateResponse response = caracalRestClient
                        .post()
                        .uri("/price-simulate")
                        .attribute("useSystemActor", false)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestPayload)
                        .retrieve()
                        .body(CaracalSimulationDtos.PriceSimulateResponse.class);

                if (response == null || response.pricing() == null || response.pricing().total() == null) {
                    throw new CaracalApiException(
                            "Caracal returned an empty pricing response while " + actionDescription + ".");
                }
                return new CaracalSimulationDtos.PriceSimulationResult(response, null);
            } catch (RestClientResponseException ex) {
                if (isBusiness4xx(ex)) {
                    String errorMessage = extractErrorMessage(ex);
                    if (!isNullOrBlank(errorMessage)) {
                        return new CaracalSimulationDtos.PriceSimulationResult(null, errorMessage);
                    }
                }
                throw ex;
            }
        });
    }

    // ==============================================================================
    // Helper Methods
    // ==============================================================================

    private <T> T get(
            String uri,
            boolean useSystemActor,
            Class<T> responseType,
            String actionDescription,
            Object... uriVars) {

        return execute(actionDescription, () -> caracalRestClient
                .get()
                .uri(uri, uriVars)
                // If using CaracalActorInterceptor, pass the flag as an attribute;
                // otherwise set .header("X-Actor", ...) directly here.
                .attribute("useSystemActor", useSystemActor)
                .retrieve()
                .body(responseType));
    }

    private <T> T execute(String actionDescription, Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException ex) {
            throw new CaracalUnavailableException(
                    "Unable to reach the Caracal platform.", ex);
        } catch (RestClientResponseException ex) {
            throw new CaracalApiException(
                    "Caracal API error (" + ex.getStatusCode().value() + ") while " + actionDescription + ".", ex);
        } catch (RestClientException ex) {
            throw new CaracalApiException(
                    "Received an invalid response format from Caracal while " + actionDescription + ".", ex);
        }
    }

    private boolean isNullOrBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean isBusiness4xx(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        return ex.getStatusCode().is4xxClientError() && status != 401 && status != 403;
    }

    private String extractErrorMessage(RestClientResponseException ex) {
        try {
            CaracalErrorResponse errorResponse = ex.getResponseBodyAs(CaracalErrorResponse.class);
            if (errorResponse != null && !isNullOrBlank(errorResponse.error())) {
                return errorResponse.error();
            }
        } catch (RestClientException ignored) {
            // Fall back to raw response body if 4xx payload is not valid JSON
        }
        return ex.getResponseBodyAsString();
    }
}