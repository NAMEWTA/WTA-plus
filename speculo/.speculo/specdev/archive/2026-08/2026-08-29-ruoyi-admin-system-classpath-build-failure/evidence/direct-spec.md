---
artifact: evidence
change: 2026-08-29-ruoyi-admin-system-classpath-build-failure
id: direct-spec
lead: codex
updated_at: 2026-08-29T13:34:06+08:00
status: done
---

# Evidence: ruoyi-admin system classpath build failure

- **Change：** `2026-08-29-ruoyi-admin-system-classpath-build-failure`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-29-ruoyi-admin-system-classpath-build-failure/spec.md</Path>`
- **Diagnosis：** `<Path>{roots.state}/specdev/changes/2026-08-29-ruoyi-admin-system-classpath-build-failure/diagnosis.md</Path>`
- **Goal Plan / Ticket：** 不适用；用户批准按诊断结论执行 Direct Spec。
- **Lead / Workspace：** `codex` / current workspace，父仓库 `main`
- **实施前基线：** 父仓库 `67000ae7c37f41dada3a825b6e4c3712423e1dc6`；后端 `8d401907b6be81c36f92cf88e73e1dee61fd26a4`；前端 `f0ea5706362af7a69f2af9ad8edb8f38ba49f081`
- **最终 checkpoint：** 父仓库 HEAD 仍为 `67000ae7c37f41dada3a825b6e4c3712423e1dc6`，加下述受管路径 working-tree blob；两个子仓库 HEAD 未改变。
- **提交授权：** `implementation_commit=not-authorized`；未执行 commit、push、merge、部署、迁移或 worktree 清理。
- **状态：** done

## 1. 实现摘要

- 后端启动在 Maven `clean install` 前取得按 canonical backend path 隔离的原子目录锁；活动 owner 冲突、未知元数据和并发 stale 回收均 fail closed。
- 父工作区关闭 Red Hat Java 自动构建，防止仍存活的 VS Code JDT language server 与 Maven 同时写入 `target/generated-sources`。
- stale owner 回收使用独立 reclaim gate 并在删除前复核 owner，避免两个回收者都取得同一锁；正常退出、INT handler 与真实 TERM 均释放当前进程所有的锁。
- reactor 成功后比较 `<Path>ruoyi-system/target/classes</Path>`、target JAR 与 `ruoyi-admin` 实际 Maven classpath 中已安装 JAR 的完整 class 集合，并检查五个认证链关键类型。
- 新增隔离 Shell 回归并接入 backend CI job；同步开发脚本目录、并发约束、失败诊断和恢复说明。
- 未修改后端 Java/POM、前端产品源码、公共 HTTP/Java/TypeScript 合同、数据库或本地 secret。

## 2. Lead Dispatch And Candidate Return

- **Implementation owner：** Lead direct；Direct Spec 未派遣 implementation subagent。
- **批准来源：** 用户明确要求执行 `<Path>speculo/workflows/specdev/I-implement/I-implement.md</Path>` 并完成该 change，直到前后端可编译启动。
- **允许动作：** current workspace 中的脚本、测试、CI 接入、文档与 SpecDev 工件修改；无提交授权。
- **Candidate/source/result：** not-applicable；没有 Ticket worktree、candidate branch 或实现提交。
- **Lead 独立核对：** pass；Lead 逐文件读取最终实现、Git 状态、Spec/Diagnosis，并亲自运行所有记录命令与 E2E。
- **只读 Agent findings：** 无；未使用 subagent。

## 3. 修改范围与路径所有权

| 路径 | 所有权 | 改动目的 | 最终 blob |
|---|---|---|---|
| `<Path>.gitignore</Path>` | writable | 只放行可交付的 VS Code 工作区设置，继续忽略其他 IDE 本地文件 | `b7c1cd349d2ca9bc2827a1ed55b41d61a8ea8f7e` |
| `<Path>.vscode/settings.json</Path>` | writable | 关闭父工作区 Java 自动构建，消除默认 JDT/Maven 生成目录竞态 | `c815e8943934fca4b6fd872d9bb756760a3b6c97` |
| `<Path>.github/workflows/quality-gates.yml</Path>` | writable | backend job 执行构建保护回归 | `d97a056a0c2621295d0e7c41752f605d71b91a63` |
| `<Path>scripts/start-dev.sh</Path>` | writable | 构建锁、classpath 定位与 system JAR 完整性门 | `bcae83a3ddec1edca4809af5431dfb2f98c97581` |
| `<Path>scripts/lib/backend-build-guard.sh</Path>` | writable | 可复用锁生命周期和 class-set 校验 | `65eba1e17ba564af3a079c2ad246cdf5d16b466b` |
| `<Path>scripts/ci/verify-dev-build-guard.sh</Path>` | writable | IDE 设置可交付性、锁、信号、竞态、入口与 JAR 负向回归 | `1f5ba7268c2424fef26a7efb433869cea59584d2` |
| `<Path>scripts/README.md</Path>` | writable | IDE/Maven 并发约束、诊断、恢复与命令文档 | `40ccf0cbe6681cc3c9ff46f9b4f3a58bac2ee6ea` |
| `<Path>{roots.state}/specdev/changes/2026-08-29-ruoyi-admin-system-classpath-build-failure/</Path>` | Lead-owned | Spec、Diagnosis、Evidence 与状态 | current workspace |
| `<Path>{roots.state}/specdev/status.json</Path>` | Lead-owned | 全局 current work 索引 | current workspace |

- **read-only 修改：** 无。
- **未声明路径：** 无；父仓库和两个子仓库中的其他 dirty 内容均为既有用户修改，未回退、未纳入本 change。
- **生成文件/锁文件：** Maven/Vite/Playwright 输出均为可再生或测试临时内容；测试自有临时目录、18080 进程和构建锁已清理。

## 4. 验收与合同映射

| Contract | 验证接缝 | 证据 | 结果 |
|---|---|---|---|
| AC-001 | Shell 锁与真实入口 | 活动 owner、嵌套获取和 `start-dev` 冲突均非零；输出 owner PID，且 Maven 提示未出现 | pass |
| AC-002 | stale 与并发回收 | 普通 stale 被替换；受控双回收红灯在旧实现复现“双成功”，reclaim gate 后仅一个成功 | pass |
| AC-003 | 生命周期 | 正常 release、INT 130 handler、真实 TERM 143 后均可重新取得锁；实际 Ctrl+C 优雅停止前后端 | pass |
| AC-004 | 完整 JAR | 临时完整 JAR class-set/哨兵通过；真实 target 与 installed system JAR 在最终启动中通过 | pass |
| AC-005 | 失败 JAR | 空 classes、缺 JAR、少 class、缺哨兵均非零并包含具体错误 | pass |
| AC-006 | 后端构建与启动 | 原始 40-module clean install 成功，两道 JAR 门执行，Spring 在默认 8080 启动并返回 HTTP 200 | pass |
| AC-007 | 前端构建与启动 | lint/typecheck/test/Playwright/dev+prod build 通过；`start-dev` 选项 1 在默认 80 ready 并返回 HTML | pass |
| AC-008 | IDE 默认构建隔离 | 缺少/被忽略设置时 Shell 门禁先红；设置可交付且为 false 后转绿；JDT 进程保持存活时原始 reactor 与 8080 启动通过 | pass |

## 5. Workspace Verification

| 命令或步骤 | cwd / 环境 | 结果 | 摘要 |
|---|---|---|---|
| `bash -n scripts/start-dev.sh scripts/lib/backend-build-guard.sh scripts/ci/verify-dev-build-guard.sh` | parent current-workspace | pass / exit 0 | 三个 Shell 文件语法有效 |
| `scripts/ci/verify-dev-build-guard.sh` | parent current-workspace | pass / exit 0 | 活锁、嵌套、普通/并发 stale、部分 owner、INT handler、TERM、JAR 正负向、干净入口全绿 |
| Ruby `YAML.load_file` | parent current-workspace | pass / exit 0 | quality-gates workflow 可解析 |
| `git diff --check` | parent current-workspace | pass / exit 0 | 无 whitespace error |
| `pnpm install --frozen-lockfile` | `<Path>plus-ui-namewta</Path>` | pass / exit 0 | pnpm 10.34.5，lockfile 未变化 |
| `pnpm architecture:check` | frontend current-workspace | pass / exit 0 | 24 packages，0 violation |
| `pnpm architecture:test` | frontend current-workspace | pass / exit 0 | 99/99 |
| `pnpm lint` | frontend current-workspace | pass / exit 0 | ESLint 通过 |
| `pnpm typecheck` | frontend current-workspace | pass / exit 0 | 23 个 active workspace project 通过 |
| `pnpm test` | frontend current-workspace | pass / exit 0 | architecture、OpenAPI 与各 package/App Vitest 全部通过 |
| `pnpm test:e2e` | frontend current-workspace | pass / exit 0 | Chromium 44/44 |
| `pnpm build:dev` | frontend current-workspace | pass / exit 0 | Vite 3357 modules；只有既有 dynamic-import warning |
| `pnpm build:prod` | frontend current-workspace | pass / exit 0 | Vite production build 通过；只有既有 warning |
| `start-dev` 选项 1 | parent current-workspace | pass / 受控停止 | Vite 690 ms ready，默认 `http://localhost:80/` 返回 HTML，停止后端口释放 |
| `./mvnw test` | `<Path>ruoyi-vue-plus-namewta</Path>` | pass / exit 0 | 40/40 reactor；190 tests，0 failure/error，11 个属性门控集成测试 skipped |
| `./mvnw clean package -DskipTests` | backend current-workspace / 构建锁内 | pass / exit 0 | 40/40 reactor；full package success |
| `scripts/ci/verify-admin-bundle.sh full` | parent current-workspace | pass / exit 0 | full bundle contents verified |
| `./mvnw clean package -Pbundle-core -Dmaven.test.skip=true` | backend current-workspace / 构建锁内 | pass / exit 0 | 40/40 reactor；core package success |
| `scripts/ci/verify-admin-bundle.sh core` | parent current-workspace | pass / exit 0 | core bundle contents verified |
| `start-dev` 选项 2，`SERVER_PORT=18080` | parent current-workspace | pass / 受控停止 | 40-module install、两道 system JAR 门、Spring 4.827 s、HTTP 200、Ctrl+C BUILD SUCCESS |
| 原始 `start-dev` 选项 2，默认 8080 | parent current-workspace / VS Code JDT 存活 | pass / 受控停止 | 40/40 install、两道 JAR 门、Spring 4.669 s、HTTP 200、Ctrl+C BUILD SUCCESS |
| SpecDev validator `--stage spec` | parent current-workspace | pass / exit 0 | 0 errors / 0 warnings |
| SpecDev validator `--stage implement` | parent current-workspace | pass / exit 0 | 0 errors / 0 warnings |
| SpecDev validator `--stage complete` | parent current-workspace | pass / exit 0 | 0 errors / 0 warnings |

