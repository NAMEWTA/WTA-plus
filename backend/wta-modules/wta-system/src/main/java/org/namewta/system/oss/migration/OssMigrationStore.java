package org.namewta.system.oss.migration;

import org.namewta.system.domain.SysOss;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface OssMigrationStore {
    /** 远端预检之前读取配置的物理身份；不含凭据。 */
    ConfigIdentity configIdentity(String key);
    /** 建立新工单前按配置主键序锁定并验证预检时的同一物理身份。 */
    void lockMigrationConfigs(ConfigIdentity source, ConfigIdentity target);
    record ConfigIdentity(String key, String bucket, String endpoint, String isHttps,
                          String region, String accessPolicy) { }
    List<SysOss> findObjects(Collection<Long> ids);
    SysOss findObject(Long ossId);
    SysOss lockObject(Long ossId);
    SysOssMigrationItem findItem(Long itemId);
    SysOssMigrationItem lockItem(Long itemId);
    SysOssMigrationItem findLatest(Long ossId);
    SysOssMigrationBatch createBatch(String targetConfigKey, int totalCount);
    SysOssMigrationItem createItem(Long batchId, SysOss oss, String targetConfigKey);
    SysOssMigrationBatch getBatch(Long batchId);
    List<SysOssMigrationItem> listItems(Long batchId);
    boolean claim(Long itemId, int version);
    boolean updateItem(SysOssMigrationItem item, int expectedVersion, OssMigrationStatus expectedStatus);
    void saveBatch(SysOssMigrationBatch batch);
    boolean compareAndSetService(Long ossId, String expectedService, String targetService);
    Set<String> activeConfigKeys();

    /**
     * 该对象最新一张仍可把目录指针拨回来源的公开工单。
     */
    SysOssMigrationItem findLatestRestorable(Long ossId);
}
