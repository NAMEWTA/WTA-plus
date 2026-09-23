package org.namewta.system.domain.constant;

/**
 * 用户登录域授权来源。
 */
public interface UserTypeGrantSource {

    /**
     * 公开注册
     */
    String SELF_REGISTER = "SELF_REGISTER";

    /**
     * 管理端创建用户时授予
     */
    String ADMIN_CREATE = "ADMIN_CREATE";

    /**
     * 管理端后续授权
     */
    String ADMIN_GRANT = "ADMIN_GRANT";

    /**
     * 系统初始化回填
     */
    String SYSTEM_INIT = "SYSTEM_INIT";

}
