package org.namewta.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.domain.row.ThirdProviderRow;

@Mapper
public interface ThirdProviderMapper extends BaseMapper<ThirdProvider> {
    ThirdProviderRow selectByProviderCode(String providerCode);
}
