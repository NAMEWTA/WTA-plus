package org.namewta.test.oss.access;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.api.OssService;
import org.namewta.system.domain.SysOss;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.service.OssLifecycleManager;
import org.namewta.system.service.impl.SysOssServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class OssObjectMetadataServiceUnitTest {

    @Test
    void metadataContractIsAnAdditiveDefaultMethodForExistingImplementations() throws Exception {
        Method method = OssService.class.getMethod("objectMetadata", Long.class);
        assertThat(method.isDefault()).isTrue();
        assertThat(method.getReturnType()).isEqualTo(OssService.OssObjectMetadata.class);
    }

    @Test
    void exposesAuthoritativePersistedUploadMetadataWithoutResolvingAUrl() {
        SysOssMapper mapper = mock(SysOssMapper.class);
        OssLifecycleManager lifecycle = mock(OssLifecycleManager.class);
        SysOssServiceImpl service = new SysOssServiceImpl(mapper, lifecycle);
        SysOss oss = oss(7L, "identity/front.JPG",
            "{\"fileSize\":1048576,\"contentType\":\"image/jpeg\"}");
        when(mapper.selectById(7L)).thenReturn(oss);

        OssService.OssObjectMetadata result = service.objectMetadata(7L);

        assertThat(result).isEqualTo(new OssService.OssObjectMetadata(
            7L, "identity/front.JPG", "front.JPG", ".JPG", 1048576L, "image/jpeg", 77L));
    }

    @Test
    void failsClosedWhenObjectOrAuthoritativeMetadataIsMissing() {
        SysOssMapper mapper = mock(SysOssMapper.class);
        OssLifecycleManager lifecycle = mock(OssLifecycleManager.class);
        SysOssServiceImpl service = new SysOssServiceImpl(mapper, lifecycle);
        when(mapper.selectById(8L)).thenReturn(oss(8L, "identity/front.jpg", "{}"));
        when(mapper.selectById(10L)).thenReturn(oss(10L, "identity/front.jpg", "not-json"));

        assertThatThrownBy(() -> service.objectMetadata(8L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("OSS_OBJECT_METADATA_UNAVAILABLE");
        assertThatThrownBy(() -> service.objectMetadata(9L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("OSS_OBJECT_NOT_FOUND");
        assertThatThrownBy(() -> service.objectMetadata(10L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("OSS_OBJECT_METADATA_UNAVAILABLE");
    }

    private SysOss oss(long id, String fileName, String ext) {
        SysOss oss = new SysOss();
        oss.setOssId(id);
        oss.setFileName(fileName);
        oss.setOriginalName(fileName.substring(fileName.lastIndexOf('/') + 1));
        oss.setFileSuffix(fileName.substring(fileName.lastIndexOf('.')));
        oss.setExt1(ext);
        oss.setCreateBy(77L);
        return oss;
    }
}
