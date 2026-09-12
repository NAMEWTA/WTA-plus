package org.namewta.third.port;

import org.namewta.third.domain.ThirdCredential;

import java.util.List;

/** Read boundary for effective provider and endpoint credentials. */
public interface ThirdCredentialStore {
    List<ThirdCredential> findByScopes(Long providerId, Long endpointId);
}
