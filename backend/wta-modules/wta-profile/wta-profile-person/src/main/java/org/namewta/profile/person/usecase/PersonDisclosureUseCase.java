package org.namewta.profile.person.usecase;

import java.util.Set;
import org.namewta.profile.api.domain.PersonDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;

/** 已核准个人档案字段发布用例，不承担外部客户端授权。 */
public interface PersonDisclosureUseCase {
    /**
     * 查询已核准字段。
     * @param userId 已认证账户主键
     * @param fields 已核准的本子域字段
     * @return 当前有效字段投影
     */
    PersonDisclosure findByUserId(Long userId, Set<ProfileDisclosureField> fields);
}

