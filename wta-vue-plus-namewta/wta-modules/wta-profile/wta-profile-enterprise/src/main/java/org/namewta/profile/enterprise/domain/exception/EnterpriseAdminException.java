package org.namewta.profile.enterprise.domain.exception;

/** EnterpriseAdminException 业务异常类型。 */
public class EnterpriseAdminException extends RuntimeException {

    /** 构造企业管理业务异常。 */
    public EnterpriseAdminException(String category) {
        super(category);
    }

    /** 构造企业管理业务异常。 */
    public EnterpriseAdminException(String category, Throwable cause) {
        super(category, cause);
    }
}
