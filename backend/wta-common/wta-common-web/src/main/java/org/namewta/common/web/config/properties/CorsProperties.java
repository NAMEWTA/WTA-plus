package org.namewta.common.web.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 跨域配置属性。
 */
@Data
@ConfigurationProperties(prefix = "web.cors")
public class CorsProperties {

    private static final Pattern ORIGIN_PATTERN = Pattern.compile(
        "(?i)^https?://(?:[a-z0-9*.-]+|\\[[0-9a-f:.]+])(?::(?:[0-9]+|\\*|\\[\\*]))?$");

    /**
     * 是否允许携带凭证。
     */
    private Boolean allowCredentials = true;

    /**
     * HTTP(S) 来源，支持精确地址、主机/IP 通配和 {@code *} 混写。
     * 端口通配可用 {@code :*} 或 {@code :[*]}；空配置不许可跨来源请求。
     */
    private List<String> allowedOrigins = List.of();

    /**
     * 允许的请求头。
     */
    private List<String> allowedHeaders = List.of("*");

    /**
     * 允许的请求方法。
     */
    private List<String> allowedMethods = List.of("*");

    /**
     * 预检请求缓存时间，单位秒。
     */
    private Long maxAge = 1800L;

    /**
     * @return 配置是否包含 {@code *}，表示允许任意 HTTP(S) 来源
     * @throws IllegalArgumentException 任一来源格式非法
     */
    public boolean allowsAnyHttpOrigin() {
        return validatedValues().contains("*");
    }

    /**
     * @return 经校验的精确 HTTP(S) 来源，通配规则由 validatedOriginPatterns 返回
     * @throws IllegalArgumentException 任一来源格式非法
     */
    public List<String> validatedOrigins() {
        return validatedValues().stream().filter(value -> !value.contains("*")).toList();
    }

    /**
     * 将通配规则转换为 Spring 支持的 Origin pattern，保持凭证请求回显实际来源。
     *
     * @return HTTP(S) 通配规则；不会向浏览器返回通配 Allow-Origin
     * @throws IllegalArgumentException 任一来源格式非法
     */
    public List<String> validatedOriginPatterns() {
        return validatedValues().stream().filter(value -> value.contains("*"))
            .flatMap(value -> "*".equals(value) ? List.of("http://*", "https://*").stream()
                : List.of(value.endsWith(":*") ? value.substring(0, value.length() - 1) + "[*]" : value).stream())
            .distinct().toList();
    }

    private List<String> validatedValues() {
        return configuredOrigins().stream().map(value -> {
            if ("*".equals(value)) {
                return value;
            }
            if (value.contains("*")) {
                if (!ORIGIN_PATTERN.matcher(value).matches()) {
                    throw new IllegalArgumentException("CORS requires HTTP(S) origin patterns without paths");
                }
                // 校验通配之外的 URL 和端口；不接受路径、用户信息或无效端口。
                exactOrigin(value.replace(":[*]", ":443").replace(":*", ":443").replace("*", "wildcard"));
                return value;
            }
            return exactOrigin(value);
        }).distinct().toList();
    }

    private List<String> configuredOrigins() {
        if (allowedOrigins == null) {
            throw new IllegalArgumentException("CORS allowed origins must be configured as a list");
        }
        if (allowedOrigins.isEmpty() || (allowedOrigins.size() == 1 && allowedOrigins.getFirst() != null
            && allowedOrigins.getFirst().isBlank())) {
            return List.of();
        }
        return allowedOrigins.stream().map(origin -> {
            if (origin == null) {
                throw new IllegalArgumentException("CORS origin cannot be null");
            }
            String value = origin.trim();
            if (value.isEmpty()) {
                throw new IllegalArgumentException("CORS origin cannot be blank");
            }
            return value;
        }).toList();
    }

    private String exactOrigin(String value) {
        if (value.contains("*")) {
            throw new IllegalArgumentException("CORS requires exact HTTP(S) origins without wildcards");
        }
        URI uri = URI.create(value);
        if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
            || uri.getHost() == null || uri.getUserInfo() != null
            || uri.getRawQuery() != null || uri.getRawFragment() != null
            || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
            || uri.getPort() == 0 || uri.getPort() > 65535
            || uri.getRawAuthority().endsWith(":")
            || !value.equals(uri.getScheme() + "://" + uri.getRawAuthority())) {
            throw new IllegalArgumentException("CORS requires exact HTTP(S) origins without paths or wildcards");
        }
        return value;
    }

}
