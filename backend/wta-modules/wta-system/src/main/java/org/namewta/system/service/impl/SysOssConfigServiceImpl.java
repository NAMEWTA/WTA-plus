package org.namewta.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.common.oss.constant.OssConstant;
import org.namewta.common.redis.utils.CacheUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.system.domain.SysOssConfig;
import org.namewta.system.domain.bo.SysOssConfigBo;
import org.namewta.system.domain.vo.SysOssConfigVo;
import org.namewta.system.event.OssConfigChangeEvent;
import org.namewta.system.mapper.SysOssConfigMapper;
import org.namewta.system.service.ISysOssConfigService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 对象存储配置Service业务层处理
 *
 * @date 2021-08-13
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysOssConfigServiceImpl implements ISysOssConfigService {

    private static final String PRIVATE_ACCESS_POLICY = "0";
    private static final String PUBLIC_READ_ACCESS_POLICY = "2";
    private static final Pattern CONFIG_KEY = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9._-]{1,19}");

    private final SysOssConfigMapper ossConfigMapper;

    /**
     * 项目启动时，初始化参数到缓存，加载配置类
     */
    @Override
    public void init() {
        List<SysOssConfig> list = ossConfigMapper.selectList();
        // 查询成功后以 DB 为权威重建专用缓存；旧默认和已删除的 configKey 均不能复活。
        CacheUtils.clear(CacheNames.SYS_OSS_CONFIG);
        RedisUtils.deleteObject(OssConstant.DEFAULT_CONFIG_KEY);
        List<SysOssConfig> valid = new java.util.ArrayList<>();
        for (SysOssConfig config : list) {
            if (locallyUsable(config)) {
                valid.add(config);
                CacheUtils.put(CacheNames.SYS_OSS_CONFIG, config.getConfigKey(), JsonUtils.toJsonString(config));
            } else {
                log.warn("忽略本地结构无效的可选OSS配置，配置ID={}", config == null ? null : config.getOssConfigId());
            }
        }
        List<SysOssConfig> defaults = valid.stream()
            .filter(config -> SystemConstants.YES.equals(config.getStatus())
                && PRIVATE_ACCESS_POLICY.equals(config.getAccessPolicy())).toList();
        if (defaults.size() == 1 && list.stream()
            .filter(config -> config != null && SystemConstants.YES.equals(config.getStatus())).count() == 1) {
            RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, defaults.getFirst().getConfigKey());
        }
    }

    /** 启动阶段仅校验本地结构，不构造 OSS 客户端或连接远端。 */
    private boolean locallyUsable(SysOssConfig config) {
        return config != null && config.getConfigKey() != null
            && CONFIG_KEY.matcher(config.getConfigKey()).matches()
            && StringUtils.isNotBlank(config.getEndpoint())
            && StringUtils.isNotBlank(config.getBucketName())
            && StringUtils.isNotBlank(config.getAccessKey())
            && StringUtils.isNotBlank(config.getSecretKey())
            && (PRIVATE_ACCESS_POLICY.equals(config.getAccessPolicy())
                || PUBLIC_READ_ACCESS_POLICY.equals(config.getAccessPolicy()))
            && (SystemConstants.YES.equals(config.getStatus()) || SystemConstants.NO.equals(config.getStatus()));
    }

    /**
     * 查询对象存储配置详情。
     *
     * @param ossConfigId 配置主键
     * @return 对象存储配置详情
     */
    @Override
    public SysOssConfigVo queryById(Long ossConfigId) {
        return ossConfigMapper.selectVoById(ossConfigId);
    }

    /**
     * 分页查询对象存储配置列表。
     *
     * @param bo        配置筛选条件
     * @param pageQuery 分页参数
     * @return 配置分页结果
     */
    @Override
    public PageResult<SysOssConfigVo> queryPageList(SysOssConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysOssConfig> lqw = buildQueryWrapper(bo);
        Page<SysOssConfigVo> result = ossConfigMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }


    /**
     * 构造对象存储配置列表查询条件。
     *
     * @param bo 配置筛选条件
     * @return 包含配置标识、桶名称和状态条件的查询包装器
     */
    private LambdaQueryWrapper<SysOssConfig> buildQueryWrapper(SysOssConfigBo bo) {
        return QueryBuilder.lambda(SysOssConfig.class)
            .eqIfText(SysOssConfig::getConfigKey, bo.getConfigKey())
            .likeIfText(SysOssConfig::getBucketName, bo.getBucketName())
            .eqIfText(SysOssConfig::getStatus, bo.getStatus())
            .orderByAsc(SysOssConfig::getOssConfigId)
            .build();
    }

    /**
     * 新增对象存储配置并刷新缓存。
     *
     * @param bo 配置业务对象
     * @return 新增成功返回 {@code true}
     */
    @Override
    @DSTransactional
    public Boolean insertByBo(SysOssConfigBo bo) {
        ossConfigMapper.lockAllConfigs();
        SysOssConfig config = BeanUtil.toBean(bo, SysOssConfig.class);
        normalizeNewConfig(config);
        validEntityBeforeSave(config);
        validateExistingDefaultIfNotSwitching(config);
        boolean flag = ossConfigMapper.insert(config) > 0;
        if (flag) {
            if (SystemConstants.YES.equals(config.getStatus())) {
                ossConfigMapper.clearOtherDefaultStatuses(config.getOssConfigId());
            }
            // 从数据库查询完整的数据做缓存
            config = ossConfigMapper.selectById(config.getOssConfigId());
            publishOssConfigSaved(config, null);
            publishDefaultConfigIfNecessary(config);
        }
        return flag;
    }

    /**
     * 更新对象存储配置并刷新缓存。
     *
     * @param bo 配置业务对象
     * @return 更新成功返回 {@code true}
     */
    @Override
    @DSTransactional
    public Boolean updateByBo(SysOssConfigBo bo) {
        ossConfigMapper.lockAllConfigs();
        SysOssConfig config = BeanUtil.toBean(bo, SysOssConfig.class);
        if (ObjectUtil.isNull(config.getOssConfigId())) {
            throw new ServiceException("OSS配置主键不能为空");
        }
        SysOssConfig oldConfig = ossConfigMapper.selectByIdForUpdate(config.getOssConfigId());
        if (ObjectUtil.isNull(oldConfig)) {
            throw new ServiceException("OSS配置不存在");
        }
        normalizeUpdatedConfig(config, oldConfig);
        validEntityBeforeSave(config);
        validateExistingDefaultIfNotSwitching(config);
        validateReferencedBoundary(oldConfig, config);
        if (SystemConstants.YES.equals(oldConfig.getStatus()) && SystemConstants.NO.equals(config.getStatus())) {
            throw new ServiceException("默认OSS配置不能通过普通编辑取消，请使用默认配置切换操作");
        }
        if (SystemConstants.YES.equals(config.getStatus())) {
            ossConfigMapper.clearOtherDefaultStatuses(config.getOssConfigId());
        }
        int updated = ossConfigMapper.updateById(config);
        if (updated != 1) {
            throw new ServiceException("OSS配置更新失败");
        }
        // 从数据库查询完整的数据做缓存
        config = ossConfigMapper.selectById(config.getOssConfigId());
        publishOssConfigSaved(config, oldConfig.getConfigKey());
        publishDefaultConfigIfNecessary(config);
        return true;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 待保存的对象存储配置实体
     */
    private void validEntityBeforeSave(SysOssConfig entity) {
        validateAccessPolicy(entity);
        validateStatus(entity);
        validatePrivateDefault(entity);
        if (StringUtils.isNotEmpty(entity.getConfigKey())
            && !checkConfigKeyUnique(entity)) {
            throw new ServiceException("操作配置'{}'失败, 配置key已存在!", entity.getConfigKey());
        }
    }

    /**
     * 删除对象存储配置并同步清理缓存。
     *
     * @param ids     主键集合
     * @param isValid 是否执行业务校验
     * @return 删除成功返回 {@code true}
     */
    @Override
    @DSTransactional
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("OSS配置主键不能为空");
        }
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new ServiceException("OSS配置主键不能为空");
        }
        if (isValid) {
            if (CollUtil.containsAny(ids, OssConstant.SYSTEM_DATA_IDS)) {
                throw new ServiceException("系统内置, 不可删除!");
            }
        }
        ossConfigMapper.lockAllConfigs();
        List<SysOssConfig> list = CollUtil.newArrayList();
        for (Long configId : ids.stream().sorted().toList()) {
            SysOssConfig config = ossConfigMapper.selectByIdForUpdate(configId);
            if (ObjectUtil.isNull(config)) {
                throw new ServiceException("OSS配置不存在");
            }
            if (SystemConstants.YES.equals(config.getStatus())) {
                throw new ServiceException("默认OSS配置不可删除，请先切换默认配置");
            }
            if (ossConfigMapper.countDefaultConfigs() != 1) {
                throw new ServiceException("OSS配置必须且只能存在一个默认配置");
            }
            if (ossConfigMapper.countOssReferences(config.getConfigKey()) > 0) {
                throw new ServiceException("OSS配置已被对象引用，不可删除");
            }
            list.add(config);
        }
        boolean flag = ossConfigMapper.deleteByIds(ids) > 0;
        if (flag) {
            list.forEach(sysOssConfig ->
                SpringUtils.context().publishEvent(OssConfigChangeEvent.remove(sysOssConfig.getConfigKey())));
        }
        return flag;
    }

    /**
     * 判断configKey是否唯一
     *
     * @param sysOssConfig 对象存储配置实体
     * @return 唯一返回 {@code true}
     */
    private boolean checkConfigKeyUnique(SysOssConfig sysOssConfig) {
        Long ossConfigId = ObjectUtil.defaultIfNull(sysOssConfig.getOssConfigId(), -1L);
        return ossConfigMapper.countConfigKeyConflicts(sysOssConfig.getConfigKey(), ossConfigId) == 0;
    }

    /**
     * 启用禁用状态
     *
     * @param bo 配置业务对象
     * @return 更新条数
     */
    @Override
    @DSTransactional
    public int updateOssConfigStatus(SysOssConfigBo bo) {
        if (ObjectUtil.isNull(bo.getOssConfigId())) {
            throw new ServiceException("OSS配置主键不能为空");
        }
        ossConfigMapper.lockAllConfigs();
        SysOssConfig config = ossConfigMapper.selectByIdForUpdate(bo.getOssConfigId());
        if (ObjectUtil.isNull(config)) {
            throw new ServiceException("OSS配置不存在");
        }
        validateAccessPolicy(config);
        if (!PRIVATE_ACCESS_POLICY.equals(config.getAccessPolicy())) {
            throw new ServiceException("默认OSS配置必须为PRIVATE");
        }
        int cleared = ossConfigMapper.clearOtherDefaultStatuses(config.getOssConfigId());
        config.setStatus(SystemConstants.YES);
        int activated = ossConfigMapper.updateById(config);
        if (activated != 1) {
            throw new ServiceException("默认OSS配置切换失败");
        }
        SpringUtils.context().publishEvent(OssConfigChangeEvent.useDefault(config.getConfigKey()));
        return cleared + activated;
    }

    private void normalizeNewConfig(SysOssConfig config) {
        if (StringUtils.isBlank(config.getStatus())) {
            config.setStatus(SystemConstants.NO);
        }
        normalizeOptionalFields(config);
    }

    private void normalizeUpdatedConfig(SysOssConfig config, SysOssConfig oldConfig) {
        if (StringUtils.isBlank(config.getStatus())) {
            config.setStatus(oldConfig.getStatus());
        }
        if (StringUtils.isBlank(config.getSecretKey())) {
            config.setSecretKey(oldConfig.getSecretKey());
        }
        normalizeOptionalFields(config);
    }

    private void normalizeOptionalFields(SysOssConfig config) {
        if (ObjectUtil.isNull(config.getPrefix())) {
            config.setPrefix("");
        }
        if (ObjectUtil.isNull(config.getRegion())) {
            config.setRegion("");
        }
        if (ObjectUtil.isNull(config.getExt1())) {
            config.setExt1("");
        }
        if (ObjectUtil.isNull(config.getRemark())) {
            config.setRemark("");
        }
    }

    private void validateAccessPolicy(SysOssConfig config) {
        if (!PRIVATE_ACCESS_POLICY.equals(config.getAccessPolicy())
            && !PUBLIC_READ_ACCESS_POLICY.equals(config.getAccessPolicy())) {
            throw new ServiceException("OSS桶权限只允许0=PRIVATE或2=PUBLIC_READ");
        }
    }

    private void validateStatus(SysOssConfig config) {
        if (!SystemConstants.YES.equals(config.getStatus()) && !SystemConstants.NO.equals(config.getStatus())) {
            throw new ServiceException("OSS配置status只表示默认配置，只允许Y或N");
        }
    }

    private void validatePrivateDefault(SysOssConfig config) {
        if (SystemConstants.YES.equals(config.getStatus())
            && !PRIVATE_ACCESS_POLICY.equals(config.getAccessPolicy())) {
            throw new ServiceException("默认OSS配置必须为PRIVATE");
        }
    }

    private void validateExistingDefaultIfNotSwitching(SysOssConfig config) {
        if (SystemConstants.NO.equals(config.getStatus()) && ossConfigMapper.countDefaultConfigs() != 1) {
            throw new ServiceException("OSS配置必须且只能存在一个默认配置");
        }
    }

    private void validateReferencedBoundary(SysOssConfig oldConfig, SysOssConfig config) {
        if (ossConfigMapper.countOssReferences(oldConfig.getConfigKey()) == 0) {
            return;
        }
        if (!StringUtils.equals(oldConfig.getConfigKey(), config.getConfigKey())
            || !StringUtils.equals(oldConfig.getBucketName(), config.getBucketName())
            || !StringUtils.equals(oldConfig.getAccessPolicy(), config.getAccessPolicy())
            || !StringUtils.equals(oldConfig.getEndpoint(), config.getEndpoint())
            || !StringUtils.equals(oldConfig.getIsHttps(), config.getIsHttps())
            // 客户端将空白 Region 解释为 us-east-1；普通字段归一化不能阻断凭据轮换。
            || !StringUtils.equals(StringUtils.defaultIfBlank(oldConfig.getRegion(), "us-east-1"),
                StringUtils.defaultIfBlank(config.getRegion(), "us-east-1"))) {
            throw new ServiceException("OSS配置已被对象或迁移工单引用，不能修改物理存储身份");
        }
    }

    private void publishDefaultConfigIfNecessary(SysOssConfig config) {
        if (SystemConstants.YES.equals(config.getStatus())) {
            SpringUtils.context().publishEvent(OssConfigChangeEvent.useDefault(config.getConfigKey()));
        }
    }

    /**
     * 发布 OSS 配置保存事件。
     *
     * @param config       当前配置
     * @param oldConfigKey 变更前配置 key
     */
    private void publishOssConfigSaved(SysOssConfig config, String oldConfigKey) {
        SpringUtils.context().publishEvent(OssConfigChangeEvent.save(
            config.getConfigKey(),
            oldConfigKey,
            JsonUtils.toJsonString(config)
        ));
    }

}
