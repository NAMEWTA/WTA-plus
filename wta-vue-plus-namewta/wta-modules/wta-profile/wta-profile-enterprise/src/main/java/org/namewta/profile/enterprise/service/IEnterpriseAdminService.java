package org.namewta.profile.enterprise.service;
import org.namewta.common.core.domain.PageResult;
import org.namewta.profile.enterprise.domain.vo.EnterpriseAccountCandidateVo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminAssignBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminBindingBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminCreateBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminDecisionBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseProfileDetailVo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminQueryBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseAdminResultVo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseReviewContextVo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminReviseBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminRevokeBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseProfileSummaryVo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseProfileAccessUrl;
import java.util.List;
/** 企业档案管理服务接口，定义管理员查询、审核和绑定处置能力。 */
public interface IEnterpriseAdminService {
    /** 分页查询档案数据。 */
    PageResult<EnterpriseProfileSummaryVo> page(EnterpriseAdminQueryBo query);
    /** 查询可绑定的用户候选。 */
    List<EnterpriseAccountCandidateVo> eligibleUsers(String keyword);
    /** 查询档案详情。 */
    EnterpriseProfileDetailVo detail(long profileId);
    /** 查询档案审核上下文。 */
    EnterpriseReviewContextVo review(long applicationId);
    /** 查询审核材料访问地址。 */
    EnterpriseProfileAccessUrl reviewMaterial(long applicationId, long materialRefId);
    /** 查询档案材料访问地址。 */
    EnterpriseProfileAccessUrl material(long profileId, long materialRefId);
    /** 提交档案审核决定。 */
    EnterpriseAdminResultVo decide(long operatorId, long applicationId, EnterpriseAdminDecisionBo command);
    /** 创建档案并返回管理结果。 */
    EnterpriseAdminResultVo create(long operatorId, EnterpriseAdminCreateBo command);
    /** 修改已退回档案申请。 */
    EnterpriseAdminResultVo revise(long operatorId, long profileId, EnterpriseAdminReviseBo command);
    /** 管理档案绑定状态。 */
    EnterpriseAdminResultVo manageBinding(long operatorId, long profileId, EnterpriseAdminBindingBo command);
    /** 分配档案负责人。 */
    EnterpriseAdminResultVo assign(long operatorId, long profileId, EnterpriseAdminAssignBo command);
    /** 撤销转移挑战或档案绑定。 */
    EnterpriseAdminResultVo revoke(long operatorId, long profileId, EnterpriseAdminRevokeBo command);
}
