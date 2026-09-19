package org.namewta.common.sms.notify;

import org.namewta.common.json.utils.JsonUtils;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * 腾讯/阿里短信只读送达查询。无发送能力，不接收厂商推送，也不保存正文/凭据。
 * 查询异常由调用方延迟重试；查不到或状态未知时保持已受理。
 */
public final class SmsDeliveryQueryClient {
    private static final String TENCENT_HOST = "sms.tencentcloudapi.com";
    private static final String ALIBABA_HOST = "dysmsapi.aliyuncs.com";
    private static final ZoneId SEND_DATE_ZONE = ZoneId.of("Asia/Shanghai");
    private final Transport transport;

    /** 使用固定HTTPS端点及有界同步连接，无后台线程或客户端池。 */
    public SmsDeliveryQueryClient() { this(new SmsQueryHttpTransport()); }

    SmsDeliveryQueryClient(Transport transport) { this.transport = transport; }

    /**
     * 读取已受理短信的最终状态，未确认返回WAITING。错误不会转换为投递失败。
     * @param account 当前启用账号，仅本次请求使用
     * @param query 精确消息/收件人及UTC受理时刻
     * @param now 当前UTC时刻
     * @return 可确认的终态或WAITING
     */
    public Status query(Account account, Query query, Instant now) {
        if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("SMS_QUERY_INTERRUPTED");
        if (account == null || query == null || blank(account.supplier()) || blank(account.keyId()) || blank(account.secret())
            || blank(query.messageId()) || blank(query.phone()) || query.acceptedAt() == null || now == null) {
            throw new IllegalArgumentException("SMS_QUERY_INVALID_INPUT");
        }
        if (query.acceptedAt().isAfter(now) || Duration.between(query.acceptedAt(), now).compareTo(Duration.ofHours(71)) >= 0) {
            return Status.WAITING;
        }
        return switch (account.supplier()) {
            case "tencent" -> tencent(account, query, now);
            case "alibaba" -> alibaba(account, query, now);
            default -> throw new IllegalArgumentException("SMS_QUERY_UNSUPPORTED_SUPPLIER");
        };
    }

