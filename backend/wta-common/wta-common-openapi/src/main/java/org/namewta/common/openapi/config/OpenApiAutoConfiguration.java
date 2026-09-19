package org.namewta.common.openapi.config;

import cn.dev33.satoken.stp.StpLogic;
import jakarta.servlet.DispatcherType;
import org.namewta.common.openapi.config.properties.OpenApiProperties;
import org.namewta.common.openapi.gateway.OpenApiGatewayFilter;
import org.namewta.common.openapi.nonce.OpenApiNonceStore;
import org.namewta.common.openapi.nonce.RedissonOpenApiNonceStore;
import org.namewta.common.openapi.protocol.OpenApiCanonicalizer;
import org.namewta.common.openapi.protocol.OpenApiSigner;
import org.namewta.common.openapi.ratelimit.OpenApiRateLimiter;
import org.namewta.common.openapi.ratelimit.RedissonOpenApiRateLimiter;
import org.namewta.common.openapi.registry.OpenApiAuthorizationMatcher;
import org.namewta.common.openapi.registry.OpenApiOperationRegistry;
import org.namewta.common.openapi.registry.SpringDocOperationSchemaResolver;
import org.namewta.common.openapi.session.DefaultOpenApiMachineSessionInvalidator;
import org.namewta.common.openapi.session.OpenApiMachineSessionBridge;
import org.namewta.common.openapi.session.OpenApiMachineSessionInvalidator;
import org.namewta.common.openapi.session.OpenApiMachineSessionOperations;
import org.namewta.common.openapi.session.SaTokenOpenApiMachineSessionOperations;
import org.namewta.common.openapi.spi.OpenApiAuthorizationResolver;
import org.namewta.common.openapi.spi.OpenApiCallEventPublisher;
import org.namewta.common.openapi.spi.OpenApiCredentialResolver;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Default-off assembly for the NAMEWTA OpenAPI gateway.
 */
@AutoConfiguration
@EnableConfigurationProperties(OpenApiProperties.class)
@ConditionalOnProperty(prefix = "openapi", name = "enabled", havingValue = "true")
public class OpenApiAutoConfiguration {

    @Bean
    OpenApiStartupValidator openApiStartupValidator(OpenApiProperties properties) {
        return new OpenApiStartupValidator(properties);
    }

    @Bean
    SpringDocOperationSchemaResolver openApiOperationSchemaResolver() {
        return new SpringDocOperationSchemaResolver();
    }

    @Bean
    OpenApiOperationRegistry openApiOperationRegistry(
        @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
        SpringDocOperationSchemaResolver schemaResolver) {
        return new OpenApiOperationRegistry(handlerMapping, schemaResolver);
    }

    @Bean
    OpenApiCanonicalizer openApiCanonicalizer() {
        return new OpenApiCanonicalizer();
    }

    @Bean
    OpenApiSigner openApiSigner(OpenApiCanonicalizer canonicalizer) {
        return new OpenApiSigner(canonicalizer);
    }

    @Bean
    OpenApiAuthorizationMatcher openApiAuthorizationMatcher() {
        return new OpenApiAuthorizationMatcher();
    }

    @Bean
    OpenApiNonceStore openApiNonceStore(RedissonClient redissonClient) {
        return new RedissonOpenApiNonceStore(redissonClient);
    }

    @Bean
    OpenApiRateLimiter openApiRateLimiter(RedissonClient redissonClient) {
        return new RedissonOpenApiRateLimiter(redissonClient);
    }

    @Bean
    OpenApiMachineSessionOperations openApiMachineSessionOperations(StpLogic stpLogic,
                                                                    RedissonClient redissonClient) {
        return new SaTokenOpenApiMachineSessionOperations(stpLogic, redissonClient);
    }

    @Bean
    OpenApiMachineSessionBridge openApiMachineSessionBridge(OpenApiAuthorizationResolver authorizationResolver,
                                                            OpenApiMachineSessionOperations sessionOperations,
                                                            OpenApiProperties properties) {
        return new OpenApiMachineSessionBridge(authorizationResolver, sessionOperations,
            properties.getMachineSessionTtl());
    }

    @Bean
    OpenApiMachineSessionInvalidator openApiMachineSessionInvalidator(
        OpenApiMachineSessionOperations sessionOperations) {
        return new DefaultOpenApiMachineSessionInvalidator(sessionOperations);
    }

    @Bean
    @ConditionalOnMissingBean(OpenApiCallEventPublisher.class)
    OpenApiCallEventPublisher openApiCallEventPublisher() {
        return event -> { };
    }

    @Bean
    FilterRegistrationBean<OpenApiGatewayFilter> openApiGatewayFilterRegistration(
        @Qualifier("requestMappingHandlerMapping")
        RequestMappingHandlerMapping handlerMapping,
        OpenApiOperationRegistry operationRegistry,
        OpenApiCredentialResolver credentialResolver,
        OpenApiSigner signer,
        OpenApiNonceStore nonceStore,
        OpenApiRateLimiter rateLimiter,
        OpenApiMachineSessionBridge sessionBridge,
        OpenApiAuthorizationMatcher authorizationMatcher,
        OpenApiCallEventPublisher eventPublisher,
        OpenApiProperties properties) {
        OpenApiGatewayFilter filter = new OpenApiGatewayFilter(handlerMapping, operationRegistry,
            credentialResolver, signer, nonceStore, rateLimiter, sessionBridge, authorizationMatcher,
            eventPublisher, properties);
        FilterRegistrationBean<OpenApiGatewayFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("openApiGatewayFilter");
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setAsyncSupported(true);
        // 在普通正文/日志/XSS之前完成原始字节验签，后续观察者复用已验证的独立预算。
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
