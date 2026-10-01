package org.namewta.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.namewta.common.mybatis.core.domain.BaseEntity;
import java.io.Serial;

/** 外部身份接入持久化对象。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_auth_registration")
public class SysAuthRegistration extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 主键。 */
    @TableId("auth_registration_id")
    private Long id;
    /** 身份源主键。 */
    private Long providerId;
    /** 本平台业务客户端标识。 */
    private String businessClientId;
    /** 外部平台分配的客户端标识。 */
    private String externalClientId;
    /** 精确登录回调地址。 */
    private String redirectUri;
    /** 精确退出回调地址。 */
    private String postLogoutRedirectUri;
    /** 首次登录策略 BIND_ONLY 或 AUTO_REGISTER。 */
    private String firstLoginPolicy;
    /** 是否启用。 */
    private Boolean enabled;
    /** 公开扩展参数 JSON。 */
    @TableField("options")
    private String optionsJson;
    /** 加密后的外部客户端密钥。 */
    @ToString.Exclude
    private String clientSecretCiphertext;
    /** 授权范围 JSON。 */
    @TableField("scopes")
    private String scopesJson;
    /** 乐观锁版本。 */
    @Version
    private Long version;
    /** 逻辑删除标志。 */
    @TableLogic
    private String delFlag;
}
