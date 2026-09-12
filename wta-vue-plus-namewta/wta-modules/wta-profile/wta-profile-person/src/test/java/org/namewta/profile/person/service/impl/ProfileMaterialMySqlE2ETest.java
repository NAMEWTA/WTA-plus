package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.adapter.api.PersonProfileMaterialOwnerContributor;
import org.namewta.profile.person.usecase.impl.PersonProfileApiUseCaseImpl;
import org.namewta.profile.person.service.PersonProfileApiService;

import org.namewta.profile.person.controller.admin.ProfileMaterialExceptionHandler;
import org.namewta.profile.person.controller.admin.PersonMaterialAdminController;
import org.namewta.profile.person.controller.self.PersonMaterialSelfController;
import org.namewta.profile.person.domain.exception.ProfileMaterialException;
import org.namewta.profile.person.port.security.ProfileMaterialAccessPolicy;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.person.mapper.ProfileMaterialMapper;
import org.namewta.profile.person.support.PersonMapperXmlTestSupport;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.system.api.OssService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "PROFILE_MYSQL_E2E_URL", matches = ".+")
class ProfileMaterialMySqlE2ETest {

    private static final long APPLICATION_ID = 940000000001L;
    private static final long SUBMISSION_ID = 940000000002L;
    private static final long USER_ID = 940000000003L;
    private static final long OSS_ID = 940000000004L;
    private static final long PORTRAIT_TAG_ID = 2100200000000000101L;

