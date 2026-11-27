package ca.sfu.cmpt276.nywfareconfig.publishing.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents one audit record returned by Caracal.
 */
public record AuditLogEntry(
        String function,
        String actorRef,
        String at,
        JsonNode after
) {
}

