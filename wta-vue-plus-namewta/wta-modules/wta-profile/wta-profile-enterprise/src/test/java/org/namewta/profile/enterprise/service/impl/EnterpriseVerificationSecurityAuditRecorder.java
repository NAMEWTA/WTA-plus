package org.namewta.profile.enterprise.service.impl;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.profile.enterprise.domain.exception.EnterpriseVerificationException;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationFailureCategory;
import org.namewta.profile.enterprise.dao.EnterpriseVerificationAttemptDao;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.tx.DsPropagation;
import org.springframework.stereotype.Service;
import java.time.Instant;
/**
 * 承载EnterpriseVerificationSecurityAuditRecorder业务规则的领域服务。
 */
@Service
public class EnterpriseVerificationSecurityAuditRecorder {
    private final EnterpriseVerificationAttemptDao dao;
    /**
     * 处理enterpriseverificationsecurityauditrecorder。
     */
    public EnterpriseVerificationSecurityAuditRecorder(EnterpriseVerificationAttemptDao dao) {
        this.dao = dao;
    }
    /** 记录认证安全审计。 */

    public void record(Long applicationId, EnterpriseVerificationFailureCategory category, Instant occurredAt) {
        String result = category == EnterpriseVerificationFailureCategory.LATE_CALLBACK ? "IGNORED" : "FAILED";
        if (dao.insertSecurityAudit(IdGeneratorUtil.nextLongId(), applicationId, result, category.name(), occurredAt) != 1) {
            throw new EnterpriseVerificationException(
                EnterpriseVerificationFailureCategory.PROVIDER_FAILURE,
                "Enterprise verification security audit could not be appended");
        }
    }
}
