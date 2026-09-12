package org.namewta.system.openapi.credential.crypto;

/**
 * Supplies versioned AES-256 key-encryption keys from runtime-only configuration.
 */
public interface OpenApiKekProvider {

    String activeVersion();

    byte[] keyForVersion(String version);
}
