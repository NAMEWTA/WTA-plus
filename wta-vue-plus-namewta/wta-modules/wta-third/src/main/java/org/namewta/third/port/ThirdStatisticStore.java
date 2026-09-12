package org.namewta.third.port;

import org.namewta.third.domain.ThirdStatistic;

/** Statistic persistence boundary consumed by the observability adapter. */
public interface ThirdStatisticStore {
    int upsert(ThirdStatistic statistic);
}
