package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import ca.sfu.cmpt276.nywfareconfig.publishing.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/publishing/history")
public class HistoryApiController {

    private final HistoryService historyService;

    public HistoryApiController(HistoryService historyService) {
        this.historyService = historyService;
    }

    /**
    * Returns publication history for the selected tenant.
    */
    @GetMapping("/publications")
    public ResponseEntity<JsonNode> getPublications(
            @RequestParam String tenantCode) {

        JsonNode response = historyService.getPublicationHistory(tenantCode);
        return ResponseEntity.ok(response);
    }

    /**
    * Returns recent audit log entries for the selected tenant.
    */
    @GetMapping("/audit")
    public ResponseEntity<JsonNode> getAuditHistory(
            @RequestParam String tenantCode) {

        JsonNode response = historyService.getAuditHistory(tenantCode);
        return ResponseEntity.ok(response);
    }
}
