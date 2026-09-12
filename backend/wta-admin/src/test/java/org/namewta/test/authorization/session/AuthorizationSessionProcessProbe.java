package org.namewta.test.authorization.session;

import org.namewta.common.redis.cache.CacheInvalidationHandler;
import org.namewta.common.redis.cache.ClusterCacheInvalidationCoordinator;
import org.namewta.common.redis.cache.RedissonCacheInvalidationTransport;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.service.ClientSessionService;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Child-process authorization snapshot used by the two-JVM Redis test.
 */
public final class AuthorizationSessionProcessProbe {

    private static final String NAMESPACE = "authorization-session";
    private static final String CLIENT_A_TOKEN_1 = "client-a-token-1";
    private static final String CLIENT_A_TOKEN_2 = "client-a-token-2";
    private static final String CLIENT_B_TOKEN = "client-b-token";

    private AuthorizationSessionProcessProbe() {
    }

    public static void main(String[] args) throws Exception {
        RedissonClient redis = client(Integer.parseInt(args[0]));
        Map<String, LoginUser> localSnapshots = snapshots();
        RMap<String, String> distributedTokens = redis.getMap("test:authorization:sessions", StringCodec.INSTANCE);
        localSnapshots.forEach((token, loginUser) -> distributedTokens.putIfAbsent(token, "active"));
        try (ClusterCacheInvalidationCoordinator coordinator = new ClusterCacheInvalidationCoordinator(
            new RedissonCacheInvalidationTransport(redis, JsonMapper.builder().build()),
            Duration.ofSeconds(2), args[1]);
             BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            coordinator.register(NAMESPACE, handler(localSnapshots));
            ClientSessionService service = new ClientSessionService(
                new RedisBackedTokenOperations(localSnapshots, distributedTokens, coordinator));
            System.out.println("SESSION_PROBE_READY");
            String command;
            while ((command = input.readLine()) != null) {
                if ("invalidate-client-a".equals(command)) {
                    ClientSessionService.InvalidationResult result = service.kickoutClient(10L);
                    System.out.println("SESSION_INVALIDATED_" + result.invalidatedTokenCount());
                } else if ("state".equals(command)) {
                    boolean isolated = !localSnapshots.containsKey(CLIENT_A_TOKEN_1)
                        && !localSnapshots.containsKey(CLIENT_A_TOKEN_2)
                        && localSnapshots.containsKey(CLIENT_B_TOKEN)
                        && !distributedTokens.containsKey(CLIENT_A_TOKEN_1)
                        && !distributedTokens.containsKey(CLIENT_A_TOKEN_2)
                        && distributedTokens.containsKey(CLIENT_B_TOKEN);
                    System.out.println(isolated ? "SESSION_CLIENT_ISOLATED" : "SESSION_STATE_INVALID");
                } else if ("close".equals(command)) {
                    break;
                }
            }
        } finally {
            redis.shutdown();
        }
    }

    private static Map<String, LoginUser> snapshots() {
        Map<String, LoginUser> snapshots = new LinkedHashMap<>();
        snapshots.put(CLIENT_A_TOKEN_1, loginUser(1L, 10L));
        snapshots.put(CLIENT_A_TOKEN_2, loginUser(2L, 10L));
        snapshots.put(CLIENT_B_TOKEN, loginUser(1L, 20L));
        return snapshots;
    }

    private static LoginUser loginUser(Long userId, Long clientPk) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setClientPk(clientPk);
        return loginUser;
    }

    private static CacheInvalidationHandler handler(Map<String, LoginUser> localSnapshots) {
        return new CacheInvalidationHandler() {
            @Override
            public void invalidate(String keyFingerprint) {
                localSnapshots.keySet().removeIf(token ->
                    ClusterCacheInvalidationCoordinator.fingerprint(token).equals(keyFingerprint));
            }

            @Override
            public void clear() {
                localSnapshots.clear();
            }
        };
    }

    private static RedissonClient client(int port) {
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port);
        return Redisson.create(config);
    }

    private record RedisBackedTokenOperations(Map<String, LoginUser> localSnapshots,
                                              RMap<String, String> distributedTokens,
                                              ClusterCacheInvalidationCoordinator coordinator)
        implements ClientSessionService.TokenOperations {

        @Override
        public List<String> activeTokenValues() {
            return new ArrayList<>(localSnapshots.keySet());
        }

        @Override
        public LoginUser getLoginUser(String tokenValue) {
            return localSnapshots.get(tokenValue);
        }

        @Override
        public void logout(String tokenValue) {
            distributedTokens.remove(tokenValue);
            coordinator.invalidate(NAMESPACE, tokenValue);
        }
    }
}