- **TDD red/green：** 缺少 helper、stale 清理、TERM、完整/残缺 JAR 和哨兵分别先红后绿；审查期又以确定性同步点复现并发 stale 双成功，再以 reclaim gate 转绿；干净入口先因本地配置依赖红，调整锁边界后转绿；Java 自动构建配置在缺失及被忽略时分别红，关闭并放行版本化后绿。
- **失败后修复与重跑：** 一次 full package 在 `ruoyi-demo` 只生成 mapper 接口、未生成实现；最终原始启动又分别捕获 `ruoyi-admin/target` 被重建和 `SysDeptVoToSysDeptMapperImpl.java` 尾部重复。结合存活 JDT 进程与秒级生成时间确认 IDE 自动构建竞态；父工作区关闭 auto build 后，在 JDT 仍存活时原始 40-module build、JAR 门和默认 8080 启动一次通过。
- **无效验证：** 非交互后台 Bash 忽略 SIGINT，首次直接信号夹具挂起；已中止并确认无残留，改为 INT 130 handler 直接执行，TERM 保留真实信号注入。
- **未运行检查：** `shellcheck` 未安装；远程 GitHub Actions 未运行，因为没有 commit/push 授权；Redis/MySQL/MinIO external-services job 未运行，11 个属性门控用例明确记为 skipped。
- **E2E disposition：** required，已由 Lead 在 current workspace 执行前后端真实启动。

