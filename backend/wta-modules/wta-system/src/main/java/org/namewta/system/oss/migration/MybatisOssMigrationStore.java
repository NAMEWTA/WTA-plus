package org.namewta.system.oss.migration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.namewta.system.domain.SysOss;
import org.namewta.system.domain.SysOssConfig;
import org.namewta.system.mapper.SysOssConfigMapper;
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
    private final SysOssConfigMapper configMapper;

    @Override
    public ConfigIdentity configIdentity(String key) {
        SysOssConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysOssConfig>()
            .eq(SysOssConfig::getConfigKey, key));
        return identity(config);
    }

    @Override
    public void lockMigrationConfigs(ConfigIdentity source, ConfigIdentity target) {
        List<SysOssConfig> locked = configMapper.lockAllConfigs();
        boolean sourceFound = false;
        boolean targetFound = false;
        for (SysOssConfig config : locked) {
            if (source != null && source.equals(identity(config)) && "0".equals(config.getAccessPolicy())) {
                sourceFound = true;
            }
            if (target != null && target.equals(identity(config)) && "2".equals(config.getAccessPolicy())) {
                targetFound = true;
            }
        }
        if (!sourceFound || !targetFound) {
            throw new OssMigrationException(OssMigrationError.STORAGE_NOT_SERVING,
                "迁移配置已变化");
        }
    }

    private static ConfigIdentity identity(SysOssConfig config) {
        return config == null ? null : new ConfigIdentity(config.getConfigKey(), config.getBucketName(),
            config.getEndpoint(), config.getIsHttps(), config.getRegion(), config.getAccessPolicy());
    }

    @Override
    public List<SysOss> findObjects(Collection<Long> ids) {
        return ossMapper.selectBatchIds(ids);
    }

    @Override
    public SysOss findObject(Long ossId) {
        return ossMapper.selectById(ossId);
    }

    @Override
    public SysOss lockObject(Long ossId) {
        return ossMapper.selectByIdForUpdate(ossId);
    }

    @Override
    public SysOssMigrationItem findItem(Long itemId) {
        return itemMapper.selectById(itemId);
    }

    @Override
    public SysOssMigrationItem lockItem(Long itemId) {
        return itemMapper.lockItem(itemId);
    }

    @Override
    public SysOssMigrationItem findLatest(Long ossId) {
        return itemMapper.lockLatest(ossId);
    }

    @Override
    public SysOssMigrationBatch createBatch(String targetConfigKey, int totalCount) {
        SysOssMigrationBatch batch = new SysOssMigrationBatch();
        batch.setOssMigrationBatchId(IdWorker.getId());
        batch.setTargetConfigKey(targetConfigKey);
        batch.setStatus(OssMigrationStatus.RUNNING);
        batch.setTotalCount(totalCount);
        batch.setStartedTime(Instant.now());
        if (batchMapper.insert(batch) != 1) {
            throw new IllegalStateException("OSS migration batch insert failed");
        }
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
        if (itemMapper.insert(item) != 1) {
            throw new IllegalStateException("OSS migration item insert failed");
        }
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
    public boolean updateItem(SysOssMigrationItem item, int expectedVersion, OssMigrationStatus expectedStatus) {
        return itemMapper.updateState(item, expectedVersion, expectedStatus) == 1;
    }

    @Override
    public void saveBatch(SysOssMigrationBatch batch) {
        if (batchMapper.updateById(batch) != 1) {
            throw new IllegalStateException("OSS migration batch update failed");
        }
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
        SysOssMigrationItem latest = ossId == null ? null : findLatest(ossId);
        return latest != null && latest.getStatus() == OssMigrationStatus.CLEANUP_ELIGIBLE ? latest : null;
    }
}
