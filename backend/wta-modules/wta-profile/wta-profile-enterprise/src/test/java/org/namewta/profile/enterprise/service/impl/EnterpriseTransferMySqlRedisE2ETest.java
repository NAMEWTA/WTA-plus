package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.adapter.store.RedisEnterpriseTransferChallengeStore;
import org.namewta.profile.enterprise.adapter.security.EnterpriseTransferCodeGenerator;
import org.namewta.profile.enterprise.controller.advice.EnterpriseTransferExceptionHandler;
import org.namewta.profile.enterprise.controller.self.EnterpriseTransferController;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationStatus;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.person.PersonIdentityLookupService;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityMatch;
import org.namewta.profile.enterprise.mapper.EnterpriseTransferMapper;
import org.namewta.profile.enterprise.support.EnterpriseMapperXmlTestSupport;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.MockedStatic;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "PROFILE_MYSQL_E2E_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "PROFILE_REDIS_E2E_ADDRESS", matches = ".+")
class EnterpriseTransferMySqlRedisE2ETest {

    private static final long SOURCE_USER = 976000000101L;
    private static final long TARGET_USER = 976000000102L;
    private static final long PERSON_PROFILE = 976000000201L;
    private static final long ENTERPRISE_PROFILE = 976000000301L;
    private static final long ENTERPRISE_BINDING = 976000000302L;
    private static final Instant NOW = Instant.now();

    private RedissonClient redis;
    private String challengeId;

    @AfterEach
    void cleanRedis() {
        if (redis == null) {
            return;
        }
        if (challengeId != null) {
            redis.getBucket(RedisEnterpriseTransferChallengeStore.challengeKey(challengeId)).delete();
        }
        redis.getBucket(RedisEnterpriseTransferChallengeStore.rateKey(SOURCE_USER, TARGET_USER)).delete();
        redis.shutdown();
    }

    @Test
    void currentResponsibleTransfersBySmsOnceThenTargetCanSelfUnbind() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            seed(session);
            session.commit();
            redis = redisClient();
            AtomicReference<NotificationCommand> delivery = new AtomicReference<>();
            AtomicLong currentUser = new AtomicLong(SOURCE_USER);
            MockMvc mvc = fixture(session, delivery);

            try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
                login.when(LoginHelper::getUserId).thenAnswer(ignored -> currentUser.get());

