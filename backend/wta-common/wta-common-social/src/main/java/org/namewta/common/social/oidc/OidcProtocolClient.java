package org.namewta.common.social.oidc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.annotation.PreDestroy;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 标准 OIDC 机密客户端。远端认证仅通过 HTTP/JWKS，不接触提供方数据库。 */
@Component
public class OidcProtocolClient implements AutoCloseable {
    private static final String LOGOUT_EVENT = "http://schemas.openid.net/event/backchannel-logout";
    private static final Set<String> PROFILE_CLAIMS =
            Set.of("name", "nickname", "preferred_username", "phone_number", "email");
    private static final SecureRandom RANDOM = new SecureRandom();
    private final HttpClient http;
    private final Clock clock;
    private final boolean allowHttp;

    @org.springframework.beans.factory.annotation.Autowired
    public OidcProtocolClient(Environment environment) {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build(),
                Clock.systemUTC(),
                environment.getActiveProfiles().length > 0
                        && Arrays.stream(environment.getActiveProfiles())
                                .allMatch(Set.of("local", "dev")::contains));
    }

    OidcProtocolClient(HttpClient http, Clock clock, boolean allowHttp) {
        this.http = http;
        this.clock = clock;
        this.allowHttp = allowHttp;
    }

    /** 生成 state、nonce 和浏览器事务凭据。 */
    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 用于 PKCE S256，不把 verifier 送到浏览器或写日志。 */
    public static String challenge(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest(value));
    }

    /** 长度前缀避免外部标识拼接歧义；摘要不改变原身份大小写语义。 */
    public static String identityKey(String issuer, String subject) {
        return HexFormat.of().formatHex(digest(issuer.length() + ":" + issuer + subject));
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** Discovery 缓存只包含公开元数据；精确校验发行方。 */
    public OidcMetadata discovery(String issuer) {
        checkedUri(issuer);
        String key = "auth:oidc:discovery:" + identityKey(issuer, "");
        String cached = RedisUtils.getCacheObject(key);
        if (cached != null) return JsonUtils.parseObject(cached, OidcMetadata.class);
        OidcMetadata metadata = metadata(issuer, fetchDiscovery(issuer));
        RedisUtils.setCacheObject(key, JsonUtils.toJsonString(metadata), Duration.ofMinutes(5));
        return metadata;
    }

    /**
     * 实时检查发行方公开元数据；不读写 Discovery 缓存，不发送客户端凭据或授权码。
     *
     * @param issuer 管理员登记的精确发行者地址，使用与登录相同的 HTTP(S) 策略
     * @return 当前节点实际获取的端点、能力声明及观察时间
     * @throws ServiceException 网络不可用、发行者不匹配、端点或元数据格式无效时明确失败
     */
    public OidcDiscoveryDiagnostics diagnose(String issuer) {
        try {
            checkedUri(issuer);
        } catch (ServiceException failure) {
            throw diagnosticFailure("OIDC Discovery 发行者地址不符合当前环境的 HTTP(S) 地址规则", failure);
        }
        String body;
        try {
            body = send(request(issuer.replaceAll("/$", "") + "/.well-known/openid-configuration").GET().build());
        } catch (ServiceException failure) {
            throw diagnosticFailure("OIDC Discovery 获取失败，请检查网络、发行者地址和远端服务状态", failure);
        }
        Map<String, Object> json;
        try {
            json = json(body);
        } catch (ServiceException failure) {
            throw diagnosticFailure("OIDC Discovery 元数据不是有效的 JSON 对象", failure);
        }
        if (!issuer.equals(text(json, "issuer")))
            throw new ServiceException("OIDC Discovery 返回的 issuer 与配置不一致");
        try {
            return new OidcDiscoveryDiagnostics(
                    metadata(issuer, json),
                    stringList(json, "scopes_supported", List.of()),
                    stringList(json, "response_types_supported", List.of()),
                    stringList(json, "code_challenge_methods_supported", List.of()),
                    stringList(json, "id_token_signing_alg_values_supported", List.of()),
                    stringList(json, "token_endpoint_auth_methods_supported", List.of("client_secret_basic")),
                    booleanValue(json, "backchannel_logout_supported"),
                    booleanValue(json, "backchannel_logout_session_supported"),
                    clock.instant());
        } catch (ServiceException failure) {
            throw diagnosticFailure("OIDC Discovery 元数据或端点格式无效，请检查提供方配置", failure);
        }
    }

    private static ServiceException diagnosticFailure(String message, ServiceException cause) {
        var failure = new ServiceException(message, cause);
        failure.setCode(cause.getCode());
        return failure;
    }

    /**
     * 校验配置地址，复用协议请求的地址策略且不发起网络访问。
     *
     * @param value 不含用户信息或片段的绝对 HTTP(S) 地址；HTTP 仅在全部激活环境均为 local/dev 时允许
     * @throws ServiceException 地址或协议不符合当前运行环境策略
     */
    public void validateUrl(String value) {
        checkedUri(value);
    }

    private Map<String, Object> fetchDiscovery(String issuer) {
        checkedUri(issuer);
        Map<String, Object> json =
                getJson(issuer.replaceAll("/$", "") + "/.well-known/openid-configuration", null);
        if (!issuer.equals(required(json, "issuer"))) throw failure();
        return json;
    }

    private OidcMetadata metadata(String issuer, Map<String, Object> json) {
        return new OidcMetadata(
                issuer,
                endpoint(json, "authorization_endpoint", true),
                endpoint(json, "token_endpoint", true),
                endpoint(json, "jwks_uri", true),
                endpoint(json, "userinfo_endpoint", false),
                endpoint(json, "end_session_endpoint", false));
    }

    private static List<String> stringList(Map<String, Object> json, String name, List<String> fallback) {
        if (!json.containsKey(name)) return fallback;
        if (!(json.get(name) instanceof List<?> values)
                || values.stream().anyMatch(value -> !(value instanceof String text) || text.isBlank()))
            throw failure();
        return values.stream().map(String.class::cast).distinct().toList();
    }

    private static boolean booleanValue(Map<String, Object> json, String name) {
        if (!json.containsKey(name)) return false;
        if (!(json.get(name) instanceof Boolean value)) throw failure();
        return value;
    }

    /** 构建标准授权地址，私有业务上下文仅保存于本地事务。 */
    public String authorize(
            OidcClientSettings settings, String state, String nonce, String verifier) {
        OidcMetadata metadata = discovery(settings.issuer());
        Map<String, String> query = new LinkedHashMap<>();
        query.put("response_type", "code");
        query.put("client_id", settings.clientId());
        query.put("redirect_uri", settings.redirectUri());
        query.put("scope", String.join(" ", settings.scopes()));
        query.put("state", state);
        query.put("nonce", nonce);
        query.put("code_challenge", challenge(verifier));
        query.put("code_challenge_method", "S256");
        return appendQuery(metadata.authorizationEndpoint(), query);
    }

    /** 授权码仅交换一次；网络结果不明时要求用户重新发起授权。 */
    public OidcIdentity exchange(
            OidcClientSettings settings, String code, String verifier, String nonce) {
        OidcMetadata metadata = discovery(settings.issuer());
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", settings.redirectUri());
        form.put("code_verifier", verifier);
        HttpRequest.Builder request = request(metadata.tokenEndpoint());
        if ("client_secret_post".equals(settings.authenticationMethod())) {
            form.put("client_id", settings.clientId());
            form.put("client_secret", settings.clientSecret());
        } else if ("client_secret_basic".equals(settings.authenticationMethod())) {
            request.header(
                    "Authorization",
                    "Basic "
                            + Base64.getEncoder()
                                    .encodeToString(
                                            (encode(settings.clientId())
                                                            + ":"
                                                            + encode(settings.clientSecret()))
                                                    .getBytes(StandardCharsets.UTF_8)));
        } else throw new ServiceException("OIDC客户端认证方式不支持");
        Map<String, Object> token =
                json(
                        send(
                                request.header("Content-Type", "application/x-www-form-urlencoded")
                                        .POST(HttpRequest.BodyPublishers.ofString(form(form)))
                                        .build()));
        String idToken = required(token, "id_token");
        String accessToken = required(token, "access_token");
        if (!"Bearer".equalsIgnoreCase(required(token, "token_type"))) throw failure();
        JWTClaimsSet claims = verifiedClaims(metadata, settings.clientId(), idToken, false);
        try {
            if (!nonce.equals(claims.getStringClaim("nonce"))) throw failure();
            if (claims.getSubject() == null
                    || claims.getSubject().isBlank()
                    || claims.getSubject().length() > 255) throw failure();
            String accessHash = claims.getStringClaim("at_hash");
            if (accessHash != null
                    && !MessageDigest.isEqual(
                            accessHash.getBytes(StandardCharsets.US_ASCII),
                            Base64.getUrlEncoder()
                                    .withoutPadding()
                                    .encode(Arrays.copyOf(digest(accessToken), 16))))
                throw failure();
            Map<String, Object> profile = new LinkedHashMap<>(claims.getClaims());
            if (metadata.userInfoEndpoint() != null) {
                Map<String, Object> userInfo = getJson(metadata.userInfoEndpoint(), accessToken);
                if (!claims.getSubject().equals(required(userInfo, "sub"))) throw failure();
                // 只合并实际发布的标准资料；缺失/空值保留已验证 ID Token 的值。
                // iss、sub、sid、nonce、auth_time 等协议声明始终来自验签后的 ID Token。
                for (String name : PROFILE_CLAIMS) {
                    String value = text(userInfo, name);
                    if (value != null && !value.isBlank()) profile.put(name, value);
                }
            }
            Date authTime = claims.getDateClaim("auth_time");
            return new OidcIdentity(
                    settings.issuer(),
                    claims.getSubject(),
                    claims.getStringClaim("sid"),
                    displayName(profile),
                    text(profile, "phone_number"),
                    text(profile, "email"),
                    idToken,
                    metadata.endSessionEndpoint(),
                    authTime == null
                            ? claims.getIssueTime().toInstant().getEpochSecond()
                            : authTime.toInstant().getEpochSecond());
        } catch (java.text.ParseException e) {
            throw failure();
        }
    }

    /** 验证标准后台退出通知；调用方以 sid 幂等撤销本地业务会话。 */
    public String logoutSession(OidcClientSettings settings, String logoutToken) {
        JWTClaimsSet claims =
                verifiedClaims(
                        discovery(settings.issuer()), settings.clientId(), logoutToken, true);
        try {
            Map<String, Object> events = claims.getJSONObjectClaim("events");
            String sid = claims.getStringClaim("sid");
            if (sid == null
                    || sid.isBlank()
                    || sid.length() > 255
                    || claims.getClaim("nonce") != null
                    || claims.getJWTID() == null
                    || claims.getJWTID().isBlank()
                    || events == null
                    || !(events.get(LOGOUT_EVENT) instanceof Map)) throw failure();
            return sid;
        } catch (java.text.ParseException e) {
            throw failure();
        }
    }

    /** 验签器只接受明确支持的 RS256；kid 变化时重新取 JWKS，支持提供方轮换。 */
    JWTClaimsSet verifiedClaims(
            OidcMetadata metadata, String clientId, String token, boolean logout) {
        try {
            if (token.length() > 32768) throw failure();
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())
                    || jwt.getHeader().getCriticalParams() != null) throw failure();
            JWKSet keys = JWKSet.parse(send(request(metadata.jwksUri()).GET().build()));
            JWK key =
                    jwt.getHeader().getKeyID() == null && keys.getKeys().size() == 1
                            ? keys.getKeys().getFirst()
                            : keys.getKeyByKeyId(jwt.getHeader().getKeyID());
            if (!(key instanceof RSAKey rsa)
                    || rsa.size() < 2048
                    || !jwt.verify(new RSASSAVerifier(rsa.toRSAPublicKey()))) throw failure();
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            long now = clock.instant().getEpochSecond();
            if (!metadata.issuer().equals(claims.getIssuer())
                    || !claims.getAudience().contains(clientId)
                    || claims.getExpirationTime() == null
                    || claims.getExpirationTime().toInstant().getEpochSecond() <= now
                    || claims.getIssueTime() == null
                    || claims.getIssueTime().toInstant().getEpochSecond() > now + 60
                    || claims.getNotBeforeTime() != null
                            && claims.getNotBeforeTime().toInstant().getEpochSecond() > now + 60)
                throw failure();
            String azp = claims.getStringClaim("azp");
            if (!logout
                    && (claims.getAudience().size() > 1 && !clientId.equals(azp)
                            || azp != null && !clientId.equals(azp))) throw failure();
            if (logout && claims.getIssueTime().toInstant().getEpochSecond() < now - 300)
                throw failure();
            return claims;
        } catch (java.text.ParseException | com.nimbusds.jose.JOSEException e) {
            throw failure();
        }
    }

    private Map<String, Object> getJson(String uri, String bearer) {
        HttpRequest.Builder builder = request(uri);
        if (bearer != null) builder.header("Authorization", "Bearer " + bearer);
        return json(send(builder.GET().build()));
    }

    private HttpRequest.Builder request(String uri) {
        return HttpRequest.newBuilder(checkedUri(uri))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");
    }

    private URI checkedUri(String value) {
        if (value == null || value.isBlank()) throw failure();
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null
                    || uri.getRawUserInfo() != null
                    || uri.getRawFragment() != null
                    || !("https".equals(uri.getScheme())
                            || allowHttp && "http".equals(uri.getScheme()))) throw failure();
            return uri;
        } catch (IllegalArgumentException e) {
            throw failure();
        }
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 429 || response.statusCode() >= 500)
                throw new ServiceException("SSO服务暂不可用，请稍后重试", 503);
            if (response.statusCode() != 200 || response.body().length() > 1_048_576)
                throw failure();
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("OIDC请求已取消，请重新登录", 503);
        } catch (java.io.IOException e) {
            throw new ServiceException("SSO服务暂不可用，请重新登录或使用本地登录", 503);
        }
    }

    private static Map<String, Object> json(String value) {
        try {
            Map<String, Object> result = JsonUtils.parseMap(value);
            if (result == null) throw failure();
            return result;
        } catch (RuntimeException e) {
            throw failure();
        }
    }

    private String endpoint(Map<String, Object> values, String key, boolean required) {
        String value = required ? required(values, key) : text(values, key);
        if (value != null) checkedUri(value);
        return value;
    }

    private static String required(Map<String, Object> values, String key) {
        String value = text(values, key);
        if (value == null || value.isBlank()) throw failure();
        return value;
    }

    private static String text(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof String s ? s : null;
    }

    private static String displayName(Map<String, Object> profile) {
        for (String key : List.of("name", "nickname", "preferred_username")) {
            String value = text(profile, key);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    /** 对完整登记地址追加协议参数，不接收浏览器提供的任意目标 URL。 */
    public static String appendQuery(String url, Map<String, String> query) {
        return url + (url.contains("?") ? "&" : "?") + form(query);
    }

    private static String form(Map<String, String> values) {
        return values.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static ServiceException failure() {
        return new ServiceException("OIDC响应校验失败，请重新发起登录");
    }

    @Override
    @PreDestroy
    public void close() {
        http.close();
    }
}
