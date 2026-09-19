package org.namewta.system.domain.policy;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.validation.ValidationFormat;
import org.namewta.common.core.validation.ValidationUtils;

/** 账号资料写入的手机号规则；历史空号只在受影响的写入时补齐。 */
public final class UserPhonePolicy {
    private UserPhonePolicy() {
    }

    /**
     * 校验新增或显式修改的号码，并返回实际应保存的标准化值。
     *
     * @param phoneNumber 用户输入
     * @return 去除两端空白的有效大陆手机号
     * @throws ServiceException 输入缺失、空白或格式错误
     */
    public static String requirePhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new ServiceException("手机号码不能为空");
        }
        String normalized = phoneNumber.strip();
        if (!ValidationUtils.isValid(normalized, ValidationFormat.MAINLAND_MOBILE)) {
            throw new ServiceException("请输入正确的手机号码");
        }
        return normalized;
    }

    /**
     * 校验更新后的有效号码；省略字段只校验旧值，不回写读取快照。
     *
     * @param requestedPhone 显式号码，null 表示本次未修改
     * @param existingPhone 数据库现有号码
     * @return 显式修改的标准化号码，或保留字段的 null
     * @throws ServiceException 显式号码或保留的旧值无效
     */
    public static String forUpdate(String requestedPhone, String existingPhone) {
        if (requestedPhone == null) {
            requirePhone(existingPhone);
            return null;
        }
        return requirePhone(requestedPhone);
    }
}
