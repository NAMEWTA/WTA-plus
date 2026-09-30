package org.namewta.oidc.adapter.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.support.OidcSecrets;
import org.namewta.oidc.usecase.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** 在标准授权端点恢复 WTA SSO 身份；所有跳转均在应用回调校验后构造。 */
public final class OidcLoginFilter extends OncePerRequestFilter {
    private final OidcProtocolUseCase protocol;
    private final OidcInteractionUseCase interactions;
    private final OidcProperties properties;

    /** 组装当前协议能力所需的明确依赖。 */
    public OidcLoginFilter(
            OidcProtocolUseCase protocol,
            OidcInteractionUseCase interactions,
            OidcProperties properties) {
        this.protocol = protocol;
        this.interactions = interactions;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest r) {
        return !"/oidc/authorize".equals(r.getRequestURI().substring(r.getContextPath().length()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        Map<String, String> params = null;
        boolean trustedRedirect = false;
        try {
            String browser = OidcBrowserCookies.ensure(request, response, properties),
                    sid = OidcBrowserCookies.read(request, properties.getSessionCookieName());
            String tx = request.getParameter("oidc_request");
            var interaction = tx == null ? null : interactions.require(tx, browser);
            params = interaction == null ? parameters(request) : interaction.parameters();
            var app = protocol.app(params.get("client_id"));
            if (app == null || !Boolean.TRUE.equals(app.getEnabled()))
                throw new OAuth2AuthenticationException("invalid_request");
            var view = protocol.view(app);
            if (!view.redirectUris().contains(params.get("redirect_uri")))
                throw new OAuth2AuthenticationException("invalid_request");
            trustedRedirect = true;
            validate(params, view.allowedScopes(), view.pkceRequired());
            var session = protocol.session(sid);
            var prompts = split(params.get("prompt"));
            boolean force =
                    interaction == null
                            ? ((prompts.contains("login") || prompts.contains("select_account"))
                                    || session != null
                                            && expired(
                                                    params,
                                                    session.authenticatedAt().getEpochSecond()))
                            : interaction.forceLogin();
            boolean reauthenticated =
                    interaction != null
                            && session != null
                            && !OidcSecrets.equal(
                                    interaction.previousSessionHash(), OidcSecrets.hash(sid))
                            && session.authenticatedAt().getEpochSecond()
                                    >= interaction.createdAt();
            if (session == null || force && !reauthenticated) {
                if (prompts.contains("none"))
                    throw new OAuth2AuthenticationException("login_required");
                // 旧 SSO 会话可能被第一方页面认可，但缺少 OIDC 所需认证时间，必须显式重新登录。
                if (tx == null)
                    tx = interactions.create(params, browser, sid, force || session == null);
                response.setStatus(303);
                response.setHeader(
                        "Location",
                        properties.getSsoWebUrl().replaceAll("/+$", "")
                                + "/login?oidc_request="
                                + encode(tx));
                return;
            }
            if (prompts.contains("consent"))
                throw new OAuth2AuthenticationException("consent_required");
            var snapshot = protocol.principal(sid, new LinkedHashSet<>(view.allowedFields()));
            var principal =
                    new org.namewta.oidc.domain.OidcPrincipal(
                            snapshot.name(),
                            snapshot.userId(),
                            snapshot.sessionId(),
                            snapshot.authTime(),
                            snapshot.expiresAt(),
                            snapshot.allowedFields(),
                            view.version());
            var authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal,
                            null,
                            List.of(
                                    FactorGrantedAuthority.withFactor("WTA_SSO")
                                            .issuedAt(Instant.ofEpochSecond(principal.authTime()))
                                            .build()));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            if (tx != null) interactions.consume(tx, browser);
            Map<String, String[]> values = new LinkedHashMap<>();
            params.forEach((k, v) -> values.put(k, new String[] {v}));
            String restoredQuery =
                    params.entrySet().stream()
                            .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                            .collect(java.util.stream.Collectors.joining("&"));
            HttpServletRequest wrapped =
                    new HttpServletRequestWrapper(request) {
                        @Override
                        public String getQueryString() {
                            return restoredQuery;
                        }

                        @Override
                        public String getParameter(String n) {
                            var v = values.get(n);
                            return v == null ? null : v[0];
                        }

                        @Override
                        public String[] getParameterValues(String n) {
                            return values.get(n);
                        }

                        @Override
                        public Map<String, String[]> getParameterMap() {
                            return Collections.unmodifiableMap(values);
                        }

                        @Override
                        public Enumeration<String> getParameterNames() {
                            return Collections.enumeration(values.keySet());
                        }
                    };
            chain.doFilter(wrapped, response);
        } catch (OAuth2AuthenticationException | IllegalArgumentException ex) {
            String error =
                    ex instanceof OAuth2AuthenticationException oauth
                            ? oauth.getError().getErrorCode()
                            : "invalid_request";
            if (trustedRedirect) {
                String uri = params.get("redirect_uri");
                String target = uri + (uri.contains("?") ? "&" : "?") + "error=" + encode(error);
                if (params.get("state") != null) target += "&state=" + encode(params.get("state"));
                response.setStatus(302);
                response.setHeader("Location", target);
            } else {
                response.setStatus(400);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"invalid_request\"}");
            }
        }
    }

