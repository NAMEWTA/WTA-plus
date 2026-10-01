package org.namewta.web.service.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import cn.dev33.satoken.stp.StpUtil;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.MockedStatic;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.redis.config.RedisConfig;
import org.namewta.common.redis.config.properties.RedissonProperties;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.common.social.oidc.OidcIdentity;
import org.namewta.system.api.model.ExternalAuthRegistration;
import org.namewta.web.domain.vo.LoginVo;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 用真实 Redis、生产 codec 和锁验证 RP 签发/全退竞态。每例独立 JVM 隔离 RedisUtils 的静态客户端， 仅替换 SaToken
 * 的撤销动作；独占随机前缀的数据在退出时清理。
 */
@Tag("dev")
@EnabledIfSystemProperty(named = "external.session.redis.integration", matches = "true")
class ExternalAuthSessionRedisIntegrationTest {

    @Test
    void logoutArrivingBeforeIssueRejectsTheDelayedCallback() throws Exception {
        runProbe("logout-first");
    }

    @Test
    void logoutWaitsForIssueAndRevokesTheNewBusinessToken() throws Exception {
        runProbe("issue-first");
    }

    @Test
    void otherSidClientIssuerAndLocalPasswordSessionsRemainActive() throws Exception {
        runProbe("isolation");
    }

    @Test
    void duplicateLogoutIsIdempotentAndKeepsTheRevocationTombstone() throws Exception {
        runProbe("duplicate");
    }

    @Test
    void oidcWithoutSidStoresEncryptedExpiringRpSessionAndCanLogoutOnAnotherNode() throws Exception {
        runProbe("no-sid");
    }

    private void runProbe(String scenario) throws Exception {
        int port = Integer.getInteger("external.session.redis.port", -1);
        assertTrue(port > 0, "请显式提供任务自建 Redis 的 loopback 端口");
        Path output = Files.createTempFile("external-session-redis-", ".log");
        Process process = null;
        try {
            process =
                    new ProcessBuilder(
                                    Path.of(System.getProperty("java.home"), "bin", "java")
                                            .toString(),
                                    "-cp",
                                    System.getProperty("java.class.path"),
                                    Probe.class.getName(),
                                    Integer.toString(port),
                                    scenario)
                            .redirectErrorStream(true)
                            .redirectOutput(output.toFile())
                            .start();
            assertTrue(process.waitFor(40, TimeUnit.SECONDS), "Redis 子进程超时: " + scenario);
            assertEquals(0, process.exitValue(), () -> readOutput(output));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
            Files.deleteIfExists(output);
        }
    }

    private static String readOutput(Path output) {
        try {
            return Files.readString(output);
        } catch (Exception failure) {
            return failure.toString();
        }
    }

    /** 子进程只持有本例随机前缀；不连接现有业务 Redis 或刷新整个逻辑库。 */
    public static final class Probe {

        private final Set<String> tokens = ConcurrentHashMap.newKeySet();
        private final AtomicInteger revoked = new AtomicInteger();
        private final ExternalAuthSessionStore store =
                new ExternalAuthSessionStore(
                        new SocialSecretCipher(Base64.getEncoder().encodeToString(new byte[32])));
        private final ExternalAuthRegistration registration =
                registration("https://central.test", "rp-home");

        public static void main(String[] args) throws Exception {
            String prefix = "owned-rp-session-" + UUID.randomUUID();
            RedissonClient redis = connect(Integer.parseInt(args[0]), prefix);
            try (var context = new AnnotationConfigApplicationContext()) {
                context.registerBean(RedissonClient.class, () -> redis);
                context.registerBean(SpringUtils.class);
                context.refresh();
                Probe probe = new Probe();
                switch (args[1]) {
                    case "logout-first" -> probe.logoutFirst();
                    case "issue-first" -> probe.issueFirst();
                    case "isolation" -> probe.isolation();
                    case "duplicate" -> probe.duplicate();
                    case "no-sid" -> probe.noSid();
                    default -> throw new IllegalArgumentException("未知场景");
                }
            } finally {
                try {
                    redis.getKeys().deleteByPattern(prefix + ":*");
                } finally {
                    redis.shutdown();
                }
            }
        }

