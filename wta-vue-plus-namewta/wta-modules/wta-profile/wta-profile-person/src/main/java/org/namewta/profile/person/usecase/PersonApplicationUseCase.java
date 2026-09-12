package org.namewta.profile.person.usecase;

import org.namewta.profile.person.domain.bo.PersonApplicationSaveBo;
import org.namewta.profile.person.domain.application.PersonApplicationProcessCommand;
import org.namewta.profile.person.domain.vo.PersonApplicationVo;

/**
 * PersonApplicationUseCase 应用用例合同，定义入口可调用的业务场景。
 */
public interface PersonApplicationUseCase {

    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default PersonApplicationVo current() { throw new UnsupportedOperationException("请传入 userId"); }

    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default PersonApplicationVo save(PersonApplicationSaveBo command) { throw new UnsupportedOperationException("请传入 userId"); }

    /** @deprecated 新入口必须显式传入操作者编号。 */
    @Deprecated
    default PersonApplicationVo submit(int expectedVersion) { throw new UnsupportedOperationException("请传入 userId"); }

    /**
     * 编排 current 应用用例。
     */
    default PersonApplicationVo current(long userId) { return current(); }

    /**
     * 编排 save 应用用例。
     */
    default PersonApplicationVo save(long userId, PersonApplicationSaveBo command) { return save(command); }

    /**
     * 编排 submit 应用用例。
     */
    default PersonApplicationVo submit(long userId, int expectedVersion) { return submit(expectedVersion); }

    /** 接收工作流事件并编排申请状态回写。 */
    default void handleProcess(PersonApplicationProcessCommand command) {
        throw new UnsupportedOperationException("旧适配器不支持工作流事件入口");
    }
}
