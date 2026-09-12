# T-06 Notice Migration

状态：done

公告新增仅在正常状态显式生成 `NOTICE_PUBLISHED` 快照，并提供 `/system/notice/{id}/publish`；验证码、认证欢迎消息、工作流、档案转移、演示 SMS/Mail/WebSocket 均改走 `NotificationApplicationService`。
