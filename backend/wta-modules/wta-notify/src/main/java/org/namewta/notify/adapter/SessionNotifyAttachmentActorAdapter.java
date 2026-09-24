package org.namewta.notify.adapter;

import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.notify.service.runtime.NotifyAttachmentActorPort;
import org.namewta.system.api.model.LoginUser;
import org.springframework.stereotype.Component;

/** 只在提交线程读取已认证会话，Worker 绝不重新推断附件权限。 */
@Component
public class SessionNotifyAttachmentActorAdapter implements NotifyAttachmentActorPort {
    @Override
    public Actor current() {
        LoginUser user = LoginHelper.getLoginUser();
        return user == null ? null : new Actor(user.getUserId(), user.getClientPk());
    }
}
