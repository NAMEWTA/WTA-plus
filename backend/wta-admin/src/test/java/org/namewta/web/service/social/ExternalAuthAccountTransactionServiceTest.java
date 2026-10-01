package org.namewta.web.service.social;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.social.oidc.OidcIdentity;
import org.namewta.common.social.oidc.OidcProtocolClient;
import org.namewta.system.domain.SysSocial;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.mapper.SysSocialMapper;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.service.ISysUserService;
import org.namewta.system.service.ISysUserTypeRelService;
import org.namewta.system.service.ISysUserTypeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** OIDC 只放宽显式 pairwise 绑定数量，既有身份所有权和其他协议约束不变。 */
@Tag("dev")
class ExternalAuthAccountTransactionServiceTest {
    private final SysSocialMapper bindings = mock(SysSocialMapper.class);
    private final SysUserMapper users = mock(SysUserMapper.class);
    private final ExternalAuthAccountTransactionService service = new ExternalAuthAccountTransactionService(
            bindings, users, mock(ISysUserService.class), mock(ISysUserTypeRelService.class), mock(ISysUserTypeService.class));
    private final OidcIdentity identity = new OidcIdentity(
            "https://issuer.test", "pairwise-home", null, "User", null, null, null, null, 1);

    @Test
    void explicitOidcBindingAcceptsAnotherSubjectFromTheSameIssuer() {
        activeUser();
        when(bindings.selectCount(any())).thenReturn(1L);
        service.bind(7L, "corporate", "OIDC", identity);
        var saved = ArgumentCaptor.forClass(SysSocial.class);
        verify(bindings).insert(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(7L);
        assertThat(saved.getValue().getIdentityKey())
                .isEqualTo(OidcProtocolClient.identityKey(identity.issuer(), identity.subject()));
        assertThat(saved.getValue().getSubject()).isEqualTo("pairwise-home");
    }

    @Test
    void justAuthDoesNotInheritTheOidcPairwiseException() {
        activeUser();
        when(bindings.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.bind(7L, "github", "GITHUB", identity))
                .isInstanceOf(ServiceException.class).hasMessageContaining("请先解绑");
        verify(bindings, never()).insert(any(SysSocial.class));
    }

    @Test
    void existingIdentityRemainsIdempotentForOwnerAndUnavailableToAnotherUser() {
        activeUser();
        var existing = new SysSocial();
        existing.setUserId(7L);
        when(bindings.selectOne(any())).thenReturn(existing);
        service.bind(7L, "corporate", "OIDC", identity);
        verify(bindings, never()).insert(any(SysSocial.class));
        existing.setUserId(8L);
        assertThatThrownBy(() -> service.bind(7L, "corporate", "OIDC", identity))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已绑定其他账号");
        verify(bindings, never()).insert(any(SysSocial.class));
    }

    private void activeUser() {
        when(bindings.lockUser(7L)).thenReturn(7L);
        var user = new SysUserVo();
        user.setUserId(7L);
        user.setUserName("local-user");
        user.setStatus("0");
        when(users.selectVoById(7L)).thenReturn(user);
    }
}
