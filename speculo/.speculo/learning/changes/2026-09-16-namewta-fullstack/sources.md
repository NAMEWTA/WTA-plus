# 来源：NAMEWTA 前后端全模块

访问日期：2026-09-16。路径相对于仓库根，除非标明 `{roots.*}`。

搜索范围：当前工作树 `frontend/`、`backend/`、`release-artifacts/docker/infrastructure/mysql/init/`、`.agents/skills/engineering-standards` 项目画像/模块地图/模块模式、已有四门 Learning Change 的 `course.md` / Lesson INDEX。未把 SpecDev 历史 ELI5 归档当作本课证据。

## 来源表

| Source ID | 类型 | 定位 | 访问日期 | 支持的 claim | 可信度 |
| --- | --- | --- | --- | --- | --- |
| S-001 | 工作树 | `backend/wta-admin/src/main/java/org/namewta/NamewtaApplication.java` | 2026-09-16 | 可部署主应用启动类 `NamewtaApplication` | high |
| S-002 | 工作树 | `backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java` | 2026-09-16 | `/auth/login|logout|register|client/context|social/callback` | high |
| S-003 | 工作树 | `backend/wta-admin/src/main/java/org/namewta/web/controller/CaptchaController.java` | 2026-09-16 | `/auth/code`、`/resource/sms/code`、`/resource/email/code` | high |
| S-004 | 工作树 | `backend/wta-modules/**` Controller / UseCase / `wta-api` | 2026-09-16 | 业务模块公开入口与跨模块合同 | high |
| S-005 | 工作树 | `frontend/apps/{admin-web,home-web,sso-web}` | 2026-09-16 | 三个 App 源码与 build 脚本存在；admin-web `application/services.ts` 组合全部业务 domain | high |
| S-006 | 工作树 | `frontend/packages/{domains,web-domains,platform,adapters,web-kit,api-contracts}` | 2026-09-16 | domain / web-domain / 平台端口 / 适配器 | high |
| S-007 | 硬合同 | `.agents/skills/engineering-standards/references/project/01-module-map.md` | 2026-09-16 | 模块职责、依赖方向、bundle-full/core | high，名单以 pom/目录再确认 |
| S-008 | 硬合同 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | 2026-09-16 | profile/notify/sso=layered；system/workflow/job/demo/ai=classic | high |
| S-009 | 硬合同 | `.agents/skills/engineering-standards/references/project/00-project-profile.md` | 2026-09-16 | 质量门禁、MySQL 基座、排除区 | high；其中「唯一可构建 App 是 admin-web」与 S-005 冲突 |
| S-010 | 项目事实 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 2026-09-16 | NAMEWTA 自有表与种子的唯一完整基座 | high |
| S-011 | 已有课 | `{roots.state}/learning/changes/2026-09-16-namewta-fullstack/children/2026-09-14-namewta-architecture/course.md` | 2026-09-16 | 总览 6 课；范围外指向 sso/third/notify；`coverage_depth=standard` | high（课程合同），不是覆盖完成 |
| S-012 | 已有课 | `{roots.state}/learning/changes/2026-09-16-namewta-fullstack/children/2026-09-14-wta-sso/course.md` 及 Lesson INDEX | 2026-09-16 | SSO 三切片；前端 sso-web 出范围 | high |
| S-013 | 已有课 | `{roots.state}/learning/changes/2026-09-16-namewta-fullstack/children/2026-09-14-wta-notify/course.md` 及 Lesson INDEX | 2026-09-16 | 通知八切片；前端出范围 | high |
| S-014 | 已有课 | `{roots.state}/learning/changes/2026-09-16-namewta-fullstack/children/2026-09-14-wta-third/course.md` 及 Lesson INDEX | 2026-09-16 | third 五切片；前端出范围 | high |
| S-015 | 工作树 | `backend/wta-api/src/main/java/org/namewta/{system,notify,profile,sso,third,workflow}/api` | 2026-09-16 | 跨模块 Java 合同面 | high |
| S-016 | 学习者偏好 | `{roots.state}/learning/learner-profile.md` | 2026-09-16 | `zh-CN`、`eli5`、默认 standard；本 Goal 用户改 deep | high |

## 项目事实 / 外部证据 / 类比 / 未知

| 类别 | 内容 | 来源 |
| --- | --- | --- |
| 项目事实 | 前后端合入本仓 | S-005、S-007、工作树 |
| 项目事实 | 三个前端 App 目录存在 | S-005 |
| 项目事实 | layered/classic 登记表 | S-008 |
| 项目事实 | 认证入口在 `wta-admin` 的 `AuthController`，不在 `wta-system` | S-002 |
| 外部证据 | 无 URL 权威 | — |
| 类比 | 大楼分房间 | 仅教学；失效处：房间之间仍有 `wta-api` 门，不是完全隔绝 |
| 未知 | home-web / sso-web 是否已在生产部署 | 本机未验证部署 |
| 未知 | GitHub Actions 是否 required check | S-009 `pending-decision` |

## 未决冲突

| ID | 冲突 | 本课裁决 | 状态 |
| --- | --- | --- | --- |
| C-001 | Skill 画像写唯一可构建 App 是 admin-web；工作树三个 App 都有 build | 以工作树为准：三包可被 workspace 构建；admin-web 是唯一组装全部业务 web-domain 的管理端 | contested |
| C-002 | 已有 sso/notify/third 课把前端划出范围；本 Goal 要求前后端 | 那些课升级时补前端格子，不删后端正文 | supported as plan |
| C-003 | architecture 课 standard vs 本 Goal deep | 总览六课升级为 deep，并作为 Wave 1（架构/数据流）的 C4 证据 | supported as plan |

## 不确定性处理

- Lesson 涉及 App 数量时必须同时说：工作树三个包；文档对「发布面」不一致。
- 四门课 locator 以 `locations.json` 为准：已在 `changes/2026-09-16-namewta-fullstack/children/<id>`；不得把旧独立根路径当永久链接。
- 不得发明库存里没有的 Controller / UseCase / domain 工厂。
