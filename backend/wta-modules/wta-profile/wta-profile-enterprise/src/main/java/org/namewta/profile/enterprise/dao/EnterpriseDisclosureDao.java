package org.namewta.profile.enterprise.dao;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.namewta.profile.enterprise.mapper.EnterpriseDisclosureMapper;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseDisclosureRow;

/** 企业发布字段唯一持久化入口，按主体读取当前有效绑定。 */
@Repository
@RequiredArgsConstructor
public class EnterpriseDisclosureDao {
    private final EnterpriseDisclosureMapper mapper;

    /**
     * 读取服务端白名单字段，查询规则由固定 XML 维护。
     * @param userId 已认证账户主键
     * @param fields 已核准的枚举名称
     * @return 当前有效字段；无有效绑定时为 null
     */
    public EnterpriseDisclosureRow findByUserId(long userId, Set<String> fields) {
        return mapper.selectDisclosure(userId, fields);
    }
}

