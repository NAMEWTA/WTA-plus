package org.namewta.oidc.domain.bo;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;

/** 应用登记和编辑输入；凭据与 Client ID 始终由服务端生成。 */
@Data
public class OidcApplicationBo {
    /** 应用主键以字符串传输，防止浏览器丢失雪花 ID 精度。 */
    @io.swagger.v3.oas.annotations.media.Schema(type = "string")
    private Long applicationId;

    /** 修改时必填的乐观版本；新建不需要提供。 */
    private Integer version;

    @NotBlank
    @Size(max = 128)
    private String name;

    @NotEmpty
    @Size(max = 50)
    private List<String> redirectUris;

    @Size(max = 50)
    private List<String> postLogoutRedirectUris = List.of();

    /** 可披露字段代码；未提供时只使用基础默认集合，空集合表示仅公开 sub。 */
    private List<String> allowedFields;

    /** 机密客户端认证方式，默认 HTTP Basic，可显式改为表单密钥。 */
    private String clientAuthenticationMethod = "client_secret_basic";

    /** 默认要求 S256 PKCE，只有管理员显式关闭时才允许省略。 */
    private Boolean pkceRequired = true;
}
