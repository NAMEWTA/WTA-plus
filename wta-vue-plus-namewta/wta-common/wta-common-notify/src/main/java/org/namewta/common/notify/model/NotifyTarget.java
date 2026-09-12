package org.namewta.common.notify.model;

/**
 * 已解析的通知物理目标。
 *
 * @param type  目标类型
 * @param value 完整目标值
 * @param role  渠道内角色
 */
public record NotifyTarget(String type, String value, String role) {

    public static NotifyTarget phone(String phone) {
        return new NotifyTarget(NotifyTargetType.PHONE, phone, NotifyTargetRole.DIRECT);
    }

    public static NotifyTarget email(String email) {
        return email(email, NotifyTargetRole.TO);
    }

    public static NotifyTarget email(String email, String role) {
        return new NotifyTarget(NotifyTargetType.EMAIL, email, role);
    }

    public static NotifyTarget openId(String openId) {
        return new NotifyTarget(NotifyTargetType.OPEN_ID, openId, NotifyTargetRole.DIRECT);
    }

    public static NotifyTarget user(String userId) {
        return new NotifyTarget(NotifyTargetType.USER, userId, NotifyTargetRole.DIRECT);
    }
}
