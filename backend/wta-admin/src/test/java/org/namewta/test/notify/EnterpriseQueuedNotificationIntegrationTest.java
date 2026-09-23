package org.namewta.test.notify;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.notify.model.*;
import org.namewta.notify.api.InAppNotificationPort;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.adapter.worker.NotifyOutboxWorker;
import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.namewta.notify.service.runtime.NotificationApplicationRuntimeService;
import org.namewta.notify.usecase.*;
import org.namewta.profile.api.person.PersonIdentityLookupService;
import org.namewta.profile.enterprise.adapter.store.RedisEnterpriseTransferChallengeStore;
import org.namewta.profile.enterprise.dao.EnterpriseTransferDao;
import org.namewta.profile.enterprise.domain.bo.*;
import org.namewta.profile.enterprise.domain.vo.EnterpriseTransferVo;
import org.namewta.profile.enterprise.mapper.EnterpriseTransferMapper;
import org.namewta.profile.enterprise.service.EnterpriseTransferService;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseTransferUseCaseImpl;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 隔离数据库中的真实 Profile -> Notify 排队、Worker 受理和确认；只替换供应商及公开身份查询边界。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "profile.notify.integration", matches = "true")
class EnterpriseQueuedNotificationIntegrationTest {
    private static final long SOURCE = 9310101, TARGET = 9310102, PROFILE = 9310301, BINDING = 9310302;
    private NotifyAtomicResultIntegrationTest base;
    private JdbcTemplate db;
    private RedissonClient redis;
    private RedisEnterpriseTransferChallengeStore challenges;
    private EnterpriseTransferUseCaseImpl application;
    private RollbackBoundary boundary;
    private NotifyOutboxWorker worker;
    private NotifyClient provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void open() throws Exception {
        base = new NotifyAtomicResultIntegrationTest(); base.open();
        db = field("db", JdbcTemplate.class); redis = field("redis", RedissonClient.class);
        var datasource = field("routing", DataSource.class);
        var config = new MybatisConfiguration(new Environment("owned-t31", new SpringManagedTransactionFactory(), datasource));
        config.setMapUnderscoreToCamelCase(true); config.addMapper(EnterpriseTransferMapper.class);
        String resource = "/mapper/enterprise/EnterpriseTransferMapper.xml";
        try (var stream = getClass().getResourceAsStream(resource)) {
            assertThat(stream).isNotNull(); new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
        }
        var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
        var dao = new EnterpriseTransferDao(sessions.getMapper(EnterpriseTransferMapper.class));
        var users = mock(UserService.class); var target = new UserDTO();
        target.setUserId(TARGET); target.setPhoneNumber("13800138000"); target.setStatus("0");
        when(users.selectListByIds(any())).thenReturn(List.of(target)); when(users.lockActiveById(TARGET)).thenReturn(target);
        var identity = new PersonIdentityLookupService.ActiveIdentityMatch(TARGET, 9310201L);
        var identities = mock(PersonIdentityLookupService.class);
        when(identities.findActiveExactMatches(any())).thenReturn(List.of(identity));
        when(identities.lockActiveExactMatch(any())).thenReturn(Optional.of(identity));
        var notifyDao = field("dao", NotifyNotificationDao.class);
        var settings = mock(NotifyConfigDao.class);
        var account = new NotifyChannelAccount(); account.setAccountId(9310401L); account.setChannel("SMS");
        account.setConfigKey("owned-t31"); account.setEnabled("Y"); account.setMinuteMax(60);
        var binding = new NotifySceneBinding(); binding.setBindingId(9310402L); binding.setSceneCode("enterprise-transfer");
        binding.setChannel("SMS"); binding.setAccountId(account.getAccountId()); binding.setSmsTemplateCode("OWNED_T31");
        binding.setSmsParamMappingJson("{\"code\":\"code\"}"); binding.setRestricted("N"); binding.setTemplateMinuteMax(60);
        when(settings.findBinding("enterprise-transfer", "SMS")).thenReturn(binding);
        when(settings.findAccount(account.getAccountId())).thenReturn(account);
        provider = mock(NotifyClient.class);
        when(provider.send(any())).thenAnswer(invocation -> {
            assertThat(TransactionContext.getXID()).as("Provider remains outside a DB transaction").isNull();
            redis.getAtomicLong("owned-t31-provider-calls").incrementAndGet();
            return new NotifyResult("owned", NotifyChannel.SMS, "owned-t31", NotifyStatus.ACCEPTED, List.of());
        });
        ObjectProvider<InAppNotificationPort> inApp = mock(ObjectProvider.class);
        var dispatch = new DispatchNotificationService(notifyDao, provider, inApp, settings, (key, limit, duration) -> true,
            field("results", NotifyDispatchResultUseCase.class));
        var notifications = transactional(new NotificationApplicationUseCase(new NotificationApplicationRuntimeService(
            notifyDao, users, dispatch, event -> {})));
        worker = new NotifyOutboxWorker(field("claims", NotifyOutboxClaimUseCase.class), dispatch);
        challenges = spy(new RedisEnterpriseTransferChallengeStore(redis));
        application = transactional(new EnterpriseTransferUseCaseImpl(new EnterpriseTransferService(dao, challenges,
            () -> "123456", identities, users, notifications)));
        boundary = transactional(new RollbackBoundary(application));
        db.update("insert into profile_enterprise(enterprise_profile_id,enterprise_name,unified_credit_code,enterprise_type,legal_representative_name,legal_document_type_code,legal_document_number,established_date,registered_address,business_scope,status,version,create_time,del_flag) values(?,'Owned transfer','OWNED_T31','COMPANY','Owned','CN_RESIDENT_ID','owned','2010-01-01','owned','owned','ACTIVE',0,utc_timestamp(),'0')", PROFILE);
        db.update("insert into profile_enterprise_binding(enterprise_binding_id,enterprise_profile_id,user_id,status,binding_version,source_type,source_id,bound_time,version,create_time,del_flag) values(?,?,?,'ACTIVE',7,'USER_SUBMISSION',1,utc_timestamp(),0,utc_timestamp(),'0')", BINDING, PROFILE, SOURCE);
    }

