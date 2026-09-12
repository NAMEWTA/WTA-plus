package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.adapter.api.PersonIdentityLookupServiceImpl;
import org.namewta.profile.person.usecase.impl.PersonProfileApiUseCaseImpl;
import org.namewta.profile.person.service.PersonProfileApiService;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityLock;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityMatch;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityQuery;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.person.support.PersonMapperXmlTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "PROFILE_MYSQL_E2E_URL", matches = ".+")
class PersonIdentityLookupMySqlE2ETest {

    private static final long FIRST_PROFILE_ID = 974000000001L;
    private static final long SECOND_PROFILE_ID = 974000000002L;
    private static final long REVOKED_PROFILE_ID = 974000000003L;
    private static final long SUSPENDED_PROFILE_ID = 974000000004L;
    private static final long FIRST_USER_ID = 975000000001L;
    private static final long SECOND_USER_ID = 975000000002L;
    private static final long REVOKED_USER_ID = 975000000003L;
    private static final long SUSPENDED_USER_ID = 975000000004L;
    private static final String FULL_NAME = "Identity Lookup E2E";

    @Test
    void loadsProductionXmlAndEnforcesActiveIdentityAndCandidatePair() throws SQLException {
        try (SqlSession session = sessionFactory().openSession(false)) {
            try {
                insertProfile(session, FIRST_PROFILE_ID, "ID-FIRST-abCd   ", "ACTIVE", "E2E:FIRST");
                insertProfile(session, SECOND_PROFILE_ID, "ID-SECOND-ABCD ", "ACTIVE", "E2E:SECOND");
                insertProfile(session, REVOKED_PROFILE_ID, "ID-REVOKED-AbcD ", "REVOKED", "E2E:REVOKED");
                insertProfile(session, SUSPENDED_PROFILE_ID, "ID-SUSPENDED-aBcD   ", "ACTIVE", "E2E:SUSPENDED");

                insertBinding(session, 975100000001L, FIRST_PROFILE_ID, FIRST_USER_ID, "ACTIVE");
                insertBinding(session, 975100000002L, SECOND_PROFILE_ID, SECOND_USER_ID, "ACTIVE");
                insertBinding(session, 975100000003L, REVOKED_PROFILE_ID, REVOKED_USER_ID, "ACTIVE");
                insertBinding(session, 975100000004L, SUSPENDED_PROFILE_ID, SUSPENDED_USER_ID, "SUSPENDED");

                PersonIdentityLookupServiceImpl service = new PersonIdentityLookupServiceImpl(
                    new PersonProfileApiUseCaseImpl(new PersonProfileApiService(
                        new PersonApplicationDao(session.getMapper(PersonApplicationMapper.class)))));

                assertThat(service.findActiveExactMatches(new ActiveIdentityQuery("  " + FULL_NAME + "  ", "abcd")))
                    .containsExactly(
                        new ActiveIdentityMatch(FIRST_USER_ID, FIRST_PROFILE_ID),
                        new ActiveIdentityMatch(SECOND_USER_ID, SECOND_PROFILE_ID));

                ActiveIdentityLock exact = new ActiveIdentityLock(
                    FIRST_USER_ID, FIRST_PROFILE_ID, FULL_NAME, "abcd");
                assertThat(service.lockActiveExactMatch(exact))
                    .contains(new ActiveIdentityMatch(FIRST_USER_ID, FIRST_PROFILE_ID));
                assertThat(service.lockActiveExactMatch(new ActiveIdentityLock(
                    FIRST_USER_ID, SECOND_PROFILE_ID, FULL_NAME, "ABCD"))).isEmpty();
                assertThat(service.lockActiveExactMatch(new ActiveIdentityLock(
                    SECOND_USER_ID, FIRST_PROFILE_ID, FULL_NAME, "ABCD"))).isEmpty();
                assertThat(service.lockActiveExactMatch(new ActiveIdentityLock(
                    REVOKED_USER_ID, REVOKED_PROFILE_ID, FULL_NAME, "ABCD"))).isEmpty();
                assertThat(service.lockActiveExactMatch(new ActiveIdentityLock(
                    SUSPENDED_USER_ID, SUSPENDED_PROFILE_ID, FULL_NAME, "ABCD"))).isEmpty();
            } finally {
                session.rollback(true);
            }
        }
    }

    private void insertProfile(SqlSession session,
                               long profileId,
                               String documentNumber,
                               String status,
                               String identityKey) throws SQLException {
        String sql = """
            insert into profile_person (
                person_profile_id, full_name, document_type_code, document_number,
                identity_key, gender, birth_date, status, version, create_dept,
                create_time, create_by, update_time, update_by, del_flag
            ) values (?, ?, 'E2E_DOCUMENT', ?, ?, 'UNKNOWN', '1990-01-01', ?, 0, -1,
                      current_timestamp, -1, current_timestamp, -1, '0')
            """;
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
            statement.setLong(1, profileId);
            statement.setString(2, FULL_NAME);
            statement.setString(3, documentNumber);
            statement.setString(4, identityKey);
            statement.setString(5, status);
            statement.executeUpdate();
        }
    }

    private void insertBinding(SqlSession session,
                               long bindingId,
                               long profileId,
                               long userId,
                               String status) throws SQLException {
        String sql = """
            insert into profile_person_binding (
                person_binding_id, person_profile_id, user_id, status, binding_version,
                source_type, source_id, bound_time, version, create_dept, create_time,
                create_by, update_time, update_by, del_flag
            ) values (?, ?, ?, ?, 1, 'ADMIN_CREATE', ?, current_timestamp, 0, -1,
                      current_timestamp, -1, current_timestamp, -1, '0')
            """;
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
            statement.setLong(1, bindingId);
            statement.setLong(2, profileId);
            statement.setLong(3, userId);
            statement.setString(4, status);
            statement.setLong(5, profileId);
            statement.executeUpdate();
        }
    }

    private SqlSessionFactory sessionFactory() {
        Configuration configuration = new Configuration(new Environment(
            "profile-person-identity-lookup-e2e", new JdbcTransactionFactory(), dataSource()));
        configuration.setMapUnderscoreToCamelCase(true);
        PersonMapperXmlTestSupport.parse(configuration, PersonApplicationMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private UnpooledDataSource dataSource() {
        return new UnpooledDataSource("com.mysql.cj.jdbc.Driver",
            System.getenv("PROFILE_MYSQL_E2E_URL"), System.getenv("PROFILE_MYSQL_E2E_USERNAME"),
            System.getenv("PROFILE_MYSQL_E2E_PASSWORD"));
    }
}
