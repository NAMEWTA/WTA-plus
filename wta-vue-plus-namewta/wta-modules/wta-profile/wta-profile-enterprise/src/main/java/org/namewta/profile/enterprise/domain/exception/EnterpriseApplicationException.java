package org.namewta.profile.enterprise.domain.exception;

/** EnterpriseApplicationException 业务异常类型。 */
public class EnterpriseApplicationException extends RuntimeException {

    /** 构造企业申请业务异常。 */
    public EnterpriseApplicationException(String category) {
        super(category);
    }

    /** 构造企业申请业务异常。 */
    public EnterpriseApplicationException(String category, Throwable cause) {
        super(category, cause);
    }
}