## 6. 双轴审查

### 标准轴

- **固定输入：** 基线 `67000ae7c37f41dada3a825b6e4c3712423e1dc6`，最终 checkpoint 为第 3 节受管路径 blob；commit log 为空（未授权提交）。
- **来源：** engineering standards 的 testing、quality gates、review/delivery、files/naming、documentation/comments、resource/concurrency 规则，以及 SpecDev Fowler baseline。
- **初次结果：** request-changes；发现 stale 双回收 ABA 竞态、入口测试依赖本机 local config、嵌套获取覆盖状态、部分 owner 清理不完整。
- **修正后结果：** pass；全部 finding 已以负向回归关闭，未发现 hard violation 或未处理 smell。

### 规范轴

- **固定输入与来源：** `<Path>{roots.state}/specdev/changes/2026-08-29-ruoyi-admin-system-classpath-build-failure/spec.md</Path>` 与 `diagnosis.md`，只审查 AC/DEC/NFR/IN/OUT。
- **初次结果：** request-changes；并发 stale 回收不满足 DEC-001，INT/空 classes/缺 JAR 证据不够直接。
- **修正后结果：** pass；AC-001～AC-007、DEC-001～DEC-003 与 NFR-001～NFR-004 均有实现和可定位证据，未越过 OOS-001～OOS-003。

