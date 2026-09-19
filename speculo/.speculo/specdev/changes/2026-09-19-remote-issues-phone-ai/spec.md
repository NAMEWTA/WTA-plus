---
schema_version: 3
artifact: spec
change: 2026-09-19-remote-issues-phone-ai
status: ready
ready_for_tickets: true
sources: ["USER-DECISION:2026-09-19-G-consensus", "LOG-003", "LOG-004", "LOG-005", "LOG-006"]
---

# Spec: 手机号必填与 Snail AI 退出

- **Spec：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>
- **当前 ADR / 领域上下文：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ADR.md</Path>；<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/CONTEXT.md</Path>
- **决策与代码事实：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/LOG.md</Path>；<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/grounding.md</Path>

## 1. 问题与目标

### 问题陈述

手机号在公开注册、管理维护、导入和个人资料的前后端合同不一致，允许产生或继续写入空手机号。产品已决定不再使用 Snail AI，但当前还保留注册桥、聊天 iframe、管理台、服务、数据库基座与发布依赖。

### 目标用户与场景

注册用户需要明确填写手机号；管理员通过用户表单或 Excel 新增/更新账号；已有用户保存个人资料。部署维护者需要构建一个不依赖 Snail AI 的同源产品，同时保留历史数据与未来 AI 模块边界。

### 成功标准

受影响写入入口最终保存有效手机号，现有空手机号账号仍可登录；前端提示与后端约束一致。当前产品不再呈现、调用、构建或启动 Snail AI；两份 Java AI artifact 保持可构建占位，六份 SQL 基座仍可初始化，历史 AI 数据不受处理。

### 非目标

不建设替代 AI 平台，不改变邮箱、短信验证/登录或第三方绑定流程，不修改真实环境数据，不部署或发布。

## 2. 解决方案与外部行为

### 解决方案摘要

手机号约束落在真实写入边界，并贯穿 Admin/Home 注册、管理用户和个人资料页面、领域传输、API 与 Excel 导入。Snail AI 按业务消费者退出后再收缩服务/发布资产的顺序清除；可审查的本地产物与真实环境升级分开验收。

### 主要流程

1. 新注册、新建用户、新增导入：必须提供非空、符合现有大陆手机号规则的号码；失败不创建账号。
2. 管理员编辑、个人资料保存、覆盖导入：合并现有记录和输入后的手机号必须有效。未提供号码时允许沿用原有效号码；原记录为空时必须补齐，不能通过省略绕过。
3. 更新继续采用现有 null/省略表示不修改字段的兼容语义；显式空串或仅空白不是清空指令，拒绝整个受影响写入。非空值使用既有标准化方式，校验前后不得出现不同号码。
4. 存量空手机号账号继续按原策略登录，不增加登录补录重定向、锁定或全量回填。
5. Admin 不再显示 Snail AI 聊天和控制台入口，也不再请求旧用户注册桥或生成带旧凭据的 iframe URL。存量菜单响应含两个已退役组件键时仅过滤这些退役项；无关未知组件仍按原诊断合同处理。
6. 当前源码和新发布产物退出 Snail AI 独立 server、starter、路由、环境变量与关联运行配置。已有不可变 release、原始来源、历史证据与运行库数据保持原状。

### 边界、失败与稳定错误行为

- 缺失、空白或格式错误的手机号通过既有校验异常/业务错误响应返回，不引入未经核对的新 HTTP 状态或错误码；页面显示字段错误并保留可修正输入。
- 导入维持既有逐行成功/失败报告和事务语义；无效行不得插入或覆盖，其他行按原有策略继续。必须使用真实校验器证明约束，不能使用空 validator 冒充通过。
- 原注册关闭、验证码、密码策略、Client/RBAC 与数据范围保持；不因必填切换新增验证码消耗或越权写入。
- 已退役 Snail AI API 不再由业务 Controller 提供，旧 URL 按现有未映射路由合同失败关闭；不返回模拟成功或重定向到新 AI。
- 没有 Snail AI 服务、配置和新库 vendor 表时，保留的业务应用仍能构建并通过适用启动/集成验证。

### 状态转换与不变量

账号允许存在“历史空手机号”状态，但受影响的资料写入成功后必须进入有效手机号状态；此后不能通过这些入口回到空值。独立的登录、密码重置、状态禁用等操作不被混同为资料编辑而强加新规则。既有手机号唯一性策略与 Client 语义不变；本次不引入 DB 唯一索引，不声称解决存量重复或并发唯一性。

## 3. 用户故事

