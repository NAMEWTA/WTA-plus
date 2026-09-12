package org.namewta.test.profile.contract;

import org.namewta.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.ResolvedMaterialOwner;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.SnapshotRelationship;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.api.person.PersonIdentityLookupService;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityLock;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityMatch;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityQuery;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.namewta.system.domain.SysUser;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class ProfileCrossModuleBoundaryContractTest {

    @Test
    void activeUserLockingIsCompatibleAndFailsClosedByDefault() throws Exception {
        Method method = UserService.class.getMethod("lockActiveById", Long.class);
        UserService legacyImplementation = mock(UserService.class, CALLS_REAL_METHODS);

        assertThat(method.isDefault()).isTrue();
        assertThat(method.getReturnType()).isEqualTo(UserDTO.class);
        assertThatThrownBy(() -> legacyImplementation.lockActiveById(7L))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("locking is not supported");
    }

    @Test
    void systemUserLockingUsesActiveRowFilterAndDatabaseLock() throws Exception {
        SysUserMapper mapper = mock(SysUserMapper.class);
        @SuppressWarnings("unchecked")
        LambdaCrudChainWrapper<SysUser, SysUserVo> chain = mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(mapper.lambda()).thenReturn(chain);
        when(chain.eq(any(), any())).thenReturn(chain);
        when(chain.last("for update")).thenReturn(chain);
        SysUserVo active = new SysUserVo();
        active.setUserId(7L);
        active.setPhoneNumber("13800000000");
        active.setStatus("0");
        when(chain.voOne()).thenReturn(active);
        SysUserServiceImpl service = userService(mapper);

        UserDTO result = service.lockActiveById(7L);

        assertThat(result.getUserId()).isEqualTo(7L);
        assertThat(result.getPhoneNumber()).isEqualTo("13800000000");
        verify(chain).last("for update");
        assertThatThrownBy(() -> service.lockActiveById(0L))
            .isInstanceOf(IllegalArgumentException.class);

        String source = Files.readString(backendRoot().resolve(
            "wta-modules/wta-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java"));
        assertThat(source)
            .contains("public UserDTO lockActiveById(Long userId)")
            .contains(".eq(SysUser::getUserId, userId)")
            .contains(".eq(SysUser::getStatus, SystemConstants.NORMAL)")
            .contains(".eq(SysUser::getDelFlag, SystemConstants.NORMAL)")
            .contains(".last(\"for update\")");
    }

    @Test
    void personIdentityLookupExposesOnlyNonSensitiveIds() throws Exception {
        Method find = PersonIdentityLookupService.class.getMethod(
            "findActiveExactMatches", ActiveIdentityQuery.class);
        Method lock = PersonIdentityLookupService.class.getMethod(
            "lockActiveExactMatch", ActiveIdentityLock.class);

        assertThat(find.getReturnType()).isEqualTo(List.class);
        assertThat(lock.getReturnType()).isEqualTo(Optional.class);
        assertThat(singleTypeArgument(find)).isEqualTo(ActiveIdentityMatch.class.getName());
        assertThat(singleTypeArgument(lock)).isEqualTo(ActiveIdentityMatch.class.getName());
        assertThat(componentNames(ActiveIdentityMatch.class))
            .containsExactly("userId", "personProfileId");
        assertThat(componentNames(ActiveIdentityMatch.class).toString().toLowerCase())
            .doesNotContain("name", "document", "identity", "phone", "mobile");
    }

    @Test
    void personIdentityLookupInputsAreNormalizedAndValidated() {
        ActiveIdentityQuery query = new ActiveIdentityQuery(" 张三 ", " a12z ");
        ActiveIdentityLock lock = new ActiveIdentityLock(7L, 19L, " 张三 ", " a12z ");
        ActiveIdentityMatch match = new ActiveIdentityMatch(7L, 19L);

        assertThat(query.fullName()).isEqualTo("张三");
        assertThat(query.documentLastFour()).isEqualTo("A12Z");
        assertThat(lock.fullName()).isEqualTo("张三");
        assertThat(lock.documentLastFour()).isEqualTo("A12Z");
        assertThat(match.userId()).isEqualTo(7L);
        assertThatThrownBy(() -> new ActiveIdentityQuery(" ", "1234"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActiveIdentityQuery("张三", "123"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActiveIdentityLock(0L, 19L, "张三", "1234"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActiveIdentityMatch(7L, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void materialOwnerContributorUsesSharedProfileAndOwnerTypes() throws Exception {
        Method profileType = ProfileMaterialOwnerContributor.class.getMethod("profileType");
        Method lock = ProfileMaterialOwnerContributor.class.getMethod("lockOwner", MaterialOwnerKey.class);
        Method editable = ProfileMaterialOwnerContributor.class.getMethod(
            "isWorkingEditable", MaterialOwnerKey.class);
        Method relationship = ProfileMaterialOwnerContributor.class.getMethod(
            "hasSnapshotRelationship", SnapshotRelationship.class);

        assertThat(profileType.getReturnType()).isEqualTo(ProfileType.class);
        assertThat(lock.getReturnType()).isEqualTo(Optional.class);
        assertThat(singleTypeArgument(lock)).isEqualTo(ResolvedMaterialOwner.class.getName());
        assertThat(editable.getReturnType()).isEqualTo(boolean.class);
        assertThat(relationship.getReturnType()).isEqualTo(boolean.class);
        assertThat(ProfileMaterialPort.class.getDeclaredMethods())
            .noneMatch(method -> method.getName().equals("lockOwner")
                || method.getName().equals("hasSnapshotRelationship"));
    }

    @Test
    void materialOwnerValuesRejectInvalidIdsAndSnapshotTransitions() {
        MaterialOwnerKey working = owner(ProfileType.PERSON, MaterialOwnerType.WORKING, 11L);
        MaterialOwnerKey submission = owner(ProfileType.PERSON, MaterialOwnerType.SUBMISSION, 12L);
        MaterialOwnerKey source = owner(ProfileType.PERSON, MaterialOwnerType.SOURCE, 13L);
        MaterialOwnerKey version = owner(ProfileType.PERSON, MaterialOwnerType.VERSION, 14L);

        assertThat(new ResolvedMaterialOwner(working, 7L).applicantUserId()).isEqualTo(7L);
        assertThat(new SnapshotRelationship(working, submission).source()).isEqualTo(working);
        assertThat(new SnapshotRelationship(submission, version).target()).isEqualTo(version);
        assertThat(new SnapshotRelationship(source, version).source()).isEqualTo(source);
        assertThatThrownBy(() -> new ResolvedMaterialOwner(working, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResolvedMaterialOwner(version, 0L))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SnapshotRelationship(submission, working))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SnapshotRelationship(working,
            owner(ProfileType.ENTERPRISE, MaterialOwnerType.SUBMISSION, 12L)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static MaterialOwnerKey owner(ProfileType profileType, MaterialOwnerType ownerType, Long ownerId) {
        return new MaterialOwnerKey(profileType, ownerType, ownerId);
    }

    private static List<String> componentNames(Class<?> recordType) {
        return List.of(recordType.getRecordComponents()).stream()
            .map(RecordComponent::getName)
            .toList();
    }

    private static String singleTypeArgument(Method method) {
        Type type = method.getGenericReturnType();
        String typeName = type.getTypeName();
        return typeName.substring(typeName.indexOf('<') + 1, typeName.lastIndexOf('>'));
    }

    private static SysUserServiceImpl userService(SysUserMapper mapper) {
        return new SysUserServiceImpl(mapper, null, null, null, null, null, null, null, null, null, null);
    }

    private static Path backendRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                && Files.isDirectory(current.resolve("wta-admin"))
                && Files.isDirectory(current.resolve("wta-modules"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("backend repository root not found");
    }
}
