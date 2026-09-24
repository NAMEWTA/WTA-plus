package org.namewta.test.notify.monitor;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.vo.NotificationDeliveryView;
import org.namewta.notify.mapper.NotifyAttemptMapper;
import org.namewta.notify.mapper.NotifyDeliveryMapper;
import org.namewta.notify.mapper.NotifyIntentMapper;
import org.namewta.notify.mapper.NotifyMessageMapper;
import org.namewta.notify.mapper.NotifyMessageRecipientMapper;
import org.namewta.notify.mapper.NotifyOutboxMapper;
import org.namewta.notify.mapper.NotifyRecipientMapper;
import org.namewta.notify.service.runtime.NotificationMonitorService;
import org.namewta.notify.usecase.NotificationMonitorUseCase;
import org.namewta.test.support.SqlBaselinePaths;

import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** 当前 Notify 监控 UseCase/Service/DAO/Mapper 与真实 MySQL 基座的查询合同。 */
@Tag("dev")
class NotifyMonitorMySqlIntegrationTest {

    @Test
    void filtersAndBoundsRealDeliveryRowsWithoutExposingTargets() throws Exception {
        String url = System.getProperty("notify.mysql.integration.url");
        assumeTrue(url != null && !url.isBlank(), "需要一次性 MySQL JDBC URL");
        var dataSource = new PooledDataSource("com.mysql.cj.jdbc.Driver", url,
            System.getProperty("notify.mysql.integration.username", "root"),
            System.getProperty("notify.mysql.integration.password", ""));
        boolean ownsTable = false;
        try {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                try (var result = statement.executeQuery("select database()")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).startsWith("namewta_ci");
                }
                String ddl = Files.readString(SqlBaselinePaths.file("10-cde-base-ddl.sql"));
                int start = ddl.indexOf("create table notify_delivery (");
                assertThat(start).isGreaterThanOrEqualTo(0);
                statement.execute(ddl.substring(start, ddl.indexOf(';', start) + 1));
                ownsTable = true;
                try (var insert = connection.prepareStatement("insert into notify_delivery"
                    + "(delivery_id,intent_id,recipient_id,user_id,channel,status,target_value,create_time) values(?,1,?,?,?,?,?,?)")) {
                    for (int id = 1; id <= 508; id++) {
                        insert.setLong(1, id);
                        insert.setLong(2, id);
                        insert.setLong(3, id == 506 ? 2 : 1);
                        insert.setString(4, id == 506 || id == 507 ? "SMS" : "MAIL");
                        insert.setString(5, id == 506 || id == 508 ? "DELIVERED" : "FAILED");
                        insert.setString(6, "private-target-canary@example.test");
                        insert.setTimestamp(7, Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(id)));
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
            }
            var configuration = new MybatisConfiguration(new Environment("notify-monitor-test",
                new JdbcTransactionFactory(), dataSource));
            configuration.setMapUnderscoreToCamelCase(true);
            GlobalConfigUtils.setGlobalConfig(configuration, GlobalConfigUtils.defaults());
            for (var mapper : List.of(NotifyIntentMapper.class, NotifyRecipientMapper.class, NotifyDeliveryMapper.class,
                NotifyOutboxMapper.class, NotifyAttemptMapper.class, NotifyMessageMapper.class, NotifyMessageRecipientMapper.class, org.namewta.notify.mapper.NotifyIntentAttachmentMapper.class)) {
                configuration.addMapper(mapper);
            }
            var sessions = new MybatisSqlSessionFactoryBuilder().build(configuration);
            try (var session = sessions.openSession(true)) {
                var dao = new NotifyNotificationDao(session.getMapper(NotifyIntentMapper.class), session.getMapper(NotifyRecipientMapper.class),
                    session.getMapper(NotifyDeliveryMapper.class), session.getMapper(NotifyOutboxMapper.class), session.getMapper(NotifyAttemptMapper.class),
                    session.getMapper(NotifyMessageMapper.class), session.getMapper(NotifyMessageRecipientMapper.class), session.getMapper(org.namewta.notify.mapper.NotifyIntentAttachmentMapper.class));
                var application = mock(NotificationApplicationService.class);
                var useCase = new NotificationMonitorUseCase(application, new NotificationMonitorService(dao));
                var all = useCase.deliveries(null, null, null);
                assertThat(all).hasSize(500);
                assertThat(all.getFirst().deliveryId()).isEqualTo(508L);
                assertThat(all.getLast().deliveryId()).isEqualTo(9L);
                assertThat(all.toString()).doesNotContain("private-target-canary");
                var filtered = useCase.deliveries(1L, "MAIL", "FAILED");
                assertThat(filtered).hasSize(500).allSatisfy(row -> {
                    assertThat(row.userId()).isEqualTo(1L);
                    assertThat(row.channel()).isEqualTo("MAIL");
                    assertThat(row.status()).isEqualTo("FAILED");
                });
                assertThat(filtered.getFirst().deliveryId()).isEqualTo(505L);
                assertThat(filtered.getLast().deliveryId()).isEqualTo(6L);
                assertThat(useCase.deliveries(2L, "SMS", "DELIVERED"))
                    .extracting(NotificationDeliveryView::deliveryId).containsExactly(506L);
                assertThat(useCase.deliveries(null, "MAIL' OR 1=1 --", null)).isEmpty();
                assertThat(useCase.deliveries(null, " ", "DELIVERED"))
                    .extracting(NotificationDeliveryView::deliveryId).containsExactly(508L, 506L);
                verifyNoInteractions(application);
            }
        } finally {
            try {
                if (ownsTable) {
                    try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                        statement.execute("drop table notify_delivery");
                    }
                }
            } finally { dataSource.forceCloseAll(); }
        }
    }
}
