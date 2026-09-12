package org.namewta.notify.support.outbox;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.Config;

/**
 * T-03 跨进程门禁的 Redis 夹具。缺连接时失败，禁止 skip 充当通过。
 */
public final class NotifyOutboxWakeRedisGate {

    private NotifyOutboxWakeRedisGate() {
    }

    /**
     * 建立独立 Redisson 客户端；连不上则抛出断言失败。
     *
     * @return 已连接的客户端，调用方负责 shutdown
     */
    public static RedissonClient connectOrFail(String role) {
        String host = setting("notify.outbox.wake.redis.host", "REDIS_HOST", "127.0.0.1");
        String portText = setting("notify.outbox.wake.redis.port", "REDIS_PORT", "6379");
        String password = setting("notify.outbox.wake.redis.password", "REDIS_PASSWORD", "");
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            throw fail(role, host, portText, "port is not an integer");
        }
        Config config = new Config();
        config.setCodec(new SerializationCodec());
        var server = config.useSingleServer().setAddress("redis://" + host + ":" + port).setTimeout(2000);
        if (!password.isBlank()) {
            server.setPassword(password);
        }
        RedissonClient client = Redisson.create(config);
        try {
            client.getKeys().count();
            return client;
        } catch (RuntimeException ex) {
            client.shutdown();
            throw fail(role, host, portText, ex.getMessage());
        }
    }

    private static AssertionError fail(String role, String host, String port, String detail) {
        return new AssertionError(
            "AC-002 requires Redis for " + role + " at " + host + ":" + port
                + " (set REDIS_PASSWORD / notify.outbox.wake.redis.*). Do not skip. Detail: " + detail);
    }

    private static String setting(String property, String environment, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environment);
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
