package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalSimulationDtos.CaracalErrorResponse;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalUnavailableException;

import tools.jackson.databind.JsonNode;

@Service
public class HttpCaracalApiClient implements CaracalApiClient {

    private final RestClient caracalRestClient;

    public HttpCaracalApiClient(RestClient caracalRestClient) {
        this.caracalRestClient = caracalRestClient;
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
                "/site-list?tenantCode={tenantCode}",
                true,
                CaracalCatalogDtos.SiteList.class,
                "loading site list for tenant " + tenantCode,
                tenantCode);

        if (response == null) {
            throw new CaracalApiException("Caracal returned an empty response for tenant " + tenantCode + ".");
        }
        return response.sites();
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
    
    /**
    * Retrieves recent audit log entries for a specific tenant from Caracal.
    *
    * Sends a GET request to /functions/audit-log-get with the tenant code
    * and maximum number of entries to retrieve.
    * Returns the raw JSON response until the audit schema is finalized.
    */
    @Override 
    public JsonNode fetchPublicationHistory(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException("Tenant code must not be blank.");
        }

        return get("/publication-list?tenant={tenant}", false, JsonNode.class, 
            "loading publication history for tenant " + tenantCode, tenantCode
        );
    }

    @Override 
    public JsonNode fetchAuditHistory(String tenantCode, int limit) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException("Tenant code must not be blank.");
        }
        if (limit <= 0){
            throw new IllegalArgumentException("History limit must be positive.");
        }
        return get("/audit-log-get?tenantCode={tenantCode}&limit={limit}", false, JsonNode.class, 
            "loading audit history for tenant" + tenantCode, tenantCode, limit
        );
    }


    @Override
    public BigDecimal fetchBaselinePrice(String tenantCode, String originCode,
            String destCode, String productCode) {

        Map<String, Object> requestBody = Map.of(
                "tenantCode", tenantCode,
                "productCode", productCode,
                "durationMinutes", 0,
                "siteCode", originCode,
                "dimensions", Map.of(
                        "ORIGIN", originCode,
                        "DEST", destCode
                )
        );

        JsonNode response = execute(
                "retrieving live fare price",
                () -> caracalRestClient
                        .post()
                        .uri("/quote-create")
                        .attribute("useSystemActor", false)
                        .body(requestBody)
                        .retrieve()
                        .body(JsonNode.class)
        );

        if (response == null
                || response.path("pricing").path("base").isMissingNode()
                || !response.path("pricing").path("base").isNumber()) {
            throw new CaracalApiException(
                    "Caracal returned an invalid fare pricing response.");
        }

        return response.path("pricing").path("base").decimalValue();
    }

    // @Override
    // public BigDecimal fetchSimulatedPrice(
    //         String tenantCode,
    //         String productCode,
    //         String originCode,
    //         String destCode,
    //         String at) {

    //     Map<String, Object> requestBody = Map.of(
    //             "tenantCode", tenantCode,
    //             "productCode", productCode,
    //             "siteCode", originCode,
    //             "dimensions", Map.of("DEST", destCode),
    //             "at", at,
    //             "channel", "UNATTENDED"
    //     );

    //     JsonNode response;

    //     try {
    //         response = execute(
    //                 "simulating sandbox fare price",
    //                 () -> caracalRestClient
    //                         .post()
    //                         .uri("/price-simulate")
    //                         .attribute("useSystemActor", true)
    //                         .body(requestBody)
    //                         .retrieve()
    //                         .body(JsonNode.class)
    //         );

    //     } catch (CaracalApiException ex) {
    //         if (ex.getCause() instanceof RestClientResponseException httpEx) {
    //             int status = httpEx.getStatusCode().value();

    //             if (status == 404 || status == 409 || status == 422) {
    //                 return null;
    //             }
    //         }

    //         throw ex;
    //     }

    //     if (response == null
    //             || !response.path("pricing").path("total").isNumber()) {
    //         throw new CaracalApiException(
    //                 "Caracal returned an invalid simulated pricing response.");
    //     }

    //     return response.path("pricing").path("total").decimalValue();
    // }

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
                List<CaracalCatalogDtos.RateAssignmentSummary> assignments =
                        response.assignments() != null ? response.assignments() : List.of();
                return Optional.of(assignments);
            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                    return Optional.empty();
                }
                throw ex;
            }
        });
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
                || isNullOrBlank(requestPayload.siteCode())
                || requestPayload.dimensions() == null) {
            throw new IllegalArgumentException("Invalid request payload for cell price simulate.");
        }
        /*
        For debugging:
        System.out.println("Price simulate called for product: *" + requestPayload.productCode() +
                            "* site: *" + requestPayload.siteCode() + "* at: *" + requestPayload.at() + "*.");
        */

        String actionDescription = "simulating price for product "
                + requestPayload.productCode() + " at site " + requestPayload.siteCode();

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
                        return new CaracalSimulationDtos.PriceSimulationResult(null, errorMessage, ex.getStatusCode().value());
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