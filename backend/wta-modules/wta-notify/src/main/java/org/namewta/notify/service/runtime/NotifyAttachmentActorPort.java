package org.namewta.notify.service.runtime;

/** 从服务端已验证会话取得附件提交者；HTTP 命令不能携带该事实。 */
public interface NotifyAttachmentActorPort {
    Actor current();

    record Actor(Long userId, Long clientPk) { }
}
