package org.namewta.system.oss.migration;

import org.namewta.system.domain.SysOss;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface OssMigrationStore {
    List<SysOss> findObjects(Collection<Long> ids);
    SysOss findObject(Long ossId);
    SysOssMigrationBatch createBatch(String targetConfigKey, int totalCount);
    SysOssMigrationItem createItem(Long batchId, SysOss oss, String targetConfigKey);
    SysOssMigrationBatch getBatch(Long batchId);
    List<SysOssMigrationItem> listItems(Long batchId);
    boolean claim(Long itemId, int version);
    void saveItem(SysOssMigrationItem item);
    void saveBatch(SysOssMigrationBatch batch);
    boolean compareAndSetService(Long ossId, String expectedService, String targetService);
    Set<String> activeConfigKeys();

    /**
     * 该对象最新一张仍可把目录指针拨回来源的公开工单。
     */
    SysOssMigrationItem findLatestRestorable(Long ossId);
}
