package org.namewta.profile.enterprise.service;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.profile.api.CompositeProfileDisclosureService;
import org.namewta.profile.api.domain.*;
import org.namewta.profile.enterprise.adapter.api.EnterpriseDisclosureContributor;
import org.namewta.profile.enterprise.dao.EnterpriseDisclosureDao;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseDisclosureRow;
import org.namewta.profile.enterprise.mapper.EnterpriseDisclosureMapper;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseDisclosureUseCaseImpl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("local")
@Tag("dev")
class EnterpriseDisclosureTest {
    @Test
    void fullChainPublishesOnlyApprovedFieldsEvenIfPersistenceReturnsMore() {
        EnterpriseDisclosureMapper mapper = mock(EnterpriseDisclosureMapper.class);
        EnterpriseDisclosureRow row = new EnterpriseDisclosureRow();
        row.setUserId(17L);
        row.setName("测试企业");
        row.setLegalDocumentNumber("123456789012345678");
        row.setLegalDocumentNumberMasked("**************5678");
        when(mapper.selectDisclosure(eq(17L), anySet())).thenReturn(row);
        var requested = Set.of(ProfileDisclosureField.ENTERPRISE_NAME,
            ProfileDisclosureField.ENTERPRISE_LEGAL_DOCUMENT_NUMBER_MASKED);
        var result = contributor(mapper).findByUserId(17L, requested).enterprise();
        assertThat(result.name()).isNotBlank();
        assertThat(result.legalDocumentNumberMasked()).isEqualTo("**************5678");
        assertThat(result.legalDocumentNumber()).isNull();
        assertThat(result.verified()).isNull();
        assertThat(result.profileId()).isNull();
        verify(mapper).selectDisclosure(17L, Set.of("ENTERPRISE_NAME", "ENTERPRISE_LEGAL_DOCUMENT_NUMBER_MASKED"));
    }

    @Test
    void completeDocumentNeedsIndependentExplicitField() {
        EnterpriseDisclosureMapper mapper = mock(EnterpriseDisclosureMapper.class);
        EnterpriseDisclosureRow row = new EnterpriseDisclosureRow();
        row.setUserId(17L);
        row.setLegalDocumentNumber("123456789012345678");
        when(mapper.selectDisclosure(eq(17L), anySet())).thenReturn(row);
        var result = contributor(mapper).findByUserId(17L,
            Set.of(ProfileDisclosureField.ENTERPRISE_LEGAL_DOCUMENT_NUMBER)).enterprise();
        assertThat(result.legalDocumentNumber()).isEqualTo("123456789012345678");
        assertThat(result.legalDocumentNumberMasked()).isNull();
    }

