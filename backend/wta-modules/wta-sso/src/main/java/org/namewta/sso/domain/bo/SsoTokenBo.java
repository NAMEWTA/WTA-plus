package org.namewta.sso.domain.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 换票输入。
 */
@Data
public class SsoTokenBo {

    @JsonProperty("grant_type")
    private String grantType;

    private String code;

    @JsonProperty("redirect_uri")
    private String redirectUri;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("code_verifier")
    private String codeVerifier;

    private String token;
}
