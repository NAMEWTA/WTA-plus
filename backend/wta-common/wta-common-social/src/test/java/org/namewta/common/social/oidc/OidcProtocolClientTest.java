package org.namewta.common.social.oidc;

import static org.assertj.core.api.Assertions.*;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.*;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** 使用真实 HTTP、RSA 签名和协议表单，验证 RP 不接受替换身份或错误受众。 */
@Tag("dev")
class OidcProtocolClientTest {
    private static final Instant NOW = Instant.parse("2026-09-30T12:00:00Z");
    private HttpServer server;
    private OidcProtocolClient client;
    private RSAKey signingKey;
    private String issuer;
    private OidcMetadata metadata;
    private OidcClientSettings settings;
    private String tokenResponse;
    private volatile int jwksStatus = 200;
    private String userInfo =
            "{\"sub\":\"subject-a\",\"name\":\"测试用户\",\"phone_number\":\"13800000001\"}";
    private final AtomicReference<String> exchangeForm = new AtomicReference<>();
    private final AtomicReference<String> exchangeAuthorization = new AtomicReference<>();

    @BeforeEach
    void startOwnedProvider() throws Exception {
        signingKey = new RSAKeyGenerator(2048).keyID("owned-key").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        issuer = "http://127.0.0.1:" + server.getAddress().getPort();
        metadata =
                new OidcMetadata(
                        issuer,
                        issuer + "/authorize",
                        issuer + "/token",
                        issuer + "/jwks",
                        issuer + "/userinfo",
                        issuer + "/logout");
        server.createContext(
                "/jwks",
                exchange -> {
                    byte[] body =
                            new JWKSet(signingKey.toPublicJWK())
                                    .toString()
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(jwksStatus, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        server.createContext(
                "/token",
                exchange -> {
                    exchangeForm.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8));
                    exchangeAuthorization.set(
                            exchange.getRequestHeaders().getFirst("Authorization"));
                    byte[] body = tokenResponse.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        server.createContext(
                "/userinfo",
                exchange -> {
                    byte[] body = userInfo.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        server.start();
        client =
                new OidcProtocolClient(
                        HttpClient.newHttpClient(), Clock.fixed(NOW, ZoneOffset.UTC), true) {
                    @Override
                    public OidcMetadata discovery(String configuredIssuer) {
                        return metadata;
                    }
                };
        settings =
                new OidcClientSettings(
                        issuer,
                        "admin-rp",
                        "owned-secret",
                        "https://admin.example/social-callback",
                        List.of("openid", "profile"),
                        "client_secret_basic");
        tokenResponse = token(claims().claim("nonce", "nonce-a").claim("sid", "sid-a").build());
    }

    @AfterEach
    void stopOwnedProvider() {
        client.close();
        server.stop(0);
    }

    @Test
    void codeFlowKeepsSecretOnServerAndReturnsVerifiedIdentity() {
        var identity = client.exchange(settings, "code-a", "verifier-a", "nonce-a");
        assertThat(identity.subject()).isEqualTo("subject-a");
        assertThat(identity.phoneNumber()).isEqualTo("13800000001");
        assertThat(identity.sessionId()).isEqualTo("sid-a");
        assertThat(exchangeForm.get())
                .contains(
                        "grant_type=authorization_code", "code_verifier=verifier-a", "code=code-a")
                .doesNotContain("owned-secret");
        assertThat(exchangeAuthorization.get()).startsWith("Basic ");
        assertThat(identity.toString()).doesNotContain(identity.idToken(), "subject-a");
    }

    @Test
    void jwksOutageIsRetryableAndCannotBeAcknowledgedAsAnInvalidLogoutToken() {
        for (int status : List.of(429, 500, 503)) {
            jwksStatus = status;
            assertThatThrownBy(() -> client.exchange(settings, "code-a", "verifier-a", "nonce-a"))
                    .isInstanceOfSatisfying(
                            ServiceException.class,
                            error -> assertThat(error.getCode()).isEqualTo(503));
        }
        jwksStatus = 200;
        assertThat(client.exchange(settings, "code-a", "verifier-a", "nonce-a").subject())
                .isEqualTo("subject-a");
    }

    @Test
    void authorizationCarriesStandardNonceStateAndS256() {
        String url = client.authorize(settings, "state-a", "nonce-a", "verifier-a");
        assertThat(url)
                .contains(
                        "response_type=code",
                        "client_id=admin-rp",
                        "state=state-a",
                        "nonce=nonce-a",
                        "code_challenge_method=S256")
                .doesNotContain("owned-secret", "code_verifier");
    }

    @Test
    void nonceAndUserInfoSubjectMustMatch() {
        assertThatThrownBy(() -> client.exchange(settings, "code", "verifier", "other-nonce"))
                .isInstanceOf(ServiceException.class);
        userInfo = "{\"sub\":\"another-user\"}";
        assertThatThrownBy(() -> client.exchange(settings, "code", "verifier", "nonce-a"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void rejectsWrongIssuerAudienceAndExpiry() throws Exception {
        for (JWTClaimsSet claims :
                List.of(
                        claims().issuer("https://another.example").build(),
                        claims().audience("home-rp").build(),
                        claims().expirationTime(Date.from(NOW)).build(),
                        claims().issueTime(Date.from(NOW.plusSeconds(120))).build())) {
            String jwt = signed(claims);
            assertThatThrownBy(() -> client.verifiedClaims(metadata, "admin-rp", jwt, false))
                    .isInstanceOf(ServiceException.class);
        }
    }

    @Test
    void multiAudienceRequiresTheCorrectAuthorizedParty() throws Exception {
        String absent = signed(claims().audience(List.of("admin-rp", "home-rp")).build());
        assertThatThrownBy(() -> client.verifiedClaims(metadata, "admin-rp", absent, false))
                .isInstanceOf(ServiceException.class);
        String accepted =
                signed(
                        claims().audience(List.of("admin-rp", "home-rp"))
                                .claim("azp", "admin-rp")
                                .build());
        assertThat(client.verifiedClaims(metadata, "admin-rp", accepted, false).getSubject())
                .isEqualTo("subject-a");
    }

    @Test
    void signatureFromAnUnpublishedKeyIsRejected() throws Exception {
        RSAKey other = new RSAKeyGenerator(2048).keyID("owned-key").generate();
        SignedJWT forged =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("owned-key").build(),
                        claims().build());
        forged.sign(new RSASSASigner(other));
        assertThatThrownBy(
                        () ->
                                client.verifiedClaims(
                                        metadata, "admin-rp", forged.serialize(), false))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void logoutRequiresEventSidExpiryAndNoNonce() throws Exception {
        var base =
                claims().jwtID("event-a")
                        .claim("sid", "central-a")
                        .claim(
                                "events",
                                Map.of(
                                        "http://schemas.openid.net/event/backchannel-logout",
                                        Map.of()));
        assertThat(client.logoutSession(settings, signed(base.build()))).isEqualTo("central-a");
        for (JWTClaimsSet invalid :
                List.of(
                        new JWTClaimsSet.Builder(base.build()).claim("nonce", "n").build(),
                        new JWTClaimsSet.Builder(base.build()).expirationTime(null).build(),
                        new JWTClaimsSet.Builder(base.build()).claim("events", Map.of()).build(),
                        new JWTClaimsSet.Builder(base.build()).claim("sid", null).build())) {
            String jwt = signed(invalid);
            assertThatThrownBy(() -> client.logoutSession(settings, jwt))
                    .isInstanceOf(ServiceException.class);
        }
    }

    @Test
    void issuerAndSubjectKeysPreserveCaseAndLengthBoundaries() {
        assertThat(OidcProtocolClient.identityKey("ab", "c"))
                .isNotEqualTo(OidcProtocolClient.identityKey("a", "bc"));
        assertThat(OidcProtocolClient.identityKey("issuer", "Alice"))
                .isNotEqualTo(OidcProtocolClient.identityKey("issuer", "alice"));
    }

    private JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience("admin-rp")
                .subject("subject-a")
                .issueTime(Date.from(NOW))
                .expirationTime(Date.from(NOW.plusSeconds(300)));
    }

    private String signed(JWTClaimsSet claims) throws Exception {
        SignedJWT jwt =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256)
                                .keyID(signingKey.getKeyID())
                                .build(),
                        claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    private String token(JWTClaimsSet claims) throws Exception {
        return JsonUtils.toJsonString(
                Map.of(
                        "access_token",
                        "owned-access-token",
                        "token_type",
                        "Bearer",
                        "id_token",
                        signed(claims)));
    }
}