- **US-001**：作为注册用户，我在 Admin/Home 看到明确手机号必填要求，合法填写后完成原有注册流程。
- **US-002**：作为管理员，我通过新增、编辑、Excel 导入维护有效手机号，失败时知道是哪条记录需要修正。
- **US-003**：作为已有用户，我仍能登录，保存资料时补齐手机号，已有号码不会因省略字段而丢失。
- **US-004**：作为 Admin 用户，我不再看到或进入已退出的 Snail AI 功能。
- **US-005**：作为维护者，我能构建、初始化和验收无 Snail AI 的产品，保留历史 AI 数据与 Java 占位模块。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | Admin/Home 注册开启 | 合法号码注册，以及省略/空白/非法号码提交 | 页面含必填字段，domain 正确传输；合法路径成功，无效请求拒绝且不新增用户 | S1、S2 |
| AC-002 | 有当前 Client 的管理权限 | 新增/编辑用户；分别给合法值、空值、错误值及省略字段 | 新增要求号码；编辑仅可沿用原有效值或保存新有效值；失败不部分覆盖；原权限保持 | S1、S2 |
| AC-003 | Excel 含新增、覆盖更新、有效和无效行 | 执行既有导入 | 新增空值拒绝，覆盖后号码有效；失败行不落库且有既有逐行反馈，原其他行策略保持 | S1 |
| AC-004 | 已登录用户原号码有效或为空 | 保存个人资料，测试省略、null、空串/空白、有效值 | 省略/null可保留原有效号码；原为空则必须补齐；空串/空白拒绝，不清空已有号码 | S1、S2 |
| AC-005 | 已有空手机号用户、注册关闭及跨 Client 场景 | 登录或尝试相关受限操作 | 原密码登录可用；无强制补录跳转；注册开关、密码/验证码、权限与数据范围无回归 | S1、S2 |
| AC-006 | 新菜单或存量仍返回两个旧 AI 组件键 | 登录恢复导航、访问旧聊天/控制台入口 | 不呈现旧 AI 菜单/页面，不发起旧桥请求或 iframe；仅特定退役键被过滤，无关未知键仍诊断 | S2、S3 |
| AC-007 | AI 业务消费者退出 | 调用旧桥、检查 Java AI 模块和 full/core 产物 | 旧 Controller/starter/自动配置不在业务接入面；两份 Maven 占位可构建；full/core 既有 AI artifact 选择合同保持 | S1、S4 |
| AC-008 | 业务调用与前端依赖已迁出 | 对当前受管源与前端构建执行检查 | 旧 domain/web-domain 实现和依赖、旧 monitor target、凭据 URL 与未使用配置退出；文档与架构检查承接最终边界 | S3、S4 |
| AC-009 | 最终同一源码候选 | 构建后端/前端并生成和验证本地 release manifest/Compose/Nginx | 不产出或启动 Snail AI server，不要求其 env、upstream/回调端口；SnailJob、MCP、NAMEWTA OpenAPI 继续有效 | S4、S5 |
| AC-010 | 两个隔离场景：全新数据库；持有历史 sai_* 哨兵数据的数据库 | 新库执行六文件基座；已有库仅验证新候选启动/退出合同，不重放基座 | 新库无 sai_* 表；旧表/数据摘要不变，无迁移/删除；40 槽位合法空占位，其他基座功能正常 | S5 |
| AC-011 | 最终账号与 AI 合同已实现 | 使用正式工具更新当前 OpenAPI、初始化数据、文档和发布事实 | 当前快照包含手机号最终约束并退出旧 AI API；示例账号满足正常写入场景，旧 AI 初始化菜单退出；历史快照不手改 | S3、S5 |
| AC-012 | 全部切片集成于同一候选 | 运行受影响完整门禁与源/产物检查 | 前后端、双 bundle、release、真实初始化和退出回归有同源证据；没有把未运行、skip 或旧 change 证据当作本次通过 | S1–S5 |

## 5. 范围

### IN

上述账号入口的字段校验、传输和页面提示；Snail AI 的源码、菜单/页面、Java/前端依赖、server、配置、发布/初始化声明及当前文档事实；相应测试与可审查本地产物。

### REUSE

已有大陆手机号规则、错误壳、注册与权限流程、classic System 模式、前端 domain/web-domain 边界、六文件 MySQL 8.4 基座、正式 OpenAPI 工具和不可变发布来源校验。

### OUT

- **OOS-001**：Go/Python 新 AI 能力已归 <Path>{roots.state}/specdev/changes/2026-09-19-go-python-ai-platform/</Path>，用户暂缓。
- **OOS-002**：邮箱必填、短信认证/登录、手机号验证所有权、第三方自动注册、全量号码清洗或新的唯一性策略。
- **OOS-003**：删除/迁移真实旧 AI 数据、操作生产环境、重放已有库基座、清理历史 release 与审计材料。
- **OOS-004**：删除 SnailJob、Spring AI BOM、common-mcp、NAMEWTA 入站 OpenAPI，或全仓 cde/wta 重命名。
- **OOS-005**：提交、推送、正式发布、部署、远程关闭和归档；需要在对应阶段取得具体授权。

## 6. 已锁定实现约束

