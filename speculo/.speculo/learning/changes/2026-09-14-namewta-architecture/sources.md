# 来源：NAMEWTA / WTA-plus 整体架构

访问日期：2026-09-14。路径相对于仓库根，除非标明 `{roots.*}`。

搜索范围：根 README、`docs/namewta-enhancements.md`、`frontend/apps/**` 与 `frontend/package.json`、`backend/wta-admin` 启动类、`.agents/skills/engineering-standards` 的项目画像/模块地图/模块模式/架构规则/决策。未把 SpecDev 历史 ELI5 归档当作本课证据。

## 来源表

| Source ID | 类型 | 定位 | 访问日期 | 支持的 claim | 可信度 |
| --- | --- | --- | --- | --- | --- |
| S-001 | 项目事实 | `README.md` | 2026-09-14 | 单一 monorepo；frontend/backend 合入；admin-web / home-web / sso-web 在根 README 标为已激活；admin 组装、api 合同、common 基础能力 | high（产品说明，需与工作树对读） |
| S-002 | 项目事实 | `docs/namewta-enhancements.md` | 2026-09-14 | 前端分层职责；domain 对齐后端模块；Client/RBAC；发布面写 admin-web + home-web | high，与 S-001/S-008 对“sso-web 是否发布面”有张力 |
| S-003 | Skill 摘要 | `.agents/skills/engineering-standards/references/project/00-project-profile.md` | 2026-09-14 | Java 21 / Spring Boot 4.1.0；pnpm 前端；质量门禁表；写“唯一可构建 App 是 admin-web” | medium：门禁与拓扑有用；App 数量与工作树冲突 |
| S-004 | Skill 摘要 | `.agents/skills/engineering-standards/references/project/01-module-map.md` | 2026-09-14 | 后端模块职责、依赖方向、bundle-full/core、前端 App→web-domain→domain→platform | high，模块名单需以 pom/目录再确认 |
| S-005 | 硬合同 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | 2026-09-14 | layered/classic 登记表；profile/notify/sso = layered；system/workflow/job/demo/ai = classic | high（登记表是模式唯一入口） |
| S-006 | 硬合同 | `.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md` | 2026-09-14 | ARCH-002 后端依赖；ARCH-002A 五层；ARCH-003 前端边界；ARCH-004 合同顺序；ARCH-001 仍写 submodule | ARCH-002/003/004 high；ARCH-001 与工作树冲突 |
| S-007 | 决策 | `.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md` | 2026-09-14 | DEC-004 不以 submodule 交付；DEC-009 新模块默认 layered；Ratchet | high |
| S-008 | 工作树 | `frontend/package.json`、`frontend/pnpm-workspace.yaml`、`frontend/apps/{admin-web,home-web,sso-web}/package.json` | 2026-09-14 | 三个 App 包存在且带 build 脚本；`build:prod` 为 `pnpm -r --if-present build`；`build:dev` 显式包含 home-web | high |
| S-009 | 工作树 | `frontend/apps/README.md` | 2026-09-14 | 当前状态只点名 admin-web 与 home-web 可构建部署 | high（该文件原文），覆盖面小于 S-008 |
| S-010 | 工作树 | `backend/wta-admin/src/main/java/org/namewta/NamewtaApplication.java` | 2026-09-14 | 启动类包名 `org.namewta`，类名为 `NamewtaApplication` | high |
| S-011 | 治理 | `AGENTS.md` | 2026-09-14 | 项目 Skill 唯一根 `.agents/skills/`；工程规范入口 | high |

## 项目事实 / 外部证据 / 类比 / 未知

| 类别 | 内容 | 来源 |
| --- | --- | --- |
| 项目事实 | 前后端合入本仓，默认交付不是 git submodule | S-001, S-007, 工作树存在 `frontend/` 与 `backend/` |
| 项目事实 | 三个前端 App 目录与构建脚本存在 | S-008 |
| 项目事实 | layered 登记：profile、notify、sso；classic 登记：system、workflow、job、demo、ai | S-005 |
| 项目事实 | 启动类名为 `NamewtaApplication` | S-010 |
| 外部证据 | 无（本课未引用外部网站；无 URL 权威） | — |
| 类比 | 大楼分房间 | 仅教学；失效处见 foundation.md |
| 未知 | home-web / sso-web 是否已在生产环境部署 | 本机未验证部署 |
| 未知 | GitHub Actions 是否已成为 required check | S-003 `pending-decision` |

## 未决冲突

| ID | 冲突 | 本课裁决 | 状态 |
| --- | --- | --- | --- |
| C-001 | S-006 `ARCH-001` 要求 submodule 独立提交；S-001/S-007 与工作树是 monorepo | 按工作树与 DEC-004：合入 `frontend/`、`backend/`，不以 submodule 交付。ARCH-001 视为过期规则文本 | contested；Skill 文本待修订，不在本 Work 改 Skill |
| C-002 | S-003 写唯一可构建 App 是 admin-web；S-008 显示三个 App 都有 build | 以 S-008 工作树为准：三个包可被 workspace 递归构建 | contested |
| C-003 | S-001 三终端已激活；S-002/S-009 发布面常写成 admin-web + home-web | 事实：`sso-web` 源码与 build 存在。是否等于“产品发布面”未在本课关闭 | unresolved |
| C-004 | 产品包名 `org.namewta`，启动类是 `NamewtaApplication` | 启动类是 `NamewtaApplication`，不当作旧产品边界 | supported（命名事实），不当作架构边界 |

## 不确定性处理

- Lesson 涉及 App 数量时必须同时说：工作树三个包；文档对“发布面”不一致。
- 不得把 ARCH-001 当现行交付合同。
- 不得把本 Change 的综合写入 `context/`。
