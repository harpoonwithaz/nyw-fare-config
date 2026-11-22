package ca.sfu.cmpt276.nywfareconfig.publishing.dto;

import java.util.List;

/**
 * Response returned by our Spring Boot audit history API.
 *
 * Contains audit records for the selected tenant.
 */
public record AuditHistoryResponse(String tenantCode, List<AuditLogEntry> entries) {
}
