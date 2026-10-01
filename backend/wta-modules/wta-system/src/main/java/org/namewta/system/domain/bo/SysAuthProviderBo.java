package org.namewta.system.domain.bo;

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

/** 外部身份源编辑参数。 */
@Data
@AutoMapper(target = SysAuthProvider.class, reverseConvertGenerate = false)
public class SysAuthProviderBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 主键。 */
    @NotNull(groups = EditGroup.class)
    private Long id;
    /** 编辑/删除时用于检测并发修改的版本。 */
    @NotNull(groups = EditGroup.class)
    @PositiveOrZero(groups = EditGroup.class)
    private Long version;
    /** 身份源稳定标识。 */
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    @Size(max = 200, groups = {AddGroup.class, EditGroup.class})
    private String providerKey;
    /** 显示名称。 */
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    @Size(max = 200, groups = {AddGroup.class, EditGroup.class})
    private String name;
    /** 组件图标。 */
    @Size(max = 200, groups = {AddGroup.class, EditGroup.class})
    private String icon;
    /** OIDC 或 JustAuth source。 */
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    @Size(max = 200, groups = {AddGroup.class, EditGroup.class})
    private String protocol;
    /** OIDC 发行者地址。 */
    @Size(max = 2048, groups = {AddGroup.class, EditGroup.class})
    private String issuer;
    /** 是否启用。 */
    @NotNull(groups = {AddGroup.class, EditGroup.class})
    private Boolean enabled;
    /** 公开扩展参数；不得含密钥。 */
    private Map<String, String> options;
}
