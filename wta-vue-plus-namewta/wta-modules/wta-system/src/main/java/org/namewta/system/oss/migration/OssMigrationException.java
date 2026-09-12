package org.namewta.system.oss.migration;

public class OssMigrationException extends RuntimeException {

    private final OssMigrationError error;

    public OssMigrationException(OssMigrationError error, String message) {
        super(message);
        this.error = error;
    }

    public OssMigrationException(OssMigrationError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public OssMigrationError error() {
        return error;
    }
}
