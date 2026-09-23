package org.namewta.notify.domain.vo;

import org.namewta.common.core.domain.PageResult;

import java.io.Serial;
import java.util.Collection;

/** 本人收件箱分页结果；未读数覆盖所有有效收件关系，而非当前页。 */
public class NotifyInboxPageVo extends PageResult<NotifyInboxMessageVo> {
    @Serial
    private static final long serialVersionUID = 1L;

    private long unreadTotal;

    public NotifyInboxPageVo(Collection<NotifyInboxMessageVo> rows, long total, long unreadTotal) {
        super(rows, total);
        this.unreadTotal = unreadTotal;
    }

    public long getUnreadTotal() { return unreadTotal; }
}
