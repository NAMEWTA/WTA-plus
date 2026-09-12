# T-04 Inbox

状态：done

system 实现 `InAppNotificationPort`，先写 `sys_message` 与收件人投影，再通过 `PushHelper` 实时提示；已见/已读更新落到 `sys_message_recipient`。
