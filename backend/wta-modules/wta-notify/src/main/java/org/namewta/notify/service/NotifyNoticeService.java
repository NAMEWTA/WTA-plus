package org.namewta.notify.service;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.dao.NotifyPersistenceDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.bo.NotifyNoticeBo;
import org.namewta.notify.domain.entity.NotifyNotice;
import org.namewta.notify.domain.entity.NotifyNoticeSnapshot;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.policy.NoticeAudiencePolicy;
import org.namewta.notify.domain.vo.NotifyNoticeVo;
import org.namewta.notify.support.NotifyNoticeVersionFence;
import org.namewta.system.api.UserService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 公告草稿和生命周期规则，持久化操作统一经过 DAO。 */
@Service
@RequiredArgsConstructor
public class NotifyNoticeService {
    private final NotifyPersistenceDao dao;
    private final UserService userService;
    private final NotifyNotificationDao notificationDao;
    private static final Set<Long> STATIC_NOTICE_IDS = Set.of(1761800000000000001L, 1761800000000000002L);

    public PageResult<NotifyNoticeVo> page(NotifyNoticeBo query, Integer pageNum, Integer pageSize) {
        var page = dao.page(query, pageNum, pageSize);
        return PageResult.build(page.getRows().stream().map(this::toVo).toList(), page.getTotal());
    }

    public NotifyNoticeVo get(Long id) {
        NotifyNotice notice = dao.find(id);
        if (notice == null) throw new ServiceException("通知不存在");
        return toVo(notice);
    }

