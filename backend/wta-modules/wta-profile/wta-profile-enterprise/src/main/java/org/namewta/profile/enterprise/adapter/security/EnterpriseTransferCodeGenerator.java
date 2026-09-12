package org.namewta.profile.enterprise.adapter.security;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
/** 企业转移验证码生成器，产生一次性数字验证码。 */
@Component
public class EnterpriseTransferCodeGenerator implements org.namewta.profile.enterprise.port.security.EnterpriseTransferCodePort {
    private final SecureRandom random = new SecureRandom();
    /** 生成转移验证码。 */
    public String generate() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
