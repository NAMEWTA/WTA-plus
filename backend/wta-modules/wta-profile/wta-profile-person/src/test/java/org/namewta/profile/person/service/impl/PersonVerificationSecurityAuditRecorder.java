package org.namewta.profile.person.service.impl;
import org.namewta.profile.person.domain.exception.PersonVerificationException;
import org.namewta.profile.person.domain.verification.PersonVerificationFailureCategory;
import org.namewta.profile.person.dao.PersonVerificationAttemptDao;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.tx.DsPropagation;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.springframework.stereotype.Service;
import java.time.Instant;
/**
 * 承载PersonVerificationSecurityAuditRecorder业务规则的领域服务。
 */
@Service
public class PersonVerificationSecurityAuditRecorder {
    private final PersonVerificationAttemptDao dao;
    /**
     * 处理personverificationsecurityauditrecorder。
     */
    public PersonVerificationSecurityAuditRecorder(PersonVerificationAttemptDao dao) {
        this.dao = dao;
    }
    /** 记录认证安全审计。 */

    public void record(Long applicationId, PersonVerificationFailureCategory category, Instant occurredAt) {
        String result = category == PersonVerificationFailureCategory.LATE_CALLBACK ? "IGNORED" : "FAILED";
        if (dao.insertSecurityAudit(
            IdGeneratorUtil.nextLongId(), applicationId, result, category.name(), occurredAt) != 1) {
            throw new PersonVerificationException(
                PersonVerificationFailureCategory.PROVIDER_FAILURE,
                "Person verification security audit could not be appended");
        }
    }
}
