package org.namewta.system.openapi.credential.crypto;

import org.namewta.common.openapi.config.properties.OpenApiProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Runtime property-backed KEK provider. No key material has a source-code default.
 */
@Component
@ConditionalOnProperty(prefix = "openapi", name = "enabled", havingValue = "true")
public class PropertyOpenApiKekProvider implements OpenApiKekProvider {

    private static final Pattern VERSION = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private final OpenApiProperties properties;
    private final Environment environment;

    public PropertyOpenApiKekProvider(OpenApiProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public String activeVersion() {
        return validateVersion(properties.getKekVersion());
    }

    @Override
    public byte[] keyForVersion(String version) {
        String validVersion = validateVersion(version);
        String encoded = validVersion.equals(properties.getKekVersion())
            ? properties.getKek()
            : environment.getProperty("openapi.keks." + validVersion);
        if (encoded == null || encoded.isBlank()) {
            throw new OpenApiCredentialCryptoException();
        }
        try {
            byte[] key = Base64.getDecoder().decode(encoded);
            if (key.length != 32) {
                java.util.Arrays.fill(key, (byte) 0);
                throw new OpenApiCredentialCryptoException();
            }
            return key;
        } catch (IllegalArgumentException e) {
            throw new OpenApiCredentialCryptoException(e);
        }
    }

    private static String validateVersion(String version) {
        if (version == null || !VERSION.matcher(version).matches()) {
            throw new OpenApiCredentialCryptoException();
        }
        return version;
    }
}
