package org.namewta.system.oss.migration.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.system.oss.migration.SysOssMigrationItem;
import org.namewta.system.oss.migration.OssMigrationStatus;

import java.util.List;

public interface SysOssMigrationItemMapper extends BaseMapperPlus<SysOssMigrationItem, SysOssMigrationItem> {

    @Select("""
        select * from sys_oss_migration_item
        where oss_migration_item_id = #{itemId} and del_flag = '0'
        for update
        """)
    SysOssMigrationItem lockItem(@Param("itemId") Long itemId);

    @Select("""
        select * from sys_oss_migration_item
        where oss_id = #{ossId} and del_flag = '0'
        order by oss_migration_item_id desc limit 1 for update
        """)
    SysOssMigrationItem lockLatest(@Param("ossId") Long ossId);

    @Update("""
        update sys_oss_migration_item
        set status = #{item.status}, stage = #{item.stage},
            source_size = #{item.sourceSize}, target_size = #{item.targetSize},
            source_etag = #{item.sourceEtag}, target_etag = #{item.targetEtag},
            retry_count = #{item.retryCount}, last_error_stage = #{item.lastErrorStage},
            error_message = #{item.errorMessage}, service_switched_time = #{item.serviceSwitchedTime},
            cleanup_eligible_time = #{item.cleanupEligibleTime}, cleaned_time = #{item.cleanedTime},
            version = version + 1, update_time = now()
        where oss_migration_item_id = #{item.ossMigrationItemId}
          and version = #{expectedVersion} and status = #{expectedStatus} and del_flag = '0'
        """)
    int updateState(@Param("item") SysOssMigrationItem item, @Param("expectedVersion") int expectedVersion,
                    @Param("expectedStatus") OssMigrationStatus expectedStatus);

    @Update("""
        update sys_oss_migration_item
        set status = 'RUNNING', version = version + 1, update_time = now()
        where oss_migration_item_id = #{itemId}
          and version = #{version}
          and status in ('PENDING', 'FAILED')
          and (status = 'PENDING' or last_error_stage is null or last_error_stage != 'COMPLETED')
          and del_flag = '0'
        """)
    int claim(@Param("itemId") Long itemId, @Param("version") int version);

    @Select("""
        select distinct config_key
        from (
            select source_config_key as config_key
            from sys_oss_migration_item
            where status not in ('COMPLETED', 'ROLLED_BACK') and del_flag = '0'
            union
            select target_config_key as config_key
            from sys_oss_migration_item
            where status not in ('COMPLETED', 'ROLLED_BACK') and del_flag = '0'
        ) active_configs
        """)
    List<String> selectActiveConfigKeys();
}
