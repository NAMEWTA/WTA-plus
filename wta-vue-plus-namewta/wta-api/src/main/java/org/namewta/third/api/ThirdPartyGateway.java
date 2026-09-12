package org.namewta.third.api;

import org.springframework.core.ParameterizedTypeReference;

/** 业务模块使用的统一第三方出口。 */
public interface ThirdPartyGateway {

    /** 使用动态 Endpoint 元数据执行 JSON/text/bytes 调用。 */
    ThirdPartyResponse<Object> execute(ThirdPartyRequest request);

    /** 使用声明的响应类型执行固定契约调用。 */
    <T> ThirdPartyResponse<T> execute(ThirdPartyRequest request, Class<T> responseType);

    /** 支持集合、分页等参数化类型的固定契约调用。 */
    <T> ThirdPartyResponse<T> execute(ThirdPartyRequest request, ParameterizedTypeReference<T> responseType);
}
