package org.namewta.system.openapi.credential.crypto;

/**
 * Non-diagnostic public crypto failure. Secret-provider details remain outside responses.
 */
public class OpenApiCredentialCryptoException extends RuntimeException {

    public OpenApiCredentialCryptoException() {
        super("OpenAPI credential crypto unavailable");
    }

    public OpenApiCredentialCryptoException(Throwable cause) {
        super("OpenAPI credential crypto unavailable", cause);
    }
}
