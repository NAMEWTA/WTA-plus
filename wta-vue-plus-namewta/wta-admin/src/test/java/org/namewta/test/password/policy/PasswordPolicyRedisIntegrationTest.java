package org.namewta.test.password.policy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Tag("dev")
class PasswordPolicyRedisIntegrationTest {

    private RedissonClient cleanupClient;

    @AfterEach
    void cleanup() {
        if (cleanupClient != null) {
            cleanupClient.getMap(PasswordPolicyRedisProcessProbe.MAP_NAME).delete();
            cleanupClient.shutdown();
        }
    }

    @Test
    void shouldRefreshValidatedPolicyAcrossTwoIndependentJvmProcesses() throws Exception {
        int port = Integer.getInteger("password.policy.redis.integration.port", -1);
        Assumptions.assumeTrue(port > 0, "requires a disposable Redis port");
        cleanupClient = PasswordPolicyRedisProcessProbe.client(port);
        cleanupClient.getMap(PasswordPolicyRedisProcessProbe.MAP_NAME).delete();

        try (ProbeProcess nodeA = ProbeProcess.start(port, "policy-node-a");
             ProbeProcess nodeB = ProbeProcess.start(port, "policy-node-b")) {
            nodeA.await("POLICY_PROBE_READY");
            nodeB.await("POLICY_PROBE_READY");

            nodeA.send("update");
            nodeA.await("POLICY_UPDATED");
            nodeB.send("minimum");

            nodeB.await("POLICY_MINIMUM_10");
        }
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
                PasswordPolicyRedisProcessProbe.class.getName(), Integer.toString(port), nodeId)
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
