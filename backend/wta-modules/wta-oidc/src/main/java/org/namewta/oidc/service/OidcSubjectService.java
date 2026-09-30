package org.namewta.oidc.service;

import lombok.RequiredArgsConstructor;

import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.oidc.dao.OidcSubjectDao;
import org.namewta.oidc.domain.OidcSubject;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** 发行方内稳定、不随资料变更的公开身份。 */
@Service
@RequiredArgsConstructor
public class OidcSubjectService {
    private final OidcSubjectDao dao;

    /** 为账户分配或读取不复用的稳定公开标识。 */
    public String subject(Long userId) {
        var found = dao.find(userId);
        if (found != null) return found.getSubject();
        var row = new OidcSubject();
        row.setSubjectId(IdGeneratorUtil.nextLongId());
        row.setUserId(userId);
        row.setSubject(UUID.randomUUID().toString());
        row.setVersion(0);
        row.setDelFlag("0");
        try {
            dao.insert(row);
            return row.getSubject();
        } catch (DuplicateKeyException race) {
            var other = dao.find(userId);
            if (other == null) throw race;
            return other.getSubject();
        }
    }
}
