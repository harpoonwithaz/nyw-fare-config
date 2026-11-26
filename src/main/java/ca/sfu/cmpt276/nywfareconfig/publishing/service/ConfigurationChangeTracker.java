package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ConfigurationChangeTracker {

    private final Map<String, Long> revisions =
            new ConcurrentHashMap<>();

    public String getCurrentRevision(String tenantCode) {

        String key = normalizeTenant(tenantCode);

        return String.valueOf(
                revisions.getOrDefault(key, 0L));
    }

    public void markChanged(String tenantCode) {

        String key = normalizeTenant(tenantCode);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {

            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                throw new IllegalStateException(
                        "Transaction synchronization is unavailable.");
            }

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            incrementRevision(key);
                        }
                    });

        } else {
            incrementRevision(key);
        }
    }

    private void incrementRevision(String tenantCode) {

        revisions.merge(tenantCode, 1L, Long::sum);
    }

    private String normalizeTenant(String tenantCode) {

        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant code is required.");
        }

        return tenantCode.trim().toUpperCase(Locale.ROOT);
    }
}
