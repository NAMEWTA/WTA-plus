package org.namewta.profile.enterprise.usecase;

import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferConfirmBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferSendBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseTransferVo;

/**
 * EnterpriseTransferUseCase 应用用例合同，定义入口可调用的业务场景。
 */
public interface EnterpriseTransferUseCase {
    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default EnterpriseTransferVo send(EnterpriseTransferSendBo command) { throw new UnsupportedOperationException("请传入 userId"); }
    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default EnterpriseTransferVo confirm(EnterpriseTransferConfirmBo command) { throw new UnsupportedOperationException("请传入 userId"); }
    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default EnterpriseTransferVo unbind() { throw new UnsupportedOperationException("请传入 userId"); }
    /**
     * 编排 send 应用用例。
     */
    default EnterpriseTransferVo send(long userId, EnterpriseTransferSendBo command) { return send(command); }
    /**
     * 编排 confirm 应用用例。
     */
    default EnterpriseTransferVo confirm(long userId, EnterpriseTransferConfirmBo command) { return confirm(command); }
    /**
     * 编排 unbind 应用用例。
     */
    default EnterpriseTransferVo unbind(long userId) { return unbind(); }
}