    private NotifyNoticeVo toVo(NotifyNotice entity) {
        NotifyNoticeVo vo = new NotifyNoticeVo();
        vo.setNoticeId(entity.getNoticeId());
        vo.setNoticeTitle(entity.getNoticeTitle());
        vo.setNoticeType(entity.getNoticeType());
        vo.setNoticeContent(entity.getNoticeContent());
        vo.setRecipientType(entity.getRecipientType() == null ? "ALL" : entity.getRecipientType());
        vo.setRecipientIds(JsonUtils.parseArray(entity.getRecipientIdsJson(), Long.class));
        vo.setUserTypeIds(JsonUtils.parseArray(entity.getUserTypeIdsJson(), Long.class));
        vo.setChannels(entity.getChannelsJson() == null ? List.of("IN_APP") : JsonUtils.parseArray(entity.getChannelsJson(), String.class));
        vo.setStatus(entity.getStatus());
        vo.setLifecycle(entity.getLifecycle());
        vo.setPublishedAt(entity.getPublishedAt());
        vo.setRetractedAt(entity.getRetractedAt());
        vo.setRemark(entity.getRemark());
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    public int add(NotifyNoticeBo bo) {
        NotifyNotice entity = toEntity(bo);
        entity.setLifecycle("DRAFT");
        entity.setStatus("1");
        return dao.insert(entity);
    }

    public int update(NotifyNoticeBo bo) {
        NotifyNotice current = requireLocked(bo.getNoticeId());
        requireEditable(current);
        NotifyNotice entity = toEntity(bo);
        entity.setLifecycle(current.getLifecycle());
        entity.setStatus(current.getStatus());
        entity.setPublishedAt(current.getPublishedAt());
        entity.setRetractedAt(current.getRetractedAt());
        return dao.update(entity);
    }

    private NotifyNotice toEntity(NotifyNoticeBo bo) {
        var audience = NoticeAudiencePolicy.normalize(bo.getRecipientType(), bo.getRecipientIds(), bo.getUserTypeIds(), bo.getChannels());
        // 草稿也验证目录权限，不能绕过候选查询把越权目标写入草稿。
        if ("USER".equals(audience.recipientType())) userService.selectNotificationUsers(audience.recipientIds());
        if ("USER_TYPE".equals(audience.recipientType())) userService.selectUsersByUserTypeIds(audience.userTypeIds());
        NotifyNotice entity = new NotifyNotice();
        entity.setNoticeId(bo.getNoticeId());
        entity.setNoticeTitle(bo.getNoticeTitle());
        entity.setNoticeType(bo.getNoticeType());
        entity.setNoticeContent(bo.getNoticeContent());
        entity.setRecipientType(audience.recipientType());
        entity.setRecipientIdsJson(JsonUtils.toJsonString(audience.recipientIds()));
        entity.setUserTypeIdsJson(JsonUtils.toJsonString(audience.userTypeIds()));
        entity.setChannelsJson(JsonUtils.toJsonString(audience.channels()));
        entity.setRemark(bo.getRemark());
        return entity;
    }

    public int delete(Long[] ids) {
        if (ids == null || ids.length == 0 || Arrays.stream(ids).anyMatch(id -> id == null || id <= 0)) throw new ServiceException("通知编号不能为空且必须为正数");
        // 固定加锁顺序，批量操作不会与其他删除请求形成反向锁顺序。
        List<Long> ordered = Arrays.stream(ids).distinct().sorted().toList();
        ordered.forEach(id -> requireEditable(requireLocked(id)));
        return dao.deleteBatch(ordered);
    }

    /** 返回 null 表示此版本已经发布，调用方不再创建投递任务。 */
    public NotifyNotice publish(Long id) {
        NotifyNotice entity = requireLocked(id);
        if ("PUBLISHED".equals(entity.getLifecycle())) return null;
        requireEditable(entity);
        entity.setStatus("0");
        entity.setLifecycle("PUBLISHED");
        entity.setPublishedAt(LocalDateTime.now());
        entity.setRetractedAt(null);
        if (dao.update(entity) != 1) throw new IllegalStateException("公告发布状态写入冲突");
        return entity;
    }

    public NotifyNotice retract(Long id) {
        NotifyNotice entity = requireLocked(id);
        if (!"PUBLISHED".equals(entity.getLifecycle()) && !"RETRACTED".equals(entity.getLifecycle())) {
            throw new ServiceException("仅已发布通知允许撤回");
        }
        NotifyNoticeSnapshot snapshot = dao.latestSnapshot(id);
        if (snapshot == null || snapshot.getSnapshotVersion() == null || snapshot.getSnapshotVersion() <= 0) {
            throw new ServiceException("公告发布版本事实不一致");
        }
        String key = NotifyNoticeVersionFence.idempotencyKey(id, snapshot.getSnapshotVersion());
        NotifyIntent intent = notificationDao.lockNoticeIntent(key);
        if (intent == null) {
            if (!staticSeedWithoutIntent(entity, snapshot)) throw new ServiceException("公告发布任务缺失");
        } else {
            String metadata = NotifyNoticeVersionFence.retractedMetadata(intent, id,
                snapshot.getSnapshotId(), snapshot.getSnapshotVersion());
            if (NotifyNoticeVersionFence.state(intent) != NotifyNoticeVersionFence.State.RETRACTED
                && notificationDao.saveNoticeMetadata(intent, metadata) != 1) {
                throw new IllegalStateException("公告版本撤回写入冲突");
            }
        }
        if ("PUBLISHED".equals(entity.getLifecycle())) {
            entity.setLifecycle("RETRACTED");
            entity.setRetractedAt(LocalDateTime.now());
            if (dao.update(entity) != 1) throw new IllegalStateException("公告撤回状态写入冲突");
        }
        return entity;
    }

    /** 只有六 SQL 中两条可精确证明未创建投递任务的静态 V1 公告允许无 Intent 撤回。 */
    private boolean staticSeedWithoutIntent(NotifyNotice notice, NotifyNoticeSnapshot snapshot) {
        if (!STATIC_NOTICE_IDS.contains(notice.getNoticeId()) || !Integer.valueOf(1).equals(snapshot.getSnapshotVersion())
            || !Objects.equals(snapshot.getSnapshotId(), notice.getNoticeId())
            || !Objects.equals(snapshot.getNoticeId(), notice.getNoticeId())
            || dao.countSnapshots(notice.getNoticeId()) != 1
            || notificationDao.countNoticeBusinessIntents(notice.getNoticeId()) != 0
            || !"0".equals(notice.getStatus()) || !"ALL".equals(notice.getRecipientType())
            || !Objects.equals(notice.getNoticeTitle(), snapshot.getTitleSnapshot())
            || !Objects.equals(notice.getNoticeContent(), snapshot.getContentSnapshot())
            || !Objects.equals(notice.getNoticeType(), snapshot.getNoticeType())
            || !Objects.equals(notice.getPublishedAt(), snapshot.getPublishedAt())
            || !Objects.equals(notice.getCreateTime(), snapshot.getCreateTime())
            || !Objects.equals(notice.getCreateBy(), snapshot.getCreateBy())) return false;
        try {
            return JsonUtils.parseArray(notice.getRecipientIdsJson(), Long.class).isEmpty()
                && JsonUtils.parseArray(notice.getUserTypeIdsJson(), Long.class).isEmpty()
                && List.of("IN_APP").equals(JsonUtils.parseArray(notice.getChannelsJson(), String.class));
        } catch (RuntimeException invalidSeed) {
            return false;
        }
    }

    private NotifyNotice requireLocked(Long id) {
        if (id == null || id <= 0) throw new ServiceException("通知编号必须为正数");
        NotifyNotice entity = dao.findForUpdate(id);
        if (entity == null) throw new ServiceException("通知不存在");
        return entity;
    }

    private void requireEditable(NotifyNotice entity) {
        if (!"DRAFT".equals(entity.getLifecycle()) && !"RETRACTED".equals(entity.getLifecycle())) {
            throw new ServiceException("仅草稿或已撤回通知允许编辑、发布或删除");
        }
    }
}
