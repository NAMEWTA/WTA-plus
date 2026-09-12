package org.namewta.test.oss.config;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.annotation.DsTxEventListener;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.tx.DsTxEventListenerFactory;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.system.domain.SysOssConfig;
import org.namewta.system.domain.bo.SysOssConfigBo;
import org.namewta.system.event.OssConfigChangeEvent;
import org.namewta.system.mapper.SysOssConfigMapper;
import org.namewta.system.service.impl.SysOssConfigServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.aop.framework.ProxyFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class OssConfigGovernanceUnitTest {

    @Test
    void rejectsUnknownPolicyAndPublicDefaultBeforeWriting() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);

        SysOssConfigBo unknown = bo(null, "archive", "bucket-a", "1", "N");
        assertThatThrownBy(() -> service.insertByBo(unknown))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("0=PRIVATE或2=PUBLIC_READ");

        SysOssConfigBo publicDefault = bo(null, "portal", "bucket-public", "2", "Y");
        assertThatThrownBy(() -> service.insertByBo(publicDefault))
            .isInstanceOf(ServiceException.class)
            .hasMessage("默认OSS配置必须为PRIVATE");

        verify(mapper, never()).insert(any(SysOssConfig.class));
    }

    @Test
    void referencedConfigCannotChangeOwnershipBoundary() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);
        SysOssConfig old = config(11L, "private-main", "bucket-private", "0", "N");
        when(mapper.selectByIdForUpdate(11L)).thenReturn(old);
        when(mapper.countConfigKeyConflicts("private-main", 11L)).thenReturn(0L);
        when(mapper.countDefaultConfigs()).thenReturn(1L);
        when(mapper.countOssReferences("private-main")).thenReturn(3L);

        SysOssConfigBo edit = bo(11L, "private-main", "bucket-public", "0", "N");
        assertThatThrownBy(() -> service.updateByBo(edit))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不能通过普通编辑修改");

        verify(mapper, never()).updateById(any(SysOssConfig.class));
    }

    @Test
    void referencedConfigCanRotateEndpointAndPreservesOmittedSecret() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);
        SysOssConfig old = config(11L, "private-main", "bucket-private", "0", "N");
        old.setSecretKey("existing-secret");
        old.setEndpoint("old.example.test");
        SysOssConfig persisted = config(11L, "private-main", "bucket-private", "0", "N");
        persisted.setSecretKey("existing-secret");
        persisted.setEndpoint("new.example.test");
        when(mapper.selectByIdForUpdate(11L)).thenReturn(old);
        when(mapper.countConfigKeyConflicts("private-main", 11L)).thenReturn(0L);
        when(mapper.countDefaultConfigs()).thenReturn(1L);
        when(mapper.countOssReferences("private-main")).thenReturn(7L);
        when(mapper.updateById(any(SysOssConfig.class))).thenReturn(1);
        when(mapper.selectById(11L)).thenReturn(persisted);
        SysOssConfigBo edit = bo(11L, "private-main", "bucket-private", "0", "N");
        edit.setSecretKey(null);
        edit.setEndpoint("new.example.test");
        List<Object> events = new ArrayList<>();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(JsonMapper.class, () -> JsonMapper.builder().build());
            context.registerBean(SpringUtils.class);
            context.refresh();
            context.addApplicationListener(events::add);
            assertThat(service.updateByBo(edit)).isTrue();
        }

        ArgumentCaptor<SysOssConfig> saved = ArgumentCaptor.forClass(SysOssConfig.class);
        verify(mapper).updateById(saved.capture());
        assertThat(saved.getValue().getSecretKey()).isEqualTo("existing-secret");
        assertThat(saved.getValue().getEndpoint()).isEqualTo("new.example.test");
        assertThat(saved.getValue().getBucketName()).isEqualTo("bucket-private");
        assertThat(events).anySatisfy(event -> {
            assertThat(event).isInstanceOf(PayloadApplicationEvent.class);
            assertThat(((PayloadApplicationEvent<?>) event).getPayload()).isInstanceOf(OssConfigChangeEvent.class);
        });
    }

    @Test
    void referencedOrDefaultConfigCannotBeDeletedAndPublicCannotBecomeDefault() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);
        SysOssConfig referenced = config(12L, "private-docs", "bucket-docs", "0", "N");
        when(mapper.selectByIdForUpdate(12L)).thenReturn(referenced);
        when(mapper.countDefaultConfigs()).thenReturn(1L);
        when(mapper.countOssReferences("private-docs")).thenReturn(1L);
        assertThatThrownBy(() -> service.deleteWithValidByIds(List.of(12L), true))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已被对象引用");

        SysOssConfig defaultConfig = config(13L, "private-default", "bucket-default", "0", "Y");
        when(mapper.selectByIdForUpdate(13L)).thenReturn(defaultConfig);
        assertThatThrownBy(() -> service.deleteWithValidByIds(List.of(13L), true))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("默认OSS配置不可删除");

        SysOssConfig publicConfig = config(14L, "public-portal", "bucket-public", "2", "N");
        when(mapper.selectByIdForUpdate(14L)).thenReturn(publicConfig);
        SysOssConfigBo switchRequest = new SysOssConfigBo();
        switchRequest.setOssConfigId(14L);
        assertThatThrownBy(() -> service.updateOssConfigStatus(switchRequest))
            .isInstanceOf(ServiceException.class)
            .hasMessage("默认OSS配置必须为PRIVATE");

        verify(mapper, never()).deleteByIds(any());
    }

    @Test
    void initializationFailsClosedWhenDefaultInvariantIsBroken() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);
        when(mapper.selectList()).thenReturn(List.of(
            config(1L, "a", "bucket-a", "0", "Y"),
            config(2L, "b", "bucket-b", "0", "Y")
        ));
        assertThatThrownBy(service::init)
            .isInstanceOf(ServiceException.class)
            .hasMessage("OSS配置必须且只能存在一个默认配置");
    }

    @Test
    void updateFailureAfterClearingDefaultsMustAbortTheTransaction() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);
        SysOssConfig old = config(21L, "private-secondary", "bucket-secondary", "0", "N");
        when(mapper.selectByIdForUpdate(21L)).thenReturn(old);
        when(mapper.countConfigKeyConflicts("private-secondary", 21L)).thenReturn(0L);
        when(mapper.countOssReferences("private-secondary")).thenReturn(0L);
        when(mapper.clearOtherDefaultStatuses(21L)).thenReturn(1);
        when(mapper.updateById(any(SysOssConfig.class))).thenReturn(0);

        SysOssConfigBo edit = bo(21L, "private-secondary", "bucket-secondary", "0", "Y");
        assertThatThrownBy(() -> service.updateByBo(edit))
            .isInstanceOf(ServiceException.class)
            .hasMessage("OSS配置更新失败");

        verify(mapper).clearOtherDefaultStatuses(21L);
        verify(mapper).updateById(any(SysOssConfig.class));
        verify(mapper, never()).selectById(21L);
    }

    @Test
    void defaultSwitchFailureAfterClearingDefaultsMustAbortTheTransaction() {
        SysOssConfigMapper mapper = mock(SysOssConfigMapper.class);
        SysOssConfigServiceImpl service = new SysOssConfigServiceImpl(mapper);
        SysOssConfig target = config(22L, "private-secondary", "bucket-secondary", "0", "N");
        when(mapper.selectByIdForUpdate(22L)).thenReturn(target);
        when(mapper.clearOtherDefaultStatuses(22L)).thenReturn(1);
        when(mapper.updateById(any(SysOssConfig.class))).thenReturn(0);

        SysOssConfigBo switchRequest = new SysOssConfigBo();
        switchRequest.setOssConfigId(22L);
        assertThatThrownBy(() -> service.updateOssConfigStatus(switchRequest))
            .isInstanceOf(ServiceException.class)
            .hasMessage("默认OSS配置切换失败");

        verify(mapper).clearOtherDefaultStatuses(22L);
        verify(mapper).updateById(target);
    }

    @Test
    void everyConfigMutationUsesDynamicDatasourceTransaction() throws Exception {
        assertThat(SysOssConfigServiceImpl.class.getMethod("insertByBo", SysOssConfigBo.class)
            .getAnnotation(DSTransactional.class)).isNotNull();
        assertThat(SysOssConfigServiceImpl.class.getMethod("updateByBo", SysOssConfigBo.class)
            .getAnnotation(DSTransactional.class)).isNotNull();
        assertThat(SysOssConfigServiceImpl.class.getMethod("deleteWithValidByIds", java.util.Collection.class,
            Boolean.class).getAnnotation(DSTransactional.class)).isNotNull();
        assertThat(SysOssConfigServiceImpl.class.getMethod("updateOssConfigStatus", SysOssConfigBo.class)
            .getAnnotation(DSTransactional.class)).isNotNull();
    }

    @Test
    void dynamicTransactionEventsRunOnlyAfterCommitAndNeverAfterRollback() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DsTxEventListenerFactory.class);
            context.registerBean(TxEventProbe.class);
            context.refresh();
            TxEventProbe probe = context.getBean(TxEventProbe.class);
            ProxyFactory proxyFactory = new ProxyFactory(new TxEventPublisher(context, probe));
            proxyFactory.setProxyTargetClass(true);
            proxyFactory.addAdvice(new DynamicLocalTransactionInterceptor(true));
            TxEventPublisher publisher = (TxEventPublisher) proxyFactory.getProxy();

            assertThat(publisher.publish("committed", false)).isZero();
            assertThat(probe.events).containsExactly("committed");
            assertThatThrownBy(() -> publisher.publish("rolled-back", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("forced rollback");
            assertThat(probe.events).containsExactly("committed");
        }
    }

    private static SysOssConfigBo bo(Long id, String key, String bucket, String policy, String status) {
        SysOssConfigBo bo = new SysOssConfigBo();
        bo.setOssConfigId(id);
        bo.setConfigKey(key);
        bo.setAccessKey("access-key");
        bo.setSecretKey("secret-key");
        bo.setBucketName(bucket);
        bo.setEndpoint("endpoint.example.test");
        bo.setAccessPolicy(policy);
        bo.setStatus(status);
        bo.setIsHttps("Y");
        return bo;
    }

    private static SysOssConfig config(Long id, String key, String bucket, String policy, String status) {
        SysOssConfig config = new SysOssConfig();
        config.setOssConfigId(id);
        config.setConfigKey(key);
        config.setAccessKey("access-key");
        config.setSecretKey("secret-key");
        config.setBucketName(bucket);
        config.setEndpoint("endpoint.example.test");
        config.setAccessPolicy(policy);
        config.setStatus(status);
        config.setIsHttps("Y");
        return config;
    }

    static class TxEventPublisher {
        private final AnnotationConfigApplicationContext context;
        private final TxEventProbe probe;

        TxEventPublisher(AnnotationConfigApplicationContext context, TxEventProbe probe) {
            this.context = context;
            this.probe = probe;
        }

        @DSTransactional
        public int publish(String event, boolean fail) {
            context.publishEvent(event);
            int observedInsideTransaction = probe.events.size();
            if (fail) {
                throw new IllegalStateException("forced rollback");
            }
            return observedInsideTransaction;
        }
    }

    static class TxEventProbe {
        private final List<String> events = new ArrayList<>();

        @DsTxEventListener
        public void capture(String event) {
            events.add(event);
        }
    }
}
