package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.adapter.security.EnterpriseTransferCodeGenerator;
import org.namewta.profile.enterprise.domain.exception.EnterpriseTransferException;
import org.namewta.profile.enterprise.domain.transfer.EnterpriseTransferChallenge;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseTransferOwnerRow;
import org.namewta.profile.enterprise.mapper.EnterpriseTransferMapper;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore;
import cn.hutool.crypto.digest.BCrypt;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationStatus;
import org.namewta.profile.api.person.PersonIdentityLookupService;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityLock;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityMatch;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.StageResult;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.Verification;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.VerificationStatus;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.VerifiedChallenge;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferConfirmBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferSendBo;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class EnterpriseTransferServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T15:00:00Z");

    private final EnterpriseTransferMapper mapper = mock(EnterpriseTransferMapper.class);
    private final EnterpriseTransferChallengeStore challenges = mock(EnterpriseTransferChallengeStore.class);
    private final EnterpriseTransferCodeGenerator codes = mock(EnterpriseTransferCodeGenerator.class);
    private final PersonIdentityLookupService personIdentities = mock(PersonIdentityLookupService.class);
    private final UserService users = mock(UserService.class);
    private final NotificationApplicationService notify = mock(NotificationApplicationService.class);
    private final EnterpriseTransferServiceImpl service = new EnterpriseTransferServiceImpl(mapper, challenges,
        codes, personIdentities, users, notify, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void queuedNotifyCreatesAnUnactivatedChallengeInsteadOfRejectingSubmission() {
        eligibleTarget();
        when(codes.generate()).thenReturn("123456");
        when(challenges.stage(any())).thenReturn(StageResult.STAGED);
        when(notify.submit(any())).thenReturn(new NotificationReceipt("93101", NotificationStatus.QUEUED,
            true, false, List.of()));

        var view = service.send(101L, new EnterpriseTransferSendBo("张三", "3001", "13800138000"));

        assertThat(view.status()).isEqualTo("QUEUED");
        assertThat(view.challengeId()).isNotBlank();
        verify(challenges, never()).activate(any());
        verify(challenges, never()).revoke(any());
    }

    @Test
    void recordsRedactedNotificationAssociationWithoutActivatingBeforeCommit() {
        eligibleTarget();
        when(codes.generate()).thenReturn("123456");
        when(challenges.stage(any())).thenReturn(StageResult.STAGED);
        when(notify.submit(any())).thenReturn(accepted());
        when(challenges.activate(any())).thenReturn(true);

        var view = service.send(101L, new EnterpriseTransferSendBo("张三", "3001", "13800138000"));

        assertThat(view.status()).isEqualTo("QUEUED");
        assertThat(view.challengeId()).isNotBlank();
        assertThat(view.expiresInSeconds()).isEqualTo(300);
        ArgumentCaptor<EnterpriseTransferChallenge> challenge =
            ArgumentCaptor.forClass(EnterpriseTransferChallenge.class);
        verify(challenges).stage(challenge.capture());
        assertThat(challenge.getValue().state()).isEqualTo(EnterpriseTransferChallenge.State.PENDING_DELIVERY);
        assertThat(BCrypt.checkpw("123456", challenge.getValue().codeHash())).isTrue();
        assertThat(challenge.getValue().sourceBindingVersion()).isEqualTo(7);
        ArgumentCaptor<NotificationCommand> request = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notify).submit(request.capture());
        assertThat(request.getValue().metadata().get("audit")).isEqualTo("REDACT_SENSITIVE");
        assertThat(request.getValue().templateParams().get("code")).isEqualTo("123456");
        assertThat(request.getValue().templateParams()).doesNotContainKey("content");
        verify(challenges, never()).activate(view.challengeId());
    }

    @Test
    void returnsTheSameMinimalResultWhenTheThreeTargetFactorsDoNotMatch() {
        when(mapper.selectActiveOwner(101L)).thenReturn(owner());
        when(personIdentities.findActiveExactMatches(any())).thenReturn(List.of());

        var view = service.send(101L, new EnterpriseTransferSendBo("张三", "3001", "13800138000"));

        assertThat(view.status()).isEqualTo("NOT_AVAILABLE");
        assertThat(view.challengeId()).isNull();
        verifyNoInteractions(challenges, codes, users, notify);
    }

    @Test
    void failedSmsNeverLeavesAConfirmableChallenge() {
        eligibleTarget();
        when(codes.generate()).thenReturn("123456");
        when(challenges.stage(any())).thenReturn(StageResult.STAGED);
        when(notify.submit(any())).thenThrow(new IllegalStateException("rejected"));

        assertThatThrownBy(() -> service.send(101L,
            new EnterpriseTransferSendBo("张三", "3001", "13800138000")))
            .isInstanceOf(EnterpriseTransferException.class)
            .hasMessage("ENTERPRISE_TRANSFER_DELIVERY_FAILED");
        verify(challenges).revoke(any());
        verify(challenges, never()).activate(any());
    }

    private NotificationReceipt accepted() {
        return new NotificationReceipt("enterprise-transfer-challenge-1", NotificationStatus.ACCEPTED,
            false, false, List.of());
    }

    @Test
    void confirmsOnlyAfterEligibilityRecheckAndConsumesExactlyOnce() {
        EnterpriseTransferChallenge challenge = challenge(EnterpriseTransferChallenge.State.ACTIVE);
        when(challenges.verify("challenge-1", 101L, "123456"))
            .thenReturn(new Verification(VerificationStatus.VERIFIED,
                new VerifiedChallenge(challenge, "stored-token")));
        eligibleAccountOnly();
        when(challenges.consume(any())).thenReturn(true);

        var view = service.confirm(101L, new EnterpriseTransferConfirmBo("challenge-1", "123456"));

        assertThat(view.status()).isEqualTo("TRANSFERRED");
        InOrder order = inOrder(personIdentities, users, mapper);
        ArgumentCaptor<ActiveIdentityLock> identityLock = ArgumentCaptor.forClass(ActiveIdentityLock.class);
        order.verify(personIdentities).lockActiveExactMatch(identityLock.capture());
        order.verify(users).lockActiveById(202L);
        order.verify(mapper).countEffectiveBinding(202L);
        order.verify(mapper).lockActiveOwner(101L);
        assertThat(identityLock.getValue()).isEqualTo(
            new ActiveIdentityLock(202L, 8201L, "张三", "3001"));
        verify(challenges).consume(any());
    }

    @Test
    void invalidOrReplayedCodeNeverTouchesBindings() {
        when(challenges.verify("challenge-1", 101L, "000000"))
            .thenReturn(new Verification(VerificationStatus.INVALID, null));

        assertThatThrownBy(() -> service.confirm(101L,
            new EnterpriseTransferConfirmBo("challenge-1", "000000")))
            .isInstanceOf(EnterpriseTransferException.class)
            .hasMessage("ENTERPRISE_TRANSFER_CHALLENGE_INVALID");
        verify(mapper).lockChallenge("challenge-1", 101L);
        verifyNoInteractions(personIdentities, users, notify);
    }

    @Test
    void consumeRaceRollsBackInsteadOfReportingASecondTransfer() {
        EnterpriseTransferChallenge challenge = challenge(EnterpriseTransferChallenge.State.ACTIVE);
        when(challenges.verify("challenge-1", 101L, "123456"))
            .thenReturn(new Verification(VerificationStatus.VERIFIED,
                new VerifiedChallenge(challenge, "stored-token")));
        eligibleAccountOnly();
        when(challenges.consume(any())).thenReturn(false);

        assertThatThrownBy(() -> service.confirm(101L,
            new EnterpriseTransferConfirmBo("challenge-1", "123456")))
            .isInstanceOf(EnterpriseTransferException.class)
            .hasMessage("ENTERPRISE_TRANSFER_CHALLENGE_INVALID");
        verify(mapper).confirmTransferRecord(any(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyInt(), any(), anyLong());
    }

    @Test
    void currentResponsibleCanUnbindWithoutWorkflowOrDeletion() {
        successfulBindingMutation();

        assertThat(service.unbind(101L).status()).isEqualTo("UNBOUND");
        verify(mapper).unbindSource(9101L, 9201L, 101L, 7, NOW, 101L);
        verifyNoInteractions(challenges, codes, personIdentities, users, notify);
    }

    private org.namewta.profile.enterprise.domain.ProfileEnterpriseTransferRecord committedNotification(NotificationStatus status) {
        var record = new org.namewta.profile.enterprise.domain.ProfileEnterpriseTransferRecord();
        record.setChallengeId("challenge-1"); record.setNotificationId("93101");
        record.setSourceUserId(101L); record.setTargetUserId(202L); record.setEnterpriseProfileId(9201L);
        record.setSourceBindingId(9101L); record.setExpectedBindingVersion(7); record.setExpiresTime(NOW.plusSeconds(300));
        when(mapper.lockChallenge("challenge-1", 101L)).thenReturn(record);
        when(notify.query(new org.namewta.notify.api.NotificationQuery("93101", false))).thenReturn(
            new org.namewta.notify.api.NotificationSnapshot("93101", status, NOW, List.of()));
        when(challenges.activate("challenge-1")).thenReturn(true);
        return record;
    }

    @Test
    void queuedNotificationCannotActivateOrConsumeTheChallenge() {
        committedNotification(NotificationStatus.QUEUED);
        var view = service.confirm(101L, new EnterpriseTransferConfirmBo("challenge-1", "123456"));
        assertThat(view.status()).isEqualTo("QUEUED");
        assertThat(view.expiresInSeconds()).isEqualTo(300);
        verifyNoInteractions(challenges, personIdentities, users);
    }

    @Test
    void failedNotificationRevokesTheChallengeAndAllowsResend() {
        committedNotification(NotificationStatus.FAILED);
        assertThat(service.confirm(101L, new EnterpriseTransferConfirmBo("challenge-1", "123456")).status()).isEqualTo("FAILED");
        verify(challenges).revoke("challenge-1");
        verify(challenges, never()).verify(any(), anyLong(), any());
        verifyNoInteractions(personIdentities, users);
    }

    @Test
    void expiredCommittedChallengeDoesNotQueryNotifyOrVerifyOtp() {
        committedNotification(NotificationStatus.DELIVERED).setExpiresTime(NOW);
        assertThat(service.confirm(101L, new EnterpriseTransferConfirmBo("challenge-1", "123456")).status()).isEqualTo("EXPIRED");
        verify(challenges).revoke("challenge-1");
        verifyNoInteractions(notify, personIdentities, users);
    }

    @Test
    void matchingOtpCannotOverrideTheServerSideTransferAssociation() {
        eligibleAccountOnly();
        committedNotification(NotificationStatus.DELIVERED).setTargetUserId(303L);
        when(challenges.verify("challenge-1", 101L, "123456"))
            .thenReturn(new Verification(VerificationStatus.VERIFIED,
                new VerifiedChallenge(challenge(EnterpriseTransferChallenge.State.ACTIVE), "stored-token")));
        assertThatThrownBy(() -> service.confirm(101L, new EnterpriseTransferConfirmBo("challenge-1", "123456")))
            .hasMessage("ENTERPRISE_TRANSFER_CHALLENGE_INVALID");
        verifyNoInteractions(personIdentities, users);
        verify(challenges, never()).consume(any());
    }

    private void eligibleTarget() {
        when(mapper.selectActiveOwner(101L)).thenReturn(owner());
        when(mapper.insertTransferRecord(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
            any(), any(), anyInt(), any(), any())).thenReturn(1);
        when(personIdentities.findActiveExactMatches(any())).thenReturn(List.of(identity()));
        batchAccount();
    }

    private void eligibleAccountOnly() {
        committedNotification(NotificationStatus.ACCEPTED);

        when(personIdentities.lockActiveExactMatch(any())).thenReturn(Optional.of(identity()));
        when(mapper.countEffectiveBinding(202L)).thenReturn(0);
        successfulBindingMutation();
        lockedAccount();
    }

    private void batchAccount() {
        when(users.selectListByIds(any())).thenReturn(List.of(activeUser()));
    }

    private void lockedAccount() {
        when(users.lockActiveById(202L)).thenReturn(activeUser());
    }

    private UserDTO activeUser() {
        UserDTO user = new UserDTO();
        user.setUserId(202L);
        user.setStatus("0");
        user.setPhoneNumber("13800138000");
        return user;
    }

    private void successfulBindingMutation() {
        when(mapper.lockActiveOwner(101L)).thenReturn(owner());
        when(mapper.lockEffectiveBindingId(202L)).thenReturn(null);
        when(mapper.unbindSource(anyLong(), anyLong(), anyLong(), anyInt(), any(), anyLong())).thenReturn(1);
        when(mapper.insertBinding(anyLong(), anyLong(), anyLong(), any(), anyLong(), any(), anyLong())).thenReturn(1);
        when(mapper.insertEvent(anyLong(), anyLong(), anyLong(), anyLong(), any(), anyInt(),
            any(), anyLong(), any(), any(), anyLong())).thenReturn(1);
        when(mapper.confirmTransferRecord(any(), anyLong(), anyLong(), anyLong(), anyLong(),
            anyInt(), any(), anyLong())).thenReturn(1);
    }

    private EnterpriseTransferOwnerRow owner() {
        EnterpriseTransferOwnerRow row = new EnterpriseTransferOwnerRow();
        row.setBindingId(9101L);
        row.setProfileId(9201L);
        row.setUserId(101L);
        row.setBindingVersion(7);
        return row;
    }

    private ActiveIdentityMatch identity() {
        return new ActiveIdentityMatch(202L, 8201L);
    }

    private EnterpriseTransferChallenge challenge(EnterpriseTransferChallenge.State state) {
        return new EnterpriseTransferChallenge("challenge-1", 101L, 202L, 9201L, 9101L, 7,
            8201L, "张三", "3001", "13800138000", "$2a$10$redacted", state, 0,
            NOW.plusSeconds(300).toEpochMilli());
    }
}
