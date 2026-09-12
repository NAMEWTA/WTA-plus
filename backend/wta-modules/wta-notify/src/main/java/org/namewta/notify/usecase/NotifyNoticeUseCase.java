package org.namewta.notify.usecase;

import lombok.RequiredArgsConstructor;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.namewta.common.core.domain.PageResult;
import org.namewta.notify.domain.bo.NotifyNoticeBo;
import org.namewta.notify.domain.vo.NotifyNoticeVo;
import org.namewta.notify.service.NotifyNoticePublisherService;
import org.namewta.notify.service.NotifyNoticeService;
import org.springframework.stereotype.Service;

/** 公告管理用例，Controller 只能依赖此入口。 */
@Service
@RequiredArgsConstructor
public class NotifyNoticeUseCase {
    private final NotifyNoticeService noticeService;
    private final NotifyNoticePublisherService publisher;

    public PageResult<NotifyNoticeVo> list(NotifyNoticeBo query, Integer pageNum, Integer pageSize) {
        return noticeService.page(query, pageNum, pageSize);
    }
    public NotifyNoticeVo get(Long id) { return noticeService.get(id); }
    @DSTransactional
    public int save(NotifyNoticeBo bo) { return bo.getNoticeId() == null ? noticeService.add(bo) : noticeService.update(bo); }
    @DSTransactional
    public void publish(Long id) {
        var notice = noticeService.publish(id);
        if (notice != null) publisher.publish(notice);
    }
    @DSTransactional
    public int retract(Long id) { return noticeService.retract(id) == null ? 0 : 1; }
    @DSTransactional
    public int remove(Long[] ids) { return noticeService.delete(ids); }
}
