package org.namewta.common.notify.model;

/**
 * 富文本通知内容。
 */
public record NotifyRichContent(String subject, String content, boolean html) implements NotifyContent {

    @Override
    public String contentSnapshot() {
        return content;
    }
}