    @Test
    void absentActiveBindingReturnsOnlyApprovedFalseStatusAndEmptyRequestDoesNotRead() {
        EnterpriseDisclosureMapper mapper = mock(EnterpriseDisclosureMapper.class);
        var contributor = contributor(mapper);
        var result = contributor.findByUserId(17L,
            Set.of(ProfileDisclosureField.ENTERPRISE_VERIFIED, ProfileDisclosureField.ENTERPRISE_NAME)).enterprise();
        assertThat(result.verified()).isFalse();
        assertThat(result.name()).isNull();
        clearInvocations(mapper);
        assertThat(contributor.findByUserId(17L, Set.of()).enterprise()).isNull();
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsDifferentSubjectAndForeignFields() {
        EnterpriseDisclosureMapper mapper = mock(EnterpriseDisclosureMapper.class);
        EnterpriseDisclosureRow row = new EnterpriseDisclosureRow();
        row.setUserId(99L);
        when(mapper.selectDisclosure(eq(17L), anySet())).thenReturn(row);
        assertThatThrownBy(() -> contributor(mapper).findByUserId(17L, Set.of(ProfileDisclosureField.ENTERPRISE_VERIFIED)))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> contributor(mapper).findByUserId(17L,
            Set.of(ProfileDisclosureField.PERSON_VERIFIED)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actualMapperSqlMasksBeforeReadingAndRequiresCurrentActiveOwnership() throws Exception {
        String sql = boundSql(Set.of("ENTERPRISE_LEGAL_DOCUMENT_NUMBER_MASKED"));
        assertThat(sql).contains("null as legal_document_number")
            .contains("right(v.legal_document_number, 4)")
            .contains("b.user_id = ?")
            .contains("b.status = 'ACTIVE' and b.del_flag = '0'")
            .contains("p.status = 'ACTIVE' and p.del_flag = '0'")
            .contains("v.status = 'CURRENT' and v.del_flag = '0'");
        assertThat(sql).doesNotContain(", v.legal_document_number as legal_document_number");
        String full = boundSql(Set.of("ENTERPRISE_LEGAL_DOCUMENT_NUMBER"));
        assertThat(full).contains("v.legal_document_number as legal_document_number")
            .doesNotContain("right(");
    }

    @Test
    void compositeRejectsDuplicateContributorsAndInvalidSubjects() {
        var source = contributor(mock(EnterpriseDisclosureMapper.class));
        var composite = new CompositeProfileDisclosureService(java.util.List.of(source, source));
        assertThatThrownBy(() -> composite.findByUserId(17L, Set.of(ProfileDisclosureField.ENTERPRISE_VERIFIED)))
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
            "enterprise-disclosure-e2e", new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(), source));
        configuration.setMapUnderscoreToCamelCase(true);
        String resource = "mapper/enterprise/EnterpriseDisclosureMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        try (var session = new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(configuration).openSession(false)) {
            try {
                execute(session, "insert into profile_enterprise (enterprise_profile_id,current_version_id,enterprise_name,unified_credit_code,enterprise_type,legal_representative_name,legal_document_type_code,legal_document_number,established_date,business_term_from,business_term_until,registered_address,business_scope,contact_name,contact_phone,email,registered_capital,industry_code,website,status) values(987650001,987650002,'published-name','DISCLOSURE-E2E-001','published-type','published-legalRepresentativeName','published-legalDocumentType','123456789012345678','2020-01-02','2020-01-02','2020-01-02','published-registeredAddress','published-businessScope','published-contactName','published-contactPhone','published-email',100.25,'published-industryCode','published-website','ACTIVE')");
                execute(session, "insert into profile_enterprise_version (enterprise_version_id,enterprise_profile_id,version_no,source_type,source_id,enterprise_name,unified_credit_code,enterprise_type,legal_representative_name,legal_document_type_code,legal_document_number,established_date,business_term_from,business_term_until,registered_address,business_scope,contact_name,contact_phone,email,registered_capital,industry_code,website,status,published_time) values(987650002,987650001,1,'ADMIN_CREATE',987650003,'published-name','DISCLOSURE-E2E-001','published-type','published-legalRepresentativeName','published-legalDocumentType','123456789012345678','2020-01-02','2020-01-02','2020-01-02','published-registeredAddress','published-businessScope','published-contactName','published-contactPhone','published-email',100.25,'published-industryCode','published-website','CURRENT','2026-09-30 10:00:00')");
                execute(session, "insert into profile_enterprise_binding (enterprise_binding_id,enterprise_profile_id,user_id,status,bound_time) values(987650004,987650001,987650005,'ACTIVE','2026-09-30 10:00:00')");
                var sourceAdapter = contributor(session.getMapper(EnterpriseDisclosureMapper.class));
                Set<ProfileDisclosureField> all = java.util.Arrays.stream(ProfileDisclosureField.values())
                    .filter(field -> field.profileType() == ProfileType.ENTERPRISE)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
                var result = sourceAdapter.findByUserId(987650005L, all).enterprise();
                for (var component : result.getClass().getRecordComponents()) {
                    assertThat(component.getAccessor().invoke(result)).as(component.getName()).isNotNull();
                }
                assertThat(result.legalDocumentNumber()).isEqualTo("123456789012345678");
                assertThat(result.legalDocumentNumberMasked()).isEqualTo("**************5678");
                var masked = sourceAdapter.findByUserId(987650005L,
                    Set.of(ProfileDisclosureField.ENTERPRISE_LEGAL_DOCUMENT_NUMBER_MASKED)).enterprise();
                assertThat(masked.legalDocumentNumber()).isNull();
                assertThat(masked.legalDocumentNumberMasked()).isEqualTo("**************5678");
                assertThat(sourceAdapter.findByUserId(999999999L, all).enterprise().verified()).isFalse();

                for (String state : java.util.List.of("SUSPENDED", "UNBOUND")) {
                    execute(session, "update profile_enterprise_binding set status='" + state + "' where enterprise_binding_id=987650004");
                    assertThat(sourceAdapter.findByUserId(987650005L, all).enterprise().verified()).isFalse();
                }
                execute(session, "update profile_enterprise_binding set status='ACTIVE' where enterprise_binding_id=987650004");
                execute(session, "update profile_enterprise_version set status='SUPERSEDED' where enterprise_version_id=987650002");
                assertThat(sourceAdapter.findByUserId(987650005L, all).enterprise().verified()).isFalse();
                execute(session, "update profile_enterprise_version set status='CURRENT' where enterprise_version_id=987650002");
                execute(session, "update profile_enterprise set status='REVOKED' where enterprise_profile_id=987650001");
                assertThat(sourceAdapter.findByUserId(987650005L, all).enterprise().verified()).isFalse();
                execute(session, "update profile_enterprise set status='ACTIVE' where enterprise_profile_id=987650001");

                for (String table : java.util.List.of("profile_enterprise_binding", "profile_enterprise", "profile_enterprise_version")) {
                    execute(session, "update " + table + " set del_flag='1' where enterprise_profile_id=987650001");
                    assertThat(sourceAdapter.findByUserId(987650005L, all).enterprise().verified()).isFalse();
                    execute(session, "update " + table + " set del_flag='0' where enterprise_profile_id=987650001");
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

    private EnterpriseDisclosureContributor contributor(EnterpriseDisclosureMapper mapper) {
        return new EnterpriseDisclosureContributor(new EnterpriseDisclosureUseCaseImpl(
            new EnterpriseDisclosureService(new EnterpriseDisclosureDao(mapper))));
    }

    private String boundSql(Set<String> fields) throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/enterprise/EnterpriseDisclosureMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration.getMappedStatement("org.namewta.profile.enterprise.mapper.EnterpriseDisclosureMapper.selectDisclosure")
            .getBoundSql(Map.of("userId", 17L, "fields", fields)).getSql().replaceAll("\\s+", " ").trim();
    }
}
