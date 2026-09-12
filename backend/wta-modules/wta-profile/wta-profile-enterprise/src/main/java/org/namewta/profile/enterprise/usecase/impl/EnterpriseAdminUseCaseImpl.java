package org.namewta.profile.enterprise.usecase.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.profile.enterprise.domain.bo.*;
import org.namewta.profile.enterprise.domain.vo.*;
import org.namewta.profile.enterprise.service.EnterpriseAdminService;
import org.namewta.profile.enterprise.usecase.EnterpriseAdminUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * EnterpriseAdminUseCaseImpl 应用用例合同，定义入口可调用的业务场景。
 */
@Service
@RequiredArgsConstructor
public class EnterpriseAdminUseCaseImpl implements EnterpriseAdminUseCase {
    private final EnterpriseAdminService service;

    /**
     * 分页查询企业档案摘要。
     */
    @DSTransactional
    @Override
    public PageResult<EnterpriseProfileSummaryVo> page(EnterpriseAdminQueryBo query) {
        return service.page(query);
    }

    /** 查询可绑定的企业账号。 */
    @DSTransactional
    @Override
    public List<EnterpriseAccountCandidateVo> eligibleUsers(String keyword) {
        return service.eligibleUsers(keyword);
    }

    /** 查询企业档案详情。 */
    @DSTransactional
    @Override
    public EnterpriseProfileDetailVo detail(long profileId) {
        return service.detail(profileId);
    }

    /** 查询企业申请审核上下文。 */
    @DSTransactional
    @Override
    public EnterpriseReviewContextVo review(long applicationId) {
        return service.review(applicationId);
    }

    /** 获取企业申请材料的审核访问地址。 */
    @DSTransactional
    @Override
    public EnterpriseProfileAccessUrl reviewMaterial(long applicationId, long materialRefId) {
        return service.reviewMaterial(applicationId, materialRefId);
    }
    /** 获取企业档案材料访问地址。 */
    @DSTransactional
    @Override
    public EnterpriseProfileAccessUrl material(long profileId, long materialRefId) {
        return service.material(profileId, materialRefId);
    }
    /** 处理企业档案审核决策。 */
    @DSTransactional
    @Override
    public EnterpriseAdminResultVo decide(long operatorId, long applicationId, EnterpriseAdminDecisionBo command) {
        return service.decide(operatorId, applicationId, command);
    }
    /** 创建企业档案。 */
    @DSTransactional
    @Override
    public EnterpriseAdminResultVo create(long operatorId, EnterpriseAdminCreateBo command) {
        return service.create(operatorId, command);
    }
    /** 修改企业档案。 */
    @DSTransactional
    @Override
    public EnterpriseAdminResultVo revise(long operatorId, long profileId, EnterpriseAdminReviseBo command) {
        return service.revise(operatorId, profileId, command);
    }
    /** 管理企业档案绑定关系。 */
    @DSTransactional
    @Override
    public EnterpriseAdminResultVo manageBinding(long operatorId, long profileId, EnterpriseAdminBindingBo command) {
        return service.manageBinding(operatorId, profileId, command);
    }
    /** 为企业档案分配账号。 */
    @DSTransactional
    @Override
    public EnterpriseAdminResultVo assign(long operatorId, long profileId, EnterpriseAdminAssignBo command) {
        return service.assign(operatorId, profileId, command);
    }
    /** 撤销企业档案账号绑定。 */
    @DSTransactional
    @Override
    public EnterpriseAdminResultVo revoke(long operatorId, long profileId, EnterpriseAdminRevokeBo command) {
        return service.revoke(operatorId, profileId, command);
    }
}
