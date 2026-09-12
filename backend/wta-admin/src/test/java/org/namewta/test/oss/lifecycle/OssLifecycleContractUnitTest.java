package org.namewta.test.oss.lifecycle;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.namewta.system.api.OssService;
import org.namewta.system.controller.system.SysOssController;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OSS 生命周期公共合同测试。
 */
@Tag("dev")
class OssLifecycleContractUnitTest {

    @Test
    void publicApiExposesReconciliationAndAuthorizedPresignWithoutClientOwnership() throws Exception {
        Method reconcile = OssService.class.getMethod("reconcileReferences", String.class, String.class,
            java.util.Collection.class, java.util.Collection.class);
        Method snapshot = OssService.class.getMethod("snapshot", Long.class);
        Method presign = OssService.class.getMethod("presignDownload", Long.class);

        assertNotNull(reconcile);
        assertNotNull(snapshot);
        assertEquals(OssService.OssDownloadUrl.class, presign.getReturnType());
        assertTrue(Arrays.stream(OssService.class.getMethods())
            .noneMatch(method -> method.getName().equals("bind") || method.getName().equals("unbind")));
        assertTrue(Arrays.stream(OssService.class.getMethods())
            .flatMap(method -> Arrays.stream(method.getParameterTypes()))
            .noneMatch(type -> type.getSimpleName().toLowerCase().contains("client")));
    }

    @Test
    void managementDownloadUrlRequiresDedicatedPermission() throws Exception {
        Method method = SysOssController.class.getMethod("downloadUrl", Long.class);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

        assertNotNull(permission);
        assertArrayEquals(new String[]{"system:oss:download"}, permission.value());
    }

    @Test
    void mapperLocksLifecycleRowAndUsesIndexedTempPredicate() throws Exception {
        Path repository = repositoryRoot();
        String mapper = Files.readString(repository.resolve(
            "wta-modules/wta-system/src/main/resources/mapper/system/SysOssMapper.xml")).toLowerCase();

        assertTrue(mapper.contains("selectbyidforupdate"));
        assertTrue(mapper.contains("for update"));
        assertTrue(mapper.contains("is_temp = 'y'"));
        assertTrue(mapper.contains("expire_time"));
        assertFalse(mapper.contains("client_pk"));
    }

    @Test
    void referenceMapperUsesStoredPhysicalCoordinatesWithoutDynamicTableLookup() throws Exception {
        Path repository = repositoryRoot();
        String mapper = Files.readString(repository.resolve(
            "wta-modules/wta-system/src/main/resources/mapper/system/SysOssRefMapper.xml")).toLowerCase();

        assertTrue(mapper.contains("ref_type"));
        assertTrue(mapper.contains("ref_id"));
        assertTrue(mapper.contains("del_flag = '0'"));
        assertFalse(mapper.contains("${"));
        assertFalse(mapper.contains("client_pk"));
    }

    private Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir"));
        return current.getFileName().toString().equals("wta-admin") ? current.getParent() : current;
    }
}
