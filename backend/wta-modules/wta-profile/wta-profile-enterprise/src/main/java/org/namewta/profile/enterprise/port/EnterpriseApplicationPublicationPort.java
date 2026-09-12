package org.namewta.profile.enterprise.port;

import org.namewta.profile.enterprise.domain.application.EnterpriseDocumentTypeRule;
import org.namewta.profile.enterprise.domain.application.EnterprisePublication;
import org.namewta.profile.enterprise.domain.application.EnterpriseSubmission;

import java.time.Instant;
import java.util.Optional;

/** EnterpriseApplicationPublicationPort 业务端口合同，隔离企业管理流程与外部适配实现。 */
public interface EnterpriseApplicationPublicationPort {

    /**
     * 处理 findDocumentType 业务步骤。
     */
    Optional<EnterpriseDocumentTypeRule> findDocumentType(String documentTypeCode);

    /**
     * 处理 requireSubmission 业务步骤。
     */
    EnterpriseSubmission requireSubmission(long applicationId, int submissionSeq);

    /**
     * 处理 publishApproved 业务步骤。
     */
    EnterprisePublication publishApproved(long applicationId, int snapshotVersion, Instant finishedTime);
}
