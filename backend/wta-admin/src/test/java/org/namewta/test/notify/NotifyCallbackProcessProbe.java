package org.namewta.test.notify;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.handler.InjectionMetaObjectHandler;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.dao.NotifyProviderReceiptDao;
import org.namewta.notify.mapper.*;
import org.namewta.notify.service.runtime.NotifyDispatchResultService;
import org.namewta.notify.service.runtime.ProviderCallbackService;
import org.namewta.notify.usecase.NotifyDispatchResultUseCase;
import org.namewta.notify.usecase.ProviderCallbackUseCase;
import org.springframework.aop.framework.ProxyFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** 仅供隔离集成测试启动独立 JVM，验证幂等事实不会随着应用进程退出而丢失。 */
public final class NotifyCallbackProcessProbe {
    private NotifyCallbackProcessProbe() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !args[0].matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/namewta_notify_test_[a-zA-Z0-9_]+.*")) {
            throw new IllegalArgumentException("Only the owned loopback test database is allowed");
        }
        String password = System.getenv("T36_MYSQL_PASSWORD");
        if (password == null) throw new IllegalArgumentException("Private owned MySQL credential is required");
        int code = 0;
        try (var pool = new HikariDataSource()) {
            pool.setJdbcUrl(args[0]);
            pool.setUsername(System.getProperty("notify.mysql.integration.username", "root"));
            pool.setPassword(password); pool.setMaximumPoolSize(2);
            var routing = new DynamicRoutingDataSource(List.of()); routing.setPrimary("master"); routing.setStrict(true);
            routing.addDataSource("master", pool);
            try {
                var config = new MybatisConfiguration(new Environment("owned-callback-process", new SpringManagedTransactionFactory(), routing));
                config.setMapUnderscoreToCamelCase(true);
                GlobalConfigUtils.setGlobalConfig(config, GlobalConfigUtils.defaults().setMetaObjectHandler(new InjectionMetaObjectHandler()));
                for (Class<?> mapper : List.of(NotifyIntentMapper.class, NotifyRecipientMapper.class, NotifyDeliveryMapper.class,
                    NotifyOutboxMapper.class, NotifyAttemptMapper.class, NotifyMessageMapper.class, NotifyMessageRecipientMapper.class,
                    org.namewta.notify.mapper.NotifyIntentAttachmentMapper.class,
                    NotifyProviderReceiptMapper.class)) config.addMapper(mapper);
                String resource = "/mapper/notify/NotifyOutboxMapper.xml";
                try (var stream = NotifyCallbackProcessProbe.class.getResourceAsStream(resource)) {
                    new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
                }
                var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
                var dao = new NotifyNotificationDao(sessions.getMapper(NotifyIntentMapper.class), sessions.getMapper(NotifyRecipientMapper.class),
                    sessions.getMapper(NotifyDeliveryMapper.class), sessions.getMapper(NotifyOutboxMapper.class), sessions.getMapper(NotifyAttemptMapper.class),
                    sessions.getMapper(NotifyMessageMapper.class), sessions.getMapper(NotifyMessageRecipientMapper.class), sessions.getMapper(org.namewta.notify.mapper.NotifyIntentAttachmentMapper.class));
                var results = transactional(new NotifyDispatchResultUseCase(new NotifyDispatchResultService(dao)), NotifyDispatchResultUseCase.class);
                var receipts = new NotifyProviderReceiptDao(sessions.getMapper(NotifyProviderReceiptMapper.class));
                var callbacks = transactional(new ProviderCallbackUseCase(new ProviderCallbackService(dao, results, receipts)), ProviderCallbackUseCase.class);
                String body = JsonUtils.toJsonString(Map.of("eventId", "process-restart-event", "timestamp", Instant.now().toString(),
                    "providerKey", "owned-smtp", "providerMessageId", "owned-message", "status", args[1], "target", ""));
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec("owned-t22-only".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                try {
                    callbacks.apply("MAIL", HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8))), body, "owned-t22-only");
                } catch (ServiceException conflict) {
                    if (!Integer.valueOf(409).equals(conflict.getCode())) throw conflict;
                    code = 3;
                }
            } finally { routing.destroy(); }
        }
        System.out.println("OWNED_CALLBACK_PROBE exit=" + code);
        System.exit(code);
    }

    private static <T> T transactional(T target, Class<T> type) {
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvisor(new DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class));
        return type.cast(proxy.getProxy());
    }
}
