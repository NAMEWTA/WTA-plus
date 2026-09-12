package org.namewta.notify.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.domain.vo.NotifySceneBindingVo;

/**
 * 场景渠道绑定数据访问。
 */
@Mapper
public interface NotifySceneBindingMapper extends BaseMapperPlus<NotifySceneBinding, NotifySceneBindingVo> {
}
