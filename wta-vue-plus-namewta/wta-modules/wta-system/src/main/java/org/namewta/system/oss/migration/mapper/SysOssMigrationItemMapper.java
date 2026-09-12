package org.namewta.system.oss.migration.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.system.oss.migration.SysOssMigrationItem;

import java.util.List;

public interface SysOssMigrationItemMapper extends BaseMapperPlus<SysOssMigrationItem, SysOssMigrationItem> {

    @Update("""
        update sys_oss_migration_item
        set status = 'RUNNING', version = version + 1, update_time = now()
        where oss_migration_item_id = #{itemId}
          and version = #{version}
          and status != 'RUNNING'
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
