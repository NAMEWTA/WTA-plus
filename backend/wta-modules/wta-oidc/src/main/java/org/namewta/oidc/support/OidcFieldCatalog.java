package org.namewta.oidc.support;

import org.namewta.oidc.domain.vo.OidcFieldVo;
import org.namewta.profile.api.domain.*;

import java.util.*;

/** 唯一字段目录；scope 和字段权限分别控制披露。 */
public final class OidcFieldCatalog {
    /** 返回可授权身份字段目录，不返回实际资料。 */
    public static List<OidcFieldVo> fields() {
        List<OidcFieldVo> result =
                new ArrayList<>(
                        List.of(
                                new OidcFieldVo(
                                        "preferred_username",
                                        "登录名",
                                        "profile",
                                        false,
                                        true,
                                        "account"),
                                new OidcFieldVo(
                                        "nickname", "昵称", "profile", false, true, "account"),
                                new OidcFieldVo("picture", "头像", "profile", false, true, "account"),
                                new OidcFieldVo("email", "邮箱", "email", false, true, "account"),
                                new OidcFieldVo(
                                        "phone_number", "手机号", "phone", false, false, "account")));
        for (var field : ProfileDisclosureField.values()) {
            String group = field.profileType() == ProfileType.PERSON ? "person" : "enterprise";
            result.add(
                    new OidcFieldVo(
                            field.claimName(),
                            field.label(),
                            "wta_" + group,
                            field.sensitive(),
                            false,
                            group));
        }
        return List.copyOf(result);
    }

    /** 返回默认启用的基础账户字段，完整证件始终默认关闭。 */
    public static Set<String> defaults() {
        var result = new LinkedHashSet<String>();
        fields().stream().filter(OidcFieldVo::defaultEnabled).forEach(f -> result.add(f.key()));
        return result;
    }

    /** 从逐字段授权推导应用允许请求的 scope。 */
    public static Set<String> scopes(Collection<String> allowed) {
        var result = new LinkedHashSet<String>();
        result.add("openid");
        fields().stream()
                .filter(f -> allowed.contains(f.key()))
                .forEach(f -> result.add(f.scope()));
        return result;
    }

    /** 计算授权快照、当前策略和已授权 scope 的字段交集。 */
    public static Set<String> permitted(
            Collection<String> snapshot, Collection<String> current, Set<String> scopes) {
        var result = new LinkedHashSet<String>();
        fields().stream()
                .filter(
                        f ->
                                snapshot.contains(f.key())
                                        && current.contains(f.key())
                                        && scopes.contains(f.scope()))
                .forEach(f -> result.add(f.key()));
        return result;
    }
}
