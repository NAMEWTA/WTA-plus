package org.namewta.profile.person.usecase.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.profile.person.domain.bo.*;
import org.namewta.profile.person.domain.vo.*;
import org.namewta.profile.person.service.PersonAdminService;
import org.namewta.profile.person.usecase.PersonAdminUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * PersonAdminUseCaseImpl 应用用例合同，定义入口可调用的业务场景。
 */
@Service
@RequiredArgsConstructor
public class PersonAdminUseCaseImpl implements PersonAdminUseCase {

    private final PersonAdminService service;

    /** 分页查询人员档案摘要。 */
    @DSTransactional
    @Override
    public PageResult<PersonProfileSummaryVo> page(PersonAdminQueryBo query) {
        return service.page(query);
    }

    /** 查询可绑定的人员账号。 */
    @DSTransactional
    @Override
    public List<PersonAccountCandidateVo> eligibleUsers(String keyword) {
        return service.eligibleUsers(keyword);
    }

    /** 查询人员档案详情。 */
    @DSTransactional
    @Override
    public PersonProfileDetailVo detail(long profileId) {
        return service.detail(profileId);
    }

    /** 查询人员申请审核上下文。 */
    @DSTransactional
    @Override
    public PersonReviewContextVo review(long applicationId) {
        return service.review(applicationId);
    }

    /** 获取人员申请材料的审核访问地址。 */
    @DSTransactional
    @Override
    public PersonProfileAccessUrl reviewMaterial(long applicationId, long materialRefId) {
        return service.reviewMaterial(applicationId, materialRefId);
    }

    /** 获取人员档案材料访问地址。 */
    @DSTransactional
    @Override
    public PersonProfileAccessUrl material(long profileId, long materialRefId) {
        return service.material(profileId, materialRefId);
    }
    /** 处理人员档案审核决策。 */
    @DSTransactional
    @Override
    public PersonAdminResultVo decide(long operatorId, long applicationId, PersonAdminDecisionBo command) {
        return service.decide(operatorId, applicationId, command);
    }
    /** 创建人员档案。 */
    @DSTransactional
    @Override
    public PersonAdminResultVo create(long operatorId, PersonAdminCreateBo command) {
        return service.create(operatorId, command);
    }
    /** 修改人员档案。 */
    @DSTransactional
    @Override
    public PersonAdminResultVo revise(long operatorId, long profileId, PersonAdminReviseBo command) {
        return service.revise(operatorId, profileId, command);
    }
    /** 管理人员档案绑定关系。 */
    @DSTransactional
    @Override
    public PersonAdminResultVo manageBinding(long operatorId, long profileId, PersonAdminBindingBo command) {
        return service.manageBinding(operatorId, profileId, command);
    }
    /** 为人员档案分配账号。 */
    @DSTransactional
    @Override
    public PersonAdminResultVo assign(long operatorId, long profileId, PersonAdminAssignBo command) {
        return service.assign(operatorId, profileId, command);
    }
    /** 撤销人员档案账号绑定。 */
    @DSTransactional
    @Override
    public PersonAdminResultVo revoke(long operatorId, long profileId, PersonAdminRevokeBo command) {
        return service.revoke(operatorId, profileId, command);
    }
}
