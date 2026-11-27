package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
import ca.sfu.cmpt276.nywfareconfig.security.ActorService;

import tools.jackson.databind.JsonNode;

@Service
public class HttpCaracalApiClient implements CaracalApiClient {

    private final RestClient caracalRestClient;
    private final ActorService actorService;
    private final MetraJsonZonalMetadataProvider zonalMetadataProvider;

    private static final int SITE_LIST_LIMIT = 500;

    @Autowired
    public HttpCaracalApiClient(
            RestClient caracalRestClient,
            ActorService actorService,
            MetraJsonZonalMetadataProvider zonalMetadataProvider) {

        this.caracalRestClient = caracalRestClient;
        this.actorService = actorService;
        this.zonalMetadataProvider = zonalMetadataProvider;
    }

    // Compatibility constructor for existing publication tests.
    public HttpCaracalApiClient(
            RestClient caracalRestClient,
            ActorService actorService) {

        this(caracalRestClient, actorService, null);
    }

    // ============================================================
    // Catalog Discovery
    // ============================================================

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

    @Override
    public CaracalCatalogDtos.TenantDetail fetchTenant(String tenantCode) {
        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException(
                    "Tenant code must not be blank.");
        }

        CaracalCatalogDtos.TenantDetail response = get(
                "/tenant-get?tenantCode={tenantCode}",
                false,
                CaracalCatalogDtos.TenantDetail.class,
                "loading details for tenant " + tenantCode,
                tenantCode);

        if (response == null) {
            throw new CaracalApiException(
                    "Caracal returned an empty response for tenant "
                            + tenantCode + ".");
        }

