package org.namewta.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 授权管理对象 sys_client
 *
 * @author Michelle.Chung
 * @date 2023-05-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_client")
public class SysClient extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 客户端id
     */
    private String clientId;

    /**
     * 客户端key
     */
    private String clientKey;

    /**
     * 客户端秘钥
     */
    private String clientSecret;

    /**
     * 授权类型
     */
    private String grantType;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 允许访问路径
     */
    private String accessPath;

    /**
     * IP白名单
     */
    private String ipWhitelist;

    /**
     * token活跃超时时间
     */
    private Long activeTimeout;

    /**
     * token固定超时时间
     */
    private Long timeout;

    /**
     * 登录域ID
     */
    private Long userTypeId;

    /**
     * 是否开放公开注册
     */
    private Boolean registerEnabled;

    /**
     * 默认角色ID
     */
    private Long defaultRoleId;

    /**
     * 是否启用 SSO 接入
     */
    private Boolean ssoEnabled;

    /**
     * 登录模式 local / sso / both
     */
    private String ssoAuthMode;

    /**
     * OAuth 客户端类型 public / confidential
     */
    private String ssoClientKind;

    /**
     * SSO 精确回调白名单
     */
    private String ssoRedirectUris;

    /**
     * 是否强制 PKCE
     */
    private Boolean ssoPkceRequired;

    /**
     * 是否自动同意
     */
    private Boolean ssoAutoConsent;

    /**
     * SSO 默认 scope
     */
    private String ssoScope;

    /**
     * SSO 客户端密钥哈希
     */
    private String ssoSecretHash;

    /**
     * SSO 密钥最近轮换时间
     */
    private java.time.LocalDateTime ssoSecretRotatedAt;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;


}
