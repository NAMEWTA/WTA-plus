package org.namewta.common.sms.notify;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.namewta.common.json.utils.JsonUtils;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.namewta.common.sms.notify.SmsDeliveryQueryClient.Status.*;

/** 协议夹具与loopback HTTP；不调用真实供应商、不发送短信。 */
@Tag("dev")
class SmsDeliveryQueryClientTest {
    private static final Instant NOW = Instant.parse("2026-09-19T02:03:04Z");
    private static final SmsDeliveryQueryClient.Account TENCENT = new SmsDeliveryQueryClient.Account("tencent", "fixture-key", "fixture-secret", "1400000000");
    private static final SmsDeliveryQueryClient.Account ALIBABA = new SmsDeliveryQueryClient.Account("alibaba", "fixture-key", "fixture-secret", "");
    private static final SmsDeliveryQueryClient.Query QUERY = new SmsDeliveryQueryClient.Query("13800000000", "message-1", NOW.minusSeconds(120));

    @Test void signaturesMatchIndependentPythonVectorsIncludingUtf8AndQueryEscaping() {
        String body = "{\"PhoneNumber\":\"+8613800000000\",\"说明\":\"状态查询\"}";
        assertEquals("TC3-HMAC-SHA256 Credential=fixture-key/2026-09-19/sms/tc3_request, SignedHeaders=content-type;host, Signature=c94280d8884c260e73deaa673106300a79bc44929cdf86e11026111c9a4ba047",
            SmsQuerySignature.tencent("fixture-key", "fixture-secret", "sms.tencentcloudapi.com", body, NOW));
        String query = SmsQuerySignature.query(Map.of("BizId", "a+/=", "CurrentPage", "1", "PageSize", "2", "PhoneNumber", "13800000000", "SendDate", "20260919"));
        assertEquals("BizId=a%2B%2F%3D&CurrentPage=1&PageSize=2&PhoneNumber=13800000000&SendDate=20260919", query);
        var headers = new HashMap<String, String>();
        headers.put("host", "dysmsapi.aliyuncs.com"); headers.put("content-type", "application/json; charset=utf-8");
        headers.put("x-acs-action", "QuerySendDetails"); headers.put("x-acs-date", NOW.toString());
        headers.put("x-acs-version", "2017-05-25"); headers.put("x-acs-signature-nonce", "fixture-nonce");
        headers.put("x-acs-content-sha256", SmsQuerySignature.sha256(""));
        assertTrue(SmsQuerySignature.alibaba("fixture-key", "fixture-secret", query, headers)
            .endsWith("Signature=29e8afc6966f44b31f459dce08713d60e800c440c8b5887887877bd70a671ba2"));
        assertNotEquals(SmsQuerySignature.tencent("fixture-key", "fixture-secret", "sms.tencentcloudapi.com", body, NOW),
            SmsQuerySignature.tencent("fixture-key", "fixture-secret", "sms.tencentcloudapi.com", body + " ", NOW));
    }

    @Test void tencentQueriesExactAccountPhoneAndMessageAcrossAllPages() {
        var requests = new ArrayList<Map<String, Object>>();
        var client = new SmsDeliveryQueryClient((uri, headers, body) -> {
            assertEquals("https://sms.tencentcloudapi.com/", uri.toString());
            assertEquals("DescribeSendRecordList", headers.get("X-TC-Action"));
            assertEquals("2021-01-11", headers.get("X-TC-Version"));
            assertTrue(headers.get("Authorization").startsWith("TC3-HMAC-SHA256 "));
            Map<String, Object> payload = JsonUtils.parseMap(body); requests.add(payload);
            assertEquals("+8613800000000", payload.get("PhoneNumber"));
            assertEquals("1400000000", payload.get("SmsSdkAppId"));
            var rows = new ArrayList<Map<String, Object>>();
            if (requests.size() == 1) for (int i = 0; i < 50; i++) rows.add(tcRow("other-" + i, "+8613800000000", 2));
            else rows.add(tcRow("message-1", "+8613800000000", 4));
            return tcResponse(51, rows);
        });
        assertEquals(UNDELIVERABLE, client.query(TENCENT, QUERY, NOW));
        assertEquals(2, requests.size());
        assertEquals(50, ((Number) requests.get(1).get("Offset")).intValue());
    }

