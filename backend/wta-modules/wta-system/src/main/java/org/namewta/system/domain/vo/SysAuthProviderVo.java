package org.namewta.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.ToString;
import org.namewta.system.domain.SysAuthProvider;
import org.namewta.common.core.validate.AddGroup;
import org.namewta.common.core.validate.EditGroup;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** 外部身份源响应；不包含密钥或密文。 */
@Data
@AutoMapper(target = SysAuthProvider.class)
public class SysAuthProviderVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 主键。 */
    private Long id;
    /** 编辑/删除时用于检测并发修改的版本。 */
    private Long version;
    /** 身份源稳定标识。 */
    private String providerKey;
    /** 显示名称。 */
    private String name;
    /** 组件图标。 */
    private String icon;
    /** OIDC 或 JustAuth source。 */
    private String protocol;
    /** OIDC 发行者地址。 */
    private String issuer;
    /** 是否启用。 */
    private Boolean enabled;
    /** 公开扩展参数；不得含密钥。 */
    private Map<String, String> options;
}
