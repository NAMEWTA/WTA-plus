package org.namewta.profile.enterprise.domain.exception;

/** EnterpriseTransferException 业务异常类型。 */
public class EnterpriseTransferException extends RuntimeException {

    /** 构造企业转移业务异常。 */
    public EnterpriseTransferException(String category) {
        super(category);
    }

    /** 构造企业转移业务异常。 */
    public EnterpriseTransferException(String category, Throwable cause) {
        super(category, cause);
    }
}
