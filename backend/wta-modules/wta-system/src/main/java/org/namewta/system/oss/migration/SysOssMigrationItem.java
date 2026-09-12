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
@TableName("sys_oss_migration_item")
public class SysOssMigrationItem extends BaseEntity {
    @TableId("oss_migration_item_id")
    private Long ossMigrationItemId;
    private Long ossMigrationBatchId;
    private Long ossId;
    private String sourceConfigKey;
    private String targetConfigKey;
    private String objectKey;
    private OssMigrationStatus status;
    private OssMigrationStage stage;
    private Long sourceSize;
    private Long targetSize;
    private String sourceEtag;
    private String targetEtag;
    private Integer retryCount = 0;
    private OssMigrationStage lastErrorStage;
    private String errorMessage;
    private Instant serviceSwitchedTime;
    private Instant cleanupEligibleTime;
    private Instant cleanedTime;
    private Integer version = 0;
    @TableLogic
    private String delFlag = "0";
}
