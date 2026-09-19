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
    /**
     * 编排 current 应用用例。
     */
    EnterpriseApplicationVo current(long userId);
    /**
     * 编排 save 应用用例。
     */
    EnterpriseApplicationVo save(long userId, EnterpriseApplicationSaveBo command);
    /**
     * 编排 submit 应用用例。
     */
    EnterpriseApplicationVo submit(long userId, int expectedVersion);
    /**
     * 编排 probe 应用用例。
     */
    EnterpriseApplicationProbeVo probe(EnterpriseApplicationProbeBo command);

    /** 接收工作流事件并编排申请状态回写。 */
    void handleProcess(EnterpriseApplicationProcessCommand command);
}
