package org.namewta.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.namewta.common.mybatis.core.domain.BaseEntity;
import java.io.Serial;

/** 外部身份源持久化对象。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_auth_provider")
public class SysAuthProvider extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 主键。 */
    @TableId("auth_provider_id")
    private Long id;
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
    /** 公开扩展参数 JSON。 */
    @TableField("options")
    private String optionsJson;
    /** 乐观锁版本。 */
    @Version
    private Long version;
    /** 逻辑删除标志。 */
    @TableLogic
    private String delFlag;
}
