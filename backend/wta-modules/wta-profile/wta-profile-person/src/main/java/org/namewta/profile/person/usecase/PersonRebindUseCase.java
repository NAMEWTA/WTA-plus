package org.namewta.profile.person.usecase;

import org.namewta.profile.person.domain.application.PersonRebindPublication;
import org.namewta.profile.person.domain.application.PersonRebindProcessCommand;
import org.namewta.profile.person.domain.bo.*;
import org.namewta.profile.person.domain.vo.*;

import java.time.Instant;
import java.util.Optional;

/**
 * PersonRebindUseCase 应用用例合同，定义入口可调用的业务场景。
 */
public interface PersonRebindUseCase {


    /**
     * 编排 probe 应用用例。
     */
    PersonRebindProbeVo probe(PersonRebindProbeBo command);
    /**
     * 编排 match 应用用例。
     */
    PersonRebindMatchVo match(long userId, PersonRebindMatchBo command);
    /**
     * 编排 confirm 应用用例。
     */
    PersonRebindConfirmationVo confirm(long userId, PersonRebindConfirmBo command);
    /**
     * 编排 submit 应用用例。
     */
    PersonRebindSubmissionVo submit(long userId, PersonRebindSubmitBo command);
    /**
     * 编排 unbind 应用用例。
     */
    PersonRebindUnbindVo unbind(long userId);
    /**
     * 编排 publishApproved 应用用例。
     */
    Optional<PersonRebindPublication> publishApproved(long applicationId, int snapshotVersion, Instant finishedTime);

    /** 接收工作流事件并编排换绑发布。 */
    void handleProcess(PersonRebindProcessCommand command);
}
