package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationChangeTrackerTest {

    private ConfigurationChangeTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new ConfigurationChangeTracker();
    }

    @Test
    void initialRevisionIsZero() {
        assertEquals("0",
                tracker.getCurrentRevision("NYW_SANDBOX"));
    }

    @Test
    void configurationChangeIncrementsRevision() {
        tracker.markChanged("NYW_SANDBOX");

        assertEquals("1",
                tracker.getCurrentRevision("NYW_SANDBOX"));
    }

    @Test
    void multipleChangesIncrementRevision() {
        tracker.markChanged("NYW_SANDBOX");
        tracker.markChanged("NYW_SANDBOX");

        assertEquals("2",
                tracker.getCurrentRevision("NYW_SANDBOX"));
    }

    @Test
    void tenantsHaveIndependentRevisions() {
        tracker.markChanged("NYW_SANDBOX");

        assertEquals("1",
                tracker.getCurrentRevision("NYW_SANDBOX"));

        assertEquals("0",
                tracker.getCurrentRevision("OTHER_TENANT"));
    }

    @Test
    void tenantCodeIsNormalized() {
        tracker.markChanged(" nyw_sandbox ");

        assertEquals("1",
                tracker.getCurrentRevision("NYW_SANDBOX"));
    }

    @Test
    void invalidTenantIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> tracker.markChanged(null));

        assertThrows(IllegalArgumentException.class,
                () -> tracker.getCurrentRevision(""));
    }

    @Test
void revisionIncrementsAfterCommit() {

    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);

    try {
        tracker.markChanged("NYW_SANDBOX");

        assertEquals("0",
                tracker.getCurrentRevision("NYW_SANDBOX"));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        assertEquals("1",
                tracker.getCurrentRevision("NYW_SANDBOX"));

    } finally {
        TransactionSynchronizationManager.clear();
    }
}

@Test
void rollbackDoesNotIncrementRevision() {

    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);

    try {
        tracker.markChanged("NYW_SANDBOX");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK));

        assertEquals("0",
                tracker.getCurrentRevision("NYW_SANDBOX"));

    } finally {
        TransactionSynchronizationManager.clear();
    }
}
}