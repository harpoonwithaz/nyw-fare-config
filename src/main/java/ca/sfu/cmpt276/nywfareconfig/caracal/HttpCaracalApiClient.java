package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.util.List;
import java.util.function.Supplier;

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

    // Queries GET /functions/site-list to populate O-D terminals and Zonal station
    // directories.
    @Override
    public List<CaracalCatalogDtos.SiteSummary> fetchSites(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
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
        if (tenantCode == null || tenantCode.isBlank()) {
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