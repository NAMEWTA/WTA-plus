package org.namewta.oidc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/** OIDC 持久化事实，凭据完整内容仅允许保存认证加密密文。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oidc_authorization")
public class OidcAuthorization extends BaseEntity {
    @TableId private Long authorizationId;
    private String frameworkId;
    private Long applicationId;
    private Long userId;
    private String sessionHash;
    private String subject;
    private String allowedFieldsJson;
    private String authorizedScopes;
    private String status;
    private Boolean sessionClosed;
    private String codeHash;
    private LocalDateTime codeExpiresAt;
    private Boolean codeConsumed;
    private String accessTokenHash;
    private LocalDateTime accessExpiresAt;
    private String idTokenHash;
    private String authorizationJson;
    private LocalDateTime expiresAt;
    @Version private Integer version;
    @TableLogic private String delFlag;
}
