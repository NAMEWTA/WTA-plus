package org.namewta.common.social.oidc;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 一次实时 Discovery 观察结果，仅包含公开端点和声明的能力，不证明客户端凭据或完整登录可用。
 *
 * <p>未声明的能力列表为空；客户端认证方式未声明时按协议使用 client_secret_basic。
 * PKCE 列表为空表示未声明，不能据此认定提供方拒绝 S256。</p>
 */
public record OidcDiscoveryDiagnostics(
        OidcMetadata metadata,
        List<String> scopesSupported,
        List<String> responseTypesSupported,
        List<String> codeChallengeMethodsSupported,
        List<String> idTokenSigningAlgValuesSupported,
        List<String> tokenEndpointAuthMethodsSupported,
        boolean backchannelLogoutSupported,
        boolean backchannelLogoutSessionSupported,
        Instant checkedAt) {

    /** 固定公开能力快照，防止调用方修改后影响诊断展示。 */
    public OidcDiscoveryDiagnostics {
        Objects.requireNonNull(metadata, "metadata");
        scopesSupported = List.copyOf(scopesSupported);
        responseTypesSupported = List.copyOf(responseTypesSupported);
        codeChallengeMethodsSupported = List.copyOf(codeChallengeMethodsSupported);
        idTokenSigningAlgValuesSupported = List.copyOf(idTokenSigningAlgValuesSupported);
        tokenEndpointAuthMethodsSupported = List.copyOf(tokenEndpointAuthMethodsSupported);
        Objects.requireNonNull(checkedAt, "checkedAt");
    }

    /** 是否声明支持当前 RP 使用的纯授权码响应。 */
    public boolean supportsAuthorizationCode() {
        return responseTypesSupported.contains("code");
    }

    /** 是否明确声明支持 S256；false 也可能表示元数据没有声明 PKCE 能力。 */
    public boolean supportsPkceS256() {
        return codeChallengeMethodsSupported.contains("S256");
    }

    /** 是否声明支持当前 RP 验签算法。 */
    public boolean supportsRs256() {
        return idTokenSigningAlgValuesSupported.contains("RS256");
    }

    /** 是否支持 HTTP Basic 方式的机密客户端认证。 */
    public boolean supportsClientSecretBasic() {
        return tokenEndpointAuthMethodsSupported.contains("client_secret_basic");
    }

    /** 是否支持表单方式的机密客户端认证。 */
    public boolean supportsClientSecretPost() {
        return tokenEndpointAuthMethodsSupported.contains("client_secret_post");
    }

    /** 提供方是否公布 RP 发起退出端点；实际退出仍需当前接入登记退出回跳。 */
    public boolean rpInitiatedLogoutSupported() {
        return metadata.endSessionEndpoint() != null;
    }
}
