package org.namewta.profile.person.port.notification;

import org.namewta.profile.person.event.PersonReboundEvent;

/**
 * 个人换绑通知端口。
 *
 * <p>换绑业务只依赖通知能力合同，通知渠道实现由基础设施适配器提供。</p>
 */
public interface PersonRebindNotificationPort {

    /** 暂存换绑通知，等待事务提交后投递。 */
    void stage(PersonReboundEvent event);
}
