package org.namewta.notify.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.notify.domain.entity.NotifyIntentAttachment;

/** 通知附件真实归属关系 Mapper。 */
@Mapper
public interface NotifyIntentAttachmentMapper extends BaseMapperPlus<NotifyIntentAttachment, NotifyIntentAttachment> { }
