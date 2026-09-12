package org.namewta.common.notify.attachment;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 将通知附件快照写入调用方指定的本地路径。
 */
@FunctionalInterface
public interface NotifyAttachmentMaterializer {

    void materialize(Path target) throws IOException;
}
