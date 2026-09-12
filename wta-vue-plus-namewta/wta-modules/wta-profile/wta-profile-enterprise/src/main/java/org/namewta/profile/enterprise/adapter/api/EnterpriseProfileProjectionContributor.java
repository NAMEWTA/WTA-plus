package org.namewta.profile.enterprise.adapter.api;
import org.namewta.profile.api.ProfileProjectionContributor;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.enterprise.usecase.EnterpriseProfileApiUseCase;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;
/**
 * 创建企业档案投影处理器。
 */
@Component
public class EnterpriseProfileProjectionContributor implements ProfileProjectionContributor {
    private final EnterpriseProfileApiUseCase useCase;
    /**
     * 处理enterpriseprofileprojectioncontributor。
     */
    public EnterpriseProfileProjectionContributor(EnterpriseProfileApiUseCase useCase) {
        this.useCase = useCase;
    }
    /**
     * 返回材料所属档案类型
     */
    @Override
    public ProfileType profileType() {
        return ProfileType.ENTERPRISE;
    }
    /**
     * 查询生效的绑定关系
     */
    @Override
    public Map<Long, ProfileBindingSummary> findActiveBindings(Set<Long> userIds) {
        return useCase.findActiveBindings(userIds);
    }
}
