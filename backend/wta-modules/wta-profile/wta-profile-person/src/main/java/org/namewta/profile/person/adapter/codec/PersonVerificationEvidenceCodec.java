package org.namewta.profile.person.adapter.codec;
import org.springframework.stereotype.Component;
import org.namewta.common.json.utils.JsonUtils;
/** 个人认证证据编解码器，保护提供方回调证据的存储格式。 */
@Component
public class PersonVerificationEvidenceCodec {
    private static final int SCHEMA_VERSION = 1;
    /** 创建个人认证证据编解码器。 */
    public PersonVerificationEvidenceCodec() {
    }
    /** 编码认证证据。 */
    public String encode(String callbackDigest, String providerEvidenceJson) {
        return JsonUtils.toJsonString(
            new StoredEvidence(SCHEMA_VERSION, callbackDigest, providerEvidenceJson));
    }
    /** 解码认证证据。 */
    public DecodedEvidence decode(String storedJson) {
        if (storedJson == null) {
            return new DecodedEvidence(null, null);
        }
        try {
            StoredEvidence evidence = JsonUtils.parseObject(storedJson, StoredEvidence.class);
            if (evidence.schemaVersion() == SCHEMA_VERSION) {
                return new DecodedEvidence(evidence.callbackDigest(), evidence.providerEvidenceJson());
            }
        } catch (RuntimeException ignored) {
            // Rows written before the evidence envelope remain readable as provider evidence.
        }
        return new DecodedEvidence(null, storedJson);
    }
    /** 个人认证证据存储载荷。 */
    private record StoredEvidence(
        int schemaVersion,
        String callbackDigest,
        String providerEvidenceJson
    ) {
    }
    /** 解码后的个人认证证据。 */
    public record DecodedEvidence(String callbackDigest, String providerEvidenceJson) {
    }
}
