package org.namewta.profile.enterprise.domain.exception;

import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationFailureCategory;
import java.util.Objects;

/** EnterpriseVerificationException 业务异常类型。 */
public class EnterpriseVerificationException extends RuntimeException {

    private final EnterpriseVerificationFailureCategory category;

    /** 构造企业认证业务异常。 */
    public EnterpriseVerificationException(EnterpriseVerificationFailureCategory category, String message) {
        super(message);
        this.category = Objects.requireNonNull(category, "category");
    }

    /** 构造企业认证业务异常。 */
    public EnterpriseVerificationException(EnterpriseVerificationFailureCategory category, String message,
                                           Throwable cause) {
        super(message, cause);
        this.category = Objects.requireNonNull(category, "category");
    }

    /** 返回认证失败分类。 */
    public EnterpriseVerificationFailureCategory category() {
        return category;
    }
}
