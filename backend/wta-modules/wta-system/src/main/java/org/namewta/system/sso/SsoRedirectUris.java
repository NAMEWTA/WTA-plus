package org.namewta.system.sso;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * SSO 回调白名单：只允许精确 URI，禁止通配。
 */
public final class SsoRedirectUris {

    private static final String SEPARATOR_REGEX = "[,;\\r\\n]+";

    private SsoRedirectUris() {
    }

    /**
     * 解析回调列表。
     *
     * @param rawValue  逗号/换行分隔串
     * @param listValue 列表值
     * @return 去空白后的有序列表
     */
    public static List<String> parse(String rawValue, List<String> listValue) {
        List<String> source = rawValue != null
            ? StringUtils.str2List(rawValue, SEPARATOR_REGEX, true, true)
            : listValue;
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String item : source) {
            if (StringUtils.isNotBlank(item)) {
                unique.add(item.trim());
            }
        }
        return new ArrayList<>(unique);
    }

    /**
     * 校验精确回调白名单。
     *
     * @param uris 回调列表
     */
    public static void validateExact(List<String> uris) {
        if (uris == null || uris.isEmpty()) {
            throw new ServiceException("SSO 回调白名单不能为空");
        }
        for (String uri : uris) {
            validateOne(uri);
        }
    }

    /**
     * 校验单条回调。
     *
     * @param uri 回调地址
     */
    public static void validateOne(String uri) {
        if (StringUtils.isBlank(uri)) {
            throw new ServiceException("SSO 回调地址不能为空");
        }
        String value = uri.trim();
        if (value.contains("*")) {
            throw new ServiceException("SSO 回调白名单禁止通配符");
        }
        URI parsed;
        try {
            parsed = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new ServiceException("SSO 回调地址不合法");
        }
        String scheme = parsed.getScheme() == null ? "" : parsed.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new ServiceException("SSO 回调地址必须是 http 或 https");
        }
        if (StringUtils.isBlank(parsed.getHost())) {
            throw new ServiceException("SSO 回调地址缺少主机名");
        }
        if (parsed.getRawFragment() != null) {
            throw new ServiceException("SSO 回调地址不能包含 fragment");
        }
    }

    /**
     * 精确匹配已登记回调。
     *
     * @param allowed   白名单
     * @param candidate 请求回调
     * @return 是否命中
     */
    public static boolean matches(List<String> allowed, String candidate) {
        if (allowed == null || StringUtils.isBlank(candidate)) {
            return false;
        }
        String value = candidate.trim();
        for (String item : allowed) {
            if (value.equals(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 拼接入库串。
     *
     * @param uris 回调列表
     * @return 逗号分隔串，空列表返回空串
     */
    public static String join(List<String> uris) {
        if (uris == null || uris.isEmpty()) {
            return "";
        }
        return String.join(StringUtils.SEPARATOR, uris);
    }
}
