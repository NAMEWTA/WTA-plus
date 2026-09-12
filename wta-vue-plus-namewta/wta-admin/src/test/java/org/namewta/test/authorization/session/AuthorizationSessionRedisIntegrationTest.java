package org.namewta.test.authorization.session;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Tag("dev")
class AuthorizationSessionRedisIntegrationTest {

    @Test
    void invalidatesOneClientAcrossTwoIndependentJvmProcesses() throws Exception {
        int port = redisPort();
        try (ProbeProcess nodeA = ProbeProcess.start(port, "authorization-process-a");
             ProbeProcess nodeB = ProbeProcess.start(port, "authorization-process-b")) {
            nodeA.await("SESSION_PROBE_READY");
            nodeB.await("SESSION_PROBE_READY");

            nodeA.send("invalidate-client-a");
            nodeA.await("SESSION_INVALIDATED_2");
            nodeA.send("state");
            nodeB.send("state");

            nodeA.await("SESSION_CLIENT_ISOLATED");
            nodeB.await("SESSION_CLIENT_ISOLATED");
        }
    }

    private static int redisPort() {
        int port = Integer.getInteger("authorization.session.redis.integration.port", -1);
        Assumptions.assumeTrue(port > 0, "requires a disposable Redis port");
        return port;
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

        private static ProbeProcess start(int port, String nodeId) throws Exception {
            String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            Process process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                AuthorizationSessionProcessProbe.class.getName(), Integer.toString(port), nodeId)
                .redirectErrorStream(true)
                .start();
            return new ProbeProcess(process);
        }

        private void send(String command) throws Exception {
            input.write(command);
            input.newLine();
            input.flush();
        }

        private void await(String marker) throws Exception {
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
