package org.namewta.profile.person.domain.exception;

/** PersonApplicationException 业务异常类型。 */
public class PersonApplicationException extends RuntimeException {

    /** 构造个人申请业务异常。 */
    public PersonApplicationException(String category) {
        super(category);
    }

    /** 构造个人申请业务异常。 */
    public PersonApplicationException(String category, Throwable cause) {
        super(category, cause);
    }
}
