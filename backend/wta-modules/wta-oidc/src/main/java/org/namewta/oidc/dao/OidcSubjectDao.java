package org.namewta.oidc.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.domain.OidcSubject;
import org.namewta.oidc.mapper.OidcSubjectMapper;
import org.springframework.stereotype.Repository;

/** 不复用的账户公开标识映射。 */
@Repository
@RequiredArgsConstructor
@DS("master")
public class OidcSubjectDao {
    private final OidcSubjectMapper mapper;

    /** 读取标识对应的当前持久记录，不存在时返回空。 */
    public OidcSubject find(Long userId) {
        return mapper.selectOne(
                new LambdaQueryWrapper<OidcSubject>().eq(OidcSubject::getUserId, userId));
    }

    /** 写入新建领域记录并保留数据库唯一约束。 */
    public void insert(OidcSubject row) {
        mapper.insert(row);
    }
}
