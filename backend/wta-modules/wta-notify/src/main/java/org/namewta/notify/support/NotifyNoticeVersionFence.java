package org.namewta.notify.support;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.domain.entity.NotifyIntent;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 公告发布版本的持久元数据栅栏；缺失或损坏的版本来源绝不授权新的投递。 */
public final class NotifyNoticeVersionFence {
    private static final String APP = "notify";
    private static final String SCENE = "notice-published";
    private static final String BIZ_TYPE = "NOTICE_PUBLISHED";
    private static final String AUDIT = "NOTICE_SNAPSHOT";
    private static final String KEY = "noticeVersion";

    private NotifyNoticeVersionFence() { }

    public enum State { NOT_NOTICE, ACTIVE, RETRACTED, UNVERIFIED }

    /** 新发布只引用本次不可变公告快照，不把管理页地址作为收件人权限。 */
    public static Map<String, Object> initial(long noticeId, long snapshotId, int version) {
        return Map.of("audit", AUDIT, KEY, version(noticeId, snapshotId, version, false));
    }

    public static String idempotencyKey(long noticeId, int version) {
        return SCENE + ":" + noticeId + ":" + version;
    }

    /** Worker/人工重试的无异常保守判定；其他业务场景原样放行。 */
    public static State state(NotifyIntent intent) {
        if (!noticeLike(intent)) return State.NOT_NOTICE;
        try {
            Map<String, Object> metadata = metadata(intent.getMetadataJson());
            if (!AUDIT.equals(metadata.get("audit"))) return State.UNVERIFIED;
            Version marker = parseVersion(metadata.get(KEY));
            if (marker == null || !identityMatches(intent, marker.noticeId(), marker.version())) {
                return State.UNVERIFIED;
            }
            return marker.retracted() ? State.RETRACTED : State.ACTIVE;
        } catch (RuntimeException malformed) {
            return State.UNVERIFIED;
        }
    }

    /** 发布回执必须指向本次生成的版本及同一 Intent，不能按业务ID误连旧快照。 */
    public static void requirePublishedIdentity(NotifyIntent intent, long noticeId, long snapshotId, int version) {
        Version marker = strictVersion(intent, noticeId, version, false);
        if (marker.snapshotId() != snapshotId) throw invalid();
    }

    /**
     * 锁定公告及其精确 Intent 后将版本标为已撤回；仅严格匹配的旧 audit-only 元数据可升级。
     * @return 保留其他元数据键的新 JSON；已撤回时由调用方跳过重复写
     */
    public static String retractedMetadata(NotifyIntent intent, long noticeId, long snapshotId, int version) {
        if (!identityMatches(intent, noticeId, version)) throw invalid();
        Map<String, Object> metadata = metadata(intent.getMetadataJson());
        if (!AUDIT.equals(metadata.get("audit"))) throw invalid();
        Object raw = metadata.get(KEY);
        if (raw == null) {
            if (metadata.size() != 1) throw invalid();
        } else {
            Version marker = parseVersion(raw);
            if (marker == null || marker.noticeId() != noticeId || marker.snapshotId() != snapshotId
                || marker.version() != version) throw invalid();
        }
        Map<String, Object> copy = new LinkedHashMap<>(metadata);
        copy.put(KEY, version(noticeId, snapshotId, version, true));
        return JsonUtils.toJsonString(copy);
    }

    private static Version strictVersion(NotifyIntent intent, long noticeId, int version, boolean retracted) {
        if (!identityMatches(intent, noticeId, version)) throw invalid();
        Map<String, Object> metadata = metadata(intent.getMetadataJson());
        if (!AUDIT.equals(metadata.get("audit"))) throw invalid();
        Version marker = parseVersion(metadata.get(KEY));
        if (marker == null || marker.noticeId() != noticeId || marker.version() != version
            || marker.retracted() != retracted) throw invalid();
        return marker;
    }

    private static boolean noticeLike(NotifyIntent intent) {
        return intent != null && (SCENE.equals(intent.getSceneCode()) || SCENE.equals(intent.getTemplateCode())
            || BIZ_TYPE.equals(intent.getBizType())
            || (intent.getIdempotencyKey() != null && intent.getIdempotencyKey().startsWith(SCENE + ":")));
    }

    private static boolean identityMatches(NotifyIntent intent, long noticeId, int version) {
        return intent != null && noticeId > 0 && version > 0
            && APP.equals(intent.getAppId()) && SCENE.equals(intent.getSceneCode())
            && SCENE.equals(intent.getTemplateCode()) && BIZ_TYPE.equals(intent.getBizType())
            && Objects.equals(String.valueOf(noticeId), intent.getBizId())
            && idempotencyKey(noticeId, version).equals(intent.getIdempotencyKey());
    }

    private static Map<String, Object> metadata(String json) {
        if (json == null || json.isBlank()) throw invalid();
        Map<?, ?> raw = JsonUtils.parseObject(json, Map.class);
        if (raw == null || raw.keySet().stream().anyMatch(key -> !(key instanceof String))) throw invalid();
        Map<String, Object> copy = new LinkedHashMap<>();
        raw.forEach((key, value) -> copy.put((String) key, value));
        return copy;
    }

    private static Version parseVersion(Object raw) {
        if (!(raw instanceof Map<?, ?> values) || !(values.get("retracted") instanceof Boolean retracted)) return null;
        Long noticeId = integer(values.get("noticeId"));
        Long snapshotId = integer(values.get("snapshotId"));
        Long version = integer(values.get("version"));
        if (noticeId == null || snapshotId == null || version == null || noticeId <= 0 || snapshotId <= 0
            || version <= 0 || version > Integer.MAX_VALUE) return null;
        return new Version(noticeId, snapshotId, version.intValue(), retracted);
    }

    private static Long integer(Object raw) {
        if (!(raw instanceof Integer || raw instanceof Long || raw instanceof BigInteger)) return null;
        try { return new BigInteger(raw.toString()).longValueExact(); }
        catch (ArithmeticException malformed) { return null; }
    }

    private static Map<String, Object> version(long noticeId, long snapshotId, int version, boolean retracted) {
        return Map.of("noticeId", noticeId, "snapshotId", snapshotId, "version", version, "retracted", retracted);
    }

    private static ServiceException invalid() { return new ServiceException("公告发布版本事实不一致"); }

    private record Version(long noticeId, long snapshotId, int version, boolean retracted) { }
}
