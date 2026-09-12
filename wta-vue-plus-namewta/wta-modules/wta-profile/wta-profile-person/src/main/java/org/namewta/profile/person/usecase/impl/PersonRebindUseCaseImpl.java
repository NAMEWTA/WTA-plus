package org.namewta.profile.person.usecase.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.person.domain.application.PersonRebindPublication;
import org.namewta.profile.person.domain.application.PersonRebindProcessCommand;
import org.namewta.profile.person.domain.bo.*;
import org.namewta.profile.person.domain.vo.*;
import org.namewta.profile.person.service.PersonRebindService;
import org.namewta.profile.person.usecase.PersonRebindUseCase;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * PersonRebindUseCaseImpl 应用用例合同，定义入口可调用的业务场景。
 */
@Service
@RequiredArgsConstructor
public class PersonRebindUseCaseImpl implements PersonRebindUseCase {

    private final PersonRebindService service;

    /** 预检查换绑条件并返回可用选项。 */
    @DSTransactional
    @Override
    public PersonRebindProbeVo probe(PersonRebindProbeBo command) {
        return service.probe(command);
    }
    /** 匹配可用于换绑的人员档案。 */
    @DSTransactional
    @Override
    public PersonRebindMatchVo match(long userId, PersonRebindMatchBo command) {
        return service.match(userId, command);
    }
    /** 确认换绑目标档案。 */
    @DSTransactional
    @Override
    public PersonRebindConfirmationVo confirm(long userId, PersonRebindConfirmBo command) {
        return service.confirm(userId, command);
    }
    /** 提交换绑申请。 */
    @DSTransactional
    @Override
    public PersonRebindSubmissionVo submit(long userId, PersonRebindSubmitBo command) {
        return service.submit(userId, command);
    }
    /** 解除当前账号与人员档案的绑定。 */
    @DSTransactional
    @Override
    public PersonRebindUnbindVo unbind(long userId) {
        return service.unbind(userId);
    }

    /** 发布审核通过的换绑结果。 */
    @DSTransactional
    @Override
    public Optional<PersonRebindPublication> publishApproved(long applicationId, int snapshotVersion,
                                                              Instant finishedTime) {
        return service.publishApprovedRebind(applicationId, snapshotVersion, finishedTime);
    }

    /** 将工作流事件交给 Service 处理。 */
    @DSTransactional
    @Override
    public void handleProcess(PersonRebindProcessCommand command) {
        service.handleProcess(command);
    }
}
