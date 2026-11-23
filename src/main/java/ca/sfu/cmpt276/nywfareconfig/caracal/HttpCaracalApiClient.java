package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalUnavailableException;
import ca.sfu.cmpt276.nywfareconfig.security.ActorService;

@Service
public class HttpCaracalApiClient implements CaracalApiClient {

    private final RestClient caracalRestClient;
    private final ActorService actorService;

    public HttpCaracalApiClient(RestClient caracalRestClient, ActorService actorService) {
        this.caracalRestClient = caracalRestClient;
        this.actorService = actorService;
    }

    // ==============================================================================
    // Catalog Discovery
    // ==============================================================================

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
        if (tenantCode == null || tenantCode.isBlank()) {
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

    @Override
    public List<CaracalCatalogDtos.SiteSummary> fetchSites(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException("Tenant code must not be blank.");
        }

        CaracalCatalogDtos.SiteList response = get(
                "/site-list?tenantCode={tenantCode}",
                false,
                CaracalCatalogDtos.SiteList.class,
                "loading site list for tenant " + tenantCode,
                tenantCode);

        if (response == null || response.sites() == null) {
            return List.of();
        }
        return response.sites();
    }

    @Override
    public List<CaracalCatalogDtos.ProductSummary> fetchProducts(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException("Tenant code must not be blank.");
        }

        CaracalCatalogDtos.ProductList response = get(
                "/product-list?tenantCode={tenantCode}",
                false,
                CaracalCatalogDtos.ProductList.class,
                "loading product list for tenant " + tenantCode,
                tenantCode);

        if (response == null || response.products() == null) {
            return List.of();
        }
        return response.products();
    }

    // ==============================================================================
    // Epic 3: Rate Tables & Availability Simulation
    // ==============================================================================

    @Override
    public List<CaracalCatalogDtos.RateTableSummary> fetchRateTables(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException("Tenant code must not be blank.");
        }

        CaracalCatalogDtos.RateTableListResponse response = post(
                "/rate-table-list",
                new CaracalCatalogDtos.RateTableListRequest(tenantCode, null, 100),
                false,
                null,
                CaracalCatalogDtos.RateTableListResponse.class,
                "loading rate tables for tenant " + tenantCode);

        if (response == null || response.items() == null) {
            return List.of();
        }
        return response.items();
    }

    @Override
    public CaracalCatalogDtos.RateTableGetResponse fetchRateTable(String tenantCode, String rateTableName) {
        if (tenantCode == null || tenantCode.isBlank() || rateTableName == null || rateTableName.isBlank()) {
            throw new IllegalArgumentException("Tenant code and rate table name must not be blank.");
        }

        return post(
                "/rate-table-get",
                new CaracalCatalogDtos.RateTableGetRequest(tenantCode, rateTableName),
                false,
                null,
                CaracalCatalogDtos.RateTableGetResponse.class,
                "loading rate table " + rateTableName);
    }

    @Override
    public List<CaracalCatalogDtos.SimulatedProductAvailability> simulateAvailability(
            String tenantCode,
            String siteCode,
            Map<String, String> dimensions) {
        if (tenantCode == null || tenantCode.isBlank() || siteCode == null || siteCode.isBlank()) {
            throw new IllegalArgumentException("Tenant code and site code must not be blank.");
        }

        CaracalCatalogDtos.AvailabilitySimulateRequest request = new CaracalCatalogDtos.AvailabilitySimulateRequest(
                tenantCode,
                siteCode,
                null,
                null,
                null,
                dimensions);

        CaracalCatalogDtos.AvailabilitySimulateResponse response = post(
                "/availability-simulate",
                request,
                false,
                null,
                CaracalCatalogDtos.AvailabilitySimulateResponse.class,
                "simulating availability for site " + siteCode);

        if (response == null || response.products() == null) {
            return List.of();
        }
        return response.products();
    }

    // ==============================================================================
    // Epic 3: Authoring Sync Mutations (Story 3.4)
    // ==============================================================================

    @Override
    public String updateRateAssignment(
            CaracalCatalogDtos.RateAssignmentUpdateRequest request,
            String actorEmail) {
        CaracalCatalogDtos.CaracalIdResponse response = post(
                "/rate-assignment-update",
                request,
                false,
                actorEmail,
                CaracalCatalogDtos.CaracalIdResponse.class,
                "updating rate assignment " + request.assignmentId());

        return response != null ? response.id() : request.assignmentId();
    }

    @Override
    public String createWhenCondition(
            CaracalCatalogDtos.WhenConditionCreateRequest request,
            String actorEmail) {
        CaracalCatalogDtos.CaracalIdResponse response = post(
                "/when-condition-create",
                request,
                false,
                actorEmail,
                CaracalCatalogDtos.CaracalIdResponse.class,
                "creating when-condition " + request.name());

        if (response == null || response.id() == null) {
            throw new CaracalApiException("Caracal did not return an ID for when-condition " + request.name());
        }
        return response.id();
    }

    @Override
    public String createRateAssignment(
            CaracalCatalogDtos.RateAssignmentCreateRequest request,
            String actorEmail) {
        CaracalCatalogDtos.CaracalIdResponse response = post(
                "/rate-assignment-create",
                request,
                false,
                actorEmail,
                CaracalCatalogDtos.CaracalIdResponse.class,
                "creating rate assignment for product " + request.productCode());

        return response != null ? response.id() : null;
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
                .attribute("useSystemActor", useSystemActor)
                .retrieve()
                .body(responseType));
    }

    private <T> T post(
            String uri,
            Object body,
            boolean useSystemActor,
            String explicitActor,
            Class<T> responseType,
            String actionDescription) {

        return execute(actionDescription, () -> {
            RestClient.RequestBodySpec spec = caracalRestClient
                    .post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .attribute("useSystemActor", useSystemActor);

            if (explicitActor != null && !explicitActor.isBlank()) {
                spec.attribute("explicitActor", explicitActor);
            }

            return spec.body(body)
                    .retrieve()
                    .body(responseType);
        });
    }

    private <T> T execute(String actionDescription, Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException ex) {
            throw new CaracalUnavailableException(
                    "Unable to reach the Caracal platform. Please verify the service is running.", ex);
        } catch (RestClientResponseException ex) {
            throw new CaracalApiException(
                    "Caracal API error (" + ex.getStatusCode().value() + ") while " + actionDescription + ".", ex);
        } catch (RestClientException ex) {
            throw new CaracalApiException(
                    "Received an invalid response format from Caracal while " + actionDescription + ".", ex);
        }
    }
}