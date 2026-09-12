package org.namewta.notify.service;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.dao.NotifyPersistenceDao;
import org.namewta.notify.domain.bo.NotifyNoticeBo;
import org.namewta.notify.domain.entity.NotifyNotice;
import org.namewta.notify.domain.policy.NoticeAudiencePolicy;
import org.namewta.notify.domain.vo.NotifyNoticeVo;
import org.namewta.system.api.UserService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** 公告草稿和生命周期规则，持久化操作统一经过 DAO。 */
@Service
@RequiredArgsConstructor
public class NotifyNoticeService {
    private final NotifyPersistenceDao dao;
    private final UserService userService;

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
        dao.update(entity);
        return entity;
    }

    public NotifyNotice retract(Long id) {
        NotifyNotice entity = requireLocked(id);
        if ("RETRACTED".equals(entity.getLifecycle())) return entity;
        if (!"PUBLISHED".equals(entity.getLifecycle())) throw new ServiceException("仅已发布通知允许撤回");
        entity.setLifecycle("RETRACTED");
        entity.setRetractedAt(LocalDateTime.now());
        dao.update(entity);
        return entity;
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