    @Test void tencentDoesNotPromoteTruncatedInconsistentOrAmbiguousResults() {
        for (String response : List.of(tcResponse(1001, List.of(tcRow("message-1", "+8613800000000", 2))),
            tcResponse(2, List.of(tcRow("message-1", "+8613800000000", 2))),
            tcResponse(2, List.of(tcRow("message-1", "+8613800000000", 2), tcRow("message-1", "+8613800000000", 4))),
            tcResponse(1, List.of(tcRow("message-1", "+8613900000000", 2))),
            "{\"Response\":{\"Error\":{\"Message\":\"private-canary\"}}}")) {
            var client = new SmsDeliveryQueryClient((uri, headers, body) -> response);
            var error = assertThrows(IllegalStateException.class, () -> client.query(TENCENT, QUERY, NOW));
            assertEquals("SMS_QUERY_UNCONFIRMED_RESPONSE", error.getMessage()); assertNull(error.getCause());
        }
    }

    @Test void tencentWaitingAbsentAndUnknownStatusDoNotMeanFailure() {
        for (String response : List.of(tcResponse(0, List.of()), tcResponse(1, List.of(tcRow("other", "+8613800000000", 2))),
            tcResponse(1, List.of(tcRow("message-1", "+8613800000000", 3))), tcResponse(1, List.of(tcRow("message-1", "+8613800000000", 97))))) {
            assertEquals(WAITING, new SmsDeliveryQueryClient((uri, headers, body) -> response).query(TENCENT, QUERY, NOW));
        }
        assertEquals(DELIVERED, new SmsDeliveryQueryClient((uri, headers, body) -> tcResponse(1,
            List.of(tcRow("message-1", "+8613800000000", 2)))).query(TENCENT, QUERY, NOW));
    }

    @Test void alibabaSignsBizIdQueryAndChecksPreviousDayAtMidnight() {
        var dates = new ArrayList<String>();
        var client = new SmsDeliveryQueryClient((uri, headers, body) -> {
            assertEquals("dysmsapi.aliyuncs.com", uri.getHost()); assertEquals("https", uri.getScheme());
            assertEquals("", body); assertTrue(uri.getRawQuery().contains("BizId=a%2B%2F%3D"));
            assertTrue(headers.get("Authorization").startsWith("ACS3-HMAC-SHA256 "));
            assertEquals("QuerySendDetails", headers.get("x-acs-action"));
            dates.add(uri.getRawQuery());
            return uri.getRawQuery().contains("SendDate=20260919") ? aliResponse(0, List.of())
                : aliResponse(1, List.of(aliRow("13800000000", "2026-09-18 23:59:59", 3)));
        });
        var midnight = Instant.parse("2026-09-18T16:00:01Z");
        assertEquals(DELIVERED, client.query(ALIBABA, new SmsDeliveryQueryClient.Query("+8613800000000", "a+/=", midnight), midnight.plusSeconds(100)));
        assertEquals(2, dates.size());
    }

    @Test void alibabaRequiresSingleMatchingRecordAcrossBothDates() {
        for (String response : List.of(aliResponse(2, List.of(aliRow("13800000000", "2026-09-19 10:01:00", 3), aliRow("13800000000", "2026-09-19 10:01:00", 2))),
            aliResponse(1, List.of(aliRow("13900000000", "2026-09-19 10:01:00", 3))),
            aliResponse(1, List.of(aliRow("13800000000", "2026-09-17 10:01:00", 3))),
            "{\"Code\":\"Forbidden\",\"Message\":\"private-canary\"}")) {
            assertThrows(IllegalStateException.class, () -> new SmsDeliveryQueryClient((uri, headers, body) -> response).query(ALIBABA, QUERY, NOW));
        }
        var client = new SmsDeliveryQueryClient((uri, headers, body) -> aliResponse(1, List.of(aliRow("13800000000",
            uri.getQuery().contains("20260919") ? "2026-09-19 00:00:01" : "2026-09-18 23:59:59", 3))));
        assertThrows(IllegalStateException.class, () -> client.query(ALIBABA, QUERY, NOW));
    }

    @Test void malformedProviderPayloadNeverEscapesWithItsPrivateContents() {
        for (String response : List.of("private-canary{", "null", "[]", "{\"Response\":{\"TotalCount\":\"wrong\"}}")) {
            var error = assertThrows(IllegalStateException.class, () -> new SmsDeliveryQueryClient((uri, headers, body) -> response).query(TENCENT, QUERY, NOW));
            assertFalse(error.toString().contains("private-canary")); assertNull(error.getCause());
        }
        assertFalse(TENCENT.toString().contains("fixture-secret")); assertFalse(QUERY.toString().contains("13800000000"));
    }

