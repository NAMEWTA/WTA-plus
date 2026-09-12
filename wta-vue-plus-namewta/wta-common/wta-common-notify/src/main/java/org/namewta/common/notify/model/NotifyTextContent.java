package org.namewta.common.notify.model;

/**
 * 纯文本通知内容。
 */
public record NotifyTextContent(String subject, String text) implements NotifyContent {

    @Override
    public String contentSnapshot() {
        return text;
    }
}
