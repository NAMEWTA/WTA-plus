package org.namewta.common.notify.model;

/**
 * Controls which request data may be retained by notification audit adapters.
 * REDACT_SENSITIVE masks request and result data in published monitoring events only;
 * the original request still reaches the provider and the idempotency coordinator.
 */
public enum NotifyAuditPolicy {
    FULL,
    REDACT_SENSITIVE
}
