---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 .agents/skills/*/SKILL.md：适用 engineering-standards（Java VO/MapStruct、测试与质量门禁；scope path:ruoyi-modules/ruoyi-demo domain/vo）。不适用 ruoyi-module-guide（未新增模块、非 Profile/System/Workflow/Notify/Third 跨模块接入，仅为 demo 存量 VO 注解修复）；不适用 namewta-fullstack-development（无前端/菜单/CRUD 垂直切片）；不适用 ruoyi-common-modules-guide（不改 common 入口）；不适用 java-api-compatibility / deploy-namewta-environment / upstream-fork-sync / project-customization-delivery。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"acac7d1d4341d3361c298ebc5a61841d794cafbe9f235171b56fddd7ecb0037d","phase":"implement","operation":"apply-demo-vo-automapper","inputs":["本Ticket路径契约与诊断修复契约","TestRichTextSummaryVo/TestRichTextVo 与对照 TestDemoVo/TestTreeVo 源码"],"outputs":["SummaryVo（及可选 Vo）注解落点符合同模块 AutoMapper 模式","未改 BaseMapperPlus/MapstructUtils/归档的差异说明"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/engineering-standards/references/java/crud-query-and-common.md</Path>","sha256":"0c5ba625a70daaff7622cda918caa8a455478260ce704a0716ac28da9209c25f","when":"编写或核对 Entity/VO AutoMapper 与字段映射前"},{"path":"<Path>.agents/skills/engineering-standards/references/java/core.md</Path>","sha256":"e05cef33b92b0333c753f11ed57bbc7d6ef62e34de67ac289bd1c5208934e213","when":"改 ruoyi-vue-plus-namewta Java 源前"}]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"acac7d1d4341d3361c298ebc5a61841d794cafbe9f235171b56fddd7ecb0037d","phase":"verify","operation":"run-demo-list-regression-gates","inputs":["本Ticket验证矩阵","ruoyi-demo 富文本 list 非空回归测范围"],"outputs":["含命令与退出码的 Evidence","修复前红/后绿与残余风险记录"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/engineering-standards/references/rules/testing.md</Path>","sha256":"913ab52d941f6533f93f22e0486c150c6c62163c61130a987ad0e082b88feac5","when":"设计或运行定向回归测前"}]}
resource_claims:
  - "demo.richtext.summary-vo.automapper"
  - "demo.richtext.list.nonempty-convert-regression"
artifact: ticket
change: 2026-09-12-richtext-list-automapper
id: T-01
title: 修复富文本 list SummaryVo AutoMapper 并补非空回归
status: done
planning_depth: standard
planning_depth_reason: 根因与修复契约已由诊断锁定；写集收紧到 demo VO 注解与新建回归测，需 3–7 步有序路线与可运行验证，但不改 schema/对外 API/全局基础设施。
ready: true
risk: low
blocked_by: []
contract_ids: [AC-001, AC-002, AC-003, AC-004]
owner: rvp-implement:4c753e13-b41b-408e-8cc8-275810f995e4
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestRichTextSummaryVo.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestRichTextVo.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/test/java/org/dromara/demo/service/impl/TestRichTextListAutoMapperTest.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestRichTextSummaryVo.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestRichTextVo.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/test/java/org/dromara/demo/</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestDemoVo.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestTreeVo.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/TestRichText.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/mapper/TestRichTextMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/service/impl/TestRichTextServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mybatis/src/main/java/org/dromara/common/mybatis/core/mapper/BaseMapperPlus.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/utils/MapstructUtils.java</Path>"
  - "<Path>{roots.state}/specdev/archive/2026-09/2026-09-08-richtext-third-oss/</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/diagnosis.md</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/diagnostics/</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-01: 修复富文本 list SummaryVo AutoMapper 并补非空回归

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/ticket/01-richtext-list-automapper-regression.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/spec.md</Path>`（诊断契约镜像；Lead 跳过正式 S 访谈）
- **上游诊断：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/diagnosis.md</Path>`
- **关联只读归档：** `<Path>{roots.state}/specdev/archive/2026-09/2026-09-08-richtext-third-oss/</Path>`（勿改、勿 reopen）
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/evidence/T-01.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map，读取项目 Skill 的 frontmatter 与入口并只展开适用于 `ALL`/`T-01` 的匹配项，再读取本 Ticket 与诊断全文。Map 中的 Skill 是最低必读集合；新的匹配项先由 Lead 同步到 Map 并重新校验。

## 1. 战略与来源

- **目标：** 让 `GET /demo/rich-text/list` 在库中已有当前用户+client 可见记录时，能把 `TestRichText` 转成 `TestRichTextSummaryVo` 并返回应用码 200，消除 `ConvertException` / 错误编号 `85384381`。
- **可观察产出：** create 成功后 list 非空路径不再 500；SummaryVo 带 `@AutoMapper(target = TestRichText.class)`；定向回归测修复前红、修复后绿。
- **来源：** `DIAG-2026-09-12-richtext-list-automapper`、`AC-001`–`AC-004`、`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestRichTextSummaryVo.java</Path>`、对照 `CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestDemoVo.java</Path>` / `TestTreeVo.java`、调用缝 `TestRichTextServiceImpl.list` → `mapper.selectVoPage`。
- **当前事实：** SummaryVo/Vo 均无 `@AutoMapper`；Mapper 第二泛型为 SummaryVo；非空 `selectVoPage` 调 `MapstructUtils.convert` 必炸；create/get 走手工 `view()` 掩盖缺口；空列表早退不触发。
- **Planning Depth 原因：** 契约已锁定，但仍需标准实现契约、有序执行与可运行前后红绿验证。

## 2. 决策状态

### 已锁定决策

- 主修：`TestRichTextSummaryVo` 增加 `@AutoMapper(target = TestRichText.class)`，模式对齐同模块 `TestDemoVo`/`TestTreeVo`。
- 回归：覆盖 create 后（或等价非空）list 不再 ConvertException；修复前红 / 修复后绿。
- 可选但建议：给 `TestRichTextVo` 同步补 `@AutoMapper`；因 Entity `contentHtml` vs Vo `html`，必须加 `@AutoMapping(source = "contentHtml", target = "html")`（或等价）；**不**把 create/get 的手工 `view()` 改走 convert（除非验收明确要求）。
- OUT：不改 `BaseMapperPlus` / `MapstructUtils`；不扩 OSS/三方 observability；不 reopen/改写归档 `2026-09-08-richtext-third-oss`。

### 已采用的低影响假设

- demo 模块目前无 `src/test`；本票新建测试目录与类名 `TestRichTextListAutoMapperTest`（或同等），优先测 `ITestRichTextService.list` / `TestRichTextMapper.selectVoPage` 非空转换接缝；若模块级 Spring 测成本过高，可用能证明 converter 存在且非空 convert 不抛的同等接缝，并在 Evidence 说明。
- 编译期 mapstruct-plus 处理器会在注解落地后生成 converter；验证命令需覆盖编译或生成结果。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| SummaryVo `@AutoMapper`；建议一并补齐 Vo+`@AutoMapping`；list 非空回归测 | 现有 Client+用户隔离、手工 `view()`、SummaryVo 字段集、`selectVoPage` 调用链、同模块 AutoMapper 惯例 | 改全局 Mapper/MapStruct 工具；OSS/三方；前端页；reopen 归档；改 create/update/remove 业务语义 |

## 4. 要构建什么

用户在富文本演示页保存一条记录后刷新列表：列表应成功返回刚创建（及其他归属当前会话）的摘要行，而不再出现 ConvertException 包装的应用码 500。摘要字段（`richTextId/title/version/updateTime`）保持与实体同名对齐；详情路径若将来误用 MapStruct，Vo 侧已具备 `contentHtml→html` 映射，避免静默丢 html。仓库保留一条可重复的非空 list 回归，证明修复前红、修复后绿。

## 5. 实现契约

- **入口或接缝：** `TestRichTextServiceImpl.list` → `TestRichTextMapper.selectVoPage` → `MapstructUtils.convert(List<TestRichText>, TestRichTextSummaryVo.class)`。
- **输入与输出：** 输入为已登录且 `clientPk` 可用、结果集非空的分页查询；输出为 `PageResult<TestRichTextSummaryVo>`，应用码 200，records 含可读 title 等摘要字段。
- **公共接口变化：** 无 HTTP/权限契约变化；仅补齐 VO 侧 mapstruct-plus 注解元数据。
- **不变量：** create/update/remove 的归属与 `view()` 语义不变；SummaryVo 不含 html，不必映射 `contentHtml`；空列表仍可早退成功。
- **状态或数据流：** Entity 行 → MapStruct 生成 converter → SummaryVo 列表；可选 Vo 注解不强制改调用链。
- **错误与失败行为：** 修复后非空 list 不得再抛 `cannot find converter from TestRichText to TestRichTextSummaryVo`；会话缺 Client 仍按既有业务错误（非本票范围）。
- **兼容要求：** 与同模块已有 `@AutoMapper` Vo 行为一致；回滚删除注解即回到现状（仍红）。
- **安全与隐私要求：** 不适用：本票不改变鉴权、归属过滤或数据暴露面。

## 6. 执行路线

1. 回读诊断与对照 `TestDemoVo`/`TestTreeVo`，确认写集仅限本票 VO/测试路径。
2. 先建立或草拟非空 list 转换回归接缝，在缺 `@AutoMapper` 时预期红（ConvertException 或断言缺注解+convert 失败）。
3. 给 `TestRichTextSummaryVo` 增加 `@AutoMapper(target = TestRichText.class)` 并补必要 import。
4. （建议）给 `TestRichTextVo` 增加 `@AutoMapper(target = TestRichText.class)` 与 `@AutoMapping(source = "contentHtml", target = "html")`；保持 `view()` 不动。
5. 编译/运行定向测，确认非空 list 绿；静态确认 SummaryVo 含 `@AutoMapper`。
6. 记录 Evidence（命令、退出码、前后红绿）；确认未改归档与全局工具类。
7. 形成可验证安全落点（implementation/source commit 按 Goal/Lead 授权策略，本规划岗不执行）。

## 7. 路径访问契约

- **预计修改点：** 与 `expected_changes` 对齐。
- **可写范围：** 与 `writable_paths` 对齐；越界前必须停止。
- **只读上下文：** 对照 Vo、Entity、Mapper、ServiceImpl、BaseMapperPlus、MapstructUtils、归档与诊断/diagnostics。
- **共享路径：** 无。
- **保留或不动：** 归档 change 全文；`BaseMapperPlus`/`MapstructUtils`；前端 `RichTextPage`；并行 change `2026-09-10-notify-channel-config`。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径（修复后绿） | `ITestRichTextService.list` 或 `selectVoPage` 非空 | `cd ruoyi-vue-plus-namewta && mvn -pl ruoyi-modules/ruoyi-demo -am -Dtest=TestRichTextListAutoMapperTest test`（或 Evidence 记录的等价接缝命令） | 非空 list 不抛 ConvertException；摘要含 title | `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/evidence/T-01.md</Path>` |
| 失败路径（修复前红） | 同测在缺注解基线 / 诊断 red_command | 诊断 `red_command` 或测试在修复前断言失败 | `assert_missing_AutoMapper` 等为红；或测试失败于 ConvertException | 同上 |
| 回归 / 静态 | SummaryVo 源码 | `rg '@AutoMapper' .../TestRichTextSummaryVo.java` | 命中 `@AutoMapper(target = TestRichText.class)` | 同上 |
| 可选 Vo 映射 | TestRichTextVo 注解 | 源码审查 / 可选单测 convert 保留 html | 若补了 AutoMapper，则必有 contentHtml→html AutoMapping | 同上 |
| OUT 守门 | 归档与全局工具 | `git status` / diff 范围审查 | 无归档、无 BaseMapperPlus/MapstructUtils 变更 | 同上 |

- **Workspace checks：** current-workspace（或 Goal Plan 指定的 source-worktree）运行上述定向 `mvn ... test` 及必要编译；不靠删测过门。
- **E2E disposition：** not-required：根因与验收接缝在后端 VO/MapStruct 转换；诊断已有 E2E 捕获作背景证据。Lead 若需页面确认，可在 parent-candidate/current-workspace 另跑 `GET /demo/rich-text/list`，但不阻塞本票 Definition of Done。
- **E2E owner/environment：** Lead；若补做则 current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source commit、parent before、适用 candidate/result SHA 与父分支包含关系（由 I/Lead 按授权填写）。

## 9. 发布、迁移与恢复

- **迁移顺序：** 不适用：无 schema/数据迁移。
- **兼容窗口：** 不适用：注解生成在编译期完成，无双写窗口。
- **监控信号：** 不适用：演示模块局部修复。
- **回滚或前向恢复：** 删除新增注解/测试即可回到修复前状态（list 非空仍红）；若误映射导致摘要字段空，修正 mapping 或回滚注解。
- **不可逆操作与批准点：** 无；commit/push/集成仅在 `execution_authorization` 获批后由 Lead/I 执行。
- **收缩条件：** 不适用：无 expand-contract。

## 10. 验收标准

- [ ] `AC-001`：`TestRichTextSummaryVo` 存在 `@AutoMapper(target = TestRichText.class)`，mapstruct-plus 可生成 Entity→SummaryVo converter。
- [ ] `AC-002`：在至少一条当前用户+client 可见记录前提下，list/非空 `selectVoPage` 不再抛 ConvertException；定向回归修复前红、修复后绿。
- [ ] `AC-003`：若本票修改 `TestRichTextVo` 并添加 `@AutoMapper`，则同时具备 `contentHtml→html` 的 `@AutoMapping`（或等价）；未改 Vo 时在 Evidence 显式记录“可选未做”及理由。
- [ ] `AC-004`：未修改归档 `2026-09-08-richtext-third-oss`、未改 `BaseMapperPlus`/`MapstructUtils`、未扩 OSS/三方 observability。
- [ ] 实现开始前已完整读取 Tickets Map，已读取项目 Skill 入口并完整展开适用于 `ALL`/`T-01` 的匹配项；新发现的匹配 Skill 已由 Lead 同步回 Map。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/evidence/T-01.md</Path>`。
- [ ] 实际项目修改未超出 `writable_paths`。
- [ ] Ticket 已按 Goal Plan/Lead 策略形成非空 implementation/source commit，direct-parent 或 candidate 验证通过且父分支 result 已记录（获授权后）。
- [ ] E2E disposition 已按 not-required 执行；若 Lead 补做 HTTP 确认则记录于 Evidence。
- [ ] 未发生未批准的范围、契约或发布偏差。
- [ ] Ticket、Tickets Map 和 Evidence 状态一致。

## 11. SKILL 调用计划

- **engineering-standards / implement / apply-demo-vo-automapper：** 进入写码前读取入口与 `java/core`、`java/crud-query-and-common`；对照同模块 AutoMapper 惯例落注解；禁止扩写 common。失败则 `block-ticket`。
- **engineering-standards / verify / run-demo-list-regression-gates：** 按 `testing.md` 设计并运行定向回归；Evidence 必须含命令、退出码、前后红绿与 AC 映射。失败则 `block-ticket`。
- **扫描不适用：** `ruoyi-module-guide` 等见 frontmatter `skill_scan`；不得伪造绑定。

## 12. 停止、检查点与交付

- **用户交付要求与数量：** 与 Map `requested_deliverables: []` 对齐；本 change 不额外要求命名交付件数。
- **必需 Skill/引用/测试不可用：** 阻塞本票，报告缺口，不静默替换。
- **归属与资源冲突：** 暂停本票；不接管他人状态；不改归档。
- **检查点：** 记录源版本、绑定摘要、已完成步骤、Evidence 与未闭合动作；恢复前回读 Map/Ticket/诊断。
- **完成出口：** 全部适用验收与 Skill 证据通过后交回 Goal/Lead；规划本岗不自启 I/P。