    @Test
    void persistsMaterialLifecycleAndKeepsAuthorizationAheadOfDownloadResolution() throws Exception {
        try (SqlSession session = sessionFactory().openSession(false)) {
            seedOwners(session);
            ProfileMaterialMapper mapper = session.getMapper(ProfileMaterialMapper.class);
            OssService ossService = mock(OssService.class);
            ProfileMaterialAccessPolicy accessPolicy = mock(ProfileMaterialAccessPolicy.class);
            when(accessPolicy.requireAttach(any())).thenReturn(USER_ID);
            Clock clock = Clock.fixed(Instant.parse("2026-09-01T13:00:00Z"), ZoneOffset.UTC);
            PersonProfileApiUseCaseImpl apiUseCase = new PersonProfileApiUseCaseImpl(
                new PersonProfileApiService(new PersonApplicationDao(session.getMapper(PersonApplicationMapper.class))));
            PersonProfileMaterialOwnerContributor ownerContributor = new PersonProfileMaterialOwnerContributor(apiUseCase);
            ProfileMaterialServiceImpl service = new ProfileMaterialServiceImpl(
                mapper, ossService, accessPolicy, java.util.List.of(ownerContributor), clock);
            when(ossService.objectMetadata(OSS_ID)).thenReturn(new OssService.OssObjectMetadata(
                OSS_ID, "profile/e2e/front.jpg", "front.jpg", ".jpg", 1024, "image/jpeg", USER_ID));

            MockMvc mvc = MockMvcBuilders.standaloneSetup(
                    new PersonMaterialSelfController(service), new PersonMaterialAdminController(service))
                .setControllerAdvice(new ProfileMaterialExceptionHandler()).build();
            mvc.perform(post("/profile/person/materials/WORKING/{ownerId}", APPLICATION_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ossId\":" + OSS_ID + ",\"materialNodeId\":" + PORTRAIT_TAG_ID + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ossId").value(OSS_ID))
                .andExpect(jsonPath("$.data.materialTagCode").value("PERSON_ID_CARD_PORTRAIT"));
            session.commit();

            long workingRefId = Long.parseLong(scalar(session,
                "select material_ref_id from profile_material_ref where owner_type = 'WORKING' and owner_id = "
                    + APPLICATION_ID + " and oss_id = " + OSS_ID));
            verify(ossService).reconcileReferences("profile_material_ref", String.valueOf(workingRefId),
                Set.of(), Set.of(OSS_ID));
            assertThatThrownBy(() -> service.validateRequired(
                working(), "CN_RESIDENT_ID", Set.of("ALWAYS")))
                .hasMessageContaining("MISSING_REQUIRED_MATERIAL:PERSON_ID_CARD_EMBLEM");

            service.snapshotImmutable(working(), new MaterialOwnerKey(
                ProfileType.PERSON, MaterialOwnerType.SUBMISSION, SUBMISSION_ID));
            session.commit();
            long immutableRefId = Long.parseLong(scalar(session,
                "select material_ref_id from profile_material_ref where owner_type = 'SUBMISSION' and owner_id = "
                    + SUBMISSION_ID));
            org.junit.jupiter.api.Assertions.assertEquals("Y", scalar(session,
                "select immutable_flag from profile_material_ref where material_ref_id = " + immutableRefId));

            when(ossService.resolveAccessUrl(OSS_ID)).thenReturn(new OssService.OssAccessUrl(
                "PRIVATE", "https://storage.example.test/signed", Instant.parse("2026-09-01T13:02:00Z"), "front.jpg"));
            mvc.perform(get("/profile/person/materials/SUBMISSION/{ownerId}/{refId}/access-url",
                    SUBMISSION_ID, immutableRefId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessType").value("PRIVATE"))
                .andExpect(jsonPath("$.data.url").value("https://storage.example.test/signed"));

            service.detach(working(), workingRefId);
            session.commit();
            org.junit.jupiter.api.Assertions.assertEquals("DETACHED", scalar(session,
                "select status from profile_material_ref where material_ref_id = " + workingRefId));
            org.junit.jupiter.api.Assertions.assertEquals("ATTACHED", scalar(session,
                "select status from profile_material_ref where material_ref_id = " + immutableRefId));
            org.junit.jupiter.api.Assertions.assertEquals("2", scalar(session,
                "select count(*) from profile_material_ref where oss_id = " + OSS_ID));
            verify(ossService).reconcileReferences("profile_material_ref", String.valueOf(workingRefId),
                Set.of(OSS_ID), Set.of());

            doThrow(new ProfileMaterialException("MATERIAL_ACCESS_DENIED"))
                .when(accessPolicy).requireRead(any());
            mvc.perform(get("/profile/person/materials/SUBMISSION/{ownerId}/{refId}/access-url",
                    SUBMISSION_ID, immutableRefId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.msg").value("MATERIAL_ACCESS_DENIED"));
            verify(ossService, never()).presignDownload(eq(OSS_ID));
            verify(ossService, times(1)).resolveAccessUrl(OSS_ID);
        }
    }

    private SqlSessionFactory sessionFactory() {
        UnpooledDataSource dataSource = new UnpooledDataSource(
            "com.mysql.cj.jdbc.Driver",
            System.getenv("PROFILE_MYSQL_E2E_URL"),
            System.getenv("PROFILE_MYSQL_E2E_USERNAME"),
            System.getenv("PROFILE_MYSQL_E2E_PASSWORD"));
        Configuration configuration = new Configuration(new Environment(
            "profile-material-e2e", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        PersonMapperXmlTestSupport.parse(configuration, ProfileMaterialMapper.class);
        PersonMapperXmlTestSupport.parse(configuration, PersonApplicationMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private void seedOwners(SqlSession session) throws Exception {
        try (PreparedStatement application = session.getConnection().prepareStatement("""
            insert into profile_person_application (
                person_application_id, applicant_user_id, status, provider_code, submission_seq,
                version, create_time, update_time, del_flag
            ) values (?, ?, 'DRAFT', 'manual', 1, 0, current_timestamp, current_timestamp, '0')
            """)) {
            application.setLong(1, APPLICATION_ID);
            application.setLong(2, USER_ID);
            application.executeUpdate();
        }
        try (PreparedStatement submission = session.getConnection().prepareStatement("""
            insert into profile_person_submission (
                person_submission_id, person_application_id, submission_seq, full_name,
                document_type_code, document_number, identity_key, gender, birth_date,
                provider_code, field_snapshot_json, submitted_time, version,
                create_time, update_time, del_flag
            ) values (?, ?, 1, 'Material E2E', 'CN_RESIDENT_ID', '110101199001011234',
                'CN_RESIDENT_ID:110101199001011234', 'MALE', '1990-01-01', 'manual',
                '{}', current_timestamp, 0, current_timestamp, current_timestamp, '0')
            """)) {
            submission.setLong(1, SUBMISSION_ID);
            submission.setLong(2, APPLICATION_ID);
            submission.executeUpdate();
        }
        session.commit();
    }

    private MaterialOwnerKey working() {
        return new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.WORKING, APPLICATION_ID);
    }

    private String scalar(SqlSession session, String sql) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getString(1);
        }
    }
}
