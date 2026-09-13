package org.namewta.sso.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 换票结果，字段名对齐现有 LoginVo。
 */
@Data
public class SsoTokenVo {

    /**
     * 业务 Sa-Token
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 过期秒数
     */
    @JsonProperty("expire_in")
    private Long expireIn;

    /**
     * 目标业务 Client
     */
    @JsonProperty("client_id")
    private String clientId;
}
