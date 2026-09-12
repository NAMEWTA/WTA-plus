package org.namewta.profile.person.domain.exception;

import org.namewta.profile.person.domain.verification.PersonVerificationFailureCategory;
import java.util.Objects;

/** PersonVerificationException 业务异常类型。 */
public class PersonVerificationException extends RuntimeException {

    private final PersonVerificationFailureCategory category;

    /** 构造个人认证业务异常。 */
    public PersonVerificationException(PersonVerificationFailureCategory category, String message) {
        super(message);
        this.category = Objects.requireNonNull(category, "category");
    }

    /** 构造个人认证业务异常。 */
    public PersonVerificationException(PersonVerificationFailureCategory category, String message, Throwable cause) {
        super(message, cause);
        this.category = Objects.requireNonNull(category, "category");
    }

    /** 返回认证失败分类。 */
    public PersonVerificationFailureCategory category() {
        return category;
    }
}
