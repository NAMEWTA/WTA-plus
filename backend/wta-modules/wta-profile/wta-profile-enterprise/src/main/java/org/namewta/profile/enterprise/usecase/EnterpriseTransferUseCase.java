package org.namewta.profile.enterprise.usecase;

import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferConfirmBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferSendBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseTransferVo;

/**
 * EnterpriseTransferUseCase 应用用例合同，定义入口可调用的业务场景。
 */
public interface EnterpriseTransferUseCase {
    /**
     * 编排 send 应用用例。
     */
    EnterpriseTransferVo send(long userId, EnterpriseTransferSendBo command);
    /**
     * 编排 confirm 应用用例。
     */
    EnterpriseTransferVo confirm(long userId, EnterpriseTransferConfirmBo command);
    /**
     * 编排 unbind 应用用例。
     */
    EnterpriseTransferVo unbind(long userId);
}
