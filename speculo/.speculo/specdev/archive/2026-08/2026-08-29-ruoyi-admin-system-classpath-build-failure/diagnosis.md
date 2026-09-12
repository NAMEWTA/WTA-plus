---
schema_version: 1
artifact: diagnosis
change: 2026-08-29-ruoyi-admin-system-classpath-build-failure
status: root-cause-confirmed
feedback_loop_ready: true
red_command: "javap -classpath $HOME/.m2/repository/org/dromara/ruoyi-system/6.0.0/ruoyi-system-6.0.0.jar org.dromara.system.domain.vo.SysClientVo"
red_evidence: "exit 1: 找不到类 org.dromara.system.domain.vo.SysClientVo"
cleanup_status: clean
updated_at: 2026-08-29T12:22:55+08:00
---

# Diagnosis: ruoyi-admin 无法解析 ruoyi-system 类型

## 1. 现象与影响

- 用户执行 `<Path>scripts/start-dev.sh</Path>` 并选择后端后，Maven reactor 在 `ruoyi-admin` compile 阶段失败。
- `ruoyi-system` 自身 clean/compile/jar/install 显示成功，但 `ruoyi-admin` 报告 92 个符号缺失；缺失项集中于 `org.dromara.system` 的 entity、BO、VO、mapper、service、password 与 temporary-password 类型。
- 后端无法完成构建，因此本地服务未进入 Spring Boot 启动阶段。
- 当前产品提交不是稳定编译回归：同一 backend commit 的既有 clean candidate Evidence 已多次通过 full/core package 与 admin 可执行 JAR 构建，本次串行复核也通过。

## 2. 红灯反馈回路

- **原始命令：** `./mvnw -q -pl ruoyi-admin -DskipTests clean compile`
- **原始真实输出：** exit 1；92 个 system 类型无法解析，与用户日志一致。
- **原始耗时：** 57.58 秒；复现率 1/1。
- **最小命令：** `javap -classpath $HOME/.m2/repository/org/dromara/ruoyi-system/6.0.0/ruoyi-system-6.0.0.jar org.dromara.system.domain.vo.SysClientVo`
- **最小真实输出：** exit 1，`错误: 找不到类: org.dromara.system.domain.vo.SysClientVo`。
- **精确症状断言：** admin compile scope 中的 `ruoyi-system:6.0.0` JAR 不包含 admin 已声明引用的公开 class。
- **最小耗时：** 0.15 秒。
- **确定性/复现率：** 1/1；产物未替换前稳定为红。
- **Agent 可运行性：** autonomous。
- **无法建立时已尝试方式和所需输入：** 不适用。

## 3. 最小复现

- **环境与输入：** 当前 backend `main` commit `8d401907b6be81c36f92cf88e73e1dee61fd26a4`；Java 21.0.10；Maven 3.9.12；本地仓库 `org.dromara:ruoyi-system:6.0.0`。
- **剩余步骤：** 只需让 `javap` 读取本地仓库 system JAR 中的 `SysClientVo`；无需启动 Spring、数据库或 Redis。
- **逐项删除证据：** 删除 admin、Spring 和外部服务后仍红；替换为完整 system JAR 后立即转绿，因此唯一有负载作用的输入是该 JAR 内容。
- **最后红灯证据：** 旧 JAR 275,186 bytes、仅 155 个 class，且缺少 `SysClientVo`；`javap` exit 1。
- **捕获物：** 无；结构化证据已记录于本工件。

## 4. 假设与证伪

| 排名 | 假设与预测 | 支持证据 | 单变量实验 | 结果 |
|---|---|---|---|---|
| 1 | 本地仓库 system JAR 是共享 build output 被重叠写入时安装的半成品；只重装完整 JAR会转绿 | 旧 JAR 仅 155 class；JAR mtime `12:06:16`，目标 class 集中写于 `12:06:19-20`；旧 JAR 精确包含 compiler `createdFiles.lst` 前 155 项 | 串行 `ruoyi-system clean package` 生成 483/483 class，随后只执行 system install | confirmed：0.15 秒 `javap` 与 admin compile 均由红转绿 |
| 2 | system POM 固定排除了认证/domain class | 若成立，串行 clean package 应稳定缺同一集合 | 检查 effective POM 与串行 clean package | rejected：无 includes/excludes；串行 JAR 483/483 |
| 3 | admin 依赖 scope/profile 不含 system | 若成立，依赖树不会出现 compile scope system | `dependency:tree -Dincludes=org.dromara:ruoyi-system -Dverbose` | rejected：direct `org.dromara:ruoyi-system:jar:6.0.0:compile` |
| 4 | system 源码缺失或 compiler 排除了类型 | 若成立，`target/classes` 不存在所需 class | 核查源码、compiler output 与 required class | rejected：源码存在，编译目录包含全部所需 class |

## 5. 已确认根因

