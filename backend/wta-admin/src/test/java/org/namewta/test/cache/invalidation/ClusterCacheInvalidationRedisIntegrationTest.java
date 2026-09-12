package org.namewta.test.cache.invalidation;

import org.namewta.common.redis.cache.CacheInvalidationHandler;
import org.namewta.common.redis.cache.ClusterCacheInvalidationCoordinator;
import org.namewta.common.redis.cache.RedissonCacheInvalidationTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("dev")
class ClusterCacheInvalidationRedisIntegrationTest {

    private RedissonClient clientA;
    private RedissonClient clientB;

    @AfterEach
    void shutdown() {
        if (clientA != null) {
            clientA.shutdown();
        }
        if (clientB != null) {
            clientB.shutdown();
        }
    }

    @Test
    void shouldConfirmRemoteEvictionAcrossTwoRedisClients() {
        int port = redisPort();
        clientA = client(port);
        clientB = client(port);
        JsonMapper jsonMapper = JsonMapper.builder().build();
        Map<String, String> nodeA = new ConcurrentHashMap<>();
        Map<String, String> nodeB = new ConcurrentHashMap<>();
        String key = "spring:user:user-42";
        nodeA.put(key, "old-a");
        nodeB.put(key, "old-b");

        try (ClusterCacheInvalidationCoordinator coordinatorA = new ClusterCacheInvalidationCoordinator(
            new RedissonCacheInvalidationTransport(clientA, jsonMapper), Duration.ofSeconds(2), "node-a");
             ClusterCacheInvalidationCoordinator coordinatorB = new ClusterCacheInvalidationCoordinator(
                 new RedissonCacheInvalidationTransport(clientB, jsonMapper), Duration.ofSeconds(2), "node-b")) {
            coordinatorA.register("spring:user", handler(nodeA));
            coordinatorB.register("spring:user", handler(nodeB));

            coordinatorA.invalidate("spring:user", key);

            assertFalse(nodeA.containsKey(key));
            assertFalse(nodeB.containsKey(key));
        }
    }

    @Test
    void shouldInvalidateAcrossTwoIndependentJvmProcesses() throws Exception {
        int port = redisPort();
        try (ProbeProcess nodeA = ProbeProcess.start(port, "process-a");
             ProbeProcess nodeB = ProbeProcess.start(port, "process-b")) {
            nodeA.await("PROBE_READY");
            nodeB.await("PROBE_READY");

            nodeA.send("invalidate");
            nodeA.await("PROBE_INVALIDATED");
            nodeB.send("contains");

            nodeB.await("PROBE_MISSING");
        }
    }

    private static int redisPort() {
        int port = Integer.getInteger("cache.redis.integration.port", -1);
        Assumptions.assumeTrue(port > 0, "requires a disposable Redis port");
        return port;
    }

    private static RedissonClient client(int port) {
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port);
        return Redisson.create(config);
    }

    private static CacheInvalidationHandler handler(Map<String, String> cache) {
        return new CacheInvalidationHandler() {
            @Override
            public void invalidate(String keyFingerprint) {
                cache.keySet().removeIf(key ->
                    ClusterCacheInvalidationCoordinator.fingerprint(key).equals(keyFingerprint));
            }

            @Override
            public void clear() {
                cache.clear();
            }
        };
    }

    private static final class ProbeProcess implements AutoCloseable {
        private final Process process;
        private final BufferedReader output;
        private final BufferedWriter input;

        private ProbeProcess(Process process) {
            this.process = process;
            this.output = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            this.input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        }

        static ProbeProcess start(int port, String nodeId) throws Exception {
            String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            Process process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                CacheInvalidationProcessProbe.class.getName(), Integer.toString(port), nodeId)
                .redirectErrorStream(true)
                .start();
            return new ProbeProcess(process);
        }

        void send(String command) throws Exception {
            input.write(command);
            input.newLine();
            input.flush();
        }

        void await(String marker) throws Exception {
            try (var executor = Executors.newSingleThreadExecutor()) {
                String line = executor.submit(() -> {
                    String current;
                    while ((current = output.readLine()) != null) {
                        if (marker.equals(current)) {
                            return current;
                        }
                    }
                    return null;
                }).get(10, TimeUnit.SECONDS);
                if (!marker.equals(line)) {
                    throw new IllegalStateException("Probe exited before marker " + marker);
                }
            }
        }

        @Override
        public void close() throws Exception {
            if (process.isAlive()) {
                send("close");
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroy();
                }
            }
            input.close();
            output.close();
        }
    }
}
