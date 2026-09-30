package org.namewta.oidc.support;

import org.namewta.common.core.exception.ServiceException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

/** 已登记协议地址的纯校验，不发起DNS或网络请求。 */
public final class OidcUriPolicy {
    /** 工具类不允许实例化，调用方使用静态协议操作。 */
    private OidcUriPolicy() {}

    /**
     * 检查受控地址、声明或准入策略的领域不变量。
     *
     * @param value 待校验或转换的领域值
     * @param allowHttp 非生产环境是否显式允许HTTP地址
     * @return 已校验的 {@code String} 结果
     */
    public static String validate(String value, boolean allowHttp) {
        if (value == null
                || value.isBlank()
                || value.length() > 2048
                || !value.equals(value.strip())
                || value.chars().anyMatch(c -> c < 32 || c == 127)
                || value.contains("*")
                || value.contains("\\")
                || value.toLowerCase(Locale.ROOT).matches(".*%(0d|0a|00).*")) {
            throw new ServiceException("OIDC地址格式不合法");
        }
        try {
            URI uri = parse(value);
            if (uri.getHost() == null
                    || invalidNumericHost(uri.getHost())
                    || uri.getRawUserInfo() != null
                    || uri.getRawFragment() != null
                    || uri.getPort() > 65535
                    || uri.getPort() == 0) throw new ServiceException("OIDC地址必须为不含用户信息和片段的完整地址");
            boolean https = "https".equals(uri.getScheme());
            boolean http = "http".equals(uri.getScheme());
            if (!https && !(allowHttp && http)) throw new ServiceException("当前环境的OIDC地址必须使用HTTPS");
            return value;
        } catch (URISyntaxException e) {
            throw new ServiceException("OIDC地址格式不合法", e);
        }
    }

    /**
     * 提取已验证地址的协议、主机和端口作为精确来源。
     *
     * @param value 待校验或转换的领域值
     * @param allowHttp 非生产环境是否显式允许HTTP地址
     * @return 已校验的 {@code String} 结果
     */
    public static String origin(String value, boolean allowHttp) {
        try {
            URI uri = parse(validate(value, allowHttp));
            return uri.getScheme() + "://" + uri.getRawAuthority();
        } catch (URISyntaxException e) {
            throw new ServiceException("OIDC地址格式不合法", e);
        }
    }

    /**
     * 验证地址列表中的HTTPS、精确匹配形式和重复项。
     *
     * @param values 已经登记或待验证的受控配置列表
     * @param required 是否要求列表至少含一个地址
     * @param allowHttp 非生产环境是否显式允许HTTP地址
     * @return 已校验的 {@code List<String>} 结果
     */
    public static List<String> addresses(List<String> values, boolean required, boolean allowHttp) {
        if (values == null || required && values.isEmpty() || values.size() > 50)
            throw new ServiceException("OIDC地址白名单不能为空或超出限制");
        List<String> valid = values.stream().map(v -> validate(v, allowHttp)).distinct().toList();
        if (valid.size() != values.size()) throw new ServiceException("OIDC地址白名单存在重复值");
        return valid;
    }

    /**
     * 校验部署发行方只包含精确origin；非生产环境可显式允许HTTP。
     *
     * @param value 部署注入的发行方地址
     * @param allowHttp 非生产环境是否显式允许HTTP地址
     * @return 通过验证的原始发行方地址
     */
    public static String issuer(String value, boolean allowHttp) {
        if (!validate(value, allowHttp).equals(origin(value, allowHttp))) {
            throw new ServiceException("发行方必须为不含路径或查询参数的完整来源地址");
        }
        return value;
    }

    /**
     * Gitea 认证源名称等路径可含非 ASCII。RFC 2396 的 {@link URI} 构造器会拒绝未百分号编码的路径； 这里只解析结构，仍返回调用方原文，以便与 RP
     * 请求做精确匹配。
     */
    private static URI parse(String value) throws URISyntaxException {
        try {
            return new URI(value);
        } catch (URISyntaxException ex) {
            return new URI(encodeNonAscii(value));
        }
    }

    /** 只对非 ASCII 路径字符编码以供结构解析，精确匹配仍保留登记原文。 */
    private static String encodeNonAscii(String value) {
        StringBuilder encoded = new StringBuilder(value.length());
        value.codePoints()
                .forEach(
                        codePoint -> {
                            if (codePoint <= 0x7f) {
                                encoded.append((char) codePoint);
                                return;
                            }
                            byte[] bytes =
                                    new String(Character.toChars(codePoint))
                                            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                            for (byte item : bytes) {
                                encoded.append('%')
                                        .append(String.format(Locale.ROOT, "%02X", item & 0xff));
                            }
                        });
        return encoded.toString();
    }

    /** 拒绝浏览器可能重新解释的缩写或越界 IPv4 数字地址。 */
    private static boolean invalidNumericHost(String host) {
        for (int i = 0; i < host.length(); i++) {
            char ch = host.charAt(i);
            if (ch != '.' && (ch < '0' || ch > '9')) return false;
        }
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) return true;
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) return true;
            if (Integer.parseInt(part) > 255) return true;
        }
        return false;
    }
}
