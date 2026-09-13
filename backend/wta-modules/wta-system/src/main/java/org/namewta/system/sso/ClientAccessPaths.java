package org.namewta.system.sso;

import org.namewta.common.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 sys_client.access_path 解析为 Token extras 使用的规则。
 * home-web 登录后会请求身份接口，不能只留不存在的 {@code /home/**}。
 */
public final class ClientAccessPaths {

    static final String IDENTITY_GET_INFO = "/system/user/getInfo";
    static final String IDENTITY_MENUS = "/system/menu/getRouters";
    static final String AUTH_LOGOUT = "/auth/logout";
    static final String PROFILE = "/profile/**";

    private static final String SEPARATOR_REGEX = "[,;\\r\\n]+";
    private static final List<String> HOME_IDENTITY_APIS = List.of(
        IDENTITY_GET_INFO, IDENTITY_MENUS, AUTH_LOGOUT, PROFILE);

    private ClientAccessPaths() {
    }

    /**
     * home 客户端在已有白名单上并入身份/档案接口；其它 Client 原样返回。
     *
     * @param clientKey  客户端 key
     * @param accessPath 已配置白名单
     * @return Token extras 使用的规则串
     */
    public static String resolve(String clientKey, String accessPath) {
        if (!"home".equalsIgnoreCase(StringUtils.trim(clientKey))) {
            return accessPath;
        }
        if (StringUtils.isBlank(accessPath)) {
            return accessPath;
        }
        List<String> rules = new ArrayList<>(StringUtils.str2List(accessPath, SEPARATOR_REGEX, true, true));
        for (String needed : HOME_IDENTITY_APIS) {
            if (!allows(String.join(",", rules), sampleRequest(needed))) {
                rules.add(needed);
            }
        }
        return String.join(",", rules);
    }

    /**
     * 与 {@code SecurityConfig} 相同的路径匹配：空白名单表示不限制。
     *
     * @param accessPath  规则串
     * @param requestPath 请求路径
     * @return 是否允许
     */
    public static boolean allows(String accessPath, String requestPath) {
        if (StringUtils.isBlank(accessPath)) {
            return true;
        }
        List<String> rules = StringUtils.str2List(accessPath, SEPARATOR_REGEX, true, true);
        return StringUtils.matches(requestPath, rules);
    }

    private static String sampleRequest(String pattern) {
        if (pattern.endsWith("/**")) {
            return pattern.substring(0, pattern.length() - 3) + "/person/application";
        }
        return pattern;
    }
}
