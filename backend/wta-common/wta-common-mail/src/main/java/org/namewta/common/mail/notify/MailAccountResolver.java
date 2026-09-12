package org.namewta.common.mail.notify;

import cn.hutool.extra.mail.MailAccount;

/**
 * 按 Provider 标识解析 SMTP 账户，由通知控制面提供实现。
 */
@FunctionalInterface
public interface MailAccountResolver {

    /**
     * 解析发件账户。
     *
     * @param providerKey 渠道账号 configKey
     * @return SMTP 账户，找不到时返回 {@code null}
     */
    MailAccount resolve(String providerKey);
}
