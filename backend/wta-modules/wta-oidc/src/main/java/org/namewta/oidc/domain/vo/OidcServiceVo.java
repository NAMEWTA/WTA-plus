package org.namewta.oidc.domain.vo;

import org.namewta.oidc.domain.OidcServiceSettings;

/** 实际生效与待重启目标并列展示，不伪报结构热更新。 */
public record OidcServiceVo(
        boolean configured,
        Integer version,
        boolean restartRequired,
        OidcServiceSettings active,
        OidcServiceSettings saved,
        boolean signingKeyReady,
        boolean stateKeyReady) {}
