package org.namewta.profile.person.usecase.impl;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.person.service.PersonRebindNotificationService;
import org.namewta.profile.person.event.PersonReboundEvent;
import org.namewta.profile.person.usecase.PersonRebindNotificationUseCase;
import org.springframework.stereotype.Service;

/** 个人换绑通知事件的应用用例实现。 */
@Service
@RequiredArgsConstructor
public class PersonRebindNotificationUseCaseImpl implements PersonRebindNotificationUseCase {

    private final PersonRebindNotificationService service;

    /** 在事务提交后投递原账户通知。 */
    @DSTransactional
    @Override
    public void notifyOldAccount(PersonReboundEvent event) {
        service.notifyOldAccount(event);
    }
}
