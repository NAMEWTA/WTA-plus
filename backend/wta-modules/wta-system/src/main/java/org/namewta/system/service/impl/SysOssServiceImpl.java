package org.namewta.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.*;
import org.namewta.common.core.utils.file.FileUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.factory.OssFactory;
import org.namewta.common.oss.model.Options;
import org.namewta.common.oss.model.PutObjectResult;
import org.namewta.system.api.OssService;
import org.namewta.system.api.domain.OssDTO;
import org.namewta.system.domain.SysOss;
import org.namewta.system.domain.SysOssConfig;
import org.namewta.system.domain.SysOssExt;
import org.namewta.system.domain.bo.SysOssBo;
import org.namewta.system.domain.vo.SysOssVo;
import org.namewta.system.mapper.SysOssConfigMapper;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.migration.OssMigrationStatus;
import org.namewta.system.oss.migration.SysOssMigrationItem;
import org.namewta.system.oss.migration.mapper.SysOssMigrationItemMapper;
import org.namewta.system.oss.service.OssLifecycleManager;
import org.namewta.system.service.ISysOssService;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 文件上传 服务层实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysOssServiceImpl implements ISysOssService, OssService {

    private static final JsonMapper METADATA_JSON = JsonMapper.builder().build();

    private final SysOssMapper ossMapper;

    private final SysOssConfigMapper ossConfigMapper;

    private final SysOssMigrationItemMapper migrationItemMapper;

    private final OssLifecycleManager lifecycleManager;

    /**
     * 查询OSS对象存储列表
     *
     * @param bo        OSS对象存储分页查询对象
     * @param pageQuery 分页查询实体类
     * @return 结果
     */
    @Override
    public PageResult<SysOssVo> queryPageList(SysOssBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysOss> lqw = buildQueryWrapper(bo);
        Page<SysOssVo> result = ossMapper.selectVoPage(pageQuery.build(), lqw);
        List<SysOssVo> filterResult = StreamUtils.toList(result.getRecords(), this::managementView);
        attachStorageFacts(filterResult);
        result.setRecords(filterResult);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 根据一组 ossIds 获取对应的 SysOssVo 列表
     *
     * @param ossIds 一组文件在数据库中的唯一标识集合
     * @return 包含 SysOssVo 对象的列表
     */
    @Override
    public List<SysOssVo> listByIds(Collection<Long> ossIds) {
        SysOssServiceImpl ossService = SpringUtils.getAopProxy(this);
        List<Supplier<SysOssVo>> suppliers = ossIds.stream().map(id -> (Supplier<SysOssVo>) () -> {
            SysOssVo vo = ossService.getById(id);
            if (ObjectUtil.isNotNull(vo)) {
                return this.managementView(vo);
            }
            return null;
        }).toList();
        List<SysOssVo> list = ThreadUtils.virtualSubmitAll(suppliers);
        list.removeAll(Collections.singleton(null));
        attachStorageFacts(list);
        return list;
    }

    /**
     * 访问类型和能否恢复都由当前配置与未清理工单算出，不写入 sys_oss。
     */
    private void attachStorageFacts(List<SysOssVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<String> services = new HashSet<>();
        Set<Long> ossIds = new HashSet<>();
        for (SysOssVo row : rows) {
            if (StringUtils.isNotBlank(row.getService())) {
                services.add(row.getService());
            }
            if (row.getOssId() != null) {
                ossIds.add(row.getOssId());
            }
        }
        Map<String, String> accessByService = new HashMap<>();
        if (!services.isEmpty()) {
            List<SysOssConfig> configs = ossConfigMapper.selectList(new LambdaQueryWrapper<SysOssConfig>()
                .in(SysOssConfig::getConfigKey, services));
            for (SysOssConfig config : configs) {
                accessByService.put(config.getConfigKey(), accessPolicyName(config.getAccessPolicy()));
            }
        }
        Set<Long> restorable = new HashSet<>();
        if (!ossIds.isEmpty()) {
            List<SysOssMigrationItem> items = migrationItemMapper.selectList(new LambdaQueryWrapper<SysOssMigrationItem>()
                .in(SysOssMigrationItem::getOssId, ossIds)
                .eq(SysOssMigrationItem::getStatus, OssMigrationStatus.CLEANUP_ELIGIBLE));
            for (SysOssMigrationItem item : items) {
                restorable.add(item.getOssId());
            }
        }
        for (SysOssVo row : rows) {
            String accessPolicy = accessByService.getOrDefault(row.getService(), "UNKNOWN");
            row.setAccessPolicy(accessPolicy);
            row.setRestorable("PUBLIC_READ".equals(accessPolicy) && restorable.contains(row.getOssId()));
        }
    }

    private String accessPolicyName(String accessPolicy) {
        if ("0".equals(accessPolicy)) {
            return "PRIVATE";
        }
        if ("2".equals(accessPolicy)) {
            return "PUBLIC_READ";
        }
        return "UNKNOWN";
    }

    /**
     * 根据一组 ossIds 获取对应文件的 URL 列表。
     *
     * <p>仅供旧调用方和 OSS 翻译器兼容，新接口不应优先使用；
     * 应在业务授权后调用 {@link #resolveAccessUrl(Long)} 保留访问类型和到期时间。</p>
     *
     * @param ossIds 以逗号分隔的 ossId 字符串
     * @return 以逗号分隔的文件 URL 字符串
     * @deprecated 使用 {@link #resolveAccessUrl(Long)}
     */
    @Deprecated(since = "6.0.0", forRemoval = false)
    @Override
    public String selectUrlByIds(String ossIds) {
        List<Long> ids = StringUtils.splitTo(ossIds, Convert::toLong);
        List<Supplier<String>> suppliers = ids.stream()
            .map(id -> (Supplier<String>) () -> lifecycleManager.resolveAccessUrl(id).url())
            .toList();
        List<String> list = ThreadUtils.virtualSubmitAll(suppliers);
        list.removeAll(Collections.singleton(null));
        return StringUtils.joinComma(list);
    }

    /**
     * 根据逗号分隔的文件主键列表查询文件传输对象集合。
     *
     * <p>仅供旧调用方和 OSS 翻译器兼容，新接口不应优先使用；DTO 无法表达私有 URL 的到期时间。</p>
     *
     * @param ossIds 逗号分隔的文件主键字符串
     * @return 文件传输对象列表
     * @deprecated 使用 {@link #resolveAccessUrl(Long)}
     */
    @Deprecated(since = "6.0.0", forRemoval = false)
    @Override
    public List<OssDTO> selectByIds(String ossIds) {
        List<Long> ids = StringUtils.splitTo(ossIds, Convert::toLong);
        var ossService = SpringUtils.getAopProxy(this);
        List<Supplier<OssDTO>> suppliers = ids.stream().map(id -> (Supplier<OssDTO>) () -> {
            SysOssVo vo = ossService.getById(id);
            if (ObjectUtil.isNotNull(vo)) {
                OssDTO dto = BeanUtil.toBean(vo, OssDTO.class);
                dto.setUrl(lifecycleManager.resolveAccessUrl(id).url());
                return dto;
            }
            return null;
        }).toList();
        List<OssDTO> list = ThreadUtils.virtualSubmitAll(suppliers);
        list.removeAll(Collections.singleton(null));
        return list;
    }

    /**
     * 构造 OSS 文件列表查询条件。
     *
     * @param bo 文件筛选条件
     * @return 包含文件名、后缀、归属服务和创建时间区间的查询包装器
     */
    private LambdaQueryWrapper<SysOss> buildQueryWrapper(SysOssBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SysOss.class)
            .likeIfText(SysOss::getFileName, bo.getFileName())
            .likeIfText(SysOss::getOriginalName, bo.getOriginalName())
            .eqIfText(SysOss::getFileSuffix, bo.getFileSuffix())
            .eqIfText(SysOss::getUrl, bo.getUrl())
            .betweenParams(SysOss::getCreateTime, params, "beginCreateTime", "endCreateTime")
            .eqIfPresent(SysOss::getCreateBy, bo.getCreateBy())
            .eqIfText(SysOss::getService, bo.getService())
            .eqIfText(SysOss::getIsTemp, bo.getIsTemp())
            .orderByAsc(SysOss::getOssId)
            .build();
    }

    /**
     * 根据 ossId 从缓存或数据库中获取 SysOssVo 对象
     *
     * @param ossId 文件在数据库中的唯一标识
     * @return SysOssVo 对象，包含文件信息
     */
    @Cacheable(cacheNames = CacheNames.SYS_OSS, key = "#ossId")
    @Override
    public SysOssVo getById(Long ossId) {
        return ossMapper.selectVoById(ossId);
    }


    /**
     * 上传文件到对象存储服务，并保存文件信息到数据库
     *
     * @param file 要上传的文件对象
     * @return 上传成功后的 SysOssVo 对象，包含文件信息
     */
    @Override
    public SysOssVo upload(File file, SysOssExt ossExt) {
        if (ObjectUtil.isNull(file) || !file.isFile() || file.length() <= 0) {
            throw new ServiceException("上传文件不能为空");
        }
        String originalfileName = file.getName();
        String suffix = StringUtils.substring(originalfileName, originalfileName.lastIndexOf("."), originalfileName.length());
        OssClient instance = OssFactory.instance();
        String pathKey = instance.buildPathKey(originalfileName);
        PutObjectResult result = instance.upload(pathKey, file, Options.builder().setContentType(FileUtils.getMimeType(file.toPath())));
        SysOssExt ext1 = ossExt == null ? new SysOssExt() : ossExt;
        ext1.setFileSize(result.size());
        // 保存文件信息
        return buildResultEntity(originalfileName, suffix, instance.clientId(), result, ext1);
    }

    /**
     * 组装上传结果并持久化文件元数据。
     *
     * @param originalfileName 原始文件名
     * @param suffix           文件后缀
     * @param configKey        存储配置标识
     * @param result           上传结果
     * @param ext1             扩展属性对象
     * @return 持久化后的文件信息视图
     */
    @NotNull
    private SysOssVo buildResultEntity(String originalfileName, String suffix, String configKey, PutObjectResult result, SysOssExt ext1) {
        SysOss oss = new SysOss();
        oss.setUrl(result.url());
        oss.setFileSuffix(suffix);
        oss.setFileName(result.key());
        oss.setOriginalName(originalfileName);
        oss.setService(configKey);
        oss.setExt1(JsonUtils.toJsonString(ext1));
        ossMapper.insert(oss);
        SysOssVo sysOssVo = MapstructUtils.convert(oss, SysOssVo.class);
        sysOssVo.setUrl(lifecycleManager.resolveAccessUrl(oss.getOssId()).url());
        return sysOssVo;
    }

    /**
     * 删除OSS对象存储
     *
     * @param ids     OSS对象ID串
     * @param isValid 判断是否需要校验
     * @return 结果
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 做一些业务上的校验,判断是否需要校验
        }
        return lifecycleManager.deleteObjects(ids);
    }

    /**
     * 恢复待删除对象。不触碰对象存储，只重算数据库中的生命周期。
     *
     * @param ids OSS对象ID串
     * @return 是否恢复成功
     */
    @Override
    public Boolean restoreWithValidByIds(Collection<Long> ids) {
        return lifecycleManager.restoreObjects(ids);
    }

    @Override
    public void reconcileReferences(String refType, String refId,
                                    Collection<Long> previousOssIds, Collection<Long> currentOssIds) {
        lifecycleManager.reconcileReferences(refType, refId, previousOssIds, currentOssIds);
    }

    @Override
    public OssLifecycleSnapshot snapshot(Long ossId) {
        return lifecycleManager.snapshot(ossId);
    }

    @Override
    public OssObjectMetadata objectMetadata(Long ossId) {
        if (ossId == null || ossId <= 0) {
            throw new ServiceException("OSS_OBJECT_NOT_FOUND");
        }
        SysOss oss = ossMapper.selectById(ossId);
        if (oss == null) {
            throw new ServiceException("OSS_OBJECT_NOT_FOUND");
        }
        SysOssExt ext;
        try {
            ext = StringUtils.isBlank(oss.getExt1())
                ? null : METADATA_JSON.readValue(oss.getExt1(), SysOssExt.class);
        } catch (RuntimeException ex) {
            throw new ServiceException("OSS_OBJECT_METADATA_UNAVAILABLE");
        }
        if (ext == null || ext.getFileSize() == null || ext.getFileSize() <= 0
            || StringUtils.isBlank(ext.getContentType()) || StringUtils.isBlank(oss.getFileName())
            || StringUtils.isBlank(oss.getOriginalName()) || StringUtils.isBlank(oss.getFileSuffix())
            || oss.getCreateBy() == null || oss.getCreateBy() <= 0
            || "PENDING".equals(oss.getDeleteState())) {
            throw new ServiceException("OSS_OBJECT_METADATA_UNAVAILABLE");
        }
        return new OssObjectMetadata(ossId, oss.getFileName(), oss.getOriginalName(), oss.getFileSuffix(),
            ext.getFileSize(), ext.getContentType(), oss.getCreateBy(), ext.getUploaderClientPk());
    }

    @Override
    public OssDownloadUrl presignDownload(Long ossId) {
        return lifecycleManager.presignDownload(ossId);
    }

    @Override
    public OssDownloadUrl presignDownload(Long ossId, String policyName) {
        return lifecycleManager.presignDownload(ossId, policyName);
    }

    @Override
    public OssAccessUrl resolveAccessUrl(Long ossId) {
        return lifecycleManager.resolveAccessUrl(ossId);
    }

    /**
     * 管理查询不返回可直接使用的 URL；下载必须经过专用权限入口。
     */
    private SysOssVo managementView(SysOssVo oss) {
        SysOssVo view = BeanUtil.toBean(oss, SysOssVo.class);
        view.setUrl(null);
        OssLifecycleSnapshot snapshot = lifecycleManager.snapshot(view.getOssId());
        view.setReferenceCount((long) snapshot.references().size());
        view.setReferences(snapshot.references());
        return view;
    }

}
