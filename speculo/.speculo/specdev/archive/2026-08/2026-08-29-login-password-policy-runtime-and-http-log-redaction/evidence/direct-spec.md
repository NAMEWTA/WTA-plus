---
artifact: evidence
change: 2026-08-29-login-password-policy-runtime-and-http-log-redaction
id: direct-spec
lead: codex
updated_at: 2026-08-29T14:04:00+08:00
status: done
---

# Evidence: 登录密码策略运行态与 HTTP 日志脱敏

- **Change：** `2026-08-29-login-password-policy-runtime-and-http-log-redaction`
- **Spec / Diagnosis：** 本 change 的 `<Path>spec.md</Path>` / `<Path>diagnosis.md</Path>`
- **执行模式：** Direct Spec，Lead direct，current workspace
- **实施前基线：** parent `67000ae7c37f41dada3a825b6e4c3712423e1dc6`；backend `8d401907b6be81c36f92cf88e73e1dee61fd26a4`
- **提交授权：** not-authorized；未 commit、push、merge 或部署
- **状态：** done

## 1. 实现摘要

- 迁移前只读核对 live `sys_config`，确认密码策略行数为 0；应用权威 DML 的既有密码迁移块，并删除精确的 Redis `sys_config` cache map。
- 迁移后策略行数为 1、`JSON_VALID=1`、version=1、default mode=RANDOM；没有修改现有用户密码 hash。
- `SysLogFilter` 对敏感请求/响应头和请求参数进行名称归一化脱敏。
- 新增 JSON 日志副本递归脱敏；截断或非法 JSON 整段替换为 `[REDACTED]`，业务正文与加解密行为保持不变。

## 2. 修改范围与路径所有权

| 路径 | 所有权 | 目的 |
|---|---|---|
| `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/SysLogFilter.java</Path>` | writable | 请求/响应头与正文日志脱敏入口 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/SysLogBodySanitizer.java</Path>` | writable | JSON 凭据字段递归脱敏与失败关闭 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/SysLogMediaTypePolicy.java</Path>` | writable | JSON media type 判定 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/test/java/org/dromara/common/web/logging/SysLogFilterTest.java</Path>` | writable | 头、嵌套/截断 JSON 与业务副本回归 |
| `<Path>speculo/.speculo/specdev/changes/2026-08-29-login-password-policy-runtime-and-http-log-redaction/</Path>` | Lead-owned | Diagnosis、Spec、Evidence、状态 |
| `<Path>speculo/.speculo/specdev/status.json</Path>` | Lead-owned | active change 索引 |

- 其他 dirty 内容均为既有用户修改，未回退、未纳入本 change。
- 运行时数据动作复用既有 DML；没有修改历史 SQL 文件。

## 3. 验收映射

| Contract | 证据 | 结果 |
|---|---|---|
| AC-001 | live context HTTP 200、业务码 200、clientEnabled=true、8–30 位与四类字符策略 | pass |
| AC-002 | 没有修改 Sa-Token/JWT/auth strategy；旧 token 的 401 仍按既有前端流程清理 | pass |
| AC-003 | Authorization/Cookie/X-Api-Key/Set-Cookie/encrypt-key/password 参数测试均为 `[REDACTED]` | pass |
| AC-004 | 顶层密码、嵌套 data.accessToken、加密 wrapper 和截断 JSON 回归通过；真实 response 仍保留原 token | pass |
| AC-005 | common-web 16 tests、35-module admin package、前后端 HTTP probe 全绿 | pass |

## 4. TDD 与验证

| 命令或步骤 | 结果 | 摘要 |
|---|---|---|
| header red：`... -Dtest=SysLogFilterTest ... test` | expected fail | Authorization 原值导致 1 failure |
| header green | pass | 10 tests，0 failure |
| JSON red | expected fail | 完整 JSON 与截断 JSON 原值导致 2 failures |
| JSON green | pass | 11 tests，0 failure；含加解密 wrapper |
| parameter red/green | expected fail -> pass | password 参数先输出原值，复用敏感名称策略后转绿 |
| `./mvnw -pl ruoyi-common/ruoyi-common-web -am test` | pass | 16 tests，0 failure/error |
| `./mvnw -pl ruoyi-admin -am -DskipTests package` | pass | 35/35 reactor，ruoyi-admin package success |
| `curl http://localhost/` | pass | HTTP 200，HTML 113945 bytes |
| `curl ... /auth/client/context` | pass | HTTP 200，业务码 200，合法 policy projection |
| `git diff --check` | pass | 无 whitespace error |

- 测试中的 error/warn 输出来自故意注入 event sink/binding 失败的既有负向用例，suite 最终全绿。
- 当前 8080 进程在日志代码修改前由用户启动；登录 context 的数据修复已即时生效，日志脱敏在下一次后端重启后生效。
- 未使用真实用户凭据自动提交登录；精确报告的登录前置失败已通过公开 HTTP seam 消除。

## 5. 双轴审查

### 标准轴

- **输入：** backend 固定 HEAD 加本 change 四个 common-web working-tree 文件；engineering standards 与 Fowler baseline。
- **结果：** pass。
- **核对：** 敏感值 fail closed、普通日志行为保持、无认证语义变化、无 secret 写入工件、测试覆盖真实 Servlet interface。

### 规范轴

- **输入：** 本 change Spec 的 AC-001～AC-005 与 IN/OUT。
- **结果：** pass。
- **核对：** 所有 AC 均有 live 或自动化证据；没有修改用户密码、JWT 行为、前端源码或历史 SQL。

## 6. 交付与残余风险

- **E2E disposition：** reported-failure E2E passed；真实凭据登录未运行，因为没有读取或请求用户密码。
- **残余风险：** 已启动 JVM 仍使用旧日志类，下一次正常重启后加载脱敏实现；这不影响已经即时恢复的登录 context。
- **回滚：** common-web 代码可逆；不要通过删除策略行恢复旧故障。
- **最终 checkpoint：** parent/backend HEAD 未变化，加上述 working-tree 文件与运行时迁移状态。
- **Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-29-login-password-policy-runtime-and-http-log-redaction/evidence/direct-spec.md</Path>`
