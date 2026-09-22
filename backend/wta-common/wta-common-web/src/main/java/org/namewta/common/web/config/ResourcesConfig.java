package org.namewta.common.web.config;

import cn.hutool.core.date.DateTime;
import org.namewta.common.core.utils.DateUtils;
import org.namewta.common.core.utils.ObjectUtils;
import org.namewta.common.json.enhance.JsonValueEnhancer;
import org.namewta.common.web.advice.ResponseEnhancementAdvice;
import org.namewta.common.web.config.properties.CorsProperties;
import org.namewta.common.web.handler.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 通用配置
 *
 * @author Lion Li
 */
@AutoConfiguration
@EnableConfigurationProperties(CorsProperties.class)
public class ResourcesConfig implements WebMvcConfigurer {

    /**
     * 注册全局格式转换器。
     *
     * @param registry 格式化器注册表
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 全局日期格式转换配置
        registry.addConverter(String.class, Date.class, source -> {
            DateTime parse = DateUtils.parse(source);
            if (ObjectUtils.isNull(parse)) {
                return null;
            }
            return parse.toJdkDate();
        });
        registry.addConverter(String.class, LocalDateTime.class, source -> {
            DateTime parse = DateUtils.parse(source);
            if (ObjectUtils.isNull(parse)) {
                return null;
            }
            return parse.toLocalDateTime();
        });
    }

    /**
     * 跨域配置
     *
     * @return 全局 Cors 过滤器
     */
    @Bean
    public CorsFilter corsFilter(CorsProperties corsProperties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(corsProperties.getAllowCredentials());
        config.setAllowedOrigins(corsProperties.validatedOrigins());
        config.setAllowedHeaders(corsProperties.getAllowedHeaders());
        config.setAllowedMethods(corsProperties.getAllowedMethods());
        config.setMaxAge(corsProperties.getMaxAge());
        // 添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // 返回新的CorsFilter
        return new CorsFilter(source);
    }

    /**
     * 全局异常处理器
     *
     * @return 全局异常处理器实例
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * 注册响应增强处理器。
     *
     * @param jsonValueEnhancer JSON 字段增强器
     * @return 响应增强处理器
     */
    @Bean
    public ResponseEnhancementAdvice responseEnhancementAdvice(JsonValueEnhancer jsonValueEnhancer) {
        return new ResponseEnhancementAdvice(jsonValueEnhancer);
    }

}
