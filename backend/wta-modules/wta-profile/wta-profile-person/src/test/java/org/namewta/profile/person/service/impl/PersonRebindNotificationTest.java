package org.namewta.profile.person.service.impl;

import com.baomidou.dynamic.datasource.annotation.DsTxEventListener;
import com.baomidou.dynamic.datasource.tx.DsTxEventListenerFactory;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationStatus;
import org.namewta.profile.person.dao.PersonNotificationAuditDao;
import org.namewta.profile.person.domain.model.read.PersonNotificationAuditRow;
import org.namewta.profile.person.event.PersonReboundEvent;
import org.namewta.profile.person.listener.PersonRebindNotificationListener;
import org.namewta.profile.person.mapper.PersonNotificationAuditMapper;
import org.namewta.profile.person.service.PersonRebindNotificationService;
import org.namewta.profile.person.usecase.impl.PersonRebindNotificationUseCaseImpl;
import org.namewta.system.api.UserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.event.TransactionPhase;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class PersonRebindNotificationTest {
    private final PersonNotificationAuditMapper audits = mock(PersonNotificationAuditMapper.class);
    private final UserService users = mock(UserService.class);
    private final NotificationApplicationService notifications = mock(NotificationApplicationService.class);
    private final PersonRebindNotificationService service = new PersonRebindNotificationService(
        new PersonNotificationAuditDao(audits), notifications, users);

    @Test
    void stagesRetryableAuditRowsBeforeAnyDelivery() {
        when(audits.insertNotificationAudit(any(PersonNotificationAuditRow.class))).thenReturn(1);
        service.stage(new PersonReboundEvent(9201L, 9001L, 202L));
        ArgumentCaptor<PersonNotificationAuditRow> audit = ArgumentCaptor.forClass(PersonNotificationAuditRow.class);
        verify(audits, org.mockito.Mockito.times(2)).insertNotificationAudit(audit.capture());
        assertThat(audit.getAllValues()).extracting(PersonNotificationAuditRow::getStatus)
            .containsExactly("PENDING", "PENDING");
        verifyNoInteractions(users, notifications);
    }

    @Test
    void defersDeliveryUntilDynamicDataSourceTransactionCommits() throws Exception {
        Method listener = PersonRebindNotificationListener.class.getMethod("handle", PersonReboundEvent.class);
        assertThat(listener.getAnnotation(DsTxEventListener.class).phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        when(users.selectPhonenumberById(202L)).thenReturn("13800138000");
        when(notifications.submit(any())).thenReturn(accepted());
        stubPendingRows();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DsTxEventListenerFactory.class);
            context.registerBean(PersonRebindNotificationListener.class,
                () -> new PersonRebindNotificationListener(new PersonRebindNotificationUseCaseImpl(service)));
            context.refresh();
            TransactionContext.bind("person-rebind-test");
            try {
                context.publishEvent(new PersonReboundEvent(9201L, 9001L, 202L));
                assertThat(TransactionContext.getSynchronizations()).hasSize(1);
                TransactionContext.getSynchronizations().getFirst().afterCommit();
                verify(notifications, org.mockito.Mockito.atLeastOnce()).submit(any());
            } finally {
                TransactionContext.removeSynchronizations();
                TransactionContext.remove();
            }
        }
    }

    @Test
    void sendsBothSafeChannelsWithoutIdentityOrNewAccountData() {
        when(users.selectPhonenumberById(202L)).thenReturn("13800138000");
        when(notifications.submit(any())).thenReturn(accepted());
        stubPendingRows();
        service.notifyOldAccount(new PersonReboundEvent(9201L, 9001L, 202L));
        ArgumentCaptor<NotificationCommand> request = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notifications, org.mockito.Mockito.times(2)).submit(request.capture());
        assertThat(request.getAllValues()).allSatisfy(command -> {
            assertThat(command.priority()).isZero();
            assertThat(command.expiresAt()).isNull();
        });
        assertThat(request.getAllValues()).allSatisfy(command ->
            assertThat(command.templateParams().toString()).doesNotContain("张三", "110101", "101"));
    }

    @Test
    void deliveryFailuresAreCapturedAndNeverEscapeTheAfterCommitListener() {
        doThrow(new IllegalStateException("offline")).when(notifications).submit(any());
        when(users.selectPhonenumberById(202L)).thenReturn("13800138000");
        stubPendingRows();
        service.notifyOldAccount(new PersonReboundEvent(9201L, 9001L, 202L));
        ArgumentCaptor<PersonNotificationAuditRow> audit = ArgumentCaptor.forClass(PersonNotificationAuditRow.class);
        verify(audits, org.mockito.Mockito.times(2)).updateDelivery(audit.capture());
        assertThat(audit.getAllValues()).extracting(PersonNotificationAuditRow::getStatus)
            .containsExactly("FAILED", "FAILED");
    }

    @Test
    void failedAuditCanBeRetriedWithoutReplayingTheBindingTransaction() {
        PersonNotificationAuditRow row = new PersonNotificationAuditRow();
        row.setNotificationAuditId(9901L);
        row.setNotificationType("PERSON_REBIND_INTERNAL");
        row.setProfileId(9201L);
        row.setApplicationId(9001L);
        row.setTargetUserId(202L);
        row.setStatus("FAILED");
        row.setVersion(0);
        when(audits.lockRetryable(9901L)).thenReturn(row);
        when(audits.updateDelivery(row)).thenReturn(1);
        when(notifications.submit(any())).thenReturn(accepted());
        assertThat(service.retryFailed(9901L)).isTrue();
        assertThat(row.getStatus()).isEqualTo("ACCEPTED");
        verify(notifications).submit(any());
    }

    @Test
    void bothChannelsRecordQueuedInsteadOfClaimingAcceptanceOrFailure() {
        when(users.selectPhonenumberById(202L)).thenReturn("13800138000");
        when(notifications.submit(any())).thenReturn(new NotificationReceipt("93101", NotificationStatus.QUEUED,
            true, false, List.of()));
        stubPendingRows();
        service.notifyOldAccount(new PersonReboundEvent(9201L, 9001L, 202L));
        ArgumentCaptor<PersonNotificationAuditRow> audit = ArgumentCaptor.forClass(PersonNotificationAuditRow.class);
        verify(audits, org.mockito.Mockito.times(2)).updateDelivery(audit.capture());
        assertThat(audit.getAllValues()).allSatisfy(row -> {
            assertThat(row.getStatus()).isEqualTo("QUEUED");
            assertThat(row.getNotifyRequestId()).isEqualTo("93101");
            assertThat(row.getFailureCategory()).isNull();
        });
    }

    private void stubPendingRows() {
        when(audits.selectRetryable(anyString(), anyLong(), anyLong(), anyLong()))
            .thenAnswer(invocation -> pending(invocation.getArgument(0)));
        when(audits.updateDelivery(any())).thenReturn(1);
    }

    private PersonNotificationAuditRow pending(String type) {
        PersonNotificationAuditRow row = new PersonNotificationAuditRow();
        row.setNotificationAuditId("PERSON_REBIND_INTERNAL".equals(type) ? 9901L : 9902L);
        row.setNotificationType(type);
        row.setProfileId(9201L);
        row.setApplicationId(9001L);
        row.setTargetUserId(202L);
        row.setStatus("PENDING");
        row.setVersion(0);
        return row;
    }

    private NotificationReceipt accepted() {
        return new NotificationReceipt("notification-1", NotificationStatus.ACCEPTED, false, false, List.of());
    }
}
