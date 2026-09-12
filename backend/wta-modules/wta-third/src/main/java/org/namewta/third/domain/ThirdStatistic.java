package org.namewta.third.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("third_statistic")
public class ThirdStatistic {
    @TableId private Long statisticId;
    private String providerCode;
    private String endpointCode;
    private LocalDate statDate;
    private Long attemptCount;
    private Long successCount;
    private Long failureCount;
    private Long timeoutCount;
    private Long rejectedCount;
    private Long quotaValue;
}