                MvcResult sent = mvc.perform(post("/profile/enterprise/transfer/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"张三\",\"documentLastFour\":\"9999\","
                            + "\"phone\":\"13800138000\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("QUEUED"))
                    .andExpect(jsonPath("$.data.expiresInSeconds").value(300))
                    .andReturn();
                challengeId = JsonMapper.builder().build().readTree(sent.getResponse().getContentAsString())
                    .path("data").path("challengeId").asText();
                assertThat(challengeId).isNotBlank();
                assertThat(delivery.get().metadata().get("audit")).isEqualTo("REDACT_SENSITIVE");
                assertThat(delivery.get().recipientIds()).containsExactly("13800138000");
                String code = String.valueOf(delivery.get().templateParams().get("code"));
                session.commit(); // 发码事务先提交，后续确认只能读取已持久化的关联。

                mvc.perform(post("/profile/enterprise/transfer/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeId\":\"" + challengeId + "\",\"code\":\"" + code + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("TRANSFERRED"));
                session.commit();

                assertThat(scalar(session, "select status from profile_enterprise_binding where "
                    + "enterprise_binding_id=" + ENTERPRISE_BINDING)).isEqualTo("UNBOUND");
                assertThat(scalar(session, "select count(*) from profile_enterprise_binding where "
                    + "enterprise_profile_id=" + ENTERPRISE_PROFILE + " and user_id=" + TARGET_USER
                    + " and status='ACTIVE'")).isEqualTo("1");
                assertThat(scalar(session, "select count(*) from profile_enterprise_binding_event where "
                    + "enterprise_profile_id=" + ENTERPRISE_PROFILE + " and event_type='UNBOUND'"
                    + " and source_type='SELF_TRANSFER'")).isEqualTo("1");
                assertThat(scalar(session, "select count(*) from profile_enterprise_binding_event where "
                    + "enterprise_profile_id=" + ENTERPRISE_PROFILE + " and user_id=" + TARGET_USER
                    + " and event_type='ACTIVE' and source_type='SELF_TRANSFER'")).isEqualTo("1");
                assertThat(scalar(session, "select status from profile_enterprise_transfer_record where "
                    + "challenge_id='" + challengeId + "'")).isEqualTo("CONFIRMED");

                mvc.perform(post("/profile/enterprise/transfer/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeId\":\"" + challengeId + "\",\"code\":\"" + code + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.msg").value("ENTERPRISE_TRANSFER_CHALLENGE_INVALID"));
                session.rollback();

                currentUser.set(TARGET_USER);
                mvc.perform(post("/profile/enterprise/transfer/unbind"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("UNBOUND"));
                session.commit();
                assertThat(scalar(session, "select count(*) from profile_enterprise_binding where "
                    + "enterprise_profile_id=" + ENTERPRISE_PROFILE + " and status='ACTIVE'")).isEqualTo("0");
                assertThat(scalar(session, "select count(*) from profile_enterprise_binding_event where "
                    + "enterprise_profile_id=" + ENTERPRISE_PROFILE + " and source_type='SELF_UNBIND'"))
                    .isEqualTo("1");
                assertThat(scalar(session, "select count(*) from profile_enterprise where "
                    + "enterprise_profile_id=" + ENTERPRISE_PROFILE + " and status='ACTIVE' and del_flag='0'"))
                    .isEqualTo("1");
            }
        }
    }

    private MockMvc fixture(SqlSession session, AtomicReference<NotificationCommand> delivery) {
        EnterpriseTransferMapper mapper = session.getMapper(EnterpriseTransferMapper.class);
        RedisEnterpriseTransferChallengeStore challenges = new RedisEnterpriseTransferChallengeStore(redis);
        EnterpriseTransferCodeGenerator codes = mock(EnterpriseTransferCodeGenerator.class);
        when(codes.generate()).thenReturn("123456");
        UserService users = mock(UserService.class);
        when(users.selectListByIds(any())).thenReturn(List.of(targetUser()));
        when(users.lockActiveById(TARGET_USER)).thenReturn(targetUser());
        PersonIdentityLookupService personIdentities = mock(PersonIdentityLookupService.class);
        ActiveIdentityMatch identity = new ActiveIdentityMatch(TARGET_USER, PERSON_PROFILE);
        when(personIdentities.findActiveExactMatches(any())).thenReturn(List.of(identity));
        when(personIdentities.lockActiveExactMatch(any())).thenReturn(java.util.Optional.of(identity));
        NotificationApplicationService notify = mock(NotificationApplicationService.class);
        when(notify.submit(any())).thenAnswer(invocation -> {
            NotificationCommand request = invocation.getArgument(0);
            delivery.set(request);
            return new NotificationReceipt("enterprise-transfer-challenge-1", NotificationStatus.QUEUED,
                true, false, List.of());
        });
        when(notify.query(any())).thenReturn(new org.namewta.notify.api.NotificationSnapshot(
            "enterprise-transfer-challenge-1", NotificationStatus.ACCEPTED, NOW, List.of()));
        EnterpriseTransferServiceImpl service = new EnterpriseTransferServiceImpl(mapper, challenges, codes,
            personIdentities, users, notify, Clock.fixed(NOW, ZoneOffset.UTC));
        return MockMvcBuilders.standaloneSetup(new EnterpriseTransferController(service))
            .setControllerAdvice(new EnterpriseTransferExceptionHandler()).build();
    }

    private UserDTO targetUser() {
        UserDTO user = new UserDTO();
        user.setUserId(TARGET_USER);
        user.setPhoneNumber("13800138000");
        user.setStatus("0");
        return user;
    }

    private void seed(SqlSession session) throws Exception {
        execute(session, "insert into profile_enterprise (enterprise_profile_id,enterprise_name,"
            + "unified_credit_code,enterprise_type,legal_representative_name,legal_document_type_code,"
            + "legal_document_number,established_date,registered_address,business_scope,status,version,"
            + "create_time,create_by,update_time,update_by,del_flag) values (" + ENTERPRISE_PROFILE
            + ",'转移测试企业','91310000TRANSFER001','COMPANY','李法','CN_RESIDENT_ID',"
            + "'110101198001011234','2010-01-01','上海市测试路1号','软件服务','ACTIVE',0,"
            + "current_timestamp,-1,current_timestamp,-1,'0')");
        execute(session, "insert into profile_enterprise_binding (enterprise_binding_id,enterprise_profile_id,"
            + "user_id,status,binding_version,source_type,source_id,bound_time,version,create_time,create_by,"
            + "update_time,update_by,del_flag) values (" + ENTERPRISE_BINDING + "," + ENTERPRISE_PROFILE + ","
            + SOURCE_USER + ",'ACTIVE',7,'USER_SUBMISSION',1,current_timestamp,0,current_timestamp,-1,"
            + "current_timestamp,-1,'0')");
        execute(session, "insert into profile_enterprise_binding_event (enterprise_binding_event_id,"
            + "enterprise_binding_id,enterprise_profile_id,user_id,event_type,binding_version,source_type,source_id,"
            + "reason,occurred_time,version,create_time,create_by,update_time,update_by,del_flag) values ("
            + (ENTERPRISE_BINDING + 1) + "," + ENTERPRISE_BINDING + "," + ENTERPRISE_PROFILE + "," + SOURCE_USER
            + ",'ACTIVE',7,'USER_SUBMISSION',1,'INITIAL',current_timestamp,0,current_timestamp,-1,"
            + "current_timestamp,-1,'0')");
    }

    private SqlSessionFactory sessionFactory() {
        Configuration configuration = new Configuration(new Environment("enterprise-transfer-e2e",
            new JdbcTransactionFactory(), new UnpooledDataSource("com.mysql.cj.jdbc.Driver",
            System.getenv("PROFILE_MYSQL_E2E_URL"), System.getenv("PROFILE_MYSQL_E2E_USERNAME"),
            System.getenv("PROFILE_MYSQL_E2E_PASSWORD"))));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(EnterpriseTransferMapper.class);
        EnterpriseMapperXmlTestSupport.parse(configuration, EnterpriseTransferMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private RedissonClient redisClient() {
        Config config = new Config();
        var server = config.useSingleServer().setAddress(System.getenv("PROFILE_REDIS_E2E_ADDRESS"));
        String password = System.getenv("PROFILE_REDIS_E2E_PASSWORD");
        if (password != null && !password.isBlank()) {
            server.setPassword(password);
        }
        return Redisson.create(config);
    }

    private void execute(SqlSession session, String sql) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private String scalar(SqlSession session, String sql) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getString(1);
        }
    }
}
