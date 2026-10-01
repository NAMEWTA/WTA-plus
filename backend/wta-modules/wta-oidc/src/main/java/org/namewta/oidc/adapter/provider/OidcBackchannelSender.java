package org.namewta.oidc.adapter.provider;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.domain.OidcLogoutOutbox;
import org.namewta.oidc.port.OidcKeyPort;
import org.namewta.oidc.port.OidcLogoutDeliveryPort;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/** 标准Back-Channel Logout发送器，不追随重定向，不泄露正文。 */
@Component
@RequiredArgsConstructor
public class OidcBackchannelSender implements OidcLogoutDeliveryPort {
    private final OidcKeyPort keys;
    private final HttpClient client =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

    /** 每次尝试生成短期JWT，持久幂等身份是issuer/sid/应用而非过期的JWT。 */
    @Override
    public int deliver(OidcLogoutOutbox row) {
        try {
            String token = token(row);
            var request =
                    HttpRequest.newBuilder(URI.create(row.getTargetUri()))
                            .timeout(Duration.ofSeconds(10))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            "logout_token="
                                                    + URLEncoder.encode(
                                                            token,
                                                            java.nio.charset.StandardCharsets
                                                                    .UTF_8)))
                            .build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } catch (java.io.IOException e) {
            return 0;
        }
    }

    /** 使用ID Token同一签名键，包含规范要求的exp且禁止nonce。 */
    public String token(OidcLogoutOutbox row) {
        try {
            var key = keys.active();
            var now = Instant.now();
            var claims =
                    new JWTClaimsSet.Builder()
                            .issuer(row.getIssuer())
                            .audience(row.getClientId())
                            .issueTime(Date.from(now))
                            .expirationTime(Date.from(now.plusSeconds(120)))
                            .jwtID(UUID.randomUUID().toString())
                            .subject(row.getSubject())
                            .claim("sid", row.getSessionHash())
                            .claim(
                                    "events",
                                    Map.of(
                                            "http://schemas.openid.net/event/backchannel-logout",
                                            Map.of()))
                            .build();
            var jwt =
                    new SignedJWT(
                            new JWSHeader.Builder(JWSAlgorithm.RS256)
                                    .keyID(key.getKeyID())
                                    .type(new JOSEObjectType("logout+jwt"))
                                    .build(),
                            claims);
            jwt.sign(new RSASSASigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("退出凭据签发失败", e);
        }
    }
}
