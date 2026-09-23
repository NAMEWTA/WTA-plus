package org.namewta.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.namewta.common.excel.annotation.ExcelDictFormat;
import org.namewta.common.excel.convert.ExcelDictConvert;
import org.namewta.system.domain.SysClient;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


/**
 * 授权管理视图对象 sys_client
 *
 * @date 2023-05-15
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysClient.class)
public class SysClientVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 客户端id
     */
    @ExcelProperty(value = "客户端id")
    private String clientId;

    /**
     * 客户端key
     */
    @ExcelProperty(value = "客户端key")
    private String clientKey;

    /**
     * 客户端秘钥
     */
    @ExcelProperty(value = "客户端秘钥")
    private String clientSecret;

    /**
     * 授权类型
     */
    private List<String> grantTypeList;

    /**
     * 授权类型
     */
    @ExcelProperty(value = "授权类型")
    private String grantType;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 允许访问路径
     */
    @ExcelProperty(value = "允许访问路径")
    private String accessPath;

    /**
     * 允许访问路径列表
     */
    private List<String> accessPathList;

    /**
     * IP白名单
     */
    @ExcelProperty(value = "IP白名单")
    private String ipWhitelist;

    /**
     * IP白名单列表
     */
    private List<String> ipWhitelistList;

    /**
     * token活跃超时时间
     */
    @ExcelProperty(value = "token活跃超时时间")
    private Long activeTimeout;

    /**
     * token固定超时时间
     */
    @ExcelProperty(value = "token固定超时时间")
    private Long timeout;

    /**
     * 登录域ID
     */
    private Long userTypeId;

    /**
     * 登录域编码
     */
    @ExcelProperty(value = "登录域编码")
    private String userTypeCode;

    /**
     * 登录域名称
     */
    @ExcelProperty(value = "登录域名称")
    private String userTypeName;

    /**
     * 是否开放公开注册
     */
    @ExcelProperty(value = "公开注册", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "true=开启,false=关闭")
    private Boolean registerEnabled;

    /**
     * 默认角色ID
     */
    private Long defaultRoleId;

    /**
     * 默认角色名称
     */
    @ExcelProperty(value = "默认角色")
    private String defaultRoleName;

    /**
     * 是否启用 SSO 接入
     */
    @ExcelProperty(value = "SSO 接入", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "true=开启,false=关闭")
    private Boolean ssoEnabled;

    /**
     * 登录模式 local / sso / both
     */
    @ExcelProperty(value = "登录模式")
    private String ssoAuthMode;

    /**
     * OAuth 客户端类型 public / confidential
     */
    @ExcelProperty(value = "SSO 客户端类型")
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
     * 是否已配置 SSO 密钥哈希
     */
    private Boolean ssoSecretConfigured;

    /**
     * 仅创建或轮换时回显一次的明文密钥
     */
    private String ssoSecretOnce;

    /**
     * 密钥最近轮换时间
     */
    private java.time.LocalDateTime ssoSecretRotatedAt;

    /**
     * 查询映射用，响应前清除
     */
    @JsonIgnore
    private String ssoSecretHash;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
    private String status;


}
