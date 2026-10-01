package org.namewta.system.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/** 一次性显式导入请求；默认预览，不读取服务器启动 YAML。 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class ExternalAuthLegacyImportBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 目标业务客户端 OAuth 标识。 */
    @NotBlank
    private String businessClientId;
    /** 首次登录策略，默认只绑定。 */
    private String firstLoginPolicy = "BIND_ONLY";
    /** 只有明确为 false 才实际写入。 */
    private Boolean dryRun = true;
    /** 旧 justauth.type 内容；空白示例会被跳过。 */
    @NotEmpty
    @Size(max = 32)
    @Valid
    private Map<String, ExternalAuthLegacyProviderBo> type;
}
