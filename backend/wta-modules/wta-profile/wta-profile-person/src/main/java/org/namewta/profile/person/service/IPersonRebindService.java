package org.namewta.profile.person.service;
import org.namewta.profile.person.domain.application.PersonRebindPublication;
import org.namewta.profile.person.domain.bo.PersonRebindConfirmBo;
import org.namewta.profile.person.domain.bo.PersonRebindMatchBo;
import org.namewta.profile.person.domain.bo.PersonRebindProbeBo;
import org.namewta.profile.person.domain.bo.PersonRebindSubmitBo;
import org.namewta.profile.person.domain.vo.PersonRebindConfirmationVo;
import org.namewta.profile.person.domain.vo.PersonRebindMatchVo;
import org.namewta.profile.person.domain.vo.PersonRebindProbeVo;
import org.namewta.profile.person.domain.vo.PersonRebindSubmissionVo;
import org.namewta.profile.person.domain.vo.PersonRebindUnbindVo;
import java.time.Instant;
import java.util.Optional;
/** 个人换绑服务接口，定义候选探测、确认和解绑能力。 */
public interface IPersonRebindService {
    /** 探测换绑身份状态。 */
    PersonRebindProbeVo probe(PersonRebindProbeBo command);
    /** 匹配换绑目标身份。 */
    PersonRebindMatchVo match(long userId, PersonRebindMatchBo command);
    /** 确认换绑验证码。 */
    PersonRebindConfirmationVo confirm(long userId, PersonRebindConfirmBo command);
    /** 提交换绑申请。 */
    PersonRebindSubmissionVo submit(long userId, PersonRebindSubmitBo command);
    /** 解除档案绑定关系。 */
    PersonRebindUnbindVo unbind(long userId);
    /** 发布approvedrebind。 */
    Optional<PersonRebindPublication> publishApprovedRebind(long applicationId, int snapshotVersion,
                                                            Instant finishedTime);
}
