package org.namewta.test.oss.access;

import org.namewta.common.core.utils.SpringUtils;
import org.namewta.system.api.OssService;
import org.namewta.system.api.domain.OssDTO;
import org.namewta.system.domain.vo.SysOssVo;
import org.namewta.system.mapper.SysOssConfigMapper;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.migration.mapper.SysOssMigrationItemMapper;
import org.namewta.system.oss.exception.OssLifecycleError;
import org.namewta.system.oss.exception.OssLifecycleException;
import org.namewta.system.oss.service.OssLifecycleManager;
import org.namewta.system.service.impl.SysOssServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class OssAccessUrlServiceUnitTest {

    @Test
    void legacyUrlSelectionDelegatesToResolverAndMissingObjectFails() {
        SysOssMapper mapper = mock(SysOssMapper.class);
        OssLifecycleManager lifecycle = mock(OssLifecycleManager.class);
        SysOssServiceImpl service = new SysOssServiceImpl(mapper, mock(SysOssConfigMapper.class), mock(SysOssMigrationItemMapper.class), lifecycle);
        when(lifecycle.resolveAccessUrl(1L)).thenReturn(
            new OssService.OssAccessUrl("PUBLIC", "https://cdn.example.test/a.txt", null, "a.txt"));
        when(lifecycle.resolveAccessUrl(2L)).thenThrow(
            new OssLifecycleException(OssLifecycleError.OBJECT_NOT_FOUND, "missing"));

        assertThatThrownBy(() -> service.selectUrlByIds("1,2"))
            .hasCauseInstanceOf(OssLifecycleException.class);
        verify(lifecycle).resolveAccessUrl(1L);
        verify(lifecycle).resolveAccessUrl(2L);
    }

    @Test
    void legacyDtoSelectionFiltersMissingAndUsesUnifiedResolver() {
        SysOssMapper mapper = mock(SysOssMapper.class);
        OssLifecycleManager lifecycle = mock(OssLifecycleManager.class);
        SysOssServiceImpl service = new SysOssServiceImpl(mapper, mock(SysOssConfigMapper.class), mock(SysOssMigrationItemMapper.class), lifecycle);
        SysOssVo existing = vo(1L, "persisted-url");
        when(mapper.selectVoById(1L)).thenReturn(existing);
        when(mapper.selectVoById(2L)).thenReturn(null);
        when(lifecycle.resolveAccessUrl(1L)).thenReturn(new OssService.OssAccessUrl(
            "PRIVATE", "https://storage.example.test/signed", Instant.EPOCH, "a.txt"));

        try (MockedStatic<SpringUtils> spring = mockStatic(SpringUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(service);
            List<OssDTO> result = service.selectByIds("1,2");

            assertThat(result).singleElement().satisfies(dto -> {
                assertThat(dto.getOssId()).isEqualTo(1L);
                assertThat(dto.getUrl()).isEqualTo("https://storage.example.test/signed");
            });
        }
        verify(lifecycle).resolveAccessUrl(1L);
    }

    @Test
    void managementListNeverReturnsAccessUrl() {
        SysOssMapper mapper = mock(SysOssMapper.class);
        OssLifecycleManager lifecycle = mock(OssLifecycleManager.class);
        SysOssServiceImpl service = new SysOssServiceImpl(mapper, mock(SysOssConfigMapper.class), mock(SysOssMigrationItemMapper.class), lifecycle);
        when(mapper.selectVoById(1L)).thenReturn(vo(1L, "https://stored.example/object"));
        when(lifecycle.snapshot(1L)).thenReturn(
            new OssService.OssLifecycleSnapshot(1L, false, null, List.of()));

        try (MockedStatic<SpringUtils> spring = mockStatic(SpringUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(service);
            assertThat(service.listByIds(List.of(1L))).singleElement()
                .extracting(SysOssVo::getUrl).isNull();
        }
    }

    private SysOssVo vo(Long id, String url) {
        SysOssVo vo = new SysOssVo();
        vo.setOssId(id);
        vo.setFileName("objects/a.txt");
        vo.setOriginalName("a.txt");
        vo.setService("private");
        vo.setUrl(url);
        return vo;
    }
}