    private Status tencent(Account account, Query query, Instant now) {
        if (blank(account.sdkAppId())) throw new IllegalArgumentException("SMS_QUERY_INVALID_ACCOUNT");
        String phone = tencentPhone(query.phone());
        Status result = Status.WAITING;
        int matches = 0;
        long total = -1;
        // 完整扫描最多1000条；固定查询时间范围，页数和总数变化不能被当作完整结果。
        var seen = new HashSet<String>();
        for (int offset = 0; offset < 1000; offset += 50) {
            if (offset > 0) {
                try { Thread.sleep(200); }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("SMS_QUERY_INTERRUPTED", exception);
                }
            }
            String body = JsonUtils.toJsonString(Map.of("PhoneNumber", phone, "SmsSdkAppId", account.sdkAppId(),
                "BeginTime", now.minus(Duration.ofHours(72)).plusSeconds(60).getEpochSecond(),
                "EndTime", now.getEpochSecond(), "Limit", 50, "Offset", offset));
            var headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/json; charset=utf-8");
            headers.put("X-TC-Action", "DescribeSendRecordList");
            headers.put("X-TC-Version", "2021-01-11");
            headers.put("X-TC-Region", "ap-guangzhou");
            headers.put("X-TC-Timestamp", Long.toString(now.getEpochSecond()));
            headers.put("Authorization", SmsQuerySignature.tencent(account.keyId(), account.secret(), TENCENT_HOST, body, now));
            JsonNode response = parse(transport.post(URI.create("https://" + TENCENT_HOST + "/"), headers, body)).path("Response");
            if (!response.isObject() || response.has("Error")) throw invalidResponse();
            long count = integer(response.path("TotalCount"));
            if (count > 1000 || (total != -1 && total != count)) throw invalidResponse();
            total = count;
            JsonNode rows = response.path("SendRecordSet");
            if (!rows.isArray() || rows.size() != Math.min(50, total - offset)) throw invalidResponse();
            for (JsonNode row : rows) {
                String id = string(row.path("SerialNo"));
                if (id.isBlank() || !seen.add(id)) throw invalidResponse();
                if (!id.equals(query.messageId())) continue;
                if (!phone.equals(string(row.path("PhoneNumber")))) throw invalidResponse();
                matches++;
                result = switch ((int) integer(row.path("SendStatus"))) {
                    case 2 -> Status.DELIVERED;
                    case 4 -> Status.UNDELIVERABLE;
                    default -> Status.WAITING;
                };
            }
            if (offset + rows.size() >= total) return matches == 1 ? result : Status.WAITING;
        }
        throw invalidResponse();
    }

    private Status alibaba(Account account, Query query, Instant now) {
        String phone = alibabaPhone(query.phone());
        var acceptedDate = query.acceptedAt().atZone(SEND_DATE_ZONE).toLocalDate();
        Status result = Status.WAITING;
        int matches = 0;
        // 受理写回可能跨过午夜，始终核对两个发送日；不能见到第一条就忽略另一天的歧义。
        for (var day : java.util.List.of(acceptedDate, acceptedDate.minusDays(1))) {
            String parameters = SmsQuerySignature.query(Map.of("PhoneNumber", phone, "BizId", query.messageId(),
                "SendDate", day.format(DateTimeFormatter.BASIC_ISO_DATE), "PageSize", "2", "CurrentPage", "1"));
            var headers = new HashMap<String, String>();
            headers.put("host", ALIBABA_HOST);
            headers.put("content-type", "application/json; charset=utf-8");
            headers.put("x-acs-action", "QuerySendDetails");
            headers.put("x-acs-version", "2017-05-25");
            headers.put("x-acs-date", now.truncatedTo(ChronoUnit.SECONDS).toString());
            headers.put("x-acs-signature-nonce", UUID.randomUUID().toString());
            headers.put("x-acs-content-sha256", SmsQuerySignature.sha256(""));
            String authorization = SmsQuerySignature.alibaba(account.keyId(), account.secret(), parameters, headers);
            headers.put("Authorization", authorization);
            headers.put("Accept", "application/json");
            JsonNode response = parse(transport.post(URI.create("https://" + ALIBABA_HOST + "/?" + parameters), headers, ""));
            if (!"OK".equals(string(response.path("Code")))) throw invalidResponse();
            long total = integer(response.path("TotalCount"));
            JsonNode rows = response.path("SmsSendDetailDTOs").path("SmsSendDetailDTO");
            if (total > 1 || (total == 0 && !rows.isArray() && !rows.isMissingNode() && !rows.isNull())
                || (rows.isArray() && rows.size() != total) || (total == 1 && !rows.isArray())) throw invalidResponse();
            if (total == 0) continue;
            JsonNode row = rows.get(0);
            if (!phone.equals(string(row.path("PhoneNum"))) || !string(row.path("SendDate")).startsWith(day + " ")) {
                throw invalidResponse();
            }
            matches++;
            if (matches > 1) throw invalidResponse();
            result = switch ((int) integer(row.path("SendStatus"))) {
                case 3 -> Status.DELIVERED;
                case 2 -> Status.UNDELIVERABLE;
                default -> Status.WAITING;
            };
        }
        return result;
    }

    private static JsonNode parse(String body) {
        try {
            JsonNode node = JsonUtils.getJsonMapper().readTree(body);
            if (node == null || !node.isObject()) throw invalidResponse();
            return node;
        } catch (RuntimeException exception) {
            // Jackson异常可包含短信原文，因此不把解析异常作为cause跨出边界。
            throw invalidResponse();
        }
    }

    private static long integer(JsonNode node) {
        try {
            String value = node.isString() ? node.asString() : node.isIntegralNumber() ? node.asText() : "";
            long number = Long.parseLong(value);
            if (number < 0 || number > Integer.MAX_VALUE) throw invalidResponse();
            return number;
        } catch (NumberFormatException exception) { throw invalidResponse(); }
    }

    private static String string(JsonNode node) { return node.isString() ? node.asString() : ""; }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static IllegalStateException invalidResponse() { return new IllegalStateException("SMS_QUERY_UNCONFIRMED_RESPONSE"); }

    /** 必须复现SMS4J 3.3.5发送侧规则；不能查询一个未实际发送的规范化号码。 */
    private static String tencentPhone(String phone) {
        return phone.contains("-") ? phone.replace("-", "") : phone.startsWith("+86") ? phone : "+86" + phone;
    }

    private static String alibabaPhone(String phone) {
        String value = phone.replace("-", "");
        if (value.startsWith("+86") && value.length() == 14) return value.substring(3);
        return value.startsWith("+") ? value.substring(1) : value;
    }

    /** 凭据只在请求栈内使用，禁止record默认toString泄漏。 */
    public record Account(String supplier, String keyId, String secret, String sdkAppId) {
        @Override public String toString() { return "SmsQueryAccount[redacted]"; }
    }

    /** 一次只读查询的精确关联条件，不在日志中渲染收件人。 */
    public record Query(String phone, String messageId, Instant acceptedAt) {
        @Override public String toString() { return "SmsDeliveryQuery[redacted]"; }
    }

    /** WAITING不代表发送失败，也不触发重发。 */
    public enum Status { WAITING, DELIVERED, UNDELIVERABLE }

    @FunctionalInterface
    interface Transport {
        String post(URI endpoint, Map<String, String> headers, String body);
    }
}
