package org.namewta.profile.enterprise.service;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.namewta.profile.api.domain.EnterpriseDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.enterprise.dao.EnterpriseDisclosureDao;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseDisclosureRow;

/** 企业档案字段授权投影，保持原有非敏感摘要合同不变。 */
@Service
@RequiredArgsConstructor
public class EnterpriseDisclosureService {
    private final EnterpriseDisclosureDao dao;

    /**
     * 按可信主体及已核准字段读取当前资料，再执行输出字段裁剪。
     * @param userId 已认证账户主键
     * @param fields 本子域的已核准字段；为空时不访问数据库
     * @return 授权投影；无有效绑定时正文为空且已授权认证标志为 false
     */
    public EnterpriseDisclosure findByUserId(Long userId, Set<ProfileDisclosureField> fields) {
        if (userId == null || userId <= 0 || fields == null
            || fields.stream().anyMatch(f -> f == null || f.profileType() != ProfileType.ENTERPRISE)) {
            throw new IllegalArgumentException("Valid subject and enterprise disclosure fields are required");
        }
        Set<ProfileDisclosureField> allowed = Set.copyOf(fields);
        if (allowed.isEmpty()) return null;
        EnterpriseDisclosureRow row = dao.findByUserId(userId, allowed.stream().map(Enum::name).collect(Collectors.toUnmodifiableSet()));
        if (row != null && !userId.equals(row.getUserId())) {
            throw new IllegalStateException("Disclosure subject mismatch");
        }
        return new EnterpriseDisclosure(
            row != null,
            row == null ? null : row.getProfileId(),
            row == null ? null : row.getVerifiedAt(),
            row == null ? null : row.getName(),
            row == null ? null : row.getCreditCode(),
            row == null ? null : row.getType(),
            row == null ? null : row.getLegalRepresentativeName(),
            row == null ? null : row.getLegalDocumentType(),
            row == null ? null : row.getLegalDocumentNumberMasked(),
            row == null ? null : row.getLegalDocumentNumber(),
            row == null ? null : row.getEstablishedDate(),
            row == null ? null : row.getBusinessTermFrom(),
            row == null ? null : row.getBusinessTermUntil(),
            row == null ? null : row.getRegisteredAddress(),
            row == null ? null : row.getBusinessScope(),
            row == null ? null : row.getContactName(),
            row == null ? null : row.getContactPhone(),
            row == null ? null : row.getEmail(),
            row == null ? null : row.getRegisteredCapital(),
            row == null ? null : row.getIndustryCode(),
            row == null ? null : row.getWebsite()
        ).filtered(allowed);
    }
}

