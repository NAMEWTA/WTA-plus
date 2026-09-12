package org.namewta.test.oss.config;

import com.baomidou.dynamic.datasource.annotation.DsTxEventListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.system.controller.system.SysOssConfigController;
import org.namewta.system.domain.bo.SysOssConfigBo;
import org.namewta.system.domain.vo.SysOssConfigVo;
import org.namewta.system.listener.OssConfigChangeListener;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class OssConfigHttpContractUnitTest {

    @Test
    void queriesUseGetAndEveryMutationUsesNonConflictingPostWithAccurateLog() throws Exception {
        assertThat(method("list", SysOssConfigBo.class,
            org.namewta.common.mybatis.core.page.PageQuery.class).getAnnotation(GetMapping.class)).isNotNull();
        assertThat(method("getInfo", Long.class).getAnnotation(GetMapping.class)).isNotNull();

        assertWrite("add", "", BusinessType.INSERT, false, SysOssConfigBo.class);
        assertWrite("edit", "/edit", BusinessType.UPDATE, false, SysOssConfigBo.class);
        assertWrite("remove", "/remove/{ossConfigIds}", BusinessType.DELETE, true, Long[].class);
        assertWrite("changeStatus", "/changeStatus", BusinessType.UPDATE, false, SysOssConfigBo.class);

        for (Method method : SysOssConfigController.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(PutMapping.class)).as(method.getName()).isNull();
            assertThat(method.getAnnotation(DeleteMapping.class)).as(method.getName()).isNull();
        }
    }

    @Test
    void secretIsNotSerializedAndCacheListenerUsesDsAfterCommitHook() throws Exception {
        assertThat(SysOssConfigVo.class.getDeclaredField("secretKey").getAnnotation(JsonIgnore.class)).isNotNull();
        Method listener = OssConfigChangeListener.class.getMethod("refreshOssConfig",
            org.namewta.system.event.OssConfigChangeEvent.class);
        assertThat(listener.getAnnotation(DsTxEventListener.class)).isNotNull();
        assertThat(listener.getAnnotations()).noneMatch(annotation ->
            annotation.annotationType().getName().equals(
                "org.springframework.transaction.event.TransactionalEventListener"));
    }

    private static void assertWrite(String name, String path, BusinessType type,
                                    boolean savesRequest, Class<?>... parameterTypes) throws Exception {
        Method method = method(name, parameterTypes);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertThat(mapping).isNotNull();
        if (path.isEmpty()) {
            assertThat(mapping.value()).isEmpty();
        } else {
            assertThat(mapping.value()).containsExactly(path);
        }
        Log log = method.getAnnotation(Log.class);
        assertThat(log).isNotNull();
        assertThat(log.businessType()).isEqualTo(type);
        assertThat(log.isSaveRequestData()).isEqualTo(savesRequest);
    }

    private static Method method(String name, Class<?>... parameterTypes) throws Exception {
        return SysOssConfigController.class.getMethod(name, parameterTypes);
    }
}
