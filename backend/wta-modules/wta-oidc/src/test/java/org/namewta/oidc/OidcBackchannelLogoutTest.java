package org.namewta.oidc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.*;
import org.namewta.oidc.adapter.provider.OidcBackchannelSender;
import org.namewta.oidc.dao.OidcLogoutOutboxDao;
import org.namewta.oidc.domain.OidcLogoutOutbox;
import org.namewta.oidc.port.OidcKeyPort;
import org.namewta.oidc.service.OidcLogoutService;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.concurrent.atomic.AtomicReference;

@Tag("dev")
class OidcBackchannelLogoutTest {
    @Test
    void actualHttpCarriesSignedStandardLogoutTokenAndAcceptsEmpty204() throws Exception {
        var key = new RSAKeyGenerator(2048).keyID("rotated").generate();
        var keys = mock(OidcKeyPort.class);
        when(keys.active()).thenReturn(key);
        var sender = new OidcBackchannelSender(keys);
        var received = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/logout",
                exchange -> {
                    received.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    java.nio.charset.StandardCharsets.UTF_8));
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                });
        server.start();
        try {
            var row = row();
            row.setTargetUri("http://127.0.0.1:" + server.getAddress().getPort() + "/logout");
            assertThat(sender.deliver(row)).isEqualTo(204);
            String raw =
                    URLDecoder.decode(
                            received.get().substring("logout_token=".length()),
                            java.nio.charset.StandardCharsets.UTF_8);
            var token = SignedJWT.parse(raw);
            assertThat(token.verify(new RSASSAVerifier(key.toPublicJWK()))).isTrue();
            var claims = token.getJWTClaimsSet();
            assertThat(claims.getIssuer()).isEqualTo("https://sso.example");
            assertThat(claims.getAudience()).containsExactly("rp");
            assertThat(claims.getStringClaim("sid")).isEqualTo("session-hash");
            assertThat(claims.getClaim("nonce")).isNull();
            assertThat(claims.getExpirationTime()).isAfter(claims.getIssueTime());
            assertThat(claims.getJSONObjectClaim("events"))
                    .containsKey("http://schemas.openid.net/event/backchannel-logout");
            assertThat(token.getHeader().getType().toString()).isEqualTo("logout+jwt");
            assertThat(SignedJWT.parse(sender.token(row)).getJWTClaimsSet().getJWTID())
                    .isNotEqualTo(claims.getJWTID());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void recoverableFailuresRetryButPermanentFailureAndExhaustionRemainVisible() {
        var dao = mock(OidcLogoutOutboxDao.class);
        var service = new OidcLogoutService(dao);
        var row = row();
        row.setAttempts(1);
        service.result(row, 503);
        verify(dao).result(eq(row), eq("READY"), eq("HTTP_503"), any());
        service.result(row, 400);
        verify(dao).result(eq(row), eq("FAILED"), eq("HTTP_400"), any());
        row.setAttempts(12);
        service.result(row, 0);
        verify(dao).result(eq(row), eq("FAILED"), eq("NETWORK_ERROR"), any());
        service.result(row, 200);
        verify(dao).result(eq(row), eq("DONE"), isNull(), any());
    }

    private OidcLogoutOutbox row() {
        var row = new OidcLogoutOutbox();
        row.setClientId("rp");
        row.setIssuer("https://sso.example");
        row.setSubject("sub");
        row.setSessionHash("session-hash");
        return row;
    }
}
