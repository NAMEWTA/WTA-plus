package org.namewta.system.domain.vo;

import java.time.Instant;
import java.util.List;

/** 管理端单配置诊断的安全投影；不包含桶、对象键、端点、凭据或原始供应商错误。 */
public record OssStorageDiagnosticVo(String status, String reason, Instant checkedAt, List<Fact> facts) {
    public OssStorageDiagnosticVo {
        facts = List.copyOf(facts);
    }

    /** 固定枚举名描述的限定观察事实，不表示整个 Bucket 的有效 IAM 判定。 */
    public record Fact(String subject, String observation, String source, String scope,
                       String basis, Instant observedAt) {
    }
}
