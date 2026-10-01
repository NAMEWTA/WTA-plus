package org.namewta.oidc.adapter.api;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.port.OidcKeyMaterialPort;
import org.namewta.oidc.usecase.OidcKeyMaterialUseCase;
import org.springframework.stereotype.Component;

/** 内部密钥读取入口，经用例进入版本化材料服务。 */
@Component
@RequiredArgsConstructor
public class OidcKeyMaterialApiAdapter implements OidcKeyMaterialPort {
    private final OidcKeyMaterialUseCase useCase;

    @Override
    public String activeKid(String kind) {
        return useCase.activeKid(kind);
    }

    @Override
    public String material(String kind, String kid) {
        return useCase.material(kind, kid);
    }

    @Override
    public JWKSet publicKeys() {
        return useCase.publicKeys();
    }

    @Override
    public RSAKey signingKey() {
        return useCase.signingKey();
    }
}
