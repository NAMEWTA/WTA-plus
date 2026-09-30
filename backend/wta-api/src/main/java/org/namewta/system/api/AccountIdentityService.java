package org.namewta.system.api;

import org.namewta.system.api.domain.AccountIdentity;

/** 为外部身份协议读取正常账户，不授予任何业务 Client、登录域或 RBAC 权限。 */
public interface AccountIdentityService {
    /**
     * 读取未删除且正常的账户；调用方须先验证令牌主体，再按应用授权裁剪字段。
     * @param userId 已认证账户主键
     * @return 账户资料；不存在、停用、删除或非法主键时返回 null
     */
    AccountIdentity findActiveById(Long userId);
}

