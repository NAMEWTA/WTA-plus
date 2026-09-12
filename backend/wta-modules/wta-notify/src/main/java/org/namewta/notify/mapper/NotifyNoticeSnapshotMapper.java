package org.namewta.notify.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.namewta.notify.domain.entity.NotifyNoticeSnapshot;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 公告发布快照数据访问。
 */
@Mapper
public interface NotifyNoticeSnapshotMapper extends BaseMapperPlus<NotifyNoticeSnapshot, NotifyNoticeSnapshot> {
}
