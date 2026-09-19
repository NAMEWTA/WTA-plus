package org.namewta.profile.person.usecase;

import org.namewta.common.core.domain.PageResult;
import org.namewta.profile.person.domain.bo.*;
import org.namewta.profile.person.domain.vo.*;

import java.util.List;

/**
 * PersonAdminUseCase 应用用例合同，定义入口可调用的业务场景。
 */
public interface PersonAdminUseCase {


    /**
     * 编排 page 应用用例。
     */
    PageResult<PersonProfileSummaryVo> page(PersonAdminQueryBo query);
    /**
     * 编排 eligibleUsers 应用用例。
     */
    List<PersonAccountCandidateVo> eligibleUsers(String keyword);
    /**
     * 编排 detail 应用用例。
     */
    PersonProfileDetailVo detail(long profileId);
    /**
     * 编排 review 应用用例。
     */
    PersonReviewContextVo review(long applicationId);
    /**
     * 编排 reviewMaterial 应用用例。
     */
    PersonProfileAccessUrl reviewMaterial(long applicationId, long materialRefId);
    /**
     * 编排 material 应用用例。
     */
    PersonProfileAccessUrl material(long profileId, long materialRefId);
    /**
     * 编排 decide 应用用例。
     */
    PersonAdminResultVo decide(long operatorId, long applicationId, PersonAdminDecisionBo command);
    /**
     * 编排 create 应用用例。
     */
    PersonAdminResultVo create(long operatorId, PersonAdminCreateBo command);
    /**
     * 编排 revise 应用用例。
     */
    PersonAdminResultVo revise(long operatorId, long profileId, PersonAdminReviseBo command);
    /**
     * 编排 manageBinding 应用用例。
     */
    PersonAdminResultVo manageBinding(long operatorId, long profileId, PersonAdminBindingBo command);
    /**
     * 编排 assign 应用用例。
     */
    PersonAdminResultVo assign(long operatorId, long profileId, PersonAdminAssignBo command);
    /**
     * 编排 revoke 应用用例。
     */
    PersonAdminResultVo revoke(long operatorId, long profileId, PersonAdminRevokeBo command);
}
