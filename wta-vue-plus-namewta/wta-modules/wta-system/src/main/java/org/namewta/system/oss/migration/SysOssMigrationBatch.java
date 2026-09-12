package org.namewta.system.oss.migration;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oss_migration_batch")
public class SysOssMigrationBatch extends BaseEntity {
    @TableId("oss_migration_batch_id")
    private Long ossMigrationBatchId;
    private String targetConfigKey;
    private OssMigrationStatus status;
    private String dryRun = "N";
    private Integer totalCount = 0;
    private Integer successCount = 0;
    private Integer failedCount = 0;
    private Instant startedTime;
    private Instant completedTime;
    private String errorMessage;
    private Integer version = 0;
    @TableLogic
    private String delFlag = "0";
}
