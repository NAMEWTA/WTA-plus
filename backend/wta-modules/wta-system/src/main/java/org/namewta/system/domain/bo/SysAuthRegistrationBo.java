package org.namewta.system.domain.bo;

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

/** 外部身份接入编辑参数。 */
@Data
@AutoMapper(target = SysAuthRegistration.class, reverseConvertGenerate = false)
public class SysAuthRegistrationBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 主键。 */
    @NotNull(groups = EditGroup.class)
    private Long id;
    /** 编辑/删除时用于检测并发修改的版本。 */
    @NotNull(groups = EditGroup.class)
    @PositiveOrZero(groups = EditGroup.class)
    private Long version;
    /** 身份源主键。 */
    @NotNull(groups = {AddGroup.class, EditGroup.class})
    private Long providerId;
    /** 本平台业务客户端标识。 */
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    @Size(max = 200, groups = {AddGroup.class, EditGroup.class})
    private String businessClientId;
    /** 外部平台分配的客户端标识。 */
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    @Size(max = 200, groups = {AddGroup.class, EditGroup.class})
    private String externalClientId;
    /** 精确登录回调地址。 */
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    @Size(max = 2048, groups = {AddGroup.class, EditGroup.class})
    private String redirectUri;
    /** 精确退出回调地址。 */
    @Size(max = 2048, groups = {AddGroup.class, EditGroup.class})
    private String postLogoutRedirectUri;
    /** 首次登录策略 BIND_ONLY 或 AUTO_REGISTER。 */
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    @Size(max = 200, groups = {AddGroup.class, EditGroup.class})
    private String firstLoginPolicy;
    /** 是否启用。 */
    @NotNull(groups = {AddGroup.class, EditGroup.class})
    private Boolean enabled;
    /** 公开扩展参数；不得含密钥。 */
    private Map<String, String> options;
    /** 授权范围。 */
    private List<String> scopes;
    /** 新增或替换的密钥；编辑留空保留原值。 */
    @Size(max = 4096, groups = {AddGroup.class, EditGroup.class})
    @ToString.Exclude
    private String clientSecret;
}
