package org.namewta.notify.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.domain.entity.NotifyIntent;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 公告版本元数据须保留审计键，且损坏/旧来源不能默许 Worker 发送。 */
@Tag("dev")
class NotifyNoticeVersionFenceTest {
    @Test
    void exactVersionChangesToRetractedWithoutLosingOtherMetadata() {
        NotifyIntent intent = notice(JsonUtils.toJsonString(Map.of("audit", "NOTICE_SNAPSHOT", "other", "keep",
            "noticeVersion", Map.of("noticeId", 41L, "snapshotId", 91L, "version", 2, "retracted", false))));
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
