package org.namewta.notify.api;

import java.util.List;

/**
 * 通知中心提供的站内收件箱端口。
 */
public interface InAppNotificationPort {
    /** 调用方须在结果动态事务中持久化站内通知与收件关系，失败使整笔事务回滚。 */
    void persist(String notificationId, InAppSnapshot snapshot, List<Long> userIds);
    /** 仅由提交后监听器调用；实时失败不改变已提交的消息或投递结果。 */
    void pushRealtime(String notificationId, InAppSnapshot snapshot, List<Long> userIds);
    /** 记录用户已见或已读行为。 */
    void markEngagement(String notificationId, Long userId, boolean read);

    /** 站内内容快照。 */
    record InAppSnapshot(String title, String content, String path, String noticeType, List<String> channels) { }
}
