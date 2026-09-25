package org.namewta.test.security;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Built admin/home apps over HTTPS. API fixtures stay in the Playwright spec; the message stream is real. */
@Tag("dev")
class SessionNavigationLifecycleIntegrationTest {
    @TempDir Path directory;

    @Test
    void builtAppsKeepSessionAndNavigationBoundaries() throws Exception {
        assumeTrue(Boolean.getBoolean("session.lifecycle.integration"), "owned production App builds required");
        Path root = Path.of(System.getProperty("namewta.repo.root")).toAbsolutePath();
        Path adminDist = directory.resolve("admin-dist");
        Path homeDist = root.resolve("frontend/apps/home-web/dist");
        // Default production leaves the message stream off. This journey needs the shipped SSE client.
        command(root.resolve("frontend"), java.util.List.of("corepack", "pnpm", "--filter", "@namewta/admin-web", "exec",
            "vite", "build", "--mode", "production", "--outDir", adminDist.toString()), Map.of(
            "VITE_APP_MESSAGE_ENABLED", "true", "VITE_APP_CONTEXT_PATH", "/", "VITE_APP_BASE_API", "/prod-api"));
        assertThat(Files.readString(adminDist.resolve("index.html"))).contains("/assets/");
        assertThat(Files.readString(homeDist.resolve("index.html"))).contains("/assets/");
        command(root, java.util.List.of("keytool", "-genkeypair", "-alias", "lifecycle", "-keyalg", "RSA", "-keysize", "2048",
            "-storetype", "PKCS12", "-keystore", directory.resolve("server.p12").toString(), "-storepass",
            "owned-lifecycle-only", "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1", "-validity", "1"), Map.of());
        var server = new Server();
        var admin = https(server);
        var home = https(server);
        var apps = Map.of(admin.getLocalPort(), adminDist, home.getLocalPort(), homeDist);
        var handler = new ServletContextHandler();
        handler.setContextPath("/");
        handler.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
                if ("/prod-api/resource/message".equals(request.getRequestURI())) {
                    response.setStatus(200);
                    response.setContentType("text/event-stream");
                    response.setHeader("Cache-Control", "no-cache");
                    var async = request.startAsync();
                    async.setTimeout(0);
                    async.start(() -> {
                        try {
                            var output = response.getOutputStream();
                            while (true) {
                                output.write(": open\n\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                output.flush();
                                Thread.sleep(1000);
                            }
                        } catch (Exception ignored) {
                            Thread.currentThread().interrupt();
                        } finally {
                            async.complete();
                        }
                    });
                    return;
                }
                Path dist = apps.get(request.getLocalPort());
                Path file = dist.resolve(request.getRequestURI().substring(1)).normalize();
                if (!file.startsWith(dist) || !Files.isRegularFile(file)) file = dist.resolve("index.html");
                String name = file.getFileName().toString();
                String contentType = name.endsWith(".js") ? "application/javascript"
                    : name.endsWith(".css") ? "text/css"
                    : name.endsWith(".html") ? "text/html"
                    : name.endsWith(".svg") ? "image/svg+xml"
                    : name.endsWith(".json") ? "application/json"
                    : "application/octet-stream";
                response.setContentType(contentType);
                Files.copy(file, response.getOutputStream());
            }
        }), "/*");
        server.setHandler(handler);
        server.start();
        try {
            command(root.resolve("frontend"), java.util.List.of("corepack", "pnpm", "exec", "playwright", "test",
                "--config", "playwright.lifecycle.config.ts", "--max-failures=1"), Map.of(
                "LIFECYCLE_ADMIN_ORIGIN", "https://127.0.0.1:" + admin.getLocalPort(),
                "LIFECYCLE_HOME_ORIGIN", "https://127.0.0.1:" + home.getLocalPort()));
        } finally {
            server.stop();
        }
    }

    private ServerConnector https(Server server) {
        var tls = new SslContextFactory.Server();
        tls.setKeyStorePath(directory.resolve("server.p12").toString());
        tls.setKeyStorePassword("owned-lifecycle-only");
        var config = new HttpConfiguration();
        config.addCustomizer(new SecureRequestCustomizer());
        var connector = new ServerConnector(server, new SslConnectionFactory(tls, "http/1.1"), new HttpConnectionFactory(config));
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        server.addConnector(connector);
        try {
            connector.open();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
        return connector;
    }

    private void command(Path cwd, java.util.List<String> command, Map<String, String> environment) throws Exception {
        Path log = Files.createTempFile(directory, "command-", ".log");
        var builder = new ProcessBuilder(command).directory(cwd.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().putAll(environment);
        var process = builder.start();
        try {
            if (!process.waitFor(600, TimeUnit.SECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                throw new java.io.IOException("Owned test command timed out: " + command.getFirst());
            }
            String output = Files.readString(log);
            if (!environment.isEmpty()) System.out.println(output);
            assertThat(process.exitValue()).as(command + "\n" + output).isZero();
        } finally {
            if (process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        }
    }
}
