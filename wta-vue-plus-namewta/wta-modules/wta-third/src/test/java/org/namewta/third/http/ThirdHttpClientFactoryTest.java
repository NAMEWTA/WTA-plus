package org.namewta.third.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.service.annotation.GetExchange;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("local")
class ThirdHttpClientFactoryTest {
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/typed", this::handleTyped);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void createsTypedHttpExchangeClientOnRestClientTransport() {
        ThirdHttpClientFactory factory = new ThirdHttpClientFactory();
        TypedContract contract = factory.createTyped(factory.create(
            "http://127.0.0.1:" + server.getAddress().getPort(), 1_000, 1_000), TypedContract.class);

        assertEquals("typed", contract.get().get("result").asText());
    }

    private void handleTyped(HttpExchange exchange) throws IOException {
        byte[] response = "{\"result\":\"typed\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    @org.springframework.web.service.annotation.HttpExchange
    interface TypedContract {
        @GetExchange("/typed")
        JsonNode get();
    }
}
