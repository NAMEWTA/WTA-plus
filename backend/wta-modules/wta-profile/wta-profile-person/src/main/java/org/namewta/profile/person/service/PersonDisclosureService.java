package org.namewta.profile.person.service;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.namewta.profile.api.domain.PersonDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.person.dao.PersonDisclosureDao;
import org.namewta.profile.person.domain.model.read.PersonDisclosureRow;

/** 个人档案字段授权投影，保持原有非敏感摘要合同不变。 */
@Service
@RequiredArgsConstructor
public class PersonDisclosureService {
    private final PersonDisclosureDao dao;

    /**
     * 按可信主体及已核准字段读取当前资料，再执行输出字段裁剪。
     * @param userId 已认证账户主键
     * @param fields 本子域的已核准字段；为空时不访问数据库
     * @return 授权投影；无有效绑定时正文为空且已授权认证标志为 false
     */
    public PersonDisclosure findByUserId(Long userId, Set<ProfileDisclosureField> fields) {
        if (userId == null || userId <= 0 || fields == null
            || fields.stream().anyMatch(f -> f == null || f.profileType() != ProfileType.PERSON)) {
            throw new IllegalArgumentException("Valid subject and person disclosure fields are required");
        }
        Set<ProfileDisclosureField> allowed = Set.copyOf(fields);
        if (allowed.isEmpty()) return null;
        PersonDisclosureRow row = dao.findByUserId(userId, allowed.stream().map(Enum::name).collect(Collectors.toUnmodifiableSet()));
        if (row != null && !userId.equals(row.getUserId())) {
            throw new IllegalStateException("Disclosure subject mismatch");
        }
        return new PersonDisclosure(
            row != null,
            row == null ? null : row.getProfileId(),
            row == null ? null : row.getVerifiedAt(),
            row == null ? null : row.getFullName(),
            row == null ? null : row.getGender(),
            row == null ? null : row.getBirthDate(),
            row == null ? null : row.getDocumentType(),
            row == null ? null : row.getDocumentNumberMasked(),
            row == null ? null : row.getDocumentNumber(),
            row == null ? null : row.getValidFrom(),
            row == null ? null : row.getValidUntil()
        ).filtered(allowed);
    }
}

