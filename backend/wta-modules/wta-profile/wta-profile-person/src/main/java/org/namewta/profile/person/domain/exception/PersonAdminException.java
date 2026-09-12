package org.namewta.profile.person.domain.exception;

/** PersonAdminException 业务异常类型。 */
public class PersonAdminException extends RuntimeException {

    /** 构造个人管理业务异常。 */
    public PersonAdminException(String category) {
        super(category);
    }

    /** 构造个人管理业务异常。 */
    public PersonAdminException(String category, Throwable cause) {
        super(category, cause);
    }
}
