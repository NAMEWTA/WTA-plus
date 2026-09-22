package org.namewta.system.oss.migration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.namewta.system.domain.SysOss;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.migration.mapper.SysOssMigrationBatchMapper;
import org.namewta.system.oss.migration.mapper.SysOssMigrationItemMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class MybatisOssMigrationStore implements OssMigrationStore {

    private final SysOssMapper ossMapper;
    private final SysOssMigrationBatchMapper batchMapper;
    private final SysOssMigrationItemMapper itemMapper;

    @Override
    public List<SysOss> findObjects(Collection<Long> ids) {
        return ossMapper.selectBatchIds(ids);
    }

    @Override
    public SysOss findObject(Long ossId) {
        return ossMapper.selectById(ossId);
    }

    @Override
    public SysOssMigrationBatch createBatch(String targetConfigKey, int totalCount) {
        SysOssMigrationBatch batch = new SysOssMigrationBatch();
        batch.setOssMigrationBatchId(IdWorker.getId());
        batch.setTargetConfigKey(targetConfigKey);
        batch.setStatus(OssMigrationStatus.RUNNING);
        batch.setTotalCount(totalCount);
        batch.setStartedTime(Instant.now());
        batchMapper.insert(batch);
        return batch;
    }

    @Override
    public SysOssMigrationItem createItem(Long batchId, SysOss oss, String targetConfigKey) {
        SysOssMigrationItem item = new SysOssMigrationItem();
        item.setOssMigrationItemId(IdWorker.getId());
        item.setOssMigrationBatchId(batchId);
        item.setOssId(oss.getOssId());
        item.setSourceConfigKey(oss.getService());
        item.setTargetConfigKey(targetConfigKey);
        item.setObjectKey(oss.getFileName());
        item.setStatus(OssMigrationStatus.PENDING);
        item.setStage(OssMigrationStage.PREFLIGHT);
        itemMapper.insert(item);
        return item;
    }

    @Override
    public SysOssMigrationBatch getBatch(Long batchId) {
        return batchMapper.selectById(batchId);
    }

    @Override
    public List<SysOssMigrationItem> listItems(Long batchId) {
        return itemMapper.selectList(new LambdaQueryWrapper<SysOssMigrationItem>()
            .eq(SysOssMigrationItem::getOssMigrationBatchId, batchId)
            .orderByAsc(SysOssMigrationItem::getOssMigrationItemId));
    }

    @Override
    public boolean claim(Long itemId, int version) {
        return itemMapper.claim(itemId, version) == 1;
    }

    @Override
    public void saveItem(SysOssMigrationItem item) {
        itemMapper.updateById(item);
    }

    @Override
    public void saveBatch(SysOssMigrationBatch batch) {
        batchMapper.updateById(batch);
    }

    @Override
    public boolean compareAndSetService(Long ossId, String expectedService, String targetService) {
        return ossMapper.compareAndSetService(ossId, expectedService, targetService) == 1;
    }

    @Override
    public Set<String> activeConfigKeys() {
        return new LinkedHashSet<>(itemMapper.selectActiveConfigKeys());
    }

    @Override
    public SysOssMigrationItem findLatestRestorable(Long ossId) {
        if (ossId == null) {
            return null;
        }
        return itemMapper.selectOne(new LambdaQueryWrapper<SysOssMigrationItem>()
            .eq(SysOssMigrationItem::getOssId, ossId)
            .eq(SysOssMigrationItem::getStatus, OssMigrationStatus.CLEANUP_ELIGIBLE)
            .orderByDesc(SysOssMigrationItem::getOssMigrationItemId)
            .last("limit 1"));
    }
}
