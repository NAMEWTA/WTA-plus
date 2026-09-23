package org.namewta.notify.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.config.JacksonConfig;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.domain.entity.NotifyIntent;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

/** 公告版本元数据须保留审计键，且损坏/旧来源不能默许 Worker 发送。 */
@Tag("dev")
class NotifyNoticeVersionFenceTest {
    @Test
    void largePublishedIdsRoundTripThroughProductionJacksonModule() {
        long noticeId = 1761800000000000041L;
        long snapshotId = 1761800000000000091L;
        JsonMapper productionMapper = JsonMapper.builder()
            .addModule(new JacksonConfig().registerJavaTimeModule()).build();

        // JsonUtils has a process-wide cached mapper; scope this test without mutating that cache.
        try (var json = mockStatic(JsonUtils.class, CALLS_REAL_METHODS)) {
            json.when(JsonUtils::getJsonMapper).thenReturn(productionMapper);
            Map<String, String> initial = NotifyNoticeVersionFence.initial(noticeId, snapshotId, 2);
            assertThat(initial.get("noticeVersion"))
                .contains("\"noticeId\":\"" + noticeId + "\"")
                .contains("\"snapshotId\":\"" + snapshotId + "\"");
            NotifyIntent intent = notice(JsonUtils.toJsonString(initial));
            intent.setBizId(String.valueOf(noticeId));
            intent.setIdempotencyKey(NotifyNoticeVersionFence.idempotencyKey(noticeId, 2));

            assertThatNoException().isThrownBy(() -> NotifyNoticeVersionFence.requirePublishedIdentity(
                intent, noticeId, snapshotId, 2));
            assertThat(NotifyNoticeVersionFence.state(intent)).isEqualTo(NotifyNoticeVersionFence.State.ACTIVE);
            intent.setMetadataJson(NotifyNoticeVersionFence.retractedMetadata(intent, noticeId, snapshotId, 2));
            assertThat(NotifyNoticeVersionFence.state(intent)).isEqualTo(NotifyNoticeVersionFence.State.RETRACTED);

            long largestId = Long.MAX_VALUE;
            Map<String, String> boundary = NotifyNoticeVersionFence.initial(largestId, largestId - 1, 2);
            NotifyIntent boundaryIntent = notice(JsonUtils.toJsonString(boundary));
            boundaryIntent.setBizId(String.valueOf(largestId));
            boundaryIntent.setIdempotencyKey(NotifyNoticeVersionFence.idempotencyKey(largestId, 2));
            assertThatNoException().isThrownBy(() -> NotifyNoticeVersionFence.requirePublishedIdentity(
                boundaryIntent, largestId, largestId - 1, 2));
            assertThat(NotifyNoticeVersionFence.state(boundaryIntent))
                .isEqualTo(NotifyNoticeVersionFence.State.ACTIVE);
        }
    }

    @Test
    void malformedIdRepresentationsNeverAuthorizeNoticeDelivery() {
        for (Object invalid : new Object[] {"", " 41", "+41", "-41", "041", "41.0", "4.1e1",
            "9223372036854775808", "４１", "41\n", Boolean.TRUE, 41.0D}) {
            String marker = JsonUtils.toJsonString(Map.of("noticeId", invalid, "snapshotId", 91,
                "version", 2, "retracted", false));
            NotifyIntent intent = notice(JsonUtils.toJsonString(Map.of(
                "audit", "NOTICE_SNAPSHOT", "noticeVersion", marker)));
            assertThat(NotifyNoticeVersionFence.state(intent)).as("invalid noticeId=%s", invalid)
                .isEqualTo(NotifyNoticeVersionFence.State.UNVERIFIED);
            assertThatThrownBy(() -> NotifyNoticeVersionFence.requirePublishedIdentity(intent, 41L, 91L, 2))
                .as("invalid noticeId=%s", invalid).isInstanceOf(ServiceException.class);
        }
    }

    @Test
    void exactVersionChangesToRetractedWithoutLosingOtherMetadata() {
        Map<String, String> initial = NotifyNoticeVersionFence.initial(41L, 91L, 2);
        assertThat(initial.get("noticeVersion")).contains("\"snapshotId\":91");
        NotifyIntent intent = notice(JsonUtils.toJsonString(Map.of("audit", "NOTICE_SNAPSHOT", "other", "keep",
            "noticeVersion", initial.get("noticeVersion"))));
        assertThat(NotifyNoticeVersionFence.state(intent)).isEqualTo(NotifyNoticeVersionFence.State.ACTIVE);
        intent.setMetadataJson(NotifyNoticeVersionFence.retractedMetadata(intent, 41L, 91L, 2));
        assertThat(NotifyNoticeVersionFence.state(intent)).isEqualTo(NotifyNoticeVersionFence.State.RETRACTED);
        assertThat(JsonUtils.parseObject(intent.getMetadataJson(), Map.class)).containsEntry("other", "keep")
            .containsEntry("audit", "NOTICE_SNAPSHOT");
    }

    @Test
    void oldAuditOnlyIntentRequiresExactIdentityBeforeUpgrade() {
        NotifyIntent intent = notice("{\"audit\":\"NOTICE_SNAPSHOT\"}");
        assertThat(NotifyNoticeVersionFence.state(intent)).isEqualTo(NotifyNoticeVersionFence.State.UNVERIFIED);
        assertThatThrownBy(() -> NotifyNoticeVersionFence.retractedMetadata(intent, 42L, 91L, 2))
            .isInstanceOf(ServiceException.class);
        intent.setMetadataJson(NotifyNoticeVersionFence.retractedMetadata(intent, 41L, 91L, 2));
        assertThat(NotifyNoticeVersionFence.state(intent)).isEqualTo(NotifyNoticeVersionFence.State.RETRACTED);
    }

    @Test
    void malformedMarkerAndUnrelatedSceneHaveDifferentFailClosedMeaning() {
        NotifyIntent intent = notice("{\"audit\":\"NOTICE_SNAPSHOT\",\"noticeVersion\":{\"noticeId\":41.5}}");
        assertThat(NotifyNoticeVersionFence.state(intent)).isEqualTo(NotifyNoticeVersionFence.State.UNVERIFIED);
        assertThatThrownBy(() -> NotifyNoticeVersionFence.retractedMetadata(intent, 41L, 91L, 2))
            .isInstanceOf(ServiceException.class);
        intent.setMetadataJson("not-json");
        assertThat(NotifyNoticeVersionFence.state(intent)).isEqualTo(NotifyNoticeVersionFence.State.UNVERIFIED);
        intent.setSceneCode("workflow-task");
        intent.setTemplateCode("workflow-task");
        intent.setBizType("WORKFLOW_TASK");
        intent.setIdempotencyKey("workflow:41:2");
        assertThat(NotifyNoticeVersionFence.state(intent)).isEqualTo(NotifyNoticeVersionFence.State.NOT_NOTICE);
    }

    private static NotifyIntent notice(String metadata) {
        NotifyIntent intent = new NotifyIntent();
        intent.setAppId("notify");
        intent.setSceneCode("notice-published");
        intent.setTemplateCode("notice-published");
        intent.setBizType("NOTICE_PUBLISHED");
        intent.setBizId("41");
        intent.setIdempotencyKey("notice-published:41:2");
        intent.setMetadataJson(metadata);
        return intent;
    }
}
