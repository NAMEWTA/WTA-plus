package org.namewta.profile.enterprise.usecase;

import java.util.Set;
import org.namewta.profile.api.domain.EnterpriseDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;

/** 已核准企业档案字段发布用例，不承担外部客户端授权。 */
public interface EnterpriseDisclosureUseCase {
    /**
     * 查询已核准字段。
     * @param userId 已认证账户主键
     * @param fields 已核准的本子域字段
     * @return 当前有效字段投影
     */
    EnterpriseDisclosure findByUserId(Long userId, Set<ProfileDisclosureField> fields);
}

