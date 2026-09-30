package org.namewta.oidc.config;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.namewta.common.core.service.HttpProtocolPolicy;
import org.namewta.oidc.adapter.api.*;
import org.namewta.oidc.domain.OidcPrincipal;
import org.namewta.oidc.service.OidcKeyService;
import org.namewta.oidc.usecase.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.authentication.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.security.Principal;
import java.util.*;

/** 只接管 OIDC 精确协议端点，普通业务请求继续使用 Sa-Token。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OidcProperties.class)
public class OidcSecurityConfiguration {
    /** Spring Security 仅匹配这些标准端点，管理与第一方 SSO 不在其中。 */
    public static final Set<String> PATHS =
            Set.of(
                    "/.well-known/openid-configuration",
                    "/.well-known/oauth-authorization-server",
                    "/oidc/authorize",
                    "/oidc/token",
                    "/oidc/jwks",
                    "/oidc/userinfo",
                    "/oidc/introspect",
                    "/oidc/revoke",
                    "/oidc/logout");

    /** 统一使用去除 Servlet 挂载前缀的路径做精确协议匹配。 */
    private static String path(HttpServletRequest r) {
        return r.getRequestURI().substring(r.getContextPath().length());
    }

    @Bean
    /** 非安全 HTTP 仅供显式 local/dev 配置使用，其他环境拒绝启动。 */
    public org.springframework.beans.factory.InitializingBean oidcTransportValidation(
            OidcProperties properties, org.springframework.core.env.Environment environment) {
        return () -> {
            if (!properties.isAllowHttp()) return;
            String[] profiles = environment.getActiveProfiles();
            if (profiles.length == 0
                    || Arrays.stream(profiles)
                            .anyMatch(profile -> !Set.of("local", "dev").contains(profile))) {
                throw new IllegalArgumentException(
                        "OIDC HTTP requires explicit local/dev profiles only");
            }
        };
    }

    @Bean
    /** 声明协议原文边界及禁止采集正文和参数的路径。 */
    public HttpProtocolPolicy oidcProtocolPolicy() {
        return new HttpProtocolPolicy() {
            /** 精确判断路径是否由标准协议适配器拥有。 */
            public boolean isProtocolPath(String p) {
                return PATHS.contains(p);
            }

            /** 判断路径是否包含凭据或个人资料，应跳过日志采集。 */
            public boolean isSensitivePath(String p) {
                return p != null && p.startsWith("/oidc/");
            }
        };
    }

    @Bean
    /** 只向公开 JWKS 端点提供公钥。 */
    public JWKSource<SecurityContext> oidcJwkSource(OidcKeyService keys) {
        return (selector, context) -> selector.select(keys.keys().toPublicJWKSet());
    }

    @Bean
    /** 使用明确 activeKid 的持久私钥签发 JWT。 */
    public JwtEncoder oidcJwtEncoder(OidcKeyService keys) {
        return new NimbusJwtEncoder(
                (selector, context) -> selector.select(new JWKSet(keys.active())));
    }

    @Bean
    /** 以公开密钥配置标准 JWT 验签器。 */
    public JwtDecoder oidcJwtDecoder(JWKSource<SecurityContext> source) {
        return org.springframework.security.config.annotation.web.configuration
                .OAuth2AuthorizationServerConfiguration.jwtDecoder(source);
    }

    @Bean
    /** 固定发行方和标准端点，缺配置时使用不可达内部占位。 */
    public AuthorizationServerSettings oidcSettings(OidcProperties p) {
        var b =
                AuthorizationServerSettings.builder()
                        .authorizationEndpoint("/oidc/authorize")
                        .tokenEndpoint("/oidc/token")
                        .jwkSetEndpoint("/oidc/jwks")
                        .tokenIntrospectionEndpoint("/oidc/introspect")
                        .tokenRevocationEndpoint("/oidc/revoke")
                        .oidcUserInfoEndpoint("/oidc/userinfo")
                        .oidcLogoutEndpoint("/oidc/framework-logout");
        b.issuer(
                p.getIssuer() == null || p.getIssuer().isBlank()
                        ? "https://oidc.invalid"
                        : p.getIssuer());
        return b.build();
    }

    @Bean
    /** 只装配 ID Token 与不透明 Access Token 生成器。 */
    public OAuth2TokenGenerator<? extends OAuth2Token> oidcGenerator(
            JwtEncoder encoder, OidcClaims claims) {
        var jwt = new JwtGenerator(encoder);
        jwt.setJwtCustomizer(claims);
        return new DelegatingOAuth2TokenGenerator(jwt, new OAuth2AccessTokenGenerator());
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 20)
    /** 装配精确路径安全链，保持 WTA Sa-Token 和第一方 SSO 独立。 */
    public SecurityFilterChain oidcChain(
            HttpSecurity http,
            RegisteredClientRepository clients,
            OAuth2AuthorizationService store,
            AuthorizationServerSettings settings,
            OAuth2TokenGenerator<? extends OAuth2Token> generator,
            OidcProperties properties,
            OidcKeyService keys,
            OidcAuthorizationUseCase authorizations,
            OidcProtocolUseCase protocol,
            OidcInteractionUseCase interactions,
            OidcOpaqueIntrospector introspector)
            throws Exception {
        RequestMatcher matcher = r -> PATHS.contains(path(r));
        var server = new OAuth2AuthorizationServerConfigurer();
        http.securityMatcher(matcher)
                .securityContext(
                        c ->
                                c.securityContextRepository(
                                                new RequestAttributeSecurityContextRepository())
                                        .requireExplicitSave(true))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(c -> c.disable())
                .csrf(c -> c.ignoringRequestMatchers(matcher))
                .formLogin(c -> c.disable())
                .httpBasic(c -> c.disable())
                .logout(c -> c.disable());
        http.with(
                server,
                c ->
                        c.registeredClientRepository(clients)
                                .authorizationService(store)
                                .authorizationServerSettings(settings)
                                .tokenGenerator(generator)
                                .authorizationServerMetadataEndpoint(
                                        e ->
                                                e.authorizationServerMetadataCustomizer(
                                                        m ->
                                                                m.claims(
                                                                        v ->
                                                                                metadata(
                                                                                        v,
                                                                                        properties))))
                                .clientAuthentication(
                                        ca ->
                                                ca.authenticationProviders(
                                                        ps -> {
                                                            for (var provider : ps)
                                                                if (provider
                                                                        instanceof
                                                                        ClientSecretAuthenticationProvider
                                                                                secret)
                                                                    secret.setPasswordEncoder(
                                                                            PasswordEncoderFactories
                                                                                    .createDelegatingPasswordEncoder());
                                                        }))
                                .tokenEndpoint(
                                        e ->
                                                e.authenticationProviders(
                                                        ps -> {
                                                            for (int i = 0; i < ps.size(); i++)
                                                                if (ps.get(i)
                                                                        instanceof
                                                                        OAuth2AuthorizationCodeAuthenticationProvider)
                                                                    ps.set(
                                                                            i,
                                                                            new OidcCodeExchangeProvider(
                                                                                    ps.get(i)));
                                                        }))
                                .tokenIntrospectionEndpoint(
                                        e ->
                                                e.authenticationProviders(
                                                        ps -> {
                                                            for (int i = 0; i < ps.size(); i++)
                                                                ps.set(
                                                                        i,
                                                                        new OidcOwnedTokenProvider(
                                                                                ps.get(i),
                                                                                authorizations));
                                                        }))
                                .tokenRevocationEndpoint(
                                        e ->
                                                e.authenticationProviders(
                                                        ps -> {
                                                            for (int i = 0; i < ps.size(); i++)
                                                                ps.set(
                                                                        i,
                                                                        new OidcOwnedTokenProvider(
                                                                                ps.get(i),
                                                                                authorizations));
                                                        }))
                                .oidc(
                                        o ->
                                                o.providerConfigurationEndpoint(
                                                                e ->
                                                                        e
                                                                                .providerConfigurationCustomizer(
                                                                                        m ->
                                                                                                m
                                                                                                        .claims(
                                                                                                                v ->
                                                                                                                        metadata(
                                                                                                                                v,
                                                                                                                                properties))))
                                                        .userInfoEndpoint(
                                                                u ->
                                                                        u.userInfoMapper(
                                                                                context -> {
                                                                                    var
                                                                                            authorization =
                                                                                                    context
                                                                                                            .getAuthorization();
                                                                                    Authentication
                                                                                            authentication =
                                                                                                    authorization
                                                                                                            .getAttribute(
                                                                                                                    Principal
                                                                                                                            .class
                                                                                                                            .getName());
                                                                                    return new OidcUserInfo(
                                                                                            protocol
                                                                                                    .userInfo(
                                                                                                            (OidcPrincipal)
                                                                                                                    authentication
                                                                                                                            .getPrincipal(),
                                                                                                            authorization
                                                                                                                    .getRegisteredClientId(),
                                                                                                            context.getAccessToken()
                                                                                                                    .getScopes()));
                                                                                }))));
        var opaque =
                new org.springframework.security.authentication.ProviderManager(
                        new org.springframework.security.oauth2.server.resource.authentication
                                .OpaqueTokenAuthenticationProvider(introspector));
        http.oauth2ResourceServer(r -> r.jwt(j -> j.authenticationManager(opaque)))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        http.addFilterAfter(
                new OidcLoginFilter(protocol, interactions, properties),
                org.springframework.security.web.authentication.logout.LogoutFilter.class);
        http.addFilterBefore(
                new org.springframework.web.filter.OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(
                            HttpServletRequest request,
                            HttpServletResponse response,
                            FilterChain chain)
                            throws IOException, ServletException {
                        response.setHeader("Cache-Control", "no-store");
                        response.setHeader("Pragma", "no-cache");
                        response.setHeader("Referrer-Policy", "no-referrer");
                        response.setHeader("X-Content-Type-Options", "nosniff");
                        String p = path(request);
                        Set<String> methods = Set.of("GET", "POST");
                        if (Set.of("/oidc/token", "/oidc/introspect", "/oidc/revoke").contains(p))
                            methods = Set.of("POST");
                        else if (p.startsWith("/.well-known/")
                                || p.equals("/oidc/jwks")
                                || p.equals("/oidc/authorize")) methods = Set.of("GET");
                        if (!methods.contains(request.getMethod())) {
                            response.setStatus(405);
                            response.setHeader("Allow", String.join(", ", methods));
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"invalid_request\"}");
                            return;
                        }
                        try {
                            boolean discovery =
                                    p.startsWith("/.well-known/") || p.equals("/oidc/jwks");
                            if (properties.getIssuer() == null
                                    || properties.getIssuer().isBlank()
                                    || !discovery && (!properties.isEnabled() || !keys.ready())) {
                                unavailable(response);
                                return;
                            }
                            chain.doFilter(request, response);
                        } catch (RuntimeException failure) {
                            if (!response.isCommitted()) unavailable(response);
                        }
                    }

                    /** 在响应尚未提交时返回不携带内部故障细节的协议错误。 */
                    private void unavailable(HttpServletResponse r) throws IOException {
                        r.resetBuffer();
                        r.setStatus(503);
                        r.setContentType("application/json");
                        r.getWriter().write("{\"error\":\"temporarily_unavailable\"}");
                    }
                },
                org.springframework.security.web.context.SecurityContextHolderFilter.class);
        http.addFilterBefore(
                new OidcFormBodyFilter(),
                org.springframework.security.web.context.SecurityContextHolderFilter.class);
        return http.build();
    }

    /** 收敛框架默认发现文档，只发布当前真正支持的协议能力。 */
    private static void metadata(Map<String, Object> m, OidcProperties p) {
        m.put("grant_types_supported", List.of("authorization_code"));
        m.put("response_types_supported", List.of("code"));
        m.put("response_modes_supported", List.of("query"));
        m.put(
                "token_endpoint_auth_methods_supported",
                List.of("client_secret_basic", "client_secret_post"));
        m.put(
                "introspection_endpoint_auth_methods_supported",
                List.of("client_secret_basic", "client_secret_post"));
        m.put(
                "revocation_endpoint_auth_methods_supported",
                List.of("client_secret_basic", "client_secret_post"));
        m.put("tls_client_certificate_bound_access_tokens", false);
        m.put(
                "scopes_supported",
                List.of("openid", "profile", "email", "phone", "wta_person", "wta_enterprise"));
        m.put("code_challenge_methods_supported", List.of("S256"));
        m.put("subject_types_supported", List.of("public"));
        m.put("id_token_signing_alg_values_supported", List.of("RS256"));
        m.put("end_session_endpoint", p.getIssuer() + "/oidc/logout");
        m.put("claims_parameter_supported", false);
        m.put("request_parameter_supported", false);
        m.put("request_uri_parameter_supported", false);
        for (String k :
                List.of(
                        "device_authorization_endpoint",
                        "pushed_authorization_request_endpoint",
                        "registration_endpoint",
                        "dpop_signing_alg_values_supported",
                        "mtls_endpoint_aliases")) m.remove(k);
    }
}
