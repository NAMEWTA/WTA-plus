package org.namewta.sso.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

import org.namewta.sso.domain.SsoServiceConfig;
import org.namewta.sso.mapper.SsoServiceConfigMapper;
import org.springframework.stereotype.Repository;

/** 固定主键配置行；写操作在用例事务内锁定。 */
@Repository
@DS("master")
@RequiredArgsConstructor
public class SsoServiceConfigDao {
    private final SsoServiceConfigMapper mapper;

    /** 读取唯一服务配置；安装基座初始化该行。 */
    public SsoServiceConfig find() {
        return mapper.selectById(1L);
    }

    /** 串行管理页写入，防止覆盖同一聚合配置。 */
    public SsoServiceConfig lock() {
        return mapper.selectOne(
                new LambdaQueryWrapper<SsoServiceConfig>()
                        .eq(SsoServiceConfig::getServiceConfigId, 1L)
                        .last("FOR UPDATE"));
    }

    /** 按版本更新唯一目标配置。 */
    public int update(SsoServiceConfig row) {
        return mapper.updateById(row);
    }
}
