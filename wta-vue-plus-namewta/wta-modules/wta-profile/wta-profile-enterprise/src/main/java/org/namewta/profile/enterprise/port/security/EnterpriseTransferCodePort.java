package org.namewta.profile.enterprise.port.security;

/** 企业转移验证码生成端口。 */
public interface EnterpriseTransferCodePort {

    /** 生成一次性数字验证码。 */
    String generate();
}
