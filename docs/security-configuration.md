# 安全配置与审计处置

## 本地覆盖与凭据

后端本地覆盖不入版本控制。可参考 `backend/wta-admin/src/main/resources/application-local.yml.example`，通过环境变量注入实际连接信息。不要在 `VITE_*` 中放秘密，也不要把诊断权限作为业务账号的必需权限。

2026-09-22 审计发现旧版本本地 YAML 含未脱敏数据库/Redis 凭据。本次删除跟踪并加入忽略规则，**不代表已经轮换运行密码**。应在真实环境先轮换相关密码、撤销旧凭据并核对访问记录，再评估历史清理；历史强推和实际轮换未由这次 GitHub 修改执行。原报告与完成记录不复述凭据值。

## CORS

默认同源访问，开发使用已有 Vite 代理。只有明确跨源部署才设置 `WEB_CORS_ALLOWED_ORIGINS`，值为不带路径的准确 Origin 列表（例如 `https://admin.example.test,https://home.example.test`）。禁止通配或反射任意来源，不用扩大 CORS 来掩盖代理或回调配置错误。

## 消息配置

`VITE_APP_MESSAGE_ENABLED` 仅控制 SSE/WebSocket 实时提示。登录用户的收件箱总是通过 REST 读取；关闭实时通道不应丢失已持久化消息。