    @Test void queryWindowAndMissingIdentityPreventAnyNetworkCall() {
        var client = new SmsDeliveryQueryClient((uri, headers, body) -> { fail("unexpected network request"); return ""; });
        assertEquals(WAITING, client.query(TENCENT, QUERY, QUERY.acceptedAt().plusSeconds(71 * 3600)));
        assertEquals(WAITING, client.query(TENCENT, QUERY, QUERY.acceptedAt().minusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> client.query(TENCENT, new SmsDeliveryQueryClient.Query(QUERY.phone(), "", QUERY.acceptedAt()), NOW));
    }

    @Test void realHttpTransportPostsBodyAndRejectsRedirectsErrorsAndOversize() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger followed = new AtomicInteger();
        server.createContext("/ok", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("signed-fixture", exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] request = exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, request.length); exchange.getResponseBody().write(request); exchange.close();
        });
        server.createContext("/redirect", exchange -> { exchange.getResponseHeaders().add("Location", "/followed"); exchange.sendResponseHeaders(302, -1); exchange.close(); });
        server.createContext("/followed", exchange -> { followed.incrementAndGet(); exchange.sendResponseHeaders(200, -1); exchange.close(); });
        server.createContext("/error", exchange -> { exchange.sendResponseHeaders(503, -1); exchange.close(); });
        server.createContext("/large", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try { exchange.getResponseBody().write(new byte[SmsQueryHttpTransport.MAX_RESPONSE_BYTES + 1]); }
            catch (java.io.IOException ignored) { /* 客户端到上限主动关闭。 */ }
            finally { exchange.close(); }
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            var transport = new SmsQueryHttpTransport();
            assertEquals("{\"fixture\":true}", transport.post(URI.create(base + "/ok"), Map.of("Authorization", "signed-fixture"), "{\"fixture\":true}"));
            for (String path : List.of("/redirect", "/error", "/large")) {
                assertThrows(IllegalStateException.class, () -> transport.post(URI.create(base + path), Map.of(), ""));
            }
            assertEquals(0, followed.get());
        } finally { server.stop(0); }
    }

    @Test void actualProtocolClientRunsThroughLocalHttpAndNeverCallsSendAction() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger queries = new AtomicInteger();
        var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        server.createContext("/", exchange -> {
            try {
                assertEquals("DescribeSendRecordList", exchange.getRequestHeaders().getFirst("X-TC-Action"));
                assertTrue(exchange.getRequestHeaders().getFirst("Authorization").startsWith("TC3-HMAC-SHA256 Credential=fixture-key/"));
                var body = JsonUtils.parseMap(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                assertEquals("+8613800000000", body.get("PhoneNumber"));
                queries.incrementAndGet();
                byte[] bytes = tcResponse(1, List.of(tcRow("message-1", "+8613800000000", 2))).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes);
            } catch (AssertionError error) { failure.set(error); exchange.sendResponseHeaders(500, -1); }
            finally { exchange.close(); }
        });
        server.start();
        try {
            var transport = new SmsQueryHttpTransport();
            // 仅测试传输接缝重定向至owned loopback；生产Client没有可配置URL。
            var client = new SmsDeliveryQueryClient((uri, headers, body) -> transport.post(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"), headers, body));
            assertEquals(DELIVERED, client.query(TENCENT, QUERY, NOW));
            assertEquals(DELIVERED, client.query(TENCENT, QUERY, NOW));
            assertEquals(2, queries.get()); assertNull(failure.get());
        } finally { server.stop(0); }
    }

    @Test void transportReadTimeoutIsBoundedAndDoesNotExposeServerBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var release = new java.util.concurrent.CountDownLatch(1);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, 100);
            try { release.await(10, java.util.concurrent.TimeUnit.SECONDS); }
            catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        try {
            var error = assertThrows(IllegalStateException.class, () -> new SmsQueryHttpTransport().post(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"), Map.of(), ""));
            assertEquals("SMS_QUERY_IO_ERROR", error.getMessage());
            assertInstanceOf(java.net.SocketTimeoutException.class, error.getCause());
        } finally { release.countDown(); server.stop(0); }
    }

    private static Map<String, Object> tcRow(String id, String phone, int status) { return Map.of("SerialNo", id, "PhoneNumber", phone, "SendStatus", status); }
    private static String tcResponse(int total, List<Map<String, Object>> rows) { return JsonUtils.toJsonString(Map.of("Response", Map.of("TotalCount", total, "SendRecordSet", rows))); }
    private static Map<String, Object> aliRow(String phone, String date, int status) { return Map.of("PhoneNum", phone, "SendDate", date, "SendStatus", status, "Content", "private-canary"); }
    private static String aliResponse(int total, List<Map<String, Object>> rows) { return JsonUtils.toJsonString(Map.of("Code", "OK", "TotalCount", Integer.toString(total), "SmsSendDetailDTOs", Map.of("SmsSendDetailDTO", rows))); }
}
