package org.namewta.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.namewta.notify.domain.entity.NotifyRecipient;

/** 通知接收者 Mapper。 */
@Mapper
public interface NotifyRecipientMapper extends BaseMapper<NotifyRecipient> { }