    @AfterEach
    void close() {
        try {
            if (db != null) {
                db.execute("drop trigger if exists owned_t31_record_failure");
                for (String challenge : db.queryForList("select challenge_id from profile_enterprise_transfer_record where enterprise_profile_id=?", String.class, PROFILE)) challenges.revoke(challenge);
                if (boundary != null && boundary.lastChallenge() != null) challenges.revoke(boundary.lastChallenge());
                for (String table : List.of("profile_enterprise_transfer_record", "profile_enterprise_binding_event", "profile_enterprise_binding", "profile_enterprise")) db.update("delete from " + table + " where enterprise_profile_id=?", PROFILE);
                for (long id : db.queryForList("select intent_id from notify_intent where scene_code='enterprise-transfer'", Long.class)) {
                    for (String table : List.of("notify_attempt", "notify_outbox", "notify_delivery", "notify_recipient", "notify_intent")) db.update("delete from " + table + " where intent_id=?", id);
                }
                redis.getBucket(RedisEnterpriseTransferChallengeStore.rateKey(SOURCE, TARGET)).delete();
                redis.getAtomicLong("owned-t31-provider-calls").delete();
            }
        } finally { if (base != null) base.close(); }
    }

    @Test
    void queuedSubmissionWaitsForTheActualWorkerAndTransfersOnlyOnce() {
        var sent = application.send(SOURCE, send());
        assertThat(sent.status()).isEqualTo("QUEUED");
        assertThat(db.queryForObject("select count(*) from profile_enterprise_transfer_record where challenge_id=? and notification_id is not null", Integer.class, sent.challengeId())).isEqualTo(1);
        assertThat(confirm(sent).status()).isEqualTo("QUEUED"); unchangedBinding();
        assertThatThrownBy(() -> application.confirm(TARGET, confirmCommand(sent))).hasMessage("ENTERPRISE_TRANSFER_CHALLENGE_INVALID");
        worker.poll();
        assertThat(confirm(sent).status()).isEqualTo("TRANSFERRED");
        assertThat(db.queryForObject("select count(*) from profile_enterprise_binding where user_id=? and status='ACTIVE'", Integer.class, TARGET)).isEqualTo(1);
        assertThatThrownBy(() -> confirm(sent)).hasMessage("ENTERPRISE_TRANSFER_CHALLENGE_INVALID");
        assertThat(redis.getAtomicLong("owned-t31-provider-calls").get()).isEqualTo(1);
    }

