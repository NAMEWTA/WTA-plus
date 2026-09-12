package org.namewta.profile.person.adapter.time;
import org.namewta.profile.person.support.PersonVerificationTimeSource;
import org.springframework.stereotype.Component;
import java.time.Instant;
/**
 * 承载SystemPersonVerificationTimeSource业务规则的领域服务。
 */
@Component
public class SystemPersonVerificationTimeSource implements PersonVerificationTimeSource {
    /**
     * 获取当前时间
     */
    @Override
    public Instant now() {
        return Instant.now();
    }
}
