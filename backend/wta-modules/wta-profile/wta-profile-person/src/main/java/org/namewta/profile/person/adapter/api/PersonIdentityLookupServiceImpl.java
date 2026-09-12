package org.namewta.profile.person.adapter.api;
import org.namewta.profile.api.person.PersonIdentityLookupService;
import org.namewta.profile.person.usecase.PersonProfileApiUseCase;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
/**
 * 创建个人身份查询服务实现。
 */
@Service
public class PersonIdentityLookupServiceImpl implements PersonIdentityLookupService {
    private final PersonProfileApiUseCase useCase;
    /**
     * 处理personidentitylookupserviceimpl。
     */
    public PersonIdentityLookupServiceImpl(PersonProfileApiUseCase useCase) {
        this.useCase = useCase;
    }
    /**
     * 查询生效的精确身份匹配
     */
    @Override
    public List<ActiveIdentityMatch> findActiveExactMatches(ActiveIdentityQuery query) {
        return useCase.findActiveExactMatches(query);
    }
    /**
     * 锁定生效的精确匹配记录
     */
    @Override
    public Optional<ActiveIdentityMatch> lockActiveExactMatch(ActiveIdentityLock query) {
        return useCase.lockActiveExactMatch(query);
    }
}