        private void logoutFirst() {
            revoke(registration, "sid-a");
            AtomicInteger issued = new AtomicInteger();
            assertThrows(
                    ServiceException.class,
                    () ->
                            store.issue(
                                    registration,
                                    identity(registration, "sid-a"),
                                    key -> {
                                        issued.incrementAndGet();
                                        return issueToken("late-token");
                                    }));
            assertEquals(0, issued.get());
            assertTrue(tokens.isEmpty());
        }

        private void issueFirst() throws Exception {
            CountDownLatch insideIssue = new CountDownLatch(1);
            CountDownLatch releaseIssue = new CountDownLatch(1);
            CountDownLatch logoutStarted = new CountDownLatch(1);
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var issuing =
                        executor.submit(
                                () ->
                                        store.issue(
                                                registration,
                                                identity(registration, "sid-a"),
                                                key -> {
                                                    LoginVo token = issueToken("concurrent-token");
                                                    insideIssue.countDown();
                                                    await(releaseIssue);
                                                    return token;
                                                }));
                assertTrue(insideIssue.await(5, TimeUnit.SECONDS));
                var loggingOut =
                        executor.submit(
                                () -> revoke(registration, "sid-a", logoutStarted::countDown));
                try {
                    assertTrue(logoutStarted.await(5, TimeUnit.SECONDS));
                    assertThrows(
                            TimeoutException.class,
                            () -> loggingOut.get(150, TimeUnit.MILLISECONDS));
                    assertTrue(tokens.contains("concurrent-token"));
                } finally {
                    releaseIssue.countDown();
                }
                assertEquals("concurrent-token", issuing.get(5, TimeUnit.SECONDS).getAccessToken());
                loggingOut.get(5, TimeUnit.SECONDS);
                assertFalse(tokens.contains("concurrent-token"));
                assertEquals(1, revoked.get());
            } finally {
                releaseIssue.countDown();
            }
        }

        private void isolation() {
            ExternalAuthRegistration otherClient = registration("https://central.test", "rp-admin");
            ExternalAuthRegistration otherIssuer =
                    registration("https://other-central.test", "rp-home");
            issue(registration, "sid-a", "selected-token");
            issue(registration, "sid-b", "other-device-token");
            issue(otherClient, "sid-a", "other-client-token");
            issue(otherIssuer, "sid-a", "other-issuer-token");
            issueToken("local-password-token");
            revoke(registration, "sid-a");
            assertEquals(
                    Set.of(
                            "other-device-token",
                            "other-client-token",
                            "other-issuer-token",
                            "local-password-token"),
                    tokens);
            assertEquals(1, revoked.get());
        }

        private void duplicate() {
            issue(registration, "sid-a", "selected-token");
            revoke(registration, "sid-a");
            revoke(registration, "sid-a");
            assertEquals(1, revoked.get());
            assertTrue(tokens.isEmpty());
            assertThrows(ServiceException.class, () -> issue(registration, "sid-a", "late-token"));
        }

