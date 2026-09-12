package org.namewta.profile.enterprise.domain.vo;

import org.namewta.profile.enterprise.domain.application.EnterpriseApplication;
import org.namewta.profile.enterprise.domain.application.EnterpriseIdentityFields;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** EnterpriseApplicationVo 对外返回模型。 */
public record EnterpriseApplicationVo(
    Long enterpriseApplicationId,
    String status,
    String enterpriseName,
    String unifiedCreditCode,
    String enterpriseType,
    String legalRepresentativeName,
    String legalDocumentTypeCode,
    String legalDocumentNumber,
    boolean handlerIsLegalRepresentative,
    LocalDate establishedDate,
    LocalDate businessTermFrom,
    LocalDate businessTermUntil,
    String registeredAddress,
    String businessScope,
    String contactName,
    String contactPhone,
    String email,
    BigDecimal registeredCapital,
    String industryCode,
    String website,
    String providerCode,
    int snapshotVersion,
    int version,
    Instant submittedTime,
    Instant finishedTime
) {

    /** 根据输入创建档案并返回管理结果。 */
    public static EnterpriseApplicationVo from(EnterpriseApplication application) {
        EnterpriseIdentityFields fields = application.fields();
        return new EnterpriseApplicationVo(application.enterpriseApplicationId(), application.status(),
            fields.enterpriseName(), fields.unifiedCreditCode(), fields.enterpriseType(),
            fields.legalRepresentativeName(), fields.legalDocumentTypeCode(), fields.legalDocumentNumber(),
            fields.handlerIsLegalRepresentative(), fields.establishedDate(), fields.businessTermFrom(),
            fields.businessTermUntil(), fields.registeredAddress(), fields.businessScope(), fields.contactName(),
            fields.contactPhone(), fields.email(), fields.registeredCapital(), fields.industryCode(), fields.website(),
            application.providerCode(), application.submissionSeq(), application.version(),
            application.submittedTime(), application.finishedTime());
    }
}
