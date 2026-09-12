package org.namewta.profile.person.domain.exception;

/** PersonRebindException 业务异常类型。 */
public class PersonRebindException extends RuntimeException {

    /** 构造个人换绑业务异常。 */
    public PersonRebindException(String category) {
        super(category);
    }

    /** 构造个人换绑业务异常。 */
    public PersonRebindException(String category, Throwable cause) {
        super(category, cause);
    }
}
