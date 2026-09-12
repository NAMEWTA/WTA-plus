package org.namewta.profile.person.port;

import org.namewta.profile.person.domain.application.PersonDocumentTypeRule;
import org.namewta.profile.person.domain.application.PersonPublication;
import org.namewta.profile.person.domain.application.PersonSubmission;

import java.time.Instant;
import java.util.Optional;

/** PersonApplicationPublicationPort 业务端口合同，隔离个人管理流程与外部适配实现。 */
public interface PersonApplicationPublicationPort {

    /**
     * 处理 findDocumentType 业务步骤。
     */
    Optional<PersonDocumentTypeRule> findDocumentType(String documentTypeCode);

    /**
     * 处理 requireSubmission 业务步骤。
     */
    PersonSubmission requireSubmission(long applicationId, int submissionSeq);

    /**
     * 处理 publishApproved 业务步骤。
     */
    PersonPublication publishApproved(long applicationId, int snapshotVersion, Instant finishedTime);
}
