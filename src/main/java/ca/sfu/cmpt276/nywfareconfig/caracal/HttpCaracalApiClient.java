package ca.sfu.cmpt276.nywfareconfig.caracal;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import ca.sfu.cmpt276.nywfareconfig.caracal.dto.CaracalCatalogDtos;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalUnavailableException;
import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import tools.jackson.databind.ObjectMapper;

@Service
public class HttpCaracalApiClient implements CaracalApiClient {

    private RestClient caracalRestClient;
    private ActorService actorService;
    private ObjectMapper objectMapper;

    public HttpCaracalApiClient(RestClient caracalRestClient,
            ActorService actorService, ObjectMapper objectMapper) {
        this.caracalRestClient = caracalRestClient;
        this.actorService = actorService;
        this.objectMapper = objectMapper;
    }

    // Queries GET /functions/tenant-list using X-Actor: system
    // to discover active platform tenants.
    @Override
    public List<CaracalCatalogDtos.TenantSummary> fetchTenants() {
        try {
            CaracalCatalogDtos.TenantListResponse response = caracalRestClient
                    .get()
                    .uri("/tenant-list")
                    .retrieve()
                    .body(new ParameterizedTypeReference<CaracalCatalogDtos.TenantListResponse>() {
                    });
            return response.tenants();
        } catch (ResourceAccessException ex) {
            throw new CaracalUnavailableException(
                    "Unable to reach the Caracal platform. Please verify the service is running.", ex);
        } catch (RestClientResponseException ex) {
            throw new CaracalApiException(
                    "Caracal API error (" + ex.getStatusCode().value() + ") while loading tenants.", ex);
        } catch (RestClientException ex) {
            throw new CaracalApiException(
                    "Received an invalid response format from the Caracal tenant service.", ex);
        }
    }

    // Queries GET /functions/tenant-get?tenantCode={tenantCode} using X-Actor: operator:<email>
    // to discover specific tenant details
    @Override
    public CaracalCatalogDtos.TenantDetail fetchTenant(String tenantCode) {
        try {
            CaracalCatalogDtos.TenantDetail response = caracalRestClient
                    .get()
                    .uri("/tenant-get?tenantCode={tenantCode}", tenantCode)
                    .retrieve()
                    .body(new ParameterizedTypeReference<CaracalCatalogDtos.TenantDetail>() {

                    });
            return response;
        } catch (ResourceAccessException ex) {
            throw new CaracalUnavailableException(
                    "Unable to reach the Caracal platform. Please verify the service is running.", ex);
        } catch (RestClientResponseException ex) {
            throw new CaracalApiException(
                    "Caracal API error (" + ex.getStatusCode().value() + ") while loading tenants.", ex);
        } catch (RestClientException ex) {
            throw new CaracalApiException(
                    "Received an invalid response format from the Caracal tenant service.", ex);
        }
    }
}
