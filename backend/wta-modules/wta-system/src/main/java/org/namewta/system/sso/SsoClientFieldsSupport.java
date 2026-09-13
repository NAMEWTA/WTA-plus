package org.namewta.system.sso;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.system.domain.SysClient;
import org.namewta.system.domain.bo.SysClientBo;
import org.namewta.system.domain.vo.SysClientVo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 将 SSO 分组字段写入 sys_client，并保证密钥不以明文落库。
 */
public final class SsoClientFieldsSupport {

    private static final Set<String> AUTH_MODES = Set.of("local", "sso", "both");
    private static final Set<String> CLIENT_KINDS = Set.of("public", "confidential");

    private SsoClientFieldsSupport() {
    }

    /**
     * 在持久化映射前校验 SSO 输入。
     *
     * @param bo 管理面输入
     */
    public static void validate(SysClientBo bo) {
        if (bo == null) {
            return;
        }
        boolean ssoEnabled = Boolean.TRUE.equals(bo.getSsoEnabled());
        String authMode = normalizeToken(bo.getSsoAuthMode(), ssoEnabled ? "both" : "local");
        if (!AUTH_MODES.contains(authMode)) {
            throw new ServiceException("authMode 只允许 local、sso 或 both");
        }
        String kind = normalizeToken(bo.getSsoClientKind(), "public");
        if (!CLIENT_KINDS.contains(kind)) {
            throw new ServiceException("SSO 客户端类型只允许 public 或 confidential");
        }
        List<String> redirects = SsoRedirectUris.parse(bo.getSsoRedirectUris(), bo.getSsoRedirectUriList());
        if (ssoEnabled) {
            SsoRedirectUris.validateExact(redirects);
        } else if (!redirects.isEmpty()) {
            for (String uri : redirects) {
                SsoRedirectUris.validateOne(uri);
            }
        }
    }

    /**
     * 把管理面输入应用到实体。
     *
     * @param bo           输入
     * @param entity       待写入实体
     * @param existingHash 更新前已存哈希，新增时为空
     * @return 仅本次可回显的明文密钥；未签发时为空
     */
    public static String apply(SysClientBo bo, SysClient entity, String existingHash) {
        validate(bo);
        boolean ssoEnabled = Boolean.TRUE.equals(bo.getSsoEnabled());
        entity.setSsoEnabled(ssoEnabled);
        String authMode = normalizeToken(bo.getSsoAuthMode(), ssoEnabled ? "both" : "local");
        if (!AUTH_MODES.contains(authMode)) {
            throw new ServiceException("authMode 只允许 local、sso 或 both");
        }
        entity.setSsoAuthMode(authMode);
        String kind = normalizeToken(bo.getSsoClientKind(), "public");
        if (!CLIENT_KINDS.contains(kind)) {
            throw new ServiceException("SSO 客户端类型只允许 public 或 confidential");
        }
        entity.setSsoClientKind(kind);
        entity.setSsoPkceRequired(bo.getSsoPkceRequired() == null ? Boolean.TRUE : bo.getSsoPkceRequired());
        entity.setSsoAutoConsent(bo.getSsoAutoConsent() == null ? Boolean.TRUE : bo.getSsoAutoConsent());
        entity.setSsoScope(StringUtils.trim(bo.getSsoScope()));

        List<String> redirects = SsoRedirectUris.parse(bo.getSsoRedirectUris(), bo.getSsoRedirectUriList());
        if (ssoEnabled) {
            SsoRedirectUris.validateExact(redirects);
        } else if (!redirects.isEmpty()) {
            for (String uri : redirects) {
                SsoRedirectUris.validateOne(uri);
            }
        }
        entity.setSsoRedirectUris(SsoRedirectUris.join(redirects));

        String issued = null;
        String plaintext = StringUtils.trim(bo.getSsoSecret());
        boolean confidential = "confidential".equals(kind);
        if (StringUtils.isNotBlank(plaintext)) {
            entity.setSsoSecretHash(SsoSecretHasher.hash(plaintext));
            entity.setSsoSecretRotatedAt(LocalDateTime.now());
            issued = plaintext;
        } else if (confidential && StringUtils.isBlank(existingHash)) {
            issued = SsoSecretHasher.generatePlaintext();
            entity.setSsoSecretHash(SsoSecretHasher.hash(issued));
            entity.setSsoSecretRotatedAt(LocalDateTime.now());
        } else {
            entity.setSsoSecretHash(existingHash);
        }
        if (confidential && StringUtils.isBlank(entity.getSsoSecretHash())) {
            throw new ServiceException("confidential 客户端必须配置 SSO 密钥");
        }
        return issued;
    }

    /**
     * 轮换密钥并返回新明文。
     *
     * @param entity 已加载实体
     * @return 新明文
     */
    public static String rotate(SysClient entity) {
        String issued = SsoSecretHasher.generatePlaintext();
        entity.setSsoSecretHash(SsoSecretHasher.hash(issued));
        entity.setSsoSecretRotatedAt(LocalDateTime.now());
        return issued;
    }

    /**
     * 回填展示字段，永不输出哈希。
     *
     * @param vo     视图
     * @param hashed 是否已配置哈希
     */
    public static void fillView(SysClientVo vo, boolean hashed) {
        if (vo == null) {
            return;
        }
        vo.setSsoSecretHash(null);
        vo.setSsoSecretConfigured(hashed);
        vo.setSsoRedirectUriList(SsoRedirectUris.parse(vo.getSsoRedirectUris(), vo.getSsoRedirectUriList()));
        if (vo.getSsoEnabled() == null) {
            vo.setSsoEnabled(Boolean.FALSE);
        }
        if (StringUtils.isBlank(vo.getSsoAuthMode())) {
            vo.setSsoAuthMode(Boolean.TRUE.equals(vo.getSsoEnabled()) ? "both" : "local");
        }
        if (StringUtils.isBlank(vo.getSsoClientKind())) {
            vo.setSsoClientKind("public");
        }
        if (vo.getSsoPkceRequired() == null) {
            vo.setSsoPkceRequired(Boolean.TRUE);
        }
        if (vo.getSsoAutoConsent() == null) {
            vo.setSsoAutoConsent(Boolean.TRUE);
        }
    }

    private static String normalizeToken(String value, String fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
