package org.namewta.test.oss.upload;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.namewta.system.domain.SysOss;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.config.OssLifecycleProperties;
import org.namewta.system.oss.upload.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Direct Complete 的 TEMP 元数据登记测试。
 */
@Tag("dev")
class OssUploadMetadataStoreUnitTest {

    @Test
    void shouldRegisterTemporaryObjectWithStableMetadata() {
        SysOssMapper mapper = mock(SysOssMapper.class);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(mapper.insert(any(SysOss.class))).thenAnswer(invocation -> {
            SysOss object = invocation.getArgument(0);
            object.setOssId(9001L);
            return 1;
        });
        OssLifecycleProperties lifecycle = new OssLifecycleProperties();
        lifecycle.setTempRetention(Duration.ofHours(24));
        DefaultOssUploadMetadataStore store = new DefaultOssUploadMetadataStore(mapper, lifecycle);

        Long ossId = store.registerTemporary(ticket());

        assertEquals(9001L, ossId);
        verify(mapper).insert(argThat((SysOss object) -> "Y".equals(object.getIsTemp())
            && object.getExpireTime() != null
            && "service-a".equals(object.getService())
            && "direct/general/key.png".equals(object.getFileName())
            && object.getExt1().contains("directUpload")
            && object.getExt1().contains("image/png")));
    }

    @Test
    void shouldReuseExistingObjectWithoutInsert() {
        SysOssMapper mapper = mock(SysOssMapper.class);
        SysOss existing = new SysOss();
        existing.setOssId(9001L);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        DefaultOssUploadMetadataStore store = new DefaultOssUploadMetadataStore(mapper,
            new OssLifecycleProperties());

        assertEquals(9001L, store.registerTemporary(ticket()));
        verify(mapper, never()).insert(any(SysOss.class));
    }

    private OssUploadTicket ticket() {
        long now = System.currentTimeMillis();
        return new OssUploadTicket("token", "general", OssUploadMode.SINGLE, OssUploadState.COMPLETING,
            "service-a", "bucket-a", "direct/general/key.png", null, "avatar.png", ".png", 8,
            "image/png", "fp", "digest", 7L, 100L, 0, 0, now, now + 60_000, null);
    }
}
