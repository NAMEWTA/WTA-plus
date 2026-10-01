package org.namewta.oidc.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.domain.OidcKeyMaterial;
import org.namewta.oidc.mapper.OidcKeyMaterialMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 保存保留历史版本的签名/状态键。 */
@Repository
@DS("master")
@RequiredArgsConstructor
public class OidcKeyMaterialDao {
    private final OidcKeyMaterialMapper mapper;

    /** 按创建次序读取保留的版本。 */
    public List<OidcKeyMaterial> list() {
        return mapper.selectList(
                new LambdaQueryWrapper<OidcKeyMaterial>()
                        .orderByDesc(OidcKeyMaterial::getKeyMaterialId));
    }

    /** 精确读取一种密钥的版本，防止跨用途误用。 */
    public OidcKeyMaterial find(String kind, String kid) {
        return mapper.selectOne(
                new LambdaQueryWrapper<OidcKeyMaterial>()
                        .eq(OidcKeyMaterial::getKind, kind)
                        .eq(OidcKeyMaterial::getKid, kid));
    }

    /** 返回唯一活动键；管理轮换先锁配置行再更新。 */
    public OidcKeyMaterial active(String kind) {
        return mapper.selectOne(
                new LambdaQueryWrapper<OidcKeyMaterial>()
                        .eq(OidcKeyMaterial::getKind, kind)
                        .eq(OidcKeyMaterial::getActive, true));
    }

    /** 保留旧键供验签/解密，仅取消其新签发身份。 */
    public void deactivate(String kind) {
        mapper.update(
                null,
                new LambdaUpdateWrapper<OidcKeyMaterial>()
                        .eq(OidcKeyMaterial::getKind, kind)
                        .eq(OidcKeyMaterial::getActive, true)
                        .set(OidcKeyMaterial::getActive, false)
                        .setSql("version=version+1"));
    }

    /** 写入一个不可变密钥版本。 */
    public void insert(OidcKeyMaterial row) {
        mapper.insert(row);
    }
}
