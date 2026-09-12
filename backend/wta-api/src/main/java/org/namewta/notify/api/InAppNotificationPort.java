package org.namewta.notify.api;

import java.util.List;

/**
 * system 提供的站内收件箱端口。
 */
public interface InAppNotificationPort {
    /** 持久化站内通知快照和收件关系。 */
    void persist(String notificationId, InAppSnapshot snapshot, List<Long> userIds);
    /** 在持久化事务提交后发送实时提示。 */
    void pushRealtime(String notificationId, InAppSnapshot snapshot, List<Long> userIds);
    /** 记录用户已见或已读行为。 */
    void markEngagement(String notificationId, Long userId, boolean read);

    /** 站内内容快照。 */
    record InAppSnapshot(String title, String content, String path, String noticeType, List<String> channels) { }
}
