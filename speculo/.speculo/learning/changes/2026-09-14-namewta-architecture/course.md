# 课程设计：NAMEWTA / WTA-plus 整体架构

## 目标与期望效果

学完标准路径后，学习者能在不靠“感觉”的情况下：

1. 指出仓库里谁拥有什么（父仓文档、前端 App、后端组装、公共合同、数据基座）。
2. 说明一条业务请求为什么必须穿过特定层，而不能抄近路。
3. 遇到文档互相打架时，知道以当前工作树为准，并把冲突记成来源问题，而不是默默选一边。

本课不教通知、SSO、OSS 的完整机制；那些是后续独立 Change。本课只建立能走下去的地图、边界和合同意识。

## 学习者与表达/深度配置

| 字段 | 值 |
| --- | --- |
| 受众 | 正在本仓库工作的开发者；选题在用户跳过选项后，按当前工作区推断 |
| 交互语言 | `zh-CN` |
| expression_level | `eli5` |
| coverage_depth | `standard` |
| Lesson 时长 | `35` 分钟（允许 30–40） |
| 默认 Homework 题数 | `5`（按文件，覆盖回忆/机制/变式/迁移/误区） |

## 目标合同

| ID | 可观察目标 | 关键性 | 前置 OBJ | 证据类型 | Lesson | Homework |
| --- | --- | --- | --- | --- | --- | --- |
| OBJ-01 | 面对一张仓库树，能指出 `frontend/`、`backend/`、`docs/`、`release-artifacts/`、`.agents/skills/` 各自拥有什么，并说明交付不是 git submodule | 是 | none | 解释 + 指路径 | L-001 | HW-001 |
| OBJ-02 | 能画出后端依赖方向：可部署应用组装，业务模块走 `wta-api` 和最小 `wta-common-*`，common 不反向依赖业务 | 是 | OBJ-01 | 解释 + 画图 | L-002 | HW-001 |
| OBJ-03 | 能用登记表区分 `layered` 与 `classic`，说出新模块默认五层，并拒绝把两种模式混在同一模块 | 是 | OBJ-02 | 解释 + 对照登记表 | L-003 | HW-002 |
| OBJ-04 | 能说明前端 App → web-domain → domain → platform，以及 adapter/web-kit 的位置；能指出当前工作树里哪些 App 真实存在 | 是 | OBJ-01 | 解释 + 指包名 | L-004 | HW-003 |
| OBJ-05 | 能识别 HTTP/JSON、SQL 基座、OpenAPI transport 是公共合同；跨端变更先后端兼容合同，再更新前端 | 是 | OBJ-02, OBJ-04 | 解释 + 分类 | L-005 | HW-003 |
| OBJ-06 | 能按层口述一条管理端只读查询如何从浏览器走到 Mapper XML，并指出至少一种错误抄近路 | 是 | OBJ-02, OBJ-03, OBJ-04, OBJ-05 | 迁移/应用 | L-006 | HW-004 |

非关键、本课不做：通知 Outbox、SSO PKCE、OSS 直传内部状态机、Nacos 细节。这些内部已拆到独立 Change，不并进 L-001～L-006：

- SSO：`changes/2026-09-14-wta-sso`（`SsoOAuthController` / `SsoSessionController` 切片）
- 第三方 HTTP：`changes/2026-09-14-wta-third`（Provider/Endpoint/Credential/Observability/Gateway）
- 统一通知：`changes/2026-09-14-wta-notify`（notice/inbox/config/recipients/monitor/callback/application/outbox）


## 课程地图

结构采用**地图驱动，再走一条请求**。章节顺序可改，但每个 OBJ 仍要在对应 Lesson 里覆盖动机、直觉、精确定义与英文术语、机制、文本图、正例、反例、迁移、误区、总结和来源。

```text
L-001 仓库地图
   |
   +-- L-002 后端组装与依赖方向
   |      |
   |      +-- L-003 layered vs classic
   |
   +-- L-004 前端所有权与多 App
          |
          v
       L-005 公共合同（HTTP / SQL / OpenAPI）
          |
          v
       L-006 走一遍请求（合图）
```

| Lesson | 主题 | 估时 | 覆盖 OBJ | 活动预算（计划） |
| --- | --- | --- | --- | --- |
| L-001 | 这是谁的仓库：monorepo 所有权地图 | 35 | OBJ-01 | 定向 5 / 解释 12 / 地图 8 / 停顿 5 / 总结 5 |
| L-002 | 后端不是一锅炖：admin / api / common / modules | 35 | OBJ-02 | 定向 4 / 解释 14 / 图 8 / 停顿 4 / 总结 5 |
| L-003 | 两种后端写法：五层与 classic，为什么不能混 | 35 | OBJ-03 | 定向 4 / 机制 14 / 对照表 8 / 误区 5 / 总结 4 |
| L-004 | 前端不是 `src/api` 复制：App 只负责组装 | 35 | OBJ-04 | 定向 4 / 解释 12 / 包图 8 / 文档冲突 6 / 总结 5 |
| L-005 | 什么算合同：字段、路径、SQL、transport | 35 | OBJ-05 | 定向 4 / 分类 12 / 正反例 10 / 停顿 4 / 总结 5 |
| L-006 | 把地图走通：一次 GET 查询的层间接力 | 35 | OBJ-06 | 定向 3 / 走查 16 / 反例抄近路 8 / 迁移 4 / 总结 4 |

**标准路径**：L-001 → L-006 全上。

**较短路径**（仍要补 OBJ-03/05/06 才能声称“会改代码”）：L-001、L-002、L-004。只建立地图，不进入登记表和合同顺序。

**替代路线**：若学习者已经能指路径，可从 L-003 + L-005 起，再上 L-006；L-001/L-002 改为自学对照 `README.md` 与模块地图。

## 成功证据与范围外

成功（本课结束时仍是 `mastery.overall=unverified`，要等 Homework/Review）：

- 能不看文档口述六层所有权，再打开工作树核对。
- 能说明“新业务模块为什么不能先写 `ServiceImpl` 直接持 Mapper”。
- 能把“改一个列表页”拆成：后端合同 → domain → web-domain → 某个 App 选择。
- 能举出至少一处文档与工作树冲突，并说明以谁为准。

范围外：

- 不在本 Change 写 Lesson 正文或作业题；那是 `L-lesson` / `H-homework`。
- 不把本课综合进 `context/`；那是 `C-consolidate`。
- 不因为在本仓库写过代码就标记掌握。SSO / third / notify 函数级走查见三个新 Change，不改写本课 L-001～L-006。


## Revision 记录

| 时间 | 变化 | 原因 | 是否重新生成 Lesson |
| --- | --- | --- | --- |
| 2026-09-14T04:36:13.025Z | 初版课程地图 | 激活 A-assess-and-plan；用户跳过选题，按当前工作区推断主题 | 尚无 Lesson |
| 2026-09-14T07:42:38.672Z | 范围外改为指向三个模块深课 Change | 用户要 SSO/third/notify 切片深课；不并进 L-001～L-006 | 否 |

