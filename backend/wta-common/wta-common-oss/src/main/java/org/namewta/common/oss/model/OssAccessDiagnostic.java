package org.namewta.common.oss.model;

import org.namewta.common.oss.enums.AccessPolicy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Provider 访问边界的只读、有限范围诊断；事实不代表整个 Bucket 的有效 IAM 决策。 */
public record OssAccessDiagnostic(
    Verification verification,
    Reason reason,
    AccessPolicy expectedAccessPolicy,
    List<Fact> facts,
    Instant checkedAt
) {
    public OssAccessDiagnostic {
        Objects.requireNonNull(verification);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(checkedAt);
        facts = List.copyOf(facts);
    }

    public boolean verified() {
        return verification == Verification.VERIFIED;
    }

    /** 每条事实只说明指定来源、范围和时刻的观察；策略及 ACL 项是可理解的声明，不是实际授权结果。 */
    public record Fact(Subject subject, Observation observation, Source source, Scope scope,
                       Basis basis, Instant observedAt) {
        public Fact {
            Objects.requireNonNull(subject);
            Objects.requireNonNull(observation);
            Objects.requireNonNull(source);
            Objects.requireNonNull(scope);
            Objects.requireNonNull(basis);
            Objects.requireNonNull(observedAt);
        }
    }

    public enum Verification { VERIFIED, MISMATCH, UNVERIFIED }

    public enum Reason {
        READY, INVALID_REQUEST, UNSUPPORTED, DIAGNOSTIC_OBJECT_MISSING, POLICY_UNREADABLE,
        POLICY_MISMATCH, ANONYMOUS_READ_MISMATCH, ANONYMOUS_WRITE_ALLOWED, INSUFFICIENT_EVIDENCE,
        TIMEOUT, PROVIDER_ERROR
    }

    /** POLICY_WRITE 是已理解的危险写声明；ACL_LIST 仅是桶列举授权，均不代表匿名 PUT 实测。 */
    public enum Subject { POLICY_READ, POLICY_WRITE, ACL_LIST, ACL_WRITE_RISK, OBJECT_HEAD, OBJECT_GET }

    public enum Observation { ALLOWED, DENIED, UNKNOWN }

    public enum Source { BUCKET_POLICY, BUCKET_ACL, ANONYMOUS_HEAD, ANONYMOUS_GET }

    public enum Scope { BUCKET, OBJECT }

    public enum Basis {
        POLICY_ALLOW, POLICY_DENY, NO_SUCH_POLICY, POLICY_UNREADABLE, COMPLEX_POLICY, INVALID_POLICY,
        ACL_GRANT, ACL_NO_GRANT, ACL_UNREADABLE, HTTP_SUCCESS, HTTP_DENIED, HTTP_NOT_FOUND,
        REDIRECT, HTTP_ERROR, TIMEOUT, NETWORK_ERROR, INTERRUPTED, NOT_EVALUATED, UNSUPPORTED
    }
}
