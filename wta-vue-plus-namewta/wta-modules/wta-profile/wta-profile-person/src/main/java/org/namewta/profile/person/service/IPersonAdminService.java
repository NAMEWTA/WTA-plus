package org.namewta.profile.person.service;
import org.namewta.common.core.domain.PageResult;
import org.namewta.profile.person.domain.vo.PersonAccountCandidateVo;
import org.namewta.profile.person.domain.bo.PersonAdminAssignBo;
import org.namewta.profile.person.domain.bo.PersonAdminBindingBo;
import org.namewta.profile.person.domain.bo.PersonAdminCreateBo;
import org.namewta.profile.person.domain.bo.PersonAdminDecisionBo;
import org.namewta.profile.person.domain.vo.PersonProfileDetailVo;
import org.namewta.profile.person.domain.bo.PersonAdminQueryBo;
import org.namewta.profile.person.domain.vo.PersonAdminResultVo;
import org.namewta.profile.person.domain.vo.PersonReviewContextVo;
import org.namewta.profile.person.domain.bo.PersonAdminReviseBo;
import org.namewta.profile.person.domain.bo.PersonAdminRevokeBo;
import org.namewta.profile.person.domain.vo.PersonProfileSummaryVo;
import org.namewta.profile.person.domain.vo.PersonProfileAccessUrl;
import java.util.List;
/** 个人档案管理服务接口，定义管理员查询、审核和绑定处置能力。 */
public interface IPersonAdminService {
    /** 分页查询档案数据。 */
    PageResult<PersonProfileSummaryVo> page(PersonAdminQueryBo query);
    /** 查询可绑定的用户候选。 */
    List<PersonAccountCandidateVo> eligibleUsers(String keyword);
    /** 查询档案详情。 */
    PersonProfileDetailVo detail(long profileId);
    /** 查询档案审核上下文。 */
    PersonReviewContextVo review(long applicationId);
    /** 查询审核材料访问地址。 */
    PersonProfileAccessUrl reviewMaterial(long applicationId, long materialRefId);
    /** 查询档案材料访问地址。 */
    PersonProfileAccessUrl material(long profileId, long materialRefId);
    /** 提交档案审核决定。 */
    PersonAdminResultVo decide(long operatorId, long applicationId, PersonAdminDecisionBo command);
    /** 创建档案并返回管理结果。 */
    PersonAdminResultVo create(long operatorId, PersonAdminCreateBo command);
    /** 修改已退回档案申请。 */
    PersonAdminResultVo revise(long operatorId, long profileId, PersonAdminReviseBo command);
    /** 管理档案绑定状态。 */
    PersonAdminResultVo manageBinding(long operatorId, long profileId, PersonAdminBindingBo command);
    /** 分配档案负责人。 */
    PersonAdminResultVo assign(long operatorId, long profileId, PersonAdminAssignBo command);
    /** 撤销转移挑战或档案绑定。 */
    PersonAdminResultVo revoke(long operatorId, long profileId, PersonAdminRevokeBo command);
}
