package org.namewta.oidc.domain;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.namewta.common.mybatis.core.domain.BaseEntity;

/** OIDC 持久化事实，凭据完整内容仅允许保存认证加密密文。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oidc_application")
public class OidcApplication extends BaseEntity {
    @TableId private Long applicationId;
    private String name;
    private String clientId;
    private String clientSecretHash;
    private String redirectUrisJson;
    private String postLogoutRedirectUrisJson;
    private String allowedFieldsJson;
    private String clientAuthenticationMethod;
    private Boolean pkceRequired;
    private Boolean enabled;
    @Version private Integer version;
    @TableLogic private String delFlag;
}
