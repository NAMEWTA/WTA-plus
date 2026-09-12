package org.namewta.common.notify.model;

/**
 * 渠道无关通知内容。
 */
public sealed interface NotifyContent permits NotifyTextContent, NotifyRichContent, NotifyTemplateContent {

    String subject();

    String contentSnapshot();
}
