package org.namewta.profile.enterprise.usecase;

import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationProbeBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationSaveBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationProbeVo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationVo;
import org.namewta.profile.enterprise.domain.application.EnterpriseApplicationProcessCommand;

/**
 * EnterpriseApplicationUseCase 应用用例合同，定义入口可调用的业务场景。
 */
public interface EnterpriseApplicationUseCase {
    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default EnterpriseApplicationVo current() { throw new UnsupportedOperationException("请传入 userId"); }
    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default EnterpriseApplicationVo save(EnterpriseApplicationSaveBo command) { throw new UnsupportedOperationException("请传入 userId"); }
    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default EnterpriseApplicationVo submit(int expectedVersion) { throw new UnsupportedOperationException("请传入 userId"); }
    /**
     * 编排 current 应用用例。
     */
    default EnterpriseApplicationVo current(long userId) { return current(); }
    /**
     * 编排 save 应用用例。
     */
    default EnterpriseApplicationVo save(long userId, EnterpriseApplicationSaveBo command) { return save(command); }
    /**
     * 编排 submit 应用用例。
     */
    default EnterpriseApplicationVo submit(long userId, int expectedVersion) { return submit(expectedVersion); }
    /**
     * 编排 probe 应用用例。
     */
    EnterpriseApplicationProbeVo probe(EnterpriseApplicationProbeBo command);

    /** 接收工作流事件并编排申请状态回写。 */
    default void handleProcess(EnterpriseApplicationProcessCommand command) {
        throw new UnsupportedOperationException("旧适配器不支持工作流事件入口");
    }
}
