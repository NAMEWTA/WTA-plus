package org.namewta.test.notify;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.api.NotificationCommand;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** 从实际 Java 模型导出排队模式 schema，供正式 OpenAPI 生成器更新已有快照。 */
@Tag("dev")
class NotificationModeOpenApiTest {
    @Test
    void publicSchemaOnlyOffersTheImplementedQueuedMode() throws Exception {
        Schema<?> schema = ModelConverters.getInstance().read(NotificationCommand.class).get("NotificationCommand");
        assertThat(schema).isNotNull();
        Schema<?> mode = schema.getProperties().get("mode");
        var modes = mode.getEnum().stream().map(Object::toString).toList();
        assertThat(modes).containsExactly("ASYNC");
        String output = System.getProperty("notify.openapi.mode.output");
        if (output != null) Files.writeString(Path.of(output), JsonUtils.toJsonString(modes));
    }
}
