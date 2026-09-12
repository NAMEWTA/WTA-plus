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

    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default PersonRebindMatchVo match(PersonRebindMatchBo command) { throw new UnsupportedOperationException("请传入 userId"); }
    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default PersonRebindConfirmationVo confirm(PersonRebindConfirmBo command) { throw new UnsupportedOperationException("请传入 userId"); }
    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default PersonRebindSubmissionVo submit(PersonRebindSubmitBo command) { throw new UnsupportedOperationException("请传入 userId"); }
    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default PersonRebindUnbindVo unbind() { throw new UnsupportedOperationException("请传入 userId"); }

    /**
     * 编排 probe 应用用例。
     */
    PersonRebindProbeVo probe(PersonRebindProbeBo command);
    /**
     * 编排 match 应用用例。
     */
    default PersonRebindMatchVo match(long userId, PersonRebindMatchBo command) { return match(command); }
    /**
     * 编排 confirm 应用用例。
     */
    default PersonRebindConfirmationVo confirm(long userId, PersonRebindConfirmBo command) { return confirm(command); }
    /**
     * 编排 submit 应用用例。
     */
    default PersonRebindSubmissionVo submit(long userId, PersonRebindSubmitBo command) { return submit(command); }
    /**
     * 编排 unbind 应用用例。
     */
    default PersonRebindUnbindVo unbind(long userId) { return unbind(); }
    /**
     * 编排 publishApproved 应用用例。
     */
    Optional<PersonRebindPublication> publishApproved(long applicationId, int snapshotVersion, Instant finishedTime);

    /** 接收工作流事件并编排换绑发布。 */
    default void handleProcess(PersonRebindProcessCommand command) {
        throw new UnsupportedOperationException("旧适配器不支持工作流事件入口");
    }
}
