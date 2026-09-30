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
| S-L086-01 | 工作树 | `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssMigrationController.java` | 2026-09-22 | 迁移七个 HTTP 方法与四颗权限 | high |
| S-L086-02 | 工作树 | `backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/OssStorageMigrationService.java` | 2026-09-22 | dry-run 零写入；PRIVATE 到 PUBLIC_READ；复制、CAS、回滚、清理窗口 | high |
| S-L086-03 | 工作树 | `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssUploadController.java` | 2026-09-22 | 直传控制面；abort 为 POST | high |
| S-L086-04 | 工作树 | `backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/upload/OssUploadService.java` | 2026-09-22 | init 绑定命名策略与 readiness；complete 登记临时对象 | high |
| S-L086-05 | 工作树 | `SysOssServiceImpl.upload(File)` 与 `OssProtocolCutoverUnitTest` | 2026-09-22 | 服务端字节上传无 HTTP、无当前调用方 | high |
| S-L086-06 | 工作树 | `SysOssConfigServiceImpl`、`OssConfigChangeListener` | 2026-09-22 | 保存配置不建桶；提交后刷新缓存和 readiness | high |
| S-L086-07 | 工作树 | `OssStorageReadinessService`、`application.yml`、`application-local.yml` | 2026-09-22 | 诊断对象与访问类型不匹配则不可服务 | high |
| S-L086-08 | 工作树 | `OssClientConfig`、`DefaultOssClientImpl`、Compose MinIO 端口 | 2026-09-22 | 路径风格、endpoint 协议、宿主机端口 49000 | high |
| S-L086-09 | 工作树 | `oss-upload-browser` 客户端与 `admin-web` `.env.development` | 2026-09-22 | 浏览器只提交策略名；默认不改写预签名主机 | high |
| S-L086-10 | 项目文档 | `docs/oss-public-private-operations.md`、`docs/error/oss-login-and-direct-upload-troubleshooting.md` | 2026-09-22 | 应用不创建桶、不改 Policy；CORS 与 Lifecycle 分开看 | high |
| S-L087-01 | 工作树 | `backend/wta-admin/src/main/resources/application.yml` 第 82–218 行 | 2026-09-22 | lifecycle 与 direct-upload 的字面配置 | high |
| S-L087-02 | 工作树 | `OssUploadProperties.java` | 2026-09-22 | 直传字段校验与 `storage-config-key` 必填 | high |
| S-L087-03 | 工作树 | `OssUploadService.java`、`SysOssUploadController.java` | 2026-09-22 | 策略读取、两道权限、TTL 与 readiness | high |
| S-L087-04 | 工作树 | `OssLifecycleProperties.java`、`OssLifecycleManager.java` | 2026-09-22 | 下载 TTL；仅 `download-ttl` 可被 Nacos 精确刷新 | high |
| S-L087-05 | 工作树 | `OssUploadCleanupTask.java` | 2026-09-22 | `cleanup-enabled` 与 `cleanup-dry-run` 的直传清理语义 | high |
| S-L087-06 | 工作树 | `OssStorageReadinessService.java` | 2026-09-22 | 启用策略的 `storage-config-key` 进入必检集合 | high |
| S-L087-07 | 工作树 | `DefaultOssUploadObjectStore.java` | 2026-09-22 | 按 configKey 取客户端并拼接对象键 | high |
| S-L088-01 | 工作树 | `backend/wta-modules/wta-workflow/.../WorkflowPermissionHandler.java` | 2026-09-28 | `permissions()` 与 `getHandler()` 只返回当前用户 ID | high |
| S-L088-02 | 工作树 | `TaskAssigneeEnum`、`FlwTaskAssigneeServiceImpl`、`SysUserServiceImpl.selectUsersByRoleIds` | 2026-09-28 | 办理人只有用户/角色/部门/岗位/SpEL；角色展开不看 Client | high |
| S-L088-03 | 工作树 | `SysTaskAssigneeServiceImpl` | 2026-09-28 | 设计器角色名单限定当前 `clientPk`；用户/部门/岗位不按 Client | high |
| S-L088-04 | 工作树 | `FlwTaskMapper.getListRunTask`、`FlwTaskServiceImpl.pageByTaskWait` | 2026-09-28 | 待办查询只等于 `processed_by` | high |
| S-L088-05 | 工作树 | `SecurityConfig.validateClientAccessRules`、`ClientAccessPaths` | 2026-09-28 | 非空 `access_path` 在进接口前拦截；home 不追加工作流路径 | high |
| S-L088-06 | 工作树 | `ClientUserTypeAccessService`、`SysLoginService.buildLoginUser` | 2026-09-28 | 登录必须持有该 Client 的登录域；会话角色按 Client 加载 | high |
| S-L088-07 | 工作树 | `SysRole.clientId`、`SysRoleServiceImpl.validateUsersHaveRoleClientType` | 2026-09-28 | 角色属于 Client；授角色前检查登录域 | high |
| S-L088-08 | 项目事实 | `30-cde-workflow.sql`、`WarmFlowConfig` | 2026-09-28 | 流程表无 `client_id`；`permission_flag` 长度 200；本模块不使用 `tenant_id` | high |
| S-L088-09 | 本地源码包 | Warm-Flow `1.8.9` `ExpressionUtil`、`TaskServiceImpl.checkAuth`、`FlowParams.getPermissionFlag` | 2026-09-28 | 生成待办时展开办理人；办理比较用户标识；`ignore` 跳过 | high |
| S-L088-10 | 工作树 | `WorkflowGlobalListener.assignment`、`CompleteExecuteComponent`、`CompleteTaskBo` | 2026-09-28 | 申请节点改成发起人；办理请求可带 `ignore` 和 `handler` | high |
| S-L088-11 | 工作树 | `frontend/apps/admin-web` 的 workflow 组装；`home-web` 无 workflow 引用 | 2026-09-28 | 流程页面只在管理端 App | high |
| S-L088-12 | 工作树 | `SpelRuleComponent`、`VariablesEnum`、`FlwNodeExtServiceImpl` | 2026-09-28 | SpEL 只有部门负责人；节点扩展没有 Client 变量 | high |

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
| 项目事实 | 流程节点不能按 Client 约束办理会话；待办比较用户 ID。设计器角色名单随当前 Client 变化。种子里工作流菜单归管理端，`home` 的 `access_path` 不含 `/workflow/**`，流程页面只在 `admin-web` | S-L088-01 … S-L088-12、S-010 |
| 未知 | 运行中的库是否手工改过工作流菜单归属或其他 Client 的 `access_path` | 本课未连库；代码路径不依赖该事实 |

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
