package org.namewta.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.ToString;
import org.namewta.system.domain.SysAuthRegistration;
import org.namewta.common.core.validate.AddGroup;
import org.namewta.common.core.validate.EditGroup;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** 外部身份接入响应；不包含密钥或密文。 */
@Data
@AutoMapper(target = SysAuthRegistration.class)
public class SysAuthRegistrationVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 主键。 */
    private Long id;
    /** 编辑/删除时用于检测并发修改的版本。 */
    private Long version;
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
    /** 公开扩展参数；不得含密钥。 */
    private Map<String, String> options;
    /** 授权范围。 */
    private List<String> scopes;
    /** 是否已保存密钥。 */
    private boolean secretConfigured;
}
