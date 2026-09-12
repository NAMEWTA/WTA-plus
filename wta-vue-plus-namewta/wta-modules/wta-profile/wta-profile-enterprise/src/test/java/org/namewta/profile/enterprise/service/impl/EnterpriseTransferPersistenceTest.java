package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.domain.exception.EnterpriseTransferException;
import org.namewta.profile.enterprise.domain.transfer.EnterpriseTransferChallenge;
import org.namewta.profile.enterprise.mapper.EnterpriseTransferMapper;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseTransferOwnerRow;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.time.Clock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class EnterpriseTransferPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T15:00:00Z");

    private final EnterpriseTransferMapper mapper = mock(EnterpriseTransferMapper.class);
    private final EnterpriseTransferServiceImpl service = new EnterpriseTransferServiceImpl(
        mapper, null, null, null, null, null, Clock.systemUTC());

    @Test
    void recordsChallengeAndSwitchesBindingsWithAppendOnlyEvents() {
        EnterpriseTransferChallenge challenge = challenge();
        EnterpriseTransferOwnerRow source = owner();
        when(mapper.insertTransferRecord(anyLong(), eq(9201L), eq(9101L), eq(101L), eq(202L),
            eq("challenge-1"), eq(7), eq(Instant.ofEpochMilli(challenge.expiresAtEpochMilli())), eq(NOW)))
            .thenReturn(1);
        service.recordChallenge(challenge, NOW);

        when(mapper.lockActiveOwner(101L)).thenReturn(source);
        when(mapper.lockEffectiveBindingId(202L)).thenReturn(null);
        when(mapper.unbindSource(9101L, 9201L, 101L, 7, NOW, 101L)).thenReturn(1);
        when(mapper.insertBinding(anyLong(), eq(9201L), eq(202L), eq("SELF_TRANSFER"),
            eq(9101L), eq(NOW), eq(101L))).thenReturn(1);
        when(mapper.insertEvent(anyLong(), anyLong(), eq(9201L), anyLong(), anyString(),
            anyInt(), anyString(), eq(9101L), anyString(), eq(NOW), eq(101L))).thenReturn(1);
        when(mapper.confirmTransferRecord("challenge-1", 9201L, 9101L, 101L, 202L, 7, NOW, 101L))
            .thenReturn(1);

        service.transferBindings(challenge, NOW);

        InOrder order = inOrder(mapper);
        order.verify(mapper).lockActiveOwner(101L);
        order.verify(mapper).lockEffectiveBindingId(202L);
        order.verify(mapper).unbindSource(9101L, 9201L, 101L, 7, NOW, 101L);
        order.verify(mapper).insertEvent(anyLong(), eq(9101L), eq(9201L), eq(101L), eq("UNBOUND"),
            eq(8), eq("SELF_TRANSFER"), eq(9101L), eq("TRANSFER_CHALLENGE:challenge-1"), eq(NOW), eq(101L));
        order.verify(mapper).insertBinding(anyLong(), eq(9201L), eq(202L), eq("SELF_TRANSFER"),
            eq(9101L), eq(NOW), eq(101L));
        order.verify(mapper).insertEvent(anyLong(), anyLong(), eq(9201L), eq(202L), eq("ACTIVE"),
            eq(1), eq("SELF_TRANSFER"), eq(9101L), eq("TRANSFER_CHALLENGE:challenge-1"), eq(NOW), eq(101L));
        order.verify(mapper).confirmTransferRecord("challenge-1", 9201L, 9101L, 101L, 202L, 7, NOW, 101L);
    }

    @Test
    void staleSourceOrOccupiedTargetStopsBeforeMutation() {
        when(mapper.lockActiveOwner(101L)).thenReturn(null);
        assertThatThrownBy(() -> service.transferBindings(challenge(), NOW))
            .isInstanceOf(EnterpriseTransferException.class)
            .hasMessage("ENTERPRISE_TRANSFER_SOURCE_CHANGED");

        when(mapper.lockActiveOwner(101L)).thenReturn(owner());
        when(mapper.lockEffectiveBindingId(202L)).thenReturn(9901L);
        assertThatThrownBy(() -> service.transferBindings(challenge(), NOW))
            .isInstanceOf(EnterpriseTransferException.class)
            .hasMessage("ENTERPRISE_TRANSFER_TARGET_INELIGIBLE");
    }

    @Test
    void selfUnbindOnlyChangesTheBindingAndAppendsItsNextVersion() {
        when(mapper.lockActiveOwner(101L)).thenReturn(owner());
        when(mapper.unbindSource(9101L, 9201L, 101L, 7, NOW, 101L)).thenReturn(1);
        when(mapper.insertEvent(anyLong(), eq(9101L), eq(9201L), eq(101L), eq("UNBOUND"),
            eq(8), eq("SELF_UNBIND"), eq(9101L), eq("RESPONSIBLE_SELF_UNBIND"), eq(NOW), eq(101L)))
            .thenReturn(1);

        assertThat(service.unbindBinding(101L, NOW)).isEqualTo(9201L);
        verify(mapper).unbindSource(9101L, 9201L, 101L, 7, NOW, 101L);
    }

    private EnterpriseTransferChallenge challenge() {
        return new EnterpriseTransferChallenge("challenge-1", 101L, 202L, 9201L, 9101L, 7,
            8201L, "张三", "3001", "13800138000", "$2a$10$redacted",
            EnterpriseTransferChallenge.State.ACTIVE, 0, NOW.plusSeconds(300).toEpochMilli());
    }

    private EnterpriseTransferOwnerRow owner() {
        EnterpriseTransferOwnerRow row = new EnterpriseTransferOwnerRow();
        row.setBindingId(9101L);
        row.setProfileId(9201L);
        row.setUserId(101L);
        row.setBindingVersion(7);
        return row;
    }

}
