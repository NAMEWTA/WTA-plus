package org.namewta.common.web.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;

/**
 * 跨域配置属性。
 */
@Data
@ConfigurationProperties(prefix = "web.cors")
public class CorsProperties {

    /**
     * 是否允许携带凭证。
     */
    private Boolean allowCredentials = true;

    /**
     * 精确来源白名单，默认无跨来源访问；同源请求无需CORS许可。
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
     * @return 经校验的精确 HTTP(S) 来源；空配置不允许跨来源访问
     * @throws IllegalArgumentException 错误配置时拒绝启动，不自动放宽信任
     */
    public List<String> validatedOrigins() {
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
            if (value.isEmpty() || value.contains("*")) {
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
        }).toList();
    }

}
