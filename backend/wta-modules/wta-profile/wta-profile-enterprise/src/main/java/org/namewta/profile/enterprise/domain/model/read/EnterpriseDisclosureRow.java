package org.namewta.profile.enterprise.domain.model.read;

import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

/** 当前有效企业档案的字段白名单查询读模型，不得直接序列化为外部响应。 */
@Data
public class EnterpriseDisclosureRow {
    private Long userId;
    private Long profileId;
    private Instant verifiedAt;
    private String name;
    private String creditCode;
    private String type;
    private String legalRepresentativeName;
    private String legalDocumentType;
    private String legalDocumentNumberMasked;
    private String legalDocumentNumber;
    private LocalDate establishedDate;
    private LocalDate businessTermFrom;
    private LocalDate businessTermUntil;
    private String registeredAddress;
    private String businessScope;
    private String contactName;
    private String contactPhone;
    private String email;
    private BigDecimal registeredCapital;
    private String industryCode;
    private String website;
}

