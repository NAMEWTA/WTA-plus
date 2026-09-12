package org.namewta.profile.person.usecase;

import org.namewta.profile.person.event.PersonReboundEvent;

/** 个人换绑通知事件的应用用例合同。 */
public interface PersonRebindNotificationUseCase {

    /** 在事务提交后投递原账户通知。 */
    void notifyOldAccount(PersonReboundEvent event);
}