- **触发条件：** 后端启动脚本执行全 reactor `clean install` 时，共享的 backend `target/**` 或同一坐标的本地 Maven 产物受到另一条重叠构建写入；当前脚本没有防重入或产物完整性检查。
- **失败机制：** `ruoyi-system` 的不完整 JAR 被安装为 `org.dromara:ruoyi-system:6.0.0`。admin POM 正确解析该 compile dependency，但 javac 只能看到 155 个 class，因而对缺失的 entity/BO/VO/mapper/service 报出 92 个级联错误。
- **根因位置：** 直接故障产物为本地 Maven 仓库中的 `org.dromara:ruoyi-system:6.0.0` JAR；可修复的项目接缝为 `<Path>scripts/start-dev.sh</Path>` 的 backend build 生命周期。
- **漏检原因：** 现有 `verify-admin-bundle.sh` 只检查 fat JAR 是否包含模块，不检查模块 JAR 的 class 集合完整性；start-dev 也只依赖 Maven 退出码。正常 clean candidate 串行门禁均通过，因此没有覆盖共享 workspace 的并发启动场景。
- **为何排除其他候选：** 源码、system compiler output、admin direct dependency、effective POM 和串行 full reactor 全部正常；只替换 system JAR 即同时修复最小回路与真实 admin 编译。
- **确认实验：** 旧 JAR `javap` exit 1 -> 串行 system clean package 为 483 compiled/483 jarred -> system install -> 相同 `javap` exit 0 -> admin clean compile exit 0 -> 原始 40-module clean install exit 0 -> Spring Boot 在 8080 启动成功并被正常停止。
- **机制置信度边界：** “半成品 JAR 是直接根因”已由单变量实验确认；“当时具体哪一个外部 Maven/IDE 进程参与重叠写入”无法事后从进程表恢复，但时间顺序与 class 前缀截断对共享输出竞态具有强区分性。

## 6. 修复契约

- **必须改变：** backend start path 在任何 `clean install` 前取得以 backend canonical path 为作用域的原子互斥；重复启动必须明确失败并指出 owner/PID，不得并发清理同一 target。
- **必须改变：** reactor 成功后、进入 `spring-boot:run` 前，验证 `ruoyi-system/target/classes` 与目标/已安装 JAR 的 class 路径集合一致，并至少断言 admin 所需的 system sentinel types 存在；不完整时停止并给出恢复命令，不继续启动。
- **必须改变：** `scripts/README.md` 说明锁生命周期、并发 Maven/IDE build 约束、半成品 JAR 的诊断与恢复步骤。
- **必须保持：** Java 21、Maven Wrapper、`dev,local` profiles、前台日志、Ctrl+C 正常关闭、8080 端口与本地配置 secret 检查不变。
- **必须保持：** 不通过新增 admin->system 以外的依赖、复制 system 类型到 admin、放宽 compiler、删除 clean 或吞掉 Maven 失败来获得绿色。
- **正确测试 seam：** `<Path>scripts/start-dev.sh</Path>` 的 backend preflight/build boundary；如实现中提取锁/产物校验 helper，则在 `<Path>scripts/ci/</Path>` 增加可独立执行的 shell 回归。
- **回归测试：** 完整 JAR 校验为绿；用临时目录构造缺少 sentinel/少于 classes 集合的 JAR 时校验为红；持有锁时第二个启动立即红且不调用 Maven；释放/中断后锁可清理；`bash -n scripts/start-dev.sh`；真实 `printf '2\n' | ./scripts/start-dev.sh` 能完成 reactor 并启动 8080。
- **OUT：** 不修改认证业务代码、system/admin Java 依赖边界、Maven plugin 版本、密码策略、数据库或前端；不解决现有 LiteFlow/Netty/OSS 启动 warning。
- **风险与回滚：** 锁清理错误可能阻塞正常启动，需用 PID/owner 信息和 EXIT/INT/TERM trap，并设计 stale-lock 处置；回滚仅撤销脚本与 README 变更，不涉及数据。
- **推荐下游：** `I-implement`；局部开发工具修复，不需要公共行为 Spec 或多 Ticket Goal Plan。

### 推荐实施顺序

1. 在 `<Path>scripts/start-dev.sh</Path>` 增加 backend build 原子锁、owner metadata、signal cleanup 与清晰错误。
2. 增加 system class-set/JAR sentinel 校验 helper，并让 start-dev 在 Maven 成功后强制执行。
3. 为锁冲突、stale cleanup、完整/残缺 JAR增加 shell 回归；更新 `<Path>scripts/README.md</Path>`。
4. 运行 shell 语法/回归、`./mvnw -pl ruoyi-admin -am -DskipTests package`、原始 `clean install -Dmaven.test.skip=true -Plocal`，最后短暂启动并确认 8080。

## 7. 清理

- **原始回路重跑：** `./mvnw -q -pl ruoyi-admin -DskipTests clean compile` exit 0；原始 full reactor exit 0。
- **运行时重跑：** `spring-boot:run` 使用 `dev,local`，`DromaraApplication` 5.442 秒启动并监听 8080；随后 Ctrl+C，Maven exit 0，数据库连接池与 Jetty 正常关闭。
- **`[DEBUG-...]` 搜索：** 未添加任何 debug 插桩，无需清理。
- **一次性脚本/原型：** 无。
- **未清理项 owner 与删除条件：** 无；Maven target 与本地仓库为正常可再生构建产物。

## 8. 实施期补充诊断

- **DIAG-002（已确认）：** 最终原始启动复验期间，第一次 reactor 在 `ruoyi-admin` clean 时发现 target 被重新创建，第二次在 `ruoyi-system` 编译时生成的 `SysDeptVoToSysDeptMapperImpl.java` 尾部出现重复残片。进程快照同时存在父工作区的 Red Hat Java JDT language server，生成文件时间与失败秒级一致。
- **区分性证据：** 残片由 MapStruct `javac` 生成文件自身尾部重复构成，源码/POM 未变化；受管构建锁没有第二个 owner，说明写入者来自不获取脚本锁的 IDE 自动构建。
- **补充修复合同：** 父工作区放行版本化的 `.vscode/settings.json` 并设置 `"java.autobuild.enabled": false`，让 Maven Wrapper 成为共享 `target` 的默认唯一构建者；其他 `.vscode` 本地文件继续忽略，不终止 IDE 进程，不禁止开发者按文档显式执行串行 IDE build。
- **补充验证：** CI Shell 门禁断言该设置保持关闭；在当前 VS Code language server 仍存活的情况下重跑原始后端启动，要求 40-module reactor、JAR 门和 8080 启动通过。
