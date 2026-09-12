---
schema_version: 1
artifact: diagnosis
change: 2026-09-12-richtext-list-automapper
status: root-cause-confirmed
feedback_loop_ready: true
red_command: "cd /workspace/vp-dev/ruoyi-vue-plus-docs && set +e; VO=ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestRichTextSummaryVo.java; EV=speculo/.speculo/specdev/changes/2026-09-12-richtext-list-automapper/diagnostics/richtext-41-backend-error.txt; ! rg -q '@AutoMapper' \"$VO\"; a=$?; rg -q 'cannot find converter from TestRichText to TestRichTextSummaryVo' \"$EV\"; b=$?; rg -q '错误编号: 85384381' \"$EV\"; c=$?; echo assert_missing_AutoMapper:$a assert_ConvertException:$b assert_error_id:$c; test $a -eq 0 -a $b -eq 0 -a $c -eq 0"
red_evidence: "assert_missing_AutoMapper:0 assert_ConvertException:0 assert_error_id:0; red_exit:0 (2026-09-12T00:36+08:00). Live E2E: POST /demo/rich-text/create → code 200 then GET /demo/rich-text/list → app code 500, error 85384381, ConvertException. Capture: diagnostics/red-loop-output.txt + diagnostics/richtext-41-backend-error.txt"
cleanup_status: clean
updated_at: 2026-09-12T00:36:00+08:00
---

# Diagnosis: 富文本 list MapStruct 缺少 TestRichText→TestRichTextSummaryVo 转换器

> Supersedes / follow-up of archived `<Path>{roots.state}/specdev/archive/2026-09/2026-09-08-richtext-third-oss</Path>`（Lead 明确不 reopen；本 change 为独立 active）。

## 1. 现象与影响

- **页面：** `http://127.0.0.1:4174/demo/rich-text`
- **可观察序列：** `POST /demo/rich-text/create` 应用码 200、记录已写入；紧接着 `GET /demo/rich-text/list?pageNum=1&pageSize=50` HTTP 200 但**应用码 500**，消息含错误编号 `85384381`。
- **用户影响：** 保存看似成功后刷新列表失败，演示页报错；空列表时可能不触发（见最小复现）。
- **时间：** E2E 捕获于 2026-09-11 16:31:44Z（对应用户侧 2026-09-12 00:31+08 前后）。
- **严重度：** 功能阻断（list 在有数据时确定性失败）。

## 2. 红灯反馈回路

- **命令：** 见 frontmatter `red_command`（静态缺注解断言 + E2E 栈精确症状断言；可无人值守重跑）。
- **至少一次真实输出：** `assert_missing_AutoMapper:0 assert_ConvertException:0 assert_error_id:0` / `red_exit:0`；完整捕获 `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/diagnostics/red-loop-output.txt</Path>`。
- **精确症状断言：**
  1. `TestRichTextSummaryVo.java` **不含** `@AutoMapper`；
  2. E2E 日志含 `io.github.linpeilie.ConvertException: cannot find converter from TestRichText to TestRichTextSummaryVo`；
  3. 同一请求错误编号 `85384381`。
- **耗时：** <1s（静态+日志断言）；原始 E2E 路径约数秒（含登录与保存）。
- **确定性/复现率：** 在「库中至少 1 条当前用户+client 可见记录」前提下 list 确定性红；空结果集走 `BaseMapperPlus.selectVoPage` 早退，不调用 convert，不红。
- **Agent 可运行性：** autonomous（本 red_command）；完整 HTTP 重放需登录态/验证码，属 structured-HITL，已用既有 E2E 日志代替。
- **无法建立时已尝试方式和所需输入：** 无鉴权 `GET /demo/rich-text/list` → 401「登录状态异常」；无 captcha 的 `/auth/login` → 403。故以 E2E 捕获物作 HTTP 路径证据，不以在线登录为门控。

## 3. 最小复现

- **环境与输入：** 后端 `127.0.0.1:18080` + 前端 `4174`；已登录且会话含 `clientPk`；`test_rich_text` 中至少一条归属当前 user+client 的行（可由 create 产生）。
- **剩余步骤：**
  1. （可选）`POST /demo/rich-text/create` 写入标题/html → 200；
  2. `GET /demo/rich-text/list` → 应用码 500 + ConvertException。
- **逐项删除证据：**
  - 去掉 list：仅 create 成功，不暴露故障（create 走手工 `view()`，不经 MapStruct）。
  - 列表为空：`selectVoPage` 在 `CollUtil.isEmpty(list)` 时直接返回，不调用 `MapstructUtils.convert` → 不红（故障被遮住）。
  - 对照同模块 `TestDemoVo`/`TestTreeVo` 已有 `@AutoMapper(target=…)`，list/VO 路径正常。
- **最后红灯证据：** E2E 栈 `TestRichTextController.list` → `TestRichTextServiceImpl.list:40` → `mapper.selectVoPage` → `MapstructUtils.convert` → `Converter.convert` → ConvertException。
- **捕获物：**
  - `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/diagnostics/richtext-41-backend-error.txt</Path>`
  - `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/diagnostics/richtext-40-before-save.png</Path>`
  - `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/diagnostics/richtext-41-save-error.png</Path>`
  - 源路径（Lead E2E）：`temp/team/lead/e2e/richtext-41-backend-error.txt` 等。

## 4. 假设与证伪