    @Test
    void recordWriteFailureRollsBackTheNotificationAndRevokesTheStagedChallenge() {
        db.execute("create trigger owned_t31_record_failure before insert on profile_enterprise_transfer_record for each row signal sqlstate '45000' set message_text='owned record failure'");
        assertThatThrownBy(() -> application.send(SOURCE, send())).isInstanceOf(RuntimeException.class);
        assertNoCommittedSend(); unchangedBinding();
        assertThat(redis.getBucket(RedisEnterpriseTransferChallengeStore.rateKey(SOURCE, TARGET)).isExists()).isFalse();
    }

    @Test
    void outerRollbackLeavesOnlyAnUnconfirmableRedisChallengeWithItsOriginalTtl() {
        assertThatThrownBy(() -> boundary.sendThenFail()).hasMessage("owned after-send rollback");
        assertNoCommittedSend(); unchangedBinding();
        String id = boundary.lastChallenge();
        assertThat(redis.getBucket(RedisEnterpriseTransferChallengeStore.challengeKey(id)).remainTimeToLive()).isPositive().isLessThanOrEqualTo(300000);
        assertThatThrownBy(() -> application.confirm(SOURCE, new EnterpriseTransferConfirmBo(id, "123456"))).hasMessage("ENTERPRISE_TRANSFER_CHALLENGE_INVALID");
        verifyNoInteractions(provider);
    }

    @Test
    void consumedOtpAndRolledBackBindingsRecoverBySendingANewChallenge() {
        var sent = application.send(SOURCE, send()); worker.poll();
        assertThatThrownBy(() -> boundary.confirmThenFail(confirmCommand(sent))).hasMessage("owned after-confirm rollback");
        unchangedBinding();
        assertThat(redis.getBucket(RedisEnterpriseTransferChallengeStore.challengeKey(sent.challengeId())).isExists()).isFalse();
        assertThatThrownBy(() -> confirm(sent)).hasMessage("ENTERPRISE_TRANSFER_CHALLENGE_INVALID");
        var replacement = application.send(SOURCE, send());
        assertThat(replacement.challengeId()).isNotEqualTo(sent.challengeId()); worker.poll();
        assertThat(confirm(replacement).status()).isEqualTo("TRANSFERRED");
    }

    @Test
    void redisConsumeFailureRollsBackAllBindingAndEventWrites() {
        var sent = application.send(SOURCE, send()); worker.poll();
        doThrow(new IllegalStateException("owned redis failure")).when(challenges).consume(any());
        assertThatThrownBy(() -> confirm(sent)).hasMessage("owned redis failure"); unchangedBinding();
        assertThat(db.queryForObject("select count(*) from profile_enterprise_binding_event where enterprise_profile_id=?", Integer.class, PROFILE)).isZero();
    }

    @Test
    void changedSourceVersionRejectsEvenAnAcceptedNotificationAndCorrectCode() {
        var sent = application.send(SOURCE, send()); worker.poll();
        db.update("update profile_enterprise_binding set binding_version=8 where enterprise_binding_id=?", BINDING);
        assertThatThrownBy(() -> confirm(sent)).hasMessage("ENTERPRISE_TRANSFER_SOURCE_CHANGED"); unchangedBinding();
    }

    @Test
    void expiredAssociationAndTypedUnsentTerminalCannotActivateButPermitResend() {
        var expired = application.send(SOURCE, send());
        db.update("update profile_enterprise_transfer_record set expires_time=? where challenge_id=?", java.sql.Timestamp.from(Instant.now().minusSeconds(1)), expired.challengeId());
        assertThat(confirm(expired).status()).isEqualTo("EXPIRED");
        var failed = application.send(SOURCE, send());
        // 只有单目标明确未受理才是最终失败；泛 FAILED/empty 属结果未知。
        doAnswer(call -> {
            NotifyRequest request = call.getArgument(0, NotifyRequest.class);
            return new NotifyResult(request.requestId(), NotifyChannel.SMS, "owned-t31", NotifyStatus.FAILED,
                List.of(NotifyTargetResult.unsentTerminal(request.targets().getFirst(), "OWNED_TERMINAL", 1)));
        }).when(provider).send(any());
        worker.poll();
        assertThat(confirm(failed).status()).isEqualTo("FAILED"); unchangedBinding();
        long notificationId = db.queryForObject("select notification_id from profile_enterprise_transfer_record where challenge_id=?",
            Long.class, failed.challengeId());
        assertThat(db.queryForObject("select status from notify_delivery where intent_id=?", String.class, notificationId))
            .isEqualTo("FAILED");
        assertThat(db.queryForObject("select status from notify_outbox where intent_id=?", String.class, notificationId))
            .isEqualTo("DONE");
        assertThat(application.send(SOURCE, send()).status()).isEqualTo("QUEUED");
    }