## 7. Integration Verification

| 项目 | 结果 |
|---|---|
| Parent before SHA | `67000ae7c37f41dada3a825b6e4c3712423e1dc6` |
| Implementation/source SHA | `null`；commit 未授权 |
| Candidate branch/workspace | `not-applicable` / current workspace |
| Method/conflicts | Direct Spec working-tree checkpoint；无 merge/rebase/conflict |
| Integration checks | 第 5 节全部在 current workspace 实跑 |
| E2E disposition/result | required / passed |
| Parent result/re-read | HEAD 仍为 `67000ae7c37f41dada3a825b6e4c3712423e1dc6`；受管路径 blob 已重读 |

## 8. 偏差与决策

- **偏差：** 无；实施期按同一共享输出根因补充父工作区 Java auto-build 隔离，没有修改 Java/POM、前端产品、端口默认值或数据合同。
- **默认端口环境处置：** 较早验证因 8080 被用户进程占用而使用 18080，未终止用户进程；端口释放后最终原始路径已在默认 8080 完整通过。
- **记录：** 本 Evidence；无需 ADR/LOG。

## 9. 残余风险与交付定位

- **残余风险/已知限制：** 工作区已关闭默认 JDT auto build，但开发者显式触发的外部 IDE/Maven 仍不获取脚本锁，README 已要求串行，产物门负责 fail closed。SIGKILL 恰落在 reclaim critical section 可能留下保守阻塞的 reclaim gate，需要按 owner PID 人工核实，不会删除活动锁。
- **既有启动 warning：** LiteFlow/BeanPostProcessor、macOS Netty DNS 与 OSS CORS/lifecycle warning 不属于本 change，未伪装为修复。
- **回滚：** 撤销 `.github/workflows/quality-gates.yml` 与 `scripts/**` 本次路径变更即可；无数据或配置迁移。
- **后续 Ticket：** 无。
- **Source commit / Parent result：** `null` / `67000ae7c37f41dada3a825b6e4c3712423e1dc6`
- **Source workspace：** current workspace
- **Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-29-ruoyi-admin-system-classpath-build-failure/evidence/direct-spec.md</Path>`