| 排名 | 假设与预测 | 支持证据 | 单变量实验 | 结果 |
|---|---|---|---|---|
| 1 | `TestRichTextSummaryVo` 缺少 `@AutoMapper(target=TestRichText.class)`，mapstruct-plus 未生成 Entity→VO converter；`selectVoPage` 非空时必炸 | 源码无注解；对照 `TestDemoVo`/`TestTreeVo` 有注解；栈明确 ConvertException | `rg '@AutoMapper' TestRichTextSummaryVo.java` 应失败（缺注解）；E2E 非空 list 应 500 | **确认** |
| 2 | 会话缺 `clientPk` 导致 list 500（旧归档诊断路径） | 旧 diagnosis 曾归因于此；当前实现 `clientPk()` 会抛「当前会话缺少 Client」 | 本 E2E：create 已成功（同样调用 `clientPk()`），故会话有 Client；栈为 ConvertException 而非 ServiceException | **证伪**（非本次根因） |
| 3 | `TestRichTextVo` 缺 AutoMapper 直接导致 list 失败 | Vo 同样无注解 | list 的 `BaseMapperPlus` 第二泛型是 `TestRichTextSummaryVo`，非 Vo；create/get 用手工 `view()` | **证伪**（list 路径）；详情若将来改走 MapStruct 仍需补 |
| 4 | MyBatis/SQL 或权限过滤器故障 | HTTP 进入 controller 后才炸 | 栈顶在 Converter，非 SQL；create 同权限成功 | **证伪** |

## 5. 已确认根因

- **触发条件：** 已登录且 `clientPk` 可用；`GET /demo/rich-text/list` 查询结果**非空**。
- **失败机制：** `TestRichTextMapper extends BaseMapperPlus<TestRichText, TestRichTextSummaryVo>` → `selectVoPage` 查出 `List<TestRichText>` 后调用 `MapstructUtils.convert(list, TestRichTextSummaryVo.class)` → linpeilie `Converter` 查找 `TestRichText → TestRichTextSummaryVo` 映射失败抛 `ConvertException` → `GlobalExceptionHandler` 包装为错误编号 `85384381` 的应用码 500。
- **根因位置：** `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/domain/vo/TestRichTextSummaryVo.java</Path>`（缺失 `@AutoMapper(target = TestRichText.class)`）；调用缝 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/service/impl/TestRichTextServiceImpl.java</Path>` L40；基础设施 `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mybatis/src/main/java/org/dromara/common/mybatis/core/mapper/BaseMapperPlus.java</Path>` L384–393、`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/utils/MapstructUtils.java</Path>`。
- **漏检原因：** 归档 change `2026-09-08-richtext-third-oss` 富文本侧当时主要按 clientPk/前端 catch 收敛；create/详情用手工 `view()` 掩盖了 list 的 MapStruct 缺口；空表或未刷新 list 时不复现；缺少针对 `selectVoPage` 非空转换的回归测。
- **为何排除其他候选：** 见假设表 #2–#4；create 成功排除 clientPk/权限/SQL 为主因。
- **确认实验：** 源码缺注解 + E2E 精确 ConvertException 栈 + 对照同模块带 `@AutoMapper` 的 Vo 行为一致。

## 6. 修复契约

- **必须改变：**
  1. 给 `TestRichTextSummaryVo` 增加 `@AutoMapper(target = TestRichText.class)`（与 `TestDemoVo`/`TestTreeVo` 同模式），使 mapstruct-plus 生成 Entity→SummaryVo converter。
  2. 补 **list 非空** 回归：修复前红 / 修复后绿（见下）。
- **必须保持：**
  - create/update/remove 的 Client+用户隔离与手工 `view()` 语义（除非明确改走 MapStruct）。
  - SummaryVo 字段集（`richTextId/title/version/updateTime`）与实体同名对齐；**不必**为 SummaryVo 映射 `contentHtml`（摘要无 html 字段）。
  - 不扩大本 change 到三方 observability / OSS CORS（已在归档 change 处理）。
- **字段映射注意（详情路径，按需）：** Entity `contentHtml` vs Vo `html`。当前 `TestRichTextServiceImpl.view()` 手工 `setHtml(e.getContentHtml())`。若同时给 `TestRichTextVo` 补 `@AutoMapper` 并改用 convert，必须加 `@AutoMapping(source = "contentHtml", target = "html")`（或等价），否则详情 html 会丢；**本次 list 红灯不强制改 Vo**，但建议一并补齐以免后续 `selectVo*` 误用。
- **正确测试 seam：** 优先模块/集成测调用 `ITestRichTextService.list` 或 `TestRichTextMapper.selectVoPage` 在非空数据下不抛 ConvertException；次选已登录 HTTP `GET /demo/rich-text/list`。缺失则由 Tickets 新建测，路由 R 仅当架构争议。
- **回归测试：**
  - **修复前红：** 非空 list → ConvertException / 应用码 500 / 错误编号类未知异常；本 red_command 中 `assert_missing_AutoMapper` 为 0。
  - **修复后绿：** 同条件 list 应用码 200，records 含刚 create 的 title；`@AutoMapper` 出现在 SummaryVo；ConvertException 不再出现。
- **OUT：** 不在诊断阶段改生产 Vo 注解；不 reopen 归档 change；不改 BaseMapperPlus/MapstructUtils 全局行为。
- **风险与回滚：** 注解生成需编译期 mapstruct-plus 处理器；回滚删除注解即可恢复现状（仍红）。若误改字段名映射导致摘要字段为空，回滚注解或修正 mapping。
- **推荐下游：** **T-tickets**（切片：SummaryVo AutoMapper + 可选 Vo+AutoMapping + list 回归测）→ **I-implement**。一般无需 S-spec（契约已清晰）；无架构争议不强制 R。

## 7. 清理

- **原始回路重跑：** 已用 red_command 重跑，结果见 `diagnostics/red-loop-output.txt`。
- **`[DEBUG-...]` 搜索：** 未插入生产调试插桩。
- **一次性脚本/原型：** 无；diagnostics 仅为证据副本与 red 输出。
- **未清理项 owner 与删除条件：** 无。归档 change 只读，未改动。