    @Test
    void genericFailedWithoutSingleTargetRemainsUnknownAndCannotActivate() {
        var sent = application.send(SOURCE, send());
        db.update("update notify_outbox set max_attempts=1 where intent_id=(select notification_id "
            + "from profile_enterprise_transfer_record where challenge_id=?)", sent.challengeId());
        doReturn(new NotifyResult("owned", NotifyChannel.SMS, "owned-t31", NotifyStatus.FAILED, List.of()))
            .when(provider).send(any());

        worker.poll();

        assertThat(confirm(sent).status()).isEqualTo("QUEUED");
        unchangedBinding();
        long notificationId = db.queryForObject("select notification_id from profile_enterprise_transfer_record where challenge_id=?",
            Long.class, sent.challengeId());
        assertThat(db.queryForObject("select status from notify_delivery where intent_id=?", String.class, notificationId))
            .isEqualTo("UNKNOWN");
        assertThat(db.queryForObject("select status from notify_outbox where intent_id=?", String.class, notificationId))
            .isEqualTo("WAITING_RECEIPT");
        verify(provider, times(1)).send(any());
    }

    private void assertNoCommittedSend() {
        assertThat(db.queryForObject("select count(*) from profile_enterprise_transfer_record where enterprise_profile_id=?", Integer.class, PROFILE)).isZero();
        assertThat(db.queryForObject("select count(*) from notify_intent where scene_code='enterprise-transfer'", Integer.class)).isZero();
        assertThat(db.queryForObject("select count(*) from notify_outbox where intent_id<>9220001", Integer.class)).isZero();
    }
    private void unchangedBinding() {
        assertThat(db.queryForObject("select status from profile_enterprise_binding where enterprise_binding_id=?", String.class, BINDING)).isEqualTo("ACTIVE");
        assertThat(db.queryForObject("select count(*) from profile_enterprise_binding where user_id=?", Integer.class, TARGET)).isZero();
    }
    private EnterpriseTransferVo confirm(EnterpriseTransferVo sent) { return application.confirm(SOURCE, confirmCommand(sent)); }
    private static EnterpriseTransferConfirmBo confirmCommand(EnterpriseTransferVo sent) { return new EnterpriseTransferConfirmBo(sent.challengeId(), "123456"); }
    private static EnterpriseTransferSendBo send() { return new EnterpriseTransferSendBo("Owned", "3001", "13800138000"); }
    private <T> T field(String name, Class<T> type) { return type.cast(ReflectionTestUtils.getField(base, name)); }
    @SuppressWarnings("unchecked")
    private static <T> T transactional(T target) {
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return (T) proxy.getProxy();
    }
    /** 测试用外层事务，在业务返回之后注入失败，不修改生产可见性。 */
    public static class RollbackBoundary {
        private final EnterpriseTransferUseCaseImpl application;
        private String lastChallenge;
        public RollbackBoundary(EnterpriseTransferUseCaseImpl application) { this.application = application; }
        public String lastChallenge() { return lastChallenge; }
        @DSTransactional
        public void sendThenFail() {
            lastChallenge = application.send(SOURCE, send()).challengeId();
            throw new IllegalStateException("owned after-send rollback");
        }
        @DSTransactional
        public void confirmThenFail(EnterpriseTransferConfirmBo command) {
            assertThat(application.confirm(SOURCE, command).status()).isEqualTo("TRANSFERRED");
            throw new IllegalStateException("owned after-confirm rollback");
        }
    }
}
