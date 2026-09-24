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
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
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
import org.namewta.system.mapper.SysClientMapper;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.service.ClientUserTypeAccessService;
import org.namewta.common.core.constant.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 文件上传 服务层实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysOssServiceImpl implements ISysOssService, OssService {

    private static final JsonMapper METADATA_JSON = JsonMapper.builder().build();
    private static final Duration NOTIFICATION_COPY_DEADLINE = Duration.ofSeconds(30);
    private static final String NOTIFICATION_REF = "notify_intent_attachment";
    private static final String NOT_READY = "NOT_READY";

    @org.springframework.beans.factory.annotation.Value("${notify.attachment.max-single-bytes:10485760}")
    private long notificationAttachmentMaxBytes = NOTIFICATION_ATTACHMENT_MAX_BYTES;

    private final SysOssMapper ossMapper;

    private final SysOssConfigMapper ossConfigMapper;

    private final SysOssMigrationItemMapper migrationItemMapper;

    private final OssLifecycleManager lifecycleManager;
    @Autowired(required = false)
    private SysUserMapper notificationUserMapper;
    @Autowired(required = false)
    private SysClientMapper notificationClientMapper;
    @Autowired(required = false)
    private ClientUserTypeAccessService notificationClientAccess;

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
            || !"ACTIVE".equals(oss.getDeleteState())) {
            throw new ServiceException("OSS_OBJECT_METADATA_UNAVAILABLE");
        }
        return new OssObjectMetadata(ossId, oss.getFileName(), oss.getOriginalName(), oss.getFileSuffix(),
            ext.getFileSize(), ext.getContentType(), oss.getCreateBy(), ext.getUploaderClientPk());
    }

    /**
     * 源授权必须在提交事务中完成。配置→源对象的锁序与配置身份写操作一致；
     * 此处不调用可恢复 PENDING 的通用授权前检查，只有 ACTIVE 可取得新引用。
     */
    @Override
    @com.baomidou.dynamic.datasource.annotation.DSTransactional
    public NotificationAttachmentSource bindNotificationSource(Long relationId, Long sourceOssId,
                                                               Long actorUserId, Long actorClientPk) {
        if (relationId == null || relationId <= 0 || actorUserId == null || actorUserId <= 0
            || actorClientPk == null || actorClientPk <= 0) throw new ServiceException("附件提交身份无效");
        requireCurrentNotificationActor(actorUserId, actorClientPk);
        List<SysOssConfig> configs = ossConfigMapper.lockAllConfigs();
        SysOss source = requireNotificationSource(sourceOssId, actorUserId, actorClientPk, configs);
        lifecycleManager.bind(sourceOssId, NOTIFICATION_REF, String.valueOf(relationId));
        SysOssExt ext = notificationExt(source);
        return new NotificationAttachmentSource(source.getService(), source.getFileName(),
            source.getOriginalName(), ext.getContentType(), ext.getFileSize());
    }

    /**
     * 保留稳定对象键和不可下载的 sys_oss 行后才能进行远端复制。目标行和真实引用在同一事务，
     * T46 配置身份编辑将看到该行；不得将 NOT_READY 目标当成普通可下载对象。
     */
    @Override
    @com.baomidou.dynamic.datasource.annotation.DSTransactional
    public NotificationCopyReservation reserveNotificationSnapshot(Long relationId, Long sourceOssId,
                                                                   Long actorUserId, Long actorClientPk) {
        requireCurrentNotificationActor(actorUserId, actorClientPk);
        List<SysOssConfig> configs = ossConfigMapper.lockAllConfigs();
        SysOss source = requireNotificationSource(sourceOssId, actorUserId, actorClientPk, configs);
        if (lifecycleManager.snapshot(sourceOssId).references().stream().noneMatch(ref ->
            NOTIFICATION_REF.equals(ref.refType()) && String.valueOf(relationId).equals(ref.refId()))) {
            throw new ServiceException("通知附件来源引用不存在");
        }
        SysOssConfig sourceConfig = configs.stream().filter(item -> Objects.equals(item.getConfigKey(), source.getService()))
            .findFirst().orElseThrow(() -> new ServiceException("附件来源存储配置不存在"));
        SysOssConfig targetConfig;
        if ("0".equals(sourceConfig.getAccessPolicy())) {
            targetConfig = sourceConfig;
        } else {
            List<SysOssConfig> defaults = configs.stream()
                .filter(item -> "Y".equals(item.getStatus()) && "0".equals(item.getAccessPolicy())).toList();
            if (defaults.size() != 1) throw new ServiceException("通知附件缺少唯一私有目标存储");
            targetConfig = defaults.getFirst();
        }
        SysOssExt ext = notificationExt(source);
        if (notificationAttachmentMaxBytes <= 0 || ext.getFileSize() > notificationAttachmentMaxBytes)
            throw new ServiceException("通知附件超过复制大小上限");
        Long targetId = IdGeneratorUtil.nextLongId();
        String prefix = StringUtils.isBlank(targetConfig.getPrefix()) ? ""
            : targetConfig.getPrefix().replaceAll("/+$", "") + "/";
        String targetKey = prefix + "notify-attachment/" + relationId + "/" + UUID.randomUUID();
        if (targetKey.length() > 255) throw new ServiceException("附件私有存储路径过长");
        SysOss target = new SysOss();
        target.setOssId(targetId);
        target.setFileName(targetKey);
        target.setOriginalName(source.getOriginalName());
        target.setFileSuffix(source.getFileSuffix());
        target.setUrl("");
        target.setExt1(source.getExt1());
        target.setService(targetConfig.getConfigKey());
        target.setIsTemp("N");
        target.setDeleteState(NOT_READY);
        target.setCreateBy(actorUserId);
        target.setCreateTime(java.time.LocalDateTime.now());
        if (ossMapper.insert(target) != 1) throw new IllegalStateException("通知私有快照预约失败");
        lifecycleManager.bind(targetId, NOTIFICATION_REF, String.valueOf(relationId));
        return new NotificationCopyReservation(relationId, sourceOssId, targetId, source.getService(),
            source.getFileName(), targetConfig.getConfigKey(), targetKey, source.getOriginalName(),
            ext.getContentType(), ext.getFileSize(), actorUserId, actorClientPk);
    }

    /** 单次30秒总预算内复制并回读目标字节；异常不能使保留行消失或允许盲重拷。 */
    @Override
    public NotificationCopyResult copyNotificationSnapshot(NotificationCopyReservation reservation) {
        if (reservation == null || reservation.fileSize() <= 0
            || reservation.fileSize() > notificationAttachmentMaxBytes) throw new ServiceException("附件复制预约无效");
        requireCurrentNotificationActor(reservation.actorUserId(), reservation.actorClientPk());
        verifyNotificationReservation(reservation);
        long deadlineNanos = System.nanoTime() + NOTIFICATION_COPY_DEADLINE.toNanos();
        byte[] bytes = OssFactory.instance(reservation.sourceService()).downloadBounded(
            reservation.sourceKey(), notificationAttachmentMaxBytes, copyRemaining(deadlineNanos));
        byte[] targetBytes = null;
        try {
            if (bytes == null || bytes.length != reservation.fileSize()) {
                throw new ServiceException("附件来源字节数发生变化");
            }
            var uploaded = OssFactory.instance(reservation.targetService()).uploadBounded(
                reservation.targetKey(), bytes, reservation.contentType(), copyRemaining(deadlineNanos));
            if (uploaded == null || uploaded.size() != bytes.length) throw new ServiceException("附件私有快照长度不一致");
            targetBytes = OssFactory.instance(reservation.targetService()).downloadBounded(
                reservation.targetKey(), notificationAttachmentMaxBytes, copyRemaining(deadlineNanos));
            if (targetBytes == null || targetBytes.length != bytes.length
                || !MessageDigest.isEqual(digest(bytes), digest(targetBytes))) {
                throw new ServiceException("附件私有快照摘要校验失败");
            }
            return new NotificationCopyResult(bytes.length, HexFormat.of().formatHex(digest(bytes)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256不可用", impossible);
        } finally {
            if (bytes != null) Arrays.fill(bytes, (byte) 0);
            if (targetBytes != null) Arrays.fill(targetBytes, (byte) 0);
        }
    }

    /** 仅在关系 READY 同笔事务调用，确认未就绪目标的身份和复制长度。 */
    @Override
    @com.baomidou.dynamic.datasource.annotation.DSTransactional
    public void confirmNotificationSnapshot(NotificationCopyReservation reservation, NotificationCopyResult result) {
        SysOss target = ossMapper.selectByIdForUpdate(reservation.targetOssId());
        if (target == null || !NOT_READY.equals(target.getDeleteState())
            || !Objects.equals(target.getService(), reservation.targetService())
            || !Objects.equals(target.getFileName(), reservation.targetKey())
            || result == null || result.fileSize() != reservation.fileSize()) {
            throw new ServiceException("附件私有快照预约已失效");
        }
        target.setDeleteState("ACTIVE");
        if (ossMapper.updateById(target) != 1) throw new IllegalStateException("附件私有快照确认失败");
    }

    /** 邮件适配器仅接收已确认私有目标的本地字节，不获取供应商签名 URL。 */
    @Override
    public void materializeNotificationSnapshot(Long relationId, Long sourceOssId, Long targetOssId,
                                                 String expectedSha256, Path destination) throws IOException {
        try {
            SysOss target = requireReadyNotificationSnapshot(relationId, sourceOssId, targetOssId);
            SysOssExt ext = notificationExt(target);
            if (notificationAttachmentMaxBytes <= 0 || ext.getFileSize() > notificationAttachmentMaxBytes)
                throw new ServiceException("附件快照超过物化上限");
            byte[] bytes = OssFactory.instance(target.getService()).downloadBounded(
                target.getFileName(), notificationAttachmentMaxBytes, NOTIFICATION_COPY_DEADLINE);
            try {
                if (bytes == null || bytes.length != ext.getFileSize()
                    || expectedSha256 == null || !expectedSha256.matches("[0-9a-f]{64}")
                    || !MessageDigest.isEqual(digest(bytes), HexFormat.of().parseHex(expectedSha256))) {
                    throw new ServiceException("附件快照内容不完整或摘要不一致");
                }
                // READY 复用前再次核验原提交者、两个引用和目标私有策略，不用旧会话授权事实。
                requireReadyNotificationSnapshot(relationId, sourceOssId, targetOssId);
                Files.write(destination, bytes);
            } finally {
                if (bytes != null) Arrays.fill(bytes, (byte) 0);
            }
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256不可用", impossible);
        } catch (RuntimeException exception) {
            throw new IOException("通知附件快照物化失败", exception);
        }
    }

    private SysOss requireReadyNotificationSnapshot(Long relationId, Long sourceOssId, Long targetOssId) {
        if (relationId == null || relationId <= 0 || sourceOssId == null || sourceOssId <= 0
            || targetOssId == null || targetOssId <= 0) throw new ServiceException("附件快照归属无效");
        SysOss source = ossMapper.selectById(sourceOssId);
        SysOss target = ossMapper.selectById(targetOssId);
        if (source == null || target == null || !"ACTIVE".equals(source.getDeleteState())
            || !"ACTIVE".equals(target.getDeleteState()) || !Objects.equals(source.getCreateBy(), target.getCreateBy())
            || !Objects.equals(notificationExt(source).getUploaderClientPk(), notificationExt(target).getUploaderClientPk())) {
            throw new ServiceException("附件快照或来源已失效");
        }
        String refId = String.valueOf(relationId);
        if (lifecycleManager.snapshot(sourceOssId).references().stream().noneMatch(ref ->
            NOTIFICATION_REF.equals(ref.refType()) && refId.equals(ref.refId()))
            || lifecycleManager.snapshot(targetOssId).references().stream().noneMatch(ref ->
                NOTIFICATION_REF.equals(ref.refType()) && refId.equals(ref.refId()))) {
            throw new ServiceException("附件快照持久归属已失效");
        }
        var targetConfig = ossConfigMapper.selectOne(new LambdaQueryWrapper<SysOssConfig>()
            .eq(SysOssConfig::getConfigKey, target.getService()));
        if (targetConfig == null || !"0".equals(targetConfig.getAccessPolicy())) {
            throw new ServiceException("附件快照目标不再是私有存储");
        }
        requireCurrentNotificationActor(source.getCreateBy(), notificationExt(source).getUploaderClientPk());
        return target;
    }

    private Duration copyRemaining(long deadlineNanos) {
        long nanos = deadlineNanos - System.nanoTime();
        if (nanos < TimeUnit.MILLISECONDS.toNanos(1)) throw new ServiceException("附件复制超过总时间预算");
        return Duration.ofNanos(nanos);
    }

    private byte[] digest(byte[] bytes) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }

    @Override
    @com.baomidou.dynamic.datasource.annotation.DSTransactional
    public void releaseNotificationReferences(Long relationId, Long sourceOssId, Long targetOssId) {
        if (relationId == null || relationId <= 0 || sourceOssId == null || sourceOssId <= 0) {
            throw new ServiceException("附件引用归属无效");
        }
        String refId = String.valueOf(relationId);
        // 锁两个对象统一按 OSS 主键顺序，保持与来源授权相同顺序。
        List<Long> ids = targetOssId == null ? List.of(sourceOssId)
            : java.util.stream.Stream.of(sourceOssId, targetOssId).distinct().sorted().toList();
        for (Long id : ids) lifecycleManager.reconcileReferences(NOTIFICATION_REF, refId, List.of(id), List.of());
    }

    private SysOss requireNotificationSource(Long sourceId, Long actorUserId, Long actorClientPk,
                                            List<SysOssConfig> configs) {
        if (sourceId == null || sourceId <= 0) throw new ServiceException("附件来源编号无效");
        SysOss source = ossMapper.selectByIdForUpdate(sourceId);
        if (source == null || !"ACTIVE".equals(source.getDeleteState())
            || !actorUserId.equals(source.getCreateBy())
            || configs.stream().noneMatch(item -> Objects.equals(item.getConfigKey(), source.getService())
                && ("0".equals(item.getAccessPolicy()) || "2".equals(item.getAccessPolicy())))) {
            throw new ServiceException("附件来源不存在或无访问权");
        }
        SysOssExt ext = notificationExt(source);
        if (!actorClientPk.equals(ext.getUploaderClientPk())) throw new ServiceException("附件来源不属于当前客户端");
        return source;
    }

    private void verifyNotificationReservation(NotificationCopyReservation reservation) {
        SysOss source = ossMapper.selectById(reservation.sourceOssId());
        SysOss target = ossMapper.selectById(reservation.targetOssId());
        if (source == null || target == null || !"ACTIVE".equals(source.getDeleteState())
            || !NOT_READY.equals(target.getDeleteState())
            || !Objects.equals(source.getCreateBy(), reservation.actorUserId())
            || !Objects.equals(source.getService(), reservation.sourceService())
            || !Objects.equals(source.getFileName(), reservation.sourceKey())
            || !Objects.equals(target.getService(), reservation.targetService())
            || !Objects.equals(target.getFileName(), reservation.targetKey())
            || !Objects.equals(notificationExt(source).getUploaderClientPk(), reservation.actorClientPk())
            || !Objects.equals(notificationExt(source).getFileSize(), reservation.fileSize())) {
            throw new ServiceException("通知附件复制预约与持久对象不一致");
        }
        String refId = String.valueOf(reservation.relationId());
        if (lifecycleManager.snapshot(source.getOssId()).references().stream().noneMatch(ref ->
            NOTIFICATION_REF.equals(ref.refType()) && refId.equals(ref.refId()))
            || lifecycleManager.snapshot(target.getOssId()).references().stream().noneMatch(ref ->
                NOTIFICATION_REF.equals(ref.refType()) && refId.equals(ref.refId()))) {
            throw new ServiceException("通知附件复制预约没有持久归属");
        }
    }

    private void requireCurrentNotificationActor(Long userId, Long clientPk) {
        if (userId == null || clientPk == null || notificationUserMapper == null
            || notificationClientMapper == null || notificationClientAccess == null) {
            throw new ServiceException("通知附件身份无法核验");
        }
        var user = notificationUserMapper.selectById(userId);
        var client = notificationClientMapper.selectVoById(clientPk);
        if (user == null || client == null || !SystemConstants.NORMAL.equals(user.getStatus())
            || !SystemConstants.NORMAL.equals(client.getStatus()) || !"0".equals(user.getDelFlag())) {
            throw new ServiceException("通知附件原提交者或客户端已失效");
        }
        notificationClientAccess.requireLoginAccess(userId, client);
    }

    private SysOssExt notificationExt(SysOss object) {
        try {
            SysOssExt ext = METADATA_JSON.readValue(object.getExt1(), SysOssExt.class);
            if (ext != null && ext.getFileSize() != null && ext.getFileSize() > 0
                && StringUtils.isNotBlank(ext.getContentType()) && StringUtils.isNotBlank(object.getFileName())
                && StringUtils.isNotBlank(object.getOriginalName())) return ext;
        } catch (RuntimeException ignored) { /* 元数据损坏不能变成授权成功。 */ }
        throw new ServiceException("附件来源元数据无效");
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
