package org.namewta.oidc.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.oidc.domain.OidcKeyMaterial;

/** 密钥持久映射，外部响应禁止直接返回实体。 */
public interface OidcKeyMaterialMapper extends BaseMapperPlus<OidcKeyMaterial, OidcKeyMaterial> {}