        return response;
    }

    @Override
    public List<CaracalCatalogDtos.SiteSummary> fetchSites(
            String tenantCode) {

        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException(
                    "Tenant code must not be blank.");
        }

        CaracalCatalogDtos.SiteList response = get(
                "/site-list?tenantCode={tenantCode}&limit={limit}",
                true,
                CaracalCatalogDtos.SiteList.class,
                "loading site list for tenant " + tenantCode,
                tenantCode,
                SITE_LIST_LIMIT);

        if (response == null) {
            throw new CaracalApiException(
                    "Caracal returned an empty response for tenant "
                            + tenantCode + ".");
        }

        return response.sites() != null
                ? response.sites()
                : List.of();
    }

    @Override
    public List<CaracalCatalogDtos.ProductSummary> fetchProducts(
            String tenantCode) {

        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException(
                    "Tenant code must not be blank.");
        }

        CaracalCatalogDtos.ProductList response = get(
                "/product-list?tenantCode={tenantCode}",
                true,
                CaracalCatalogDtos.ProductList.class,
                "loading product list for tenant " + tenantCode,
                tenantCode);

        if (response == null) {
            throw new CaracalApiException(
                    "Caracal returned an empty response for tenant "
                            + tenantCode + ".");
        }

        return response.products();
    }

    // ============================================================
    // Publication and Audit History
    // ============================================================

    @Override
    public JsonNode fetchPublicationHistory(String tenantCode) {
        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException(
                    "Tenant code must not be blank.");
        }

        return get(
                "/publication-list?tenant={tenant}",
                false,
                JsonNode.class,
                "loading publication history for tenant " + tenantCode,
                tenantCode);
    }

    @Override
    public JsonNode fetchAuditHistory(String tenantCode, int limit) {
        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException(
                    "Tenant code must not be blank.");
        }

        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "History limit must be positive.");
        }

        return get(
                "/audit-log-get?tenantCode={tenantCode}&limit={limit}",
                false,
                JsonNode.class,
                "loading audit history for tenant " + tenantCode,
                tenantCode,
                limit);
    }

    // ============================================================
    // Publication and Configuration Mutations
    // ============================================================

    @Override
    public JsonNode publishCatalog(String tenantCode) {
        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException(
                    "Tenant code must not be blank.");
        }

        String actor = actorService.currentActorHeader();

        if (isNullOrBlank(actor)
                || "system".equalsIgnoreCase(actor.trim())) {

            throw new IllegalStateException(
                    "An authenticated actor is required for publication.");
        }

        Map<String, Object> requestBody = Map.of(
                "tenantCode", tenantCode);

        return execute(
                "publishing catalog for tenant " + tenantCode,
                () -> caracalRestClient
                        .post()
                        .uri("/catalog-publish")
                        .attribute("useSystemActor", false)
                        .header("X-Actor", "operator:" + actor.trim())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(JsonNode.class));
    }

    @Override
    public JsonNode updateOdMatrix(
            String tenantCode,
            String priceRuleSetName,
            String originCode,
            String destCode,
            BigDecimal price) {

        if (!"NYW_SANDBOX".equals(tenantCode)) {
            throw new IllegalArgumentException(
                    "Fare updates are restricted to NYW_SANDBOX.");
        }

        if (isNullOrBlank(priceRuleSetName)
                || isNullOrBlank(originCode)
                || isNullOrBlank(destCode)
                || price == null
                || price.signum() < 0) {

            throw new IllegalArgumentException(
                    "Invalid OD matrix update.");
        }

        Map<String, Object> requestBody = Map.of(
                "tenantCode", tenantCode,
                "priceRuleSetName", priceRuleSetName,
                "originDimensionKey", "ORIGIN",
                "destDimensionKey", "DEST",
                "rows", List.of(
                        Map.of(
                                "originSiteCode", originCode,
                                "destSiteCode", destCode,
                                "price", price)));

        return executeMutation(
                "/od-matrix-set",
                requestBody,
                "updating fare from " + originCode + " to " + destCode);
    }

    @Override
    public JsonNode updateRateAssignment(
            String tenantCode,
            String assignmentId,
            String cellType) {

        if (!"NYW_SANDBOX".equals(tenantCode)) {
            throw new IllegalArgumentException(
                    "Rate assignment updates are restricted to NYW_SANDBOX.");
        }

        if (isNullOrBlank(assignmentId)) {
            throw new IllegalArgumentException(
                    "Assignment ID is required.");
        }

        if (!"VALUE".equals(cellType)
                && !"NOT_SOLD".equals(cellType)) {

            throw new IllegalArgumentException(
                    "Invalid rate assignment cell type.");
        }

        Map<String, Object> requestBody = Map.of(
                "tenantCode", tenantCode,
                "assignmentId", assignmentId,
                "cellType", cellType);

        return executeMutation(
                "/rate-assignment-update",
                requestBody,
                "updating rate assignment " + assignmentId);
    }

    private JsonNode executeMutation(
            String endpoint,
            Map<String, Object> requestBody,
            String actionDescription) {

        String actor = actorService.currentActorHeader();

        if (isNullOrBlank(actor)
                || "system".equalsIgnoreCase(actor.trim())) {

            throw new IllegalStateException(
                    "An authenticated actor is required.");
        }

        return execute(
                actionDescription,
                () -> caracalRestClient
                        .post()
                        .uri(endpoint)
                        .attribute("useSystemActor", false)
                        .header("X-Actor", "operator:" + actor.trim())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(JsonNode.class));
    }

    // ============================================================
    // Legacy Baseline Pricing
    // ============================================================

    @Override
    public BigDecimal fetchBaselinePrice(
            String tenantCode,
            String originCode,
            String destCode,
            String productCode) {

        Map<String, Object> requestBody = Map.of(
                "tenantCode", tenantCode,
                "productCode", productCode,
                "durationMinutes", 0,
                "siteCode", originCode,
                "dimensions", Map.of(
                        "ORIGIN", originCode,
                        "DEST", destCode));

        JsonNode response = execute(
                "retrieving live fare price",
                () -> caracalRestClient
                        .post()
                        .uri("/quote-create")
                        .attribute("useSystemActor", false)
                        .body(requestBody)
                        .retrieve()
                        .body(JsonNode.class));

        if (response == null
                || response.path("pricing").path("base").isMissingNode()
                || !response.path("pricing").path("base").isNumber()) {

            throw new CaracalApiException(
                    "Caracal returned an invalid fare pricing response.");
        }

        return response.path("pricing").path("base").decimalValue();
    }

    // ============================================================
    // Rate Table Discovery
    // ============================================================

    @Override
    public Optional<List<CaracalCatalogDtos.RateAssignmentSummary>>
            fetchRateTable(
                    CaracalCatalogDtos.RateTableGetRequest requestPayload) {

        if (requestPayload == null
                || isNullOrBlank(requestPayload.tenantCode())
                || isNullOrBlank(requestPayload.rateTableName())) {

            throw new IllegalArgumentException(
                    "Invalid request payload for rate table get.");
        }

        String actionDescription =
                "loading rate table " + requestPayload.rateTableName();

        return execute(actionDescription, () -> {
            try {
                CaracalCatalogDtos.RateTableGetResponse response =
                        caracalRestClient
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
                        response.assignments() != null
                                ? response.assignments()
                                : List.of();

                return Optional.of(assignments);

            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().value()
                        == HttpStatus.NOT_FOUND.value()) {

                    return Optional.empty();
                }

                throw ex;
            }
        });
    }

    // ============================================================
    // Zonal Discovery
    // ============================================================

    @Override
    public List<CaracalZonalDtos.DimensionValueSummary> fetchZones(
            String tenantCode) {

        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException(
                    "Tenant code must not be blank.");
        }

        return zonalMetadataProvider.getZones(tenantCode);
    }

    @Override
    public List<DimensionValueSummary> fetchFareClasses(
            String tenantCode) {

        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException(
                    "Tenant code must not be blank.");
        }

        return zonalMetadataProvider.getFareClasses(tenantCode);
    }

    @Override
    public List<SiteDimensionBinding> fetchStationZoneBindings(
            String tenantCode) {

        if (isNullOrBlank(tenantCode)) {
            throw new IllegalArgumentException(
                    "Tenant code must not be blank.");
        }

        return zonalMetadataProvider.getStationZoneBindings(tenantCode);
    }

    // ============================================================
    // Matrix and Availability Hydration
    // ============================================================

    @Override
    public CaracalSimulationDtos.PriceSimulationResult simulatePrice(
            CaracalSimulationDtos.PriceSimulateRequest requestPayload) {

        if (requestPayload == null
                || isNullOrBlank(requestPayload.tenantCode())
                || isNullOrBlank(requestPayload.productCode())
                || requestPayload.dimensions() == null
                || (requestPayload.siteCode() != null
                        && requestPayload.siteCode().isBlank())
                || (requestPayload.siteCode() == null
                        && requestPayload.dimensions().isEmpty())) {

            throw new IllegalArgumentException(
                    "Invalid request payload for cell price simulate.");
        }

        String targetDescription = requestPayload.siteCode() != null
                ? "at site " + requestPayload.siteCode()
                : "with dimensions " + requestPayload.dimensions();

        String actionDescription =
                "simulating price for product "
                        + requestPayload.productCode()
                        + " " + targetDescription;

        return execute(actionDescription, () -> {
            try {
                CaracalSimulationDtos.PriceSimulateResponse response =
                        caracalRestClient
                                .post()
                                .uri("/price-simulate")
                                .attribute("useSystemActor", false)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(requestPayload)
                                .retrieve()
                                .body(CaracalSimulationDtos.PriceSimulateResponse.class);

                if (response == null
                        || response.pricing() == null
                        || response.pricing().total() == null) {

                    throw new CaracalApiException(
                            "Caracal returned an empty pricing response while "
                                    + actionDescription + ".");
                }

                return new CaracalSimulationDtos.PriceSimulationResult(
                        response, null);

            } catch (RestClientResponseException ex) {
                if (isBusiness4xx(ex)) {
                    String errorMessage = extractErrorMessage(ex);

                    if (!isNullOrBlank(errorMessage)) {
                        return new CaracalSimulationDtos.PriceSimulationResult(
                                null,
                                errorMessage,
                                ex.getStatusCode().value());
                    }
                }

                throw ex;
            }
        });
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    private <T> T get(
            String uri,
            boolean useSystemActor,
            Class<T> responseType,
            String actionDescription,
            Object... uriVars) {

        return execute(
                actionDescription,
                () -> caracalRestClient
                        .get()
                        .uri(uri, uriVars)
                        .attribute("useSystemActor", useSystemActor)
                        .retrieve()
                        .body(responseType));
    }

    private <T> T execute(
            String actionDescription,
            Supplier<T> request) {

        try {
            return request.get();

        } catch (ResourceAccessException ex) {
            throw new CaracalUnavailableException(
                    "Unable to reach the Caracal platform.", ex);

        } catch (RestClientResponseException ex) {
            throw new CaracalApiException(
                    "Caracal API error ("
                            + ex.getStatusCode().value()
                            + ") while "
                            + actionDescription + ".", ex);

        } catch (RestClientException ex) {
            throw new CaracalApiException(
                    "Received an invalid response format from Caracal while "
                            + actionDescription + ".", ex);
        }
    }

    private boolean isNullOrBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean isBusiness4xx(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();

        return ex.getStatusCode().is4xxClientError()
                && status != 401
                && status != 403;
    }

    private String extractErrorMessage(RestClientResponseException ex) {
        try {
            CaracalErrorResponse errorResponse =
                    ex.getResponseBodyAs(CaracalErrorResponse.class);

            if (errorResponse != null
                    && !isNullOrBlank(errorResponse.error())) {

                return errorResponse.error();
            }

        } catch (RestClientException ignored) {
            // Fall back to the raw response body.
        }

        return ex.getResponseBodyAsString();
    }
}