- **DEC-001**：后端真实写入边界决定有效性，前端必填不能替代 API/导入验证；省略字段合并语义与存量登录兼容按 LOG-003。
- **DEC-002**：Snail AI 明确退出，Java 两模块最终为占位；不做自动替代平台。来源 LOG-004/006。
- **DEC-003**：保留历史数据和六文件槽位，40 文件仅保留合法无 vendor 表/数据的初始化语句；不生成运行库清理迁移。来源 LOG-005/006。
- **DEC-004**：受影响 HTTP 查询 GET、变更 POST、安全 @Log、Client/RBAC 和正式生成物边界遵守项目硬约束。
- **DEC-005**：兼容行为显式记录：原先可空的新建合同将收紧，仓内调用者和测试同步；Java 公开签名不借此删除/改名。移除旧 Snail AI 能力是用户批准的行为退出，不伪造替代 API 或兼容成功。

## 7. 数据、接口与兼容

- **公共接口变化：** 注册和用户资料写入收紧手机号校验，字段名沿用 phoneNumber；省略/null更新语义保留；Snail AI 专属 HTTP 桥退役。
- **数据模型与持久化：** 不新增表，不将手机号列改为全量 NOT NULL，不添加唯一索引；保留旧空值供按需补齐。
- **兼容要求：** 无关用户 API、Client 授权、登录和未知菜单诊断保留。当前生成合同统一在收缩门关闭前刷新，之前历史快照只是原来源记录。
- **迁移要求：** 本地源代码与初始化基座修改；已有环境升级不得重放六文件，不清旧 AI 数据。当前不指定或执行真实环境源/目标 Tag 迁移。
- **发布或运维影响：** 新候选 manifest、镜像、Compose/Nginx 和配置不再要求 Snail AI。新旧前后端按同一版本交付，不能宣称兼容混版热切换；真实发布仍单独批准。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 不把完整手机号写入新增日志/错误；保留现有脱敏。旧 trustedCredential 不进入浏览器 URL 或构建物。权限失败关闭。
- **NFR-002 性能与容量：** 不设置虚构阈值；号码合并/校验遵循现有查询与事务机制，不新增无界任务或外部 I/O。
- **NFR-003 可用性与可靠性：** 不阻断历史空手机号账号登录；不因缺少 Snail AI 服务/表导致保留应用启动失败。
- **NFR-004 可观测性与运营：** 保留安全 @Log 和既有导入失败反馈；发布清单能证明退出边界与产物来源。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| S1 | Java validation/service/HTTP/导入集成 | AC-001–005、007、012 | <Path>backend/wta-admin/src/test/java/org/namewta/web/service/SysRegisterServiceRegistrationUnitTest.java</Path>；<Path>backend/wta-admin/src/test/java/org/namewta/test/password/write/PasswordImportUnitTest.java</Path>；backend 工作目录执行 ./mvnw test | 含真实 validator、合法/非法请求与无副作用断言的报告 |
| S2 | domain、页面、浏览器 | AC-001、002、004–006、012 | <Path>frontend/e2e/recoverable-registration.spec.ts</Path>；<Path>frontend/e2e/browser-https-transport.spec.ts</Path>；pnpm --dir frontend test:e2e --workers=1 | 交互/请求断言与截图 |
| S3 | 结构与生成合同 | AC-006、008、011 | pnpm --dir frontend architecture:check；architecture:test；pnpm --dir frontend --filter @namewta/tooling-openapi openapi:check | 正式工具来源、当前快照、架构/退役键测试 |
| S4 | 构建/模块/配置 | AC-007–009、012 | backend 双 bundle clean package；<Path>scripts/ci/verify-admin-bundle.sh</Path> full/core；前端 lint/typecheck/test/build:dev/build:prod | JAR 依赖清单与 App 构建报告 |
| S5 | release/隔离 MySQL/保留数据 | AC-009–012 | bash release-artifacts/scripts/verify-release.sh；bash scripts/ci/run-external-services.sh；扩展既有隔离初始化/哨兵数据测试 | SQL 清单、初始化结果、前后数据摘要、Compose/Nginx 与 manifest 合同 |

上表 pnpm 命令按项目脚本名列示；执行时统一进入 frontend cwd 使用已核验的 corepack pnpm 10.34.5，不使用系统默认 pnpm 9。真实 release 构建另须 T-03 的任务专用 shim 与实际 manifest 校验路线。

测试场景可新增到相邻稳定接缝；测试自身必须实际观察目标行为。SQL/真实服务场景使用任务自有隔离资源，由 Lead 验收，不能连接未授权运行库。

## 10. 风险、假设与未决问题

### 风险

单纯给 BO 加 @NotBlank 会破坏省略字段更新；只改 UI 会漏掉直接 API 和导入。只删新库菜单不会清除存量失效入口；只删 AI server 会遗漏发布模板或 root dependencyManagement。旧模板计数须按真实表清单更新而非降低门槛。

### 已采用的低影响假设

沿用 wta artifact、现有大陆手机号格式和错误文案体系。40 槽位可仅保留 SET NAMES utf8mb4;，说明放相关 README；通过六文件校验验证。不把整个未注册菜单集合隐藏，仅处理两条明确退役 AI 键。

### 未决问题

无。
