package org.namewta.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.namewta.common.core.validate.AddGroup;
import org.namewta.common.core.validate.EditGroup;
import org.namewta.system.domain.SysClient;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 授权管理业务对象 sys_client
 *
 * @date 2023-05-15
 */
@Data
@AutoMapper(target = SysClient.class, reverseConvertGenerate = false)
public class SysClientBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @NotNull(message = "id不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 客户端id
     */
    private String clientId;

    /**
     * 客户端key
     */
    @NotBlank(message = "客户端key不能为空", groups = {AddGroup.class, EditGroup.class})
    private String clientKey;

    /**
     * 客户端秘钥
     */
    @NotBlank(message = "客户端秘钥不能为空", groups = {AddGroup.class, EditGroup.class})
    private String clientSecret;

    /**
     * 授权类型
     */
    @NotNull(message = "授权类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private List<String> grantTypeList;

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
     * 允许访问路径列表
     */
    private List<String> accessPathList;

    /**
     * IP白名单
     */
    private String ipWhitelist;

    /**
     * IP白名单列表
     */
    private List<String> ipWhitelistList;

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
    @NotNull(message = "登录域不能为空", groups = {AddGroup.class, EditGroup.class})
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
     * SSO 精确回调列表
     */
    private List<String> ssoRedirectUriList;

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
     * 写入时的 SSO 密钥明文，不会回读
     */
    private String ssoSecret;

    /**
     * 仅本次响应可回显的明文密钥
     */
    private String ssoSecretOnce;

    /**
     * 状态（0正常 1停用）
     */
    private String status;


}
