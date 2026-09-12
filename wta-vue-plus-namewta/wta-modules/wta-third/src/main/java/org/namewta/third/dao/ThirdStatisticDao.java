package org.namewta.third.dao;

import lombok.RequiredArgsConstructor;
import org.namewta.third.domain.ThirdStatistic;
import org.namewta.third.mapper.ThirdStatisticMapper;
import org.namewta.third.port.ThirdStatisticStore;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ThirdStatisticDao implements ThirdStatisticStore {
    private final ThirdStatisticMapper mapper;

    public int upsert(ThirdStatistic statistic) { return mapper.upsert(statistic); }

    public List<ThirdStatistic> findRecent(String providerCode) { return mapper.selectRecent(providerCode); }
}
