package org.namewta.profile.enterprise.usecase.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationProbeBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationSaveBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationProbeVo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationVo;
import org.namewta.profile.enterprise.domain.application.EnterpriseApplicationProcessCommand;
import org.namewta.profile.enterprise.service.EnterpriseApplicationService;
import org.namewta.profile.enterprise.usecase.EnterpriseApplicationUseCase;
import org.springframework.stereotype.Service;

/**
 * EnterpriseApplicationUseCaseImpl 应用用例合同，定义入口可调用的业务场景。
 */
@Service
@RequiredArgsConstructor
public class EnterpriseApplicationUseCaseImpl implements EnterpriseApplicationUseCase {
    private final EnterpriseApplicationService service;

    /**
     * 查询当前用户的企业申请。
     */
    @DSTransactional
    @Override
    public EnterpriseApplicationVo current(long userId) {
        return service.current(userId);
    }

    /** 保存企业申请草稿。 */
    @DSTransactional
    @Override
    public EnterpriseApplicationVo save(long userId, EnterpriseApplicationSaveBo command) {
        return service.save(userId, command);
    }
    /** 提交企业申请。 */
    @DSTransactional
    @Override
    public EnterpriseApplicationVo submit(long userId, int expectedVersion) {
        return service.submit(userId, expectedVersion);
    }
    /** 预检查企业申请是否满足提交条件。 */
    @DSTransactional
    @Override
    public EnterpriseApplicationProbeVo probe(EnterpriseApplicationProbeBo command) {
        return service.probe(command);
    }

    /** 将企业申请工作流事件交给业务服务处理。 */
    @DSTransactional
    @Override
    public void handleProcess(EnterpriseApplicationProcessCommand command) {
        service.handleProcess(command);
    }
}