    /** 拒绝授权查询中的重复参数及超长单值，保留协议字符串原文。 */
    private Map<String, String> parameters(HttpServletRequest request) {
        Map<String, String> p = new LinkedHashMap<>();
        request.getParameterMap()
                .forEach(
                        (k, v) -> {
                            if (v.length != 1 || v[0].length() > 4096)
                                throw new OAuth2AuthenticationException("invalid_request");
                            p.put(k, v[0]);
                        });
        return p;
    }

    /** 只接受已登记范围内的授权码请求，并约束 PKCE 和交互提示。 */
    private void validate(Map<String, String> p, List<String> scopes, boolean pkce) {
        if (!"code".equals(p.get("response_type"))
                || !split(p.get("scope")).contains("openid")
                || !scopes.containsAll(split(p.get("scope"))))
            throw new OAuth2AuthenticationException("invalid_scope");
        if (p.containsKey("response_mode") && !"query".equals(p.get("response_mode")))
            throw new OAuth2AuthenticationException("invalid_request");
        if (p.containsKey("request") || p.containsKey("request_uri") || p.containsKey("claims"))
            throw new OAuth2AuthenticationException("request_not_supported");
        if (pkce || p.containsKey("code_challenge")) {
            if (!"S256".equals(p.get("code_challenge_method"))
                    || p.get("code_challenge") == null
                    || !p.get("code_challenge").matches("[A-Za-z0-9_-]{43}"))
                throw new OAuth2AuthenticationException("invalid_request");
        }
        var prompts = split(p.get("prompt"));
        if (!Set.of("none", "login", "consent", "select_account").containsAll(prompts)
                || prompts.contains("none") && prompts.size() > 1)
            throw new OAuth2AuthenticationException("invalid_request");
        if (p.containsKey("max_age") && (Long.parseLong(p.get("max_age")) < 0))
            throw new OAuth2AuthenticationException("invalid_request");
    }

    /** 按真实 SSO 认证时间判断 RP 的重新认证要求，零值强制重认证。 */
    private boolean expired(Map<String, String> p, long authTime) {
        return p.containsKey("max_age")
                && (Long.parseLong(p.get("max_age")) == 0
                        || Instant.now().getEpochSecond() - authTime
                                > Long.parseLong(p.get("max_age")));
    }

    /** 将协议空格分隔值转换为去重集合，缺失值表示未提出要求。 */
    private Set<String> split(String v) {
        return v == null || v.isBlank()
                ? Set.of()
                : new LinkedHashSet<>(Arrays.asList(v.strip().split(" +")));
    }

    /** 对单个查询参数执行 UTF-8 编码，避免回跳参数改变 URI 结构。 */
    private String encode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
