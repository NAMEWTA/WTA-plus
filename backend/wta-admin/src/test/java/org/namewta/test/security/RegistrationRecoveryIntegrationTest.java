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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Built admin/home registration pages over HTTPS. Captcha and register calls stay in the Playwright fixtures. */
@Tag("dev")
class RegistrationRecoveryIntegrationTest {
    @TempDir Path directory;

    @Test
    void builtAppsRecoverRegistrationWithoutReloading() throws Exception {
        assumeTrue(Boolean.getBoolean("registration.recovery.integration"), "owned production App builds required");
        Path root = Path.of(System.getProperty("namewta.repo.root")).toAbsolutePath();
        Path adminDist = root.resolve("frontend/apps/admin-web/dist");
        Path homeDist = root.resolve("frontend/apps/home-web/dist");
        assertThat(Files.readString(adminDist.resolve("index.html"))).contains("/assets/");
        assertThat(Files.readString(homeDist.resolve("index.html"))).contains("/assets/");
        command(root, List.of("keytool", "-genkeypair", "-alias", "registration", "-keyalg", "RSA", "-keysize", "2048",
            "-storetype", "PKCS12", "-keystore", directory.resolve("server.p12").toString(), "-storepass",
            "owned-registration-only", "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1", "-validity", "1"), Map.of());
        var server = new Server();
        var admin = https(server);
        var home = https(server);
        var apps = Map.of(admin.getLocalPort(), adminDist, home.getLocalPort(), homeDist);
        var handler = new ServletContextHandler();
        handler.setContextPath("/");
        handler.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
                Path dist = apps.get(request.getLocalPort());
                Path file = dist.resolve(request.getRequestURI().substring(1)).normalize();
                if (!file.startsWith(dist) || !Files.isRegularFile(file)) file = dist.resolve("index.html");
                String name = file.getFileName().toString();
                response.setContentType(name.endsWith(".js") ? "application/javascript"
                    : name.endsWith(".css") ? "text/css"
                    : name.endsWith(".html") ? "text/html"
                    : name.endsWith(".svg") ? "image/svg+xml"
                    : name.endsWith(".json") ? "application/json"
                    : "application/octet-stream");
                Files.copy(file, response.getOutputStream());
            }
        }), "/*");
        server.setHandler(handler);
        server.start();
        try {
            command(root.resolve("frontend"), List.of("corepack", "pnpm", "exec", "playwright", "test",
                "--config", "playwright.registration.config.ts", "--max-failures=1"), Map.of(
                "REGISTRATION_ADMIN_ORIGIN", "https://127.0.0.1:" + admin.getLocalPort(),
                "REGISTRATION_HOME_ORIGIN", "https://127.0.0.1:" + home.getLocalPort()));
        } finally {
            server.stop();
        }
    }

    private ServerConnector https(Server server) {
        var tls = new SslContextFactory.Server();
        tls.setKeyStorePath(directory.resolve("server.p12").toString());
        tls.setKeyStorePassword("owned-registration-only");
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

    private void command(Path cwd, List<String> command, Map<String, String> environment) throws Exception {
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
