package org.namewta.notify.dao;

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.notify.domain.bo.NotifyNoticeBo;
import org.namewta.notify.domain.entity.NotifyNotice;
import org.namewta.notify.mapper.NotifyNoticeMapper;
import org.namewta.notify.domain.entity.NotifyNoticeSnapshot;
import org.namewta.notify.mapper.NotifyNoticeSnapshotMapper;
import org.springframework.stereotype.Repository;

/** 通知模块统一持久化边界，复杂查询继续下沉到 Mapper/XML。 */
@Repository
@RequiredArgsConstructor
public class NotifyPersistenceDao {
    private final NotifyNoticeMapper noticeMapper;
    private final NotifyNoticeSnapshotMapper snapshotMapper;

    /** 每次查询新建 Wrapper，分页与排序仅在持久化边界处理。 */
    public PageResult<NotifyNotice> page(NotifyNoticeBo query, Integer pageNum, Integer pageSize) {
        var wrapper = new LambdaQueryWrapper<NotifyNotice>()
            .like(query.getNoticeTitle() != null && !query.getNoticeTitle().isBlank(), NotifyNotice::getNoticeTitle, query.getNoticeTitle())
            .eq(query.getNoticeType() != null && !query.getNoticeType().isBlank(), NotifyNotice::getNoticeType, query.getNoticeType())
            .eq(query.getStatus() != null && !query.getStatus().isBlank(), NotifyNotice::getStatus, query.getStatus())
            .orderByDesc(NotifyNotice::getCreateTime).orderByDesc(NotifyNotice::getNoticeId);
        var page = noticeMapper.selectPage(new PageQuery(pageSize, pageNum).build(), wrapper);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    public NotifyNotice find(Long noticeId) { return noticeId == null ? null : noticeMapper.selectById(noticeId); }
    public NotifyNotice findForUpdate(Long noticeId) { return noticeId == null ? null : noticeMapper.selectByIdForUpdate(noticeId); }
    public int insert(NotifyNotice notice) { return noticeMapper.insert(notice); }
    public int update(NotifyNotice notice) { return noticeMapper.updateById(notice); }
    public int deleteBatch(java.util.Collection<Long> ids) { return noticeMapper.deleteBatchIds(ids); }
    public NotifyNoticeSnapshot latestSnapshot(Long noticeId) {
        return snapshotMapper.selectOne(new LambdaQueryWrapper<NotifyNoticeSnapshot>()
            .eq(NotifyNoticeSnapshot::getNoticeId, noticeId)
            .orderByDesc(NotifyNoticeSnapshot::getSnapshotVersion).last("limit 1"));
    }
    public int insertSnapshot(NotifyNoticeSnapshot snapshot) { return snapshotMapper.insert(snapshot); }
    public long countSnapshots(Long noticeId) {
        return snapshotMapper.selectCount(new LambdaQueryWrapper<NotifyNoticeSnapshot>()
            .eq(NotifyNoticeSnapshot::getNoticeId, noticeId));
    }
    /** 仅本次发布的通用入口可转为本人消息深链；不改标题、正文和旧版本。 */
    public int updateSnapshotPath(NotifyNoticeSnapshot snapshot, String path) {
        return snapshotMapper.update(null, new LambdaUpdateWrapper<NotifyNoticeSnapshot>()
            .eq(NotifyNoticeSnapshot::getSnapshotId, snapshot.getSnapshotId())
            .eq(NotifyNoticeSnapshot::getNoticeId, snapshot.getNoticeId())
            .eq(NotifyNoticeSnapshot::getSnapshotVersion, snapshot.getSnapshotVersion())
            .eq(NotifyNoticeSnapshot::getPathSnapshot, "/notify/inbox")
            .set(NotifyNoticeSnapshot::getPathSnapshot, path));
    }
}
