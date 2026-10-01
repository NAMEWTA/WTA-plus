package org.namewta.sso.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.sso.api.SsoSessionRevocationParticipant;
import org.namewta.sso.config.SsoProperties;
import org.namewta.sso.dao.SsoSessionDao;
import org.namewta.sso.domain.SsoSession;
import org.namewta.sso.support.SsoBearerTokens;
import org.namewta.sso.support.SsoSessionHashes;
import org.namewta.sso.usecase.impl.SsoSessionUseCaseImpl;

import java.time.Instant;
import java.util.List;

@Tag("dev")
class SsoPersistentLogoutTest {
    @Test
    void persistentRevocationOverridesAnyRedisSession() {
        var dao = mock(SsoSessionDao.class);
        var service = new SsoPersistentSessionService(dao, mock(SocialSecretCipher.class));
        String sid = SsoBearerTokens.create(), hash = SsoSessionHashes.hash(sid);
        var row = new SsoSession();
        row.setStatus("REVOKED");
        row.setExpiresAt(Instant.now().plusSeconds(60));
        when(dao.find(hash)).thenReturn(row);
        assertThat(service.current(sid)).isNull();
        when(dao.lock(hash)).thenReturn(row);
        assertThatThrownBy(() -> service.requireLocked(sid))
                .isInstanceOf(org.namewta.common.core.exception.ServiceException.class);
    }

    @Test
    void fullLogoutLocksAndRevokesBeforeEnqueuingEveryProtocolParticipant() {
        var dao = mock(SsoSessionDao.class);
        var persistent = new SsoPersistentSessionService(dao, mock(SocialSecretCipher.class));
        var cache = mock(SsoSessionService.class);
        var participant = mock(SsoSessionRevocationParticipant.class);
        var useCase =
                new SsoSessionUseCaseImpl(
                        cache, persistent, List.of(participant), new SsoProperties());
        String sid = SsoBearerTokens.create(), hash = SsoSessionHashes.hash(sid);
        when(dao.lock(hash)).thenReturn(new SsoSession());
        useCase.logout(sid);
        var order = inOrder(dao, participant);
        order.verify(dao).lock(hash);
        order.verify(dao).revoke(hash);
        order.verify(participant).revoke(hash);
        verifyNoInteractions(cache); // 持久事实已撤销，Redis清理失败不能撤回全退事实。
    }
}