        private void noSid() {
            AtomicReference<String> sessionId = new AtomicReference<>();
            var result = store.issue(registration, identity(registration, null), key -> {
                sessionId.set(key);
                return issueToken("no-sid-token");
            });
            assertNotNull(sessionId.get());
            assertTrue(result.isGlobalLogoutAvailable());
            String key = "auth:external:session:" + sessionId.get();
            String encrypted = RedisUtils.getCacheObject(key);
            assertNotNull(encrypted);
            assertFalse(encrypted.contains("owned-id-token"));
            assertTrue(RedisUtils.getTimeToLive(key) > 3_500_000);
            assertTrue(RedisUtils.getTimeToLive(key) <= 3_600_000);
            var cipher = new SocialSecretCipher(Base64.getEncoder().encodeToString(new byte[32]));
            var remembered = JsonUtils.parseObject(cipher.decrypt("rp-session", encrypted), ExternalAuthSessionStore.Session.class);
            assertNull(remembered.sid());
            assertEquals("owned-id-token", remembered.idToken());
            try (var keys = RedisUtils.getClient().getKeys().getKeysStreamByPattern("auth:external:sid:*")) {
                assertEquals(0, keys.count());
            }

            // 模拟同集群另一个业务实例，使用相同根密钥及共享 Redis 读取发起节点的会话。
            var anotherNode = new ExternalAuthSessionStore(cipher);
            try (MockedStatic<StpUtil> actions = mockStatic(StpUtil.class)) {
                actions.when(() -> StpUtil.getExtra(ExternalAuthSessionStore.SESSION_EXTRA)).thenReturn(sessionId.get());
                actions.when(() -> StpUtil.getExtra(ExternalAuthSessionStore.SOURCE_EXTRA)).thenReturn("OIDC");
                var status = anotherNode.status();
                assertTrue(status.rpInitiatedLogoutAvailable());
                assertTrue(status.globalLogoutAvailable());
                assertFalse(status.backchannelSessionLinked());
                var logout = anotherNode.beginLogout();
                assertTrue(logout.endSessionUrl().startsWith(registration.issuer() + "/logout?"));
                assertTrue(logout.endSessionUrl().contains("id_token_hint=owned-id-token"));
                assertTrue(logout.endSessionUrl().contains("post_logout_redirect_uri=https%3A%2F%2Frp.test%2Flogged-out"));
                actions.verify(StpUtil::logout);
            }
            assertTrue(tokens.contains("no-sid-token"));
            revoke(registration, "unrelated-sid");
            assertTrue(tokens.contains("no-sid-token"));
        }

        private void issue(ExternalAuthRegistration client, String sid, String token) {
            LoginVo result =
                    store.issue(
                            client,
                            identity(client, sid),
                            sessionKey -> {
                                assertTrue(sessionKey.matches("[A-Za-z0-9_-]{43}"));
                                return issueToken(token);
                            });
            assertTrue(result.isGlobalLogoutAvailable());
        }

        private LoginVo issueToken(String value) {
            tokens.add(value);
            LoginVo result = new LoginVo();
            result.setAccessToken(value);
            result.setExpireIn(3600L);
            return result;
        }

        private void revoke(ExternalAuthRegistration client, String sid) {
            revoke(client, sid, () -> {});
        }

        private void revoke(ExternalAuthRegistration client, String sid, Runnable beforeRevoke) {
            try (MockedStatic<StpUtil> tokenActions = mockStatic(StpUtil.class)) {
                tokenActions
                        .when(() -> StpUtil.logoutByTokenValue(anyString()))
                        .thenAnswer(
                                call -> {
                                    revoked.incrementAndGet();
                                    tokens.remove(call.<String>getArgument(0));
                                    return null;
                                });
                beforeRevoke.run();
                store.revoke(client, sid);
            }
        }

        private static OidcIdentity identity(ExternalAuthRegistration client, String sid) {
            return new OidcIdentity(
                    client.issuer(),
                    "subject-a",
                    sid,
                    "Owned Test",
                    null,
                    null,
                    "owned-id-token",
                    client.issuer() + "/logout",
                    1L);
        }

        private static ExternalAuthRegistration registration(String issuer, String client) {
            return new ExternalAuthRegistration(
                    1,
                    1,
                    1,
                    "owned",
                    "OIDC",
                    issuer,
                    "Owned",
                    "",
                    "home",
                    client,
                    "owned-secret",
                    "https://rp.test/callback",
                    "https://rp.test/logged-out",
                    List.of("openid"),
                    "BIND_ONLY",
                    Map.of());
        }

        private static void await(CountDownLatch latch) {
            try {
                assertTrue(latch.await(10, TimeUnit.SECONDS), "并发用例未释放签发屏障");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            }
        }

        private static RedissonClient connect(int port, String prefix) {
            var properties = new RedissonProperties();
            properties.setThreads(2);
            properties.setNettyThreads(2);
            properties.setKeyPrefix(prefix);
            var customizer = new RedisConfig();
            ReflectionTestUtils.setField(customizer, "redissonProperties", properties);
            Config config = new Config();
            customizer.redissonCustomizer().customize(config);
            config.useSingleServer()
                    .setAddress("redis://127.0.0.1:" + port)
                    .setDatabase(14)
                    .setConnectionMinimumIdleSize(1)
                    .setConnectionPoolSize(4);
            return Redisson.create(config);
        }
    }
}
