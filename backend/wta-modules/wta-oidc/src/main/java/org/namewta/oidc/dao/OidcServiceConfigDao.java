package org.namewta.oidc.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.domain.OidcServiceConfig;
import org.namewta.oidc.mapper.OidcServiceConfigMapper;
import org.springframework.stereotype.Repository;

/** 固定主键配置行；写操作在用例事务内锁定。 */
@Repository
@DS("master")
@RequiredArgsConstructor
public class OidcServiceConfigDao {
    private final OidcServiceConfigMapper mapper;

    /** 读取唯一服务配置；安装基座初始化该行。 */
    public OidcServiceConfig find() {
        return mapper.selectById(1L);
    }

    /** 串行管理页写入，防止覆盖同一聚合配置。 */
    public OidcServiceConfig lock() {
        return mapper.selectOne(
                new LambdaQueryWrapper<OidcServiceConfig>()
                        .eq(OidcServiceConfig::getServiceConfigId, 1L)
                        .last("FOR UPDATE"));
    }

    /** 按版本更新唯一目标配置。 */
    public int update(OidcServiceConfig row) {
        return mapper.updateById(row);
    }
}
