package org.namewta.profile.person.usecase;

import org.namewta.profile.person.domain.bo.PersonApplicationSaveBo;
import org.namewta.profile.person.domain.application.PersonApplicationProcessCommand;
import org.namewta.profile.person.domain.vo.PersonApplicationVo;

/**
 * PersonApplicationUseCase 应用用例合同，定义入口可调用的业务场景。
 */
public interface PersonApplicationUseCase {




    /**
     * 编排 current 应用用例。
     */
    PersonApplicationVo current(long userId);

    /**
     * 编排 save 应用用例。
     */
    PersonApplicationVo save(long userId, PersonApplicationSaveBo command);

    /**
     * 编排 submit 应用用例。
     */
    PersonApplicationVo submit(long userId, int expectedVersion);

    /** 接收工作流事件并编排申请状态回写。 */
    void handleProcess(PersonApplicationProcessCommand command);
}
