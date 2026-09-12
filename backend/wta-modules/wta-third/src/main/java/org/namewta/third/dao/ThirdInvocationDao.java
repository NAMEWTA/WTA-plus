package org.namewta.third.dao;

import lombok.RequiredArgsConstructor;
import org.namewta.third.domain.ThirdInvocation;
import org.namewta.third.mapper.ThirdInvocationMapper;
import org.namewta.third.port.ThirdInvocationStore;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ThirdInvocationDao implements ThirdInvocationStore {
    private final ThirdInvocationMapper mapper;

    public int upsert(ThirdInvocation invocation) { return mapper.upsert(invocation); }

    public List<ThirdInvocation> findRecent(String providerCode, LocalDateTime from) { return mapper.selectRecent(providerCode, from); }
}
