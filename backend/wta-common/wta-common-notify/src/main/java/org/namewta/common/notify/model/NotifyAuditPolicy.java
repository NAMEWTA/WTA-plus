package org.namewta.common.notify.model;

/**
 * Controls which request data may be retained by notification audit adapters.
 */
public enum NotifyAuditPolicy {
    FULL,
    REDACT_SENSITIVE
}
