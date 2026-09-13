package org.namewta.sso.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * SSO 一次性授权码。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sso_authorization_code")
public class SsoAuthorizationCode extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 授权码主键
     */
    @TableId("authorization_code_id")
    private Long authorizationCodeId;

    /**
     * 一次性授权码
     */
    private String authorizationCode;

    /**
     * 目标业务客户端标识
     */
    private String clientId;

    /**
     * 绑定的回调地址
     */
    private String redirectUri;

    /**
     * PKCE S256 挑战
     */
    private String codeChallenge;

    /**
     * CSRF state
     */
    private String state;

    /**
     * 已认证用户主键
     */
    private Long userId;

    /**
     * 已认证用户名
     */
    private String username;

    /**
     * 是否已消费
     */
    private Boolean consumed;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 乐观锁版本
     */
    @Version
    private Integer version;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
