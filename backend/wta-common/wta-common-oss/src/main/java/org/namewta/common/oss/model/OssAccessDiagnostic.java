package org.namewta.common.oss.model;

import org.namewta.common.oss.enums.AccessPolicy;

import java.time.Instant;

/**
 * Provider 访问边界的只读诊断结果。
 *
 * @param verification          验证状态
 * @param reason                稳定且非敏感的结果类别
 * @param expectedAccessPolicy  声明的访问类型
 * @param anonymousHeadAllowed  匿名 HEAD 是否成功
 * @param anonymousGetAllowed   匿名 Range GET 是否成功
 * @param anonymousWriteDenied  Policy/ACL 是否证明匿名写被拒绝
 * @param checkedAt             诊断时间
 */
public record OssAccessDiagnostic(
    Verification verification,
    Reason reason,
    AccessPolicy expectedAccessPolicy,
    boolean anonymousHeadAllowed,
    boolean anonymousGetAllowed,
    boolean anonymousWriteDenied,
    Instant checkedAt
) {

    public boolean verified() {
        return verification == Verification.VERIFIED;
    }

    public enum Verification {
        VERIFIED,
        MISMATCH,
        UNVERIFIED
    }

    public enum Reason {
        READY,
        INVALID_REQUEST,
        UNSUPPORTED,
        DIAGNOSTIC_OBJECT_MISSING,
        POLICY_UNREADABLE,
        POLICY_MISMATCH,
        ANONYMOUS_READ_MISMATCH,
        ANONYMOUS_WRITE_ALLOWED,
        TIMEOUT,
        PROVIDER_ERROR
    }
}
