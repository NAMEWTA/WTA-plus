package org.namewta.profile.person.service;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.profile.api.CompositeProfileDisclosureService;
import org.namewta.profile.api.domain.*;
import org.namewta.profile.person.adapter.api.PersonDisclosureContributor;
import org.namewta.profile.person.dao.PersonDisclosureDao;
import org.namewta.profile.person.domain.model.read.PersonDisclosureRow;
import org.namewta.profile.person.mapper.PersonDisclosureMapper;
import org.namewta.profile.person.usecase.impl.PersonDisclosureUseCaseImpl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("local")
@Tag("dev")
class PersonDisclosureTest {
    @Test
    void fullChainPublishesOnlyApprovedFieldsEvenIfPersistenceReturnsMore() {
        PersonDisclosureMapper mapper = mock(PersonDisclosureMapper.class);
        PersonDisclosureRow row = new PersonDisclosureRow();
        row.setUserId(17L);
        row.setFullName("测试姓名");
        row.setDocumentNumber("123456789012345678");
        row.setDocumentNumberMasked("**************5678");
        when(mapper.selectDisclosure(eq(17L), anySet())).thenReturn(row);
        var requested = Set.of(ProfileDisclosureField.PERSON_FULL_NAME,
            ProfileDisclosureField.PERSON_DOCUMENT_NUMBER_MASKED);
        var result = contributor(mapper).findByUserId(17L, requested).person();
        assertThat(result.fullName()).isNotBlank();
        assertThat(result.documentNumberMasked()).isEqualTo("**************5678");
        assertThat(result.documentNumber()).isNull();
        assertThat(result.verified()).isNull();
        assertThat(result.profileId()).isNull();
        verify(mapper).selectDisclosure(17L, Set.of("PERSON_FULL_NAME", "PERSON_DOCUMENT_NUMBER_MASKED"));
    }

    @Test
    void completeDocumentNeedsIndependentExplicitField() {
        PersonDisclosureMapper mapper = mock(PersonDisclosureMapper.class);
        PersonDisclosureRow row = new PersonDisclosureRow();
        row.setUserId(17L);
        row.setDocumentNumber("123456789012345678");
        when(mapper.selectDisclosure(eq(17L), anySet())).thenReturn(row);
        var result = contributor(mapper).findByUserId(17L,
            Set.of(ProfileDisclosureField.PERSON_DOCUMENT_NUMBER)).person();
        assertThat(result.documentNumber()).isEqualTo("123456789012345678");
        assertThat(result.documentNumberMasked()).isNull();
    }

