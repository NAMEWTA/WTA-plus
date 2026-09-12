---
schema_version: 3
artifact: spec
change: 2026-08-29-ruoyi-admin-system-classpath-build-failure
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:2026-08-29-complete-diagnosed-startup-fix
  - DIAG-001:confirmed-partial-ruoyi-system-jar
  - DIAG-002:confirmed-vscode-jdt-generated-source-race
---

# Spec: 防止后端开发启动使用不完整的 ruoyi-system JAR

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-29-ruoyi-admin-system-classpath-build-failure/spec.md</Path>`
- **诊断：** `<Path>{roots.state}/specdev/changes/2026-08-29-ruoyi-admin-system-classpath-build-failure/diagnosis.md</Path>`
- **当前 ADR：** 不适用；不改变产品架构或公共接口。
- **当前领域上下文：** 不适用；沿用父仓库开发启动脚本与 Maven reactor 术语。

## 1. 问题与目标

### 问题陈述

后端开发者通过 `<Path>scripts/start-dev.sh</Path>` 启动本地服务时，重叠 Maven/IDE 构建可能同时清理或写入同一后端 `target`，使一个只包含部分 class 的 `ruoyi-system` JAR 被安装到本地 Maven 仓库。随后 `ruoyi-admin` 虽正确解析到 `ruoyi-system` 依赖，仍会因公开类型缺失而产生大量编译错误，开发服务无法启动。

### 目标用户与场景

目标用户是在同一工作区进行前后端开发和人工联调的开发者。主要场景是从父仓库选择后端启动，并在构建成功后获得可用的 `dev,local` Spring Boot 服务；前端启动行为继续可用。

### 成功标准

- 同一后端工作区同一时刻只允许一条受管开发构建进入 `clean install`。
- 构建成功后，启动脚本只接受与 `ruoyi-system/target/classes` class 集合一致、且包含关键公开类型的目标 JAR与已安装 JAR。
- 父工作区版本化的设置默认禁止 VS Code Java language server 自动构建，避免其与受管 Maven reactor 共同写入 `target`。
- 锁冲突、stale lock 和不完整 JAR 均产生明确、非零且可恢复的失败，不继续启动 Spring Boot。
- 前端能够完成开发构建并进入 Vite 启动状态；后端能够完成 reactor 构建并进入 8080 启动状态。

### 非目标

- 不改变认证、密码策略、Client、system/admin Java 依赖或 Maven plugin/profile。
- 不消除现有第三方启动 warning，也不管理开发者显式运行的所有外部 Maven/IDE 进程。

## 2. 解决方案与外部行为

### 解决方案摘要

父工作区关闭 Red Hat Java 自动构建，避免默认 JDT 增量编译与 Maven 共享生成目录。后端开发启动在
`clean install` 前对 canonical backend path 获取原子构建锁，并在 reactor 成功后、Spring Boot 启动前
执行模块 JAR class 完整性校验。开发脚本文档同时说明并发约束、失败原因与恢复方式。

### 主要流程

1. 用户选择后端启动，既有 Java、Wrapper、本地配置、secret ignore 与 8080 端口检查通过。
2. 启动脚本获取当前后端工作区构建锁，执行既有 `./mvnw clean install -Dmaven.test.skip=true -Plocal`。
3. 脚本比较 `ruoyi-system/target/classes`、目标 JAR 与已安装 JAR 的 class 路径集合，并检查关键公开类型。
4. 校验通过后释放构建锁，以既有 `dev,local` profiles 前台启动 `ruoyi-admin`。

### 边界、失败与稳定错误行为

- 活锁存在时，第二个启动立即以非零状态失败并报告锁 owner/PID，不调用 Maven。
- owner 进程已不存在的 stale lock 可安全清理后重试；未知内容或无法安全清理时失败关闭。
- 构建失败、class 集合不一致、JAR 缺失、零 class 或关键类型缺失时均停止启动，并输出重新执行串行后端启动的恢复指引。
- `INT`、`TERM` 或普通退出会清理当前进程拥有的构建锁；脚本不得删除其他 owner 的锁。

### 状态转换与不变量

构建锁状态只允许 `absent -> owned -> absent`，或 `stale -> absent -> owned`。锁只覆盖当前 canonical backend path 的构建与产物校验阶段；Spring Boot 前台运行继续由端口检查防止重复实例。任何失败都不得吞掉 Maven 或校验退出码。

## 3. 用户故事

- **US-001**：作为后端开发者，我希望重复启动不会并发清理同一构建目录，以便避免生成半成品模块 JAR。
- **US-002**：作为后端开发者，我希望启动前自动拒绝不完整的 system JAR，以便错误在 Spring Boot 前以可恢复方式暴露。
- **US-003**：作为全栈开发者，我希望现有前后端启动方式保持可用，以便继续在本机进行人工联调。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 同一 canonical backend path 已由存活进程持锁 | 第二个进程请求构建锁 | 立即非零退出并报告 owner/PID，未进入 Maven | Shell 锁模块回归 |
| AC-002 | 锁 owner PID 已不存在 | 新进程请求同一路径的锁 | stale lock 被安全替换，新 owner 可取得并释放锁 | Shell 锁模块回归 |
| AC-003 | 进程持有构建锁 | 进程正常退出或收到 `INT`/`TERM` | 只清理自己的锁，后续进程可取得锁 | Shell 生命周期回归 |
| AC-004 | classes 目录与完整模块 JAR class 集合相同且哨兵存在 | 执行产物校验 | 校验退出 0 | Shell 产物校验回归 |
| AC-005 | JAR 缺 class、缺哨兵、缺文件或无 class | 执行产物校验 | 校验非零并说明不完整原因 | Shell 产物校验负向回归 |
| AC-006 | 后端本地配置与端口可用 | 选择后端启动 | reactor 编译安装成功，完整性校验通过，应用监听 8080 | `start-dev.sh` 真实启动验收 |
| AC-007 | 前端依赖与端口可用 | 执行开发构建并选择前端启动 | 开发构建成功，Vite 进入固定端口启动状态 | pnpm build 与真实启动验收 |
| AC-008 | 使用父工作区 VS Code Java 配置 | language server 加载工作区设置 | Java 自动构建关闭，不与 Maven 写入相同 `target` | 工作区配置回归与真实启动验收 |

## 5. 范围

### IN

- 父仓库 VS Code Java 构建设置、开发启动脚本、可复用 Shell 构建保护模块、Shell 回归与脚本文档。
- 当前前后端代码状态下的构建和真实启动验证。

### REUSE

- 复用现有 Java 21、Maven Wrapper、pnpm lockfile、`dev,local` profiles、前台日志、端口与本地配置检查。
- 复用 JDK `jar`、POSIX 文件系统原子目录创建和现有父仓库 `scripts/ci` Shell 门禁风格。

### OUT

- **OOS-001**：不修改 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin</Path>` 或 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system</Path>` 业务源码/POM；诊断已证明依赖合同与源码正常。
- **OOS-002**：不修改 `<Path>plus-ui-namewta</Path>` 产品源码；本变更只验证现有前端编译与启动。
- **OOS-003**：不提交、推送、部署或执行数据迁移；本次未获得这些独立授权。

## 6. 已锁定实现约束

- **DEC-001**：锁按 canonical backend path 隔离，获取使用原子文件系统操作，冲突必须 fail closed。来源：`DIAG-001`。
- **DEC-002**：完整性以 class 相对路径集合相等和关键 system 公开类型同时成立为准，不能仅用 JAR 存在或 Maven exit 0 代替。来源：`DIAG-001`。
- **DEC-003**：不通过新增依赖、复制类型、放宽 compiler、删除 clean 或吞失败制造绿色。来源：`DIAG-001`。
- **DEC-004**：父工作区只放行并版本化 `.vscode/settings.json`，其中关闭 Red Hat Java 自动构建；显式 IDE/Maven build 仍由开发者按文档串行执行。来源：`DIAG-002`。

## 7. 数据、接口与兼容

- **公共接口变化：** 无产品 HTTP/Java/TypeScript 公共接口变化；只扩展父仓库开发脚本行为。
- **数据模型与持久化：** 无。
- **兼容要求：** 保持现有交互菜单、前台进程、Ctrl+C、端口、Maven/pnpm 命令与 profiles。
- **迁移要求：** 无；脚本修改可直接回滚。
- **发布或运维影响：** 不适用；仅本地开发工具。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 不读取或输出本地配置 secret；锁元数据只包含 PID、canonical backend path 与启动时间。
- **NFR-002 性能与容量：** 完整性校验只遍历单个模块的 class 条目，不引入第二次 reactor 构建。
- **NFR-003 可用性与可靠性：** 锁冲突和完整性失败均 fail closed；stale 清理不得递归删除未知目录内容。
- **NFR-004 可观测性与运营：** 错误必须指出冲突 owner/PID或具体产物差异，并给出串行重建恢复动作。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| 构建保护 Shell 接口 | 定向集成 | AC-001～AC-005 | `<Path>scripts/ci</Path>` 可执行 Shell 回归 | red/green 命令与退出码 |
| VS Code Java 工作区设置 | 静态配置/真实集成 | AC-008 | 构建保护回归与原始后端启动 | 设置断言、无 generated-source 竞态 |
| 后端 Maven reactor | 跨模块回归 | AC-006 | `./mvnw test`、full/core package、原始 local install | Maven summary 与退出码 |
| 后端开发启动 | E2E | AC-006 | `printf '2\n' \| ./scripts/start-dev.sh` | 8080 监听/启动日志/受控停止 |
| 前端质量门禁与开发构建 | 跨模块回归 | AC-007 | pnpm architecture/lint/typecheck/test/build | pnpm summary 与退出码 |
| 前端开发启动 | E2E | AC-007 | `printf '1\n' \| ./scripts/start-dev.sh` | Vite ready/HTTP 响应/受控停止 |

## 10. 风险、假设与未决问题

### 风险

- PID 复用可能造成保守的假冲突；比错误删除活动锁更安全，并会显示 PID 供人工核对。
- 显式启动的外部 Maven/IDE 构建不会主动获取脚本锁；工作区关闭默认 JDT 自动构建，构建后的 class 集合校验继续负责阻止其他半成品产物进入启动阶段。

### 已采用的低影响假设

- 当前启动命令未覆盖 Maven local repository，默认使用本机 Maven 设置解析的本地仓库；通过实际启动验证定位结果。
- 本机缺少 Docker 不影响本地前后端编译/启动合同；外部 Redis/MySQL/MinIO 门禁不属于本变更。

### 未决问题

无。
