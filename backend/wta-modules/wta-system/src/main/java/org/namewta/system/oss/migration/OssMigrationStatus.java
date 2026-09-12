package org.namewta.system.oss.migration;

public enum OssMigrationStatus {
    PENDING,
    RUNNING,
    FAILED,
    CLEANUP_ELIGIBLE,
    COMPLETED,
    ROLLED_BACK;

    public boolean terminal() {
        return this == COMPLETED || this == ROLLED_BACK;
    }
}
