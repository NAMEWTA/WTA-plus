package org.namewta.profile.api;

import java.util.Set;
import org.namewta.profile.api.domain.ProfileDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;

/** 显式授权后的档案字段发布合同，与旧的非敏感 ProfileService 摘要独立。 */
public interface ProfileDisclosureService {
    /**
     * 读取主体的有效绑定及当前有效档案版本，仅返回核准字段。
     * <p>调用方必须先认证主体并验证应用字段上限；不能将外部请求的 userId 或字段直接透传。</p>
     * @param userId 已认证账户主键
     * @param fields 已核准的封闭字段白名单；空集合不读取资料
     * @return 当前已核准字段；没有有效档案时认证状态可为 false、正文为空
     * @throws IllegalArgumentException 非法主体或字段集合
     */
    ProfileDisclosure findByUserId(Long userId, Set<ProfileDisclosureField> fields);
}

