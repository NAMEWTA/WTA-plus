package org.namewta.common.openapi.session;

import java.util.Objects;

/**
 * Credential identity that may only be constructed after request verification succeeds.
 */
public record VerifiedOpenApiIdentity(Long credentialId, Long ownerUserId) {

    public VerifiedOpenApiIdentity {
        Objects.requireNonNull(credentialId, "credentialId");
        Objects.requireNonNull(ownerUserId, "ownerUserId");
    }

}
