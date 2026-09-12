# Evidence CR-002-fix

- **Change：** 2026-09-10-notify-channel-config
- **Review：** `<Path>{roots.state}/specdev/changes/2026-09-10-notify-channel-config/reviews/CR-002.md</Path>`
- **Packet：** `temp/team/implement/PACKET-CR-002.md`
- **Workspace：** current / main
- **Push：** not-run（PACKET 禁止）

## 1. 实现摘要

多层限额在 `NotifySendPlanner.finish` 顺序占用后若后续层失败，释放已占用计数；`tryAcquire` 超限也不留下增量。收件人键改为规范化 + SHA-256 截断。`addAccount` / `updateAccount` / `changeStatus` 启用路径校验 MAIL SMTP 主机/端口/发件人/密码与 SMS 厂商/AccessKey/密钥。

## 2. 修改路径

| 路径 | 目的 |
|---|---|
| `backend/wta-modules/wta-notify/.../NotifyQuotaPort.java` | `release` 默认方法 |
| `backend/wta-modules/wta-notify/.../RedisNotifyQuotaAdapter.java` | 超限回滚 incr；`release` → `decrAtomicValue` |
| `backend/wta-modules/wta-notify/.../NotifySendPlanner.java` | 持有列表 + 失败释放；`recipientQuotaToken` |
| `backend/wta-modules/wta-notify/.../NotifyConfigService.java` | 启用无密钥/连接字段拒绝 |
| `DispatchNotificationServiceTest` | 跨层交叉：`laterLayerFailureDoesNotLeakAccountQuota` |
| `NotifySendPlannerTest` | 摘要稳定、非 hashCode |
| `NotifyConfigServiceTest` | 无密钥不可启用 MAIL/SMS |

## 3. 验收

| Finding | 测试 | 结果 |
|---|---|---|
| 多层限额不回滚 / AC-008·009 | `laterLayerFailureDoesNotLeakAccountQuota`：先耗尽模板层，账号 cap=1 的后续两次失败均为 `TEMPLATE_QUOTA` 而非泄漏的 `ACCOUNT_QUOTA`；既有 `secondSendWithinAccountMinuteCapFailsClosed` / `recipientMinuteCapIsIsolatedByScene` | pass |
| 收件人 hashCode | `recipientQuotaTokenIsStableDigestNotHashCode` | pass |
| 无密钥不可启用 | `cannotEnableMailAccountWithoutSecret` / `cannotEnableSmsAccountWithoutSecret` | pass |

## 4. Workspace Verification

| 命令 | 环境 | 结果 |
|---|---|---|
| `./mvnw -pl wta-modules/wta-notify -am -Dtest=DispatchNotificationServiceTest,NotifyConfigServiceTest,NotifySendPlannerTest -Dsurefire.failIfNoSpecifiedTests=false test` | current-workspace / `backend` | pass，exit 0；Tests run: 18 |

未运行：全仓 `./mvnw test`、真实 Redis 限额集成、git push。
