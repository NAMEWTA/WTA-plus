package org.namewta.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 用户显式提交的旧 justauth.type 单项；旧 clientId 接受为 externalClientId 别名。 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class ExternalAuthLegacyProviderBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 外部客户端标识，兼容旧配置 clientId。 */
    @JsonAlias({"clientId", "client-id"})
    private String externalClientId;
    /** 密钥只接受写入，不参与序列化。 */
    @JsonAlias("client-secret")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String clientSecret;
    /** 精确回调地址。 */
    @JsonAlias("redirect-uri")
    private String redirectUri;
    /** 申请的权限范围。 */
    private List<String> scopes;
    /** QQ unionId 选项。 */
    @JsonAlias("union-id")
    private Boolean unionId;
    /** Microsoft 租户。 */
    @JsonAlias("tenant-id")
    private String tenantId;
    /** Coding 企业名称。 */
    @JsonAlias("coding-group-name")
    private String codingGroupName;
    /** 支付宝公钥。 */
    @JsonAlias("alipay-public-key")
    private String alipayPublicKey;
    /** 企业微信应用标识。 */
    @JsonAlias("agent-id")
    private String agentId;
    /** 旧独立 API 密钥不能放入公开选项，预览会提示无法自动迁移。 */
    @JsonAlias("stack-overflow-key")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String stackOverflowKey;
    /** 设备标识。 */
    @JsonAlias("device-id")
    private String deviceId;
    /** 客户端系统类型。 */
    @JsonAlias("client-os-type")
    private String clientOsType;
    /** 自托管授权服务器地址。 */
    @JsonAlias("server-url")
    private String serverUrl;
}
