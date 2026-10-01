package org.namewta.web.service.impl;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.utils.ValidatorUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.system.api.model.SocialLoginBody;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.web.domain.vo.LoginVo;
import org.namewta.web.service.IAuthStrategy;
import org.namewta.web.service.social.ExternalAuthService;
import org.springframework.stereotype.Service;

/** 复用原 social 授权类型，将协议验证、账号关联与会话交由外部认证编排。 */
@Service("social" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class SocialAuthStrategy implements IAuthStrategy {
    private final ExternalAuthService externalAuth;

    @Override
    public LoginVo login(String body, SysClientVo client) {
        SocialLoginBody request = JsonUtils.parseObject(body, SocialLoginBody.class);
        ValidatorUtils.validate(request);
        return externalAuth.login(request, client);
    }
}
