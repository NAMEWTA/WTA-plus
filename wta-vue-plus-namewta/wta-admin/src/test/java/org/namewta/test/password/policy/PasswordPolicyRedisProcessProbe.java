package org.namewta.test.password.policy;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.namewta.common.redis.cache.ClusterCacheInvalidationCoordinator;
import org.namewta.common.redis.cache.RedissonCacheInvalidationTransport;
import org.namewta.common.redis.manager.CaffeineCacheDecorator;
import org.namewta.system.password.PasswordPolicy;
import org.namewta.system.password.PasswordPolicyConfigParser;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.spring.cache.RedissonCache;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 双 JVM 配置刷新测试使用的子进程。
 */
public final class PasswordPolicyRedisProcessProbe {

    static final String MAP_NAME = "password-policy-config-e2e";
    static final String OLD_POLICY = policy(8);
    static final String NEW_POLICY = policy(10);

    private PasswordPolicyRedisProcessProbe() {
    }

    public static void main(String[] args) throws Exception {
        RedissonClient client = client(Integer.parseInt(args[0]));
        try (ClusterCacheInvalidationCoordinator coordinator = new ClusterCacheInvalidationCoordinator(
            new RedissonCacheInvalidationTransport(client, JsonMapper.builder().build()),
            Duration.ofSeconds(2), args[1]);
             BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            RedissonCache distributed = new RedissonCache(client.getMap(MAP_NAME), true);
            distributed.putIfAbsent(PasswordPolicy.CONFIG_KEY, OLD_POLICY);
            CaffeineCacheDecorator cache = new CaffeineCacheDecorator("sys_config", distributed,
                Caffeine.newBuilder().build(), coordinator);
            cache.get(PasswordPolicy.CONFIG_KEY, String.class);
            PasswordPolicyConfigParser parser = new PasswordPolicyConfigParser(JsonMapper.builder().build());
            System.out.println("POLICY_PROBE_READY");
            String command;
            while ((command = input.readLine()) != null) {
                if ("update".equals(command)) {
                    cache.put(PasswordPolicy.CONFIG_KEY, NEW_POLICY);
                    System.out.println("POLICY_UPDATED");
                } else if ("minimum".equals(command)) {
                    String value = cache.get(PasswordPolicy.CONFIG_KEY, String.class);
                    System.out.println("POLICY_MINIMUM_" + parser.parse(value).minimumLength());
                } else if ("close".equals(command)) {
                    break;
                }
            }
        } finally {
            client.shutdown();
        }
    }

    static RedissonClient client(int port) {
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port);
        return Redisson.create(config);
    }

    private static String policy(int minimumLength) {
        return "{\"version\":1,\"minimumLength\":" + minimumLength
            + ",\"maximumLength\":30,\"requireUppercase\":true,\"requireLowercase\":true,"
            + "\"requireDigit\":true,\"requireSpecial\":true,\"allowedSpecialCharacters\":\"@$!%*?&\","
            + "\"generator\":{\"length\":12,\"uppercaseCharacters\":\"ABCDEFGHJKLMNPQRSTUVWXYZ\","
            + "\"lowercaseCharacters\":\"abcdefghijkmnopqrstuvwxyz\",\"digitCharacters\":\"23456789\","
            + "\"specialCharacters\":\"@$!%*?&\"},\"defaultPassword\":{\"mode\":\"RANDOM\"}}";
    }
}