    @Test
    void absentActiveBindingReturnsOnlyApprovedFalseStatusAndEmptyRequestDoesNotRead() {
        PersonDisclosureMapper mapper = mock(PersonDisclosureMapper.class);
        var contributor = contributor(mapper);
        var result = contributor.findByUserId(17L,
            Set.of(ProfileDisclosureField.PERSON_VERIFIED, ProfileDisclosureField.PERSON_FULL_NAME)).person();
        assertThat(result.verified()).isFalse();
        assertThat(result.fullName()).isNull();
        clearInvocations(mapper);
        assertThat(contributor.findByUserId(17L, Set.of()).person()).isNull();
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsDifferentSubjectAndForeignFields() {
        PersonDisclosureMapper mapper = mock(PersonDisclosureMapper.class);
        PersonDisclosureRow row = new PersonDisclosureRow();
        row.setUserId(99L);
        when(mapper.selectDisclosure(eq(17L), anySet())).thenReturn(row);
        assertThatThrownBy(() -> contributor(mapper).findByUserId(17L, Set.of(ProfileDisclosureField.PERSON_VERIFIED)))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> contributor(mapper).findByUserId(17L,
            Set.of(ProfileDisclosureField.ENTERPRISE_VERIFIED)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actualMapperSqlMasksBeforeReadingAndRequiresCurrentActiveOwnership() throws Exception {
        String sql = boundSql(Set.of("PERSON_DOCUMENT_NUMBER_MASKED"));
        assertThat(sql).contains("null as document_number")
            .contains("right(v.document_number, 4)")
            .contains("b.user_id = ?")
            .contains("b.status = 'ACTIVE' and b.del_flag = '0'")
            .contains("p.status = 'ACTIVE' and p.del_flag = '0'")
            .contains("v.status = 'CURRENT' and v.del_flag = '0'");
        assertThat(sql).doesNotContain(", v.document_number as document_number");
        String full = boundSql(Set.of("PERSON_DOCUMENT_NUMBER"));
        assertThat(full).contains("v.document_number as document_number")
            .doesNotContain("right(");
    }

    @Test
    void compositeRejectsDuplicateContributorsAndInvalidSubjects() {
        var source = contributor(mock(PersonDisclosureMapper.class));
        var composite = new CompositeProfileDisclosureService(java.util.List.of(source, source));
        assertThatThrownBy(() -> composite.findByUserId(17L, Set.of(ProfileDisclosureField.PERSON_VERIFIED)))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> composite.findByUserId(0L, Set.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "PROFILE_MYSQL_E2E_URL", matches = ".+")
    void realMysqlFullChainRejectsInactiveHistoryAndEnforcesEachField() throws Exception {
        var source = new org.apache.ibatis.datasource.unpooled.UnpooledDataSource("com.mysql.cj.jdbc.Driver",
            System.getenv("PROFILE_MYSQL_E2E_URL"), System.getenv("PROFILE_MYSQL_E2E_USERNAME"),
            System.getenv("PROFILE_MYSQL_E2E_PASSWORD"));
        Configuration configuration = new Configuration(new org.apache.ibatis.mapping.Environment(
            "person-disclosure-e2e", new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(), source));
        configuration.setMapUnderscoreToCamelCase(true);
        String resource = "mapper/person/PersonDisclosureMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        try (var session = new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(configuration).openSession(false)) {
            try {
                execute(session, "insert into profile_person (person_profile_id,current_version_id,full_name,gender,birth_date,document_type_code,document_number,valid_from,valid_until,identity_key,status) values(987650001,987650002,'published-fullName','published-gender','2020-01-02','published-documentType','123456789012345678','2020-01-02','2020-01-02','DISCLOSURE:E2E:PERSON','ACTIVE')");
                execute(session, "insert into profile_person_version (person_version_id,person_profile_id,version_no,source_type,source_id,full_name,gender,birth_date,document_type_code,document_number,valid_from,valid_until,identity_key,status,published_time) values(987650002,987650001,1,'ADMIN_CREATE',987650003,'published-fullName','published-gender','2020-01-02','published-documentType','123456789012345678','2020-01-02','2020-01-02','DISCLOSURE:E2E:PERSON','CURRENT','2026-09-30 10:00:00')");
                execute(session, "insert into profile_person_binding (person_binding_id,person_profile_id,user_id,status,bound_time) values(987650004,987650001,987650005,'ACTIVE','2026-09-30 10:00:00')");
                var sourceAdapter = contributor(session.getMapper(PersonDisclosureMapper.class));
                Set<ProfileDisclosureField> all = java.util.Arrays.stream(ProfileDisclosureField.values())
                    .filter(field -> field.profileType() == ProfileType.PERSON)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
                var result = sourceAdapter.findByUserId(987650005L, all).person();
                for (var component : result.getClass().getRecordComponents()) {
                    assertThat(component.getAccessor().invoke(result)).as(component.getName()).isNotNull();
                }
                assertThat(result.documentNumber()).isEqualTo("123456789012345678");
                assertThat(result.documentNumberMasked()).isEqualTo("**************5678");
                var masked = sourceAdapter.findByUserId(987650005L,
                    Set.of(ProfileDisclosureField.PERSON_DOCUMENT_NUMBER_MASKED)).person();
                assertThat(masked.documentNumber()).isNull();
                assertThat(masked.documentNumberMasked()).isEqualTo("**************5678");
                assertThat(sourceAdapter.findByUserId(999999999L, all).person().verified()).isFalse();

                for (String state : java.util.List.of("SUSPENDED", "UNBOUND")) {
                    execute(session, "update profile_person_binding set status='" + state + "' where person_binding_id=987650004");
                    assertThat(sourceAdapter.findByUserId(987650005L, all).person().verified()).isFalse();
                }
                execute(session, "update profile_person_binding set status='ACTIVE' where person_binding_id=987650004");
                execute(session, "update profile_person_version set status='SUPERSEDED' where person_version_id=987650002");
                assertThat(sourceAdapter.findByUserId(987650005L, all).person().verified()).isFalse();
                execute(session, "update profile_person_version set status='CURRENT' where person_version_id=987650002");
                execute(session, "update profile_person set status='REVOKED' where person_profile_id=987650001");
                assertThat(sourceAdapter.findByUserId(987650005L, all).person().verified()).isFalse();
                execute(session, "update profile_person set status='ACTIVE' where person_profile_id=987650001");

                for (String table : java.util.List.of("profile_person_binding", "profile_person", "profile_person_version")) {
                    execute(session, "update " + table + " set del_flag='1' where person_profile_id=987650001");
                    assertThat(sourceAdapter.findByUserId(987650005L, all).person().verified()).isFalse();
                    execute(session, "update " + table + " set del_flag='0' where person_profile_id=987650001");
                }
            } finally {
                session.rollback(true);
            }
        }
    }

    private void execute(org.apache.ibatis.session.SqlSession session, String sql) throws Exception {
        try (var statement = session.getConnection().createStatement()) {
            statement.executeUpdate(sql);
        }
        session.clearCache();
    }

    private PersonDisclosureContributor contributor(PersonDisclosureMapper mapper) {
        return new PersonDisclosureContributor(new PersonDisclosureUseCaseImpl(
            new PersonDisclosureService(new PersonDisclosureDao(mapper))));
    }

    private String boundSql(Set<String> fields) throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/person/PersonDisclosureMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration.getMappedStatement("org.namewta.profile.person.mapper.PersonDisclosureMapper.selectDisclosure")
            .getBoundSql(Map.of("userId", 17L, "fields", fields)).getSql().replaceAll("\\s+", " ").trim();
    }
}
