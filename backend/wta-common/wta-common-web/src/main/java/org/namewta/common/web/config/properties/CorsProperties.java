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
     * @return 经校验的来源；单独的 {@code *} 表示临时允许任意 Origin，其余值必须是不带路径的精确 HTTP(S) origin
     * @throws IllegalArgumentException 错误配置时拒绝启动，不自动放宽信任
     */
    public List<String> validatedOrigins() {
        return allowedOrigins.stream().map(origin -> {
            String value = origin.trim();
            if ("*".equals(value)) {
                if (allowedOrigins.size() != 1) {
                    throw new IllegalArgumentException("CORS wildcard cannot be combined with explicit origins");
                }
                return value;
            }
            URI uri = URI.create(value);
            if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || value.contains("*") || uri.getUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                || uri.getPort() == 0 || uri.getPort() > 65535) {
                throw new IllegalArgumentException("CORS requires exact HTTP(S) origins without paths or wildcards");
            }
            return value;
        }).toList();
    }

}
