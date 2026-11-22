package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;


@Service
public class HistoryService {
    private final CaracalApiClient caracalApiClient;
    
    public HistoryService(CaracalApiClient caracalApiClient) {
        this.caracalApiClient = caracalApiClient;
    }
    /**
     * Retrieve the publication history for a tenant.
     *
     * Uses the shared Caracal API client to fetch publication
     * records directly from the platform.
     */
    public JsonNode getPublicationHistory(String tenantCode) {
        return caracalApiClient.fetchPublicationHistory(tenantCode);
    }

    /**
     * Retrieve the 20 most recent audit log entries for a tenant.
     *
     * Uses the shared Caracal API client to fetch
     * audit records directly from the platform.
     */
    public JsonNode getAuditHistory(String tenantCode) {
        return caracalApiClient.fetchAuditHistory(tenantCode, 20);
    }
}
