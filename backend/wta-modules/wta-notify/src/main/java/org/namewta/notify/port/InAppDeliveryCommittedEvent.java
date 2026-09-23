package org.namewta.notify.port;

/**
 * 仅携带消息与本人收件关系主键的提交后实时提示；正文始终从已提交的收件箱读取。
 * @param messageId 已提交的站内消息主键
 * @param userId 当前收件关系的用户主键
 */
public record InAppDeliveryCommittedEvent(Long messageId, Long userId) { }
