package org.namewta.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.namewta.third.domain.ThirdStatistic;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ThirdStatisticMapper extends BaseMapper<ThirdStatistic> {
    @SelectProvider(type = QueryProvider.class, method = "selectRecent")
    List<ThirdStatistic> selectRecent(@Param("providerCode") String providerCode);

    class QueryProvider { public static String selectRecent(@Param("providerCode") String providerCode) {
        return "select * from third_statistic" + (providerCode == null ? "" : " where provider_code = #{providerCode}") + " order by stat_date desc limit 200";
    }}

    @Insert("insert into third_statistic(statistic_id,provider_code,endpoint_code,stat_date,attempt_count,success_count,failure_count,timeout_count,rejected_count,quota_value) values(#{statisticId},#{providerCode},#{endpointCode},#{statDate},#{attemptCount},#{successCount},#{failureCount},#{timeoutCount},#{rejectedCount},#{quotaValue}) on duplicate key update attempt_count=attempt_count+values(attempt_count),success_count=success_count+values(success_count),failure_count=failure_count+values(failure_count),timeout_count=timeout_count+values(timeout_count),rejected_count=rejected_count+values(rejected_count),quota_value=values(quota_value)")
    int upsert(ThirdStatistic statistic);
}
