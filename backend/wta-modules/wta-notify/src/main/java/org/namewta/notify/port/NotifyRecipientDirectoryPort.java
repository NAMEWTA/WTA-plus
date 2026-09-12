package org.namewta.notify.port;

import org.namewta.notify.domain.vo.NotifyRecipientUserVo;

import java.util.Collection;
import java.util.List;

/** 用户目录端口，隔离通知模块与 system 用户服务。 */
public interface NotifyRecipientDirectoryPort {
    List<NotifyRecipientUserVo> search(String keyword, int limit);
    List<NotifyRecipientUserVo> byIds(Collection<Long> userIds);
}
