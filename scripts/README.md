# scripts 目录说明

该目录存放父仓库级别的开发与质量门禁脚本，可以在满足前置条件时从仓库根目录手动执行。

## 目录结构

```text
scripts/
├── README.md
├── start-dev.sh
├── sso-hard-e2e.sh
├── ci/
│   ├── verify-agent-handbooks.mjs
│   ├── verify-agent-handbooks.test.mjs
│   ├── run-external-services.sh
│   ├── verify-admin-bundle.sh
│   ├── verify-dev-build-guard.sh
│   └── verify-external-tests.py
└── lib/
    ├── backend-build-guard.sh
    └── dev-runtime.sh
```

| 目录 | 作用 |
| --- | --- |
| `scripts/` | 父仓库自动化脚本的统一入口及说明文档。 |
| `scripts/ci/` | CI 质量门禁脚本，负责构建保护、后端打包内容和真实外部服务集成测试的验收。 |
| `scripts/lib/` | 开发脚本复用模块；当前提供后端构建互斥、Maven 模块 JAR 完整性校验，以及 Windows/Unix classpath 与端口探测。 |

## start-dev.sh

### 作用

开发完成后，从父仓库根目录按菜单启动一个本地人工测试进程：

1. 启动前端，再选择 `frontend/apps` 下带 `dev` 脚本的应用和启动方式；
2. 启动后端，再选择是否清理 Maven `target` 并重新安装。

脚本不创建后台进程，也不重定向服务日志。依赖或缓存准备完成后，Vite 或 Spring Boot 会直接接管脚本进程，
持续在当前终端输出实时日志；按 `Ctrl+C` 停止服务后返回调用脚本的终端。

### 使用方式

```bash
./scripts/start-dev.sh
```

Windows PowerShell 或 CMD 需要 Git for Windows 提供的 `bash`：

```powershell
bash scripts/start-dev.sh
```

第一级菜单只有「启动前端」和「启动后端」。前端应用按目录名排序，菜单显示包名和 development 环境的
`VITE_APP_PORT`。端口按 Vite 顺序读取 `.env`、`.env.local`、`.env.development`、`.env.development.local`，
后读到的非空值覆盖先前的值；脚本只取端口和 `VITE_APP_CONTEXT_PATH`，不打印其他键。

前端启动方式：

| 选项 | 行为 |
| --- | --- |
| 直接启动 | 保留 Vite 预构建缓存。`frontend/node_modules` 已存在时跳过 install，否则 `pnpm install --frozen-lockfile`。 |
| 清理当前应用缓存 | 删除该应用以及工作区根上的 `node_modules/.vite`、`node_modules/.cache`、`node_modules/.unocss`、应用内 `.vite` 和 `*.tsbuildinfo`，再用 `vite --force` 重新预构建。不删除 `node_modules` 和 `dist`。 |
| 清理并重装 | 在上一档之外再删除该应用 `dist`，并总是按 lockfile 重装依赖。 |

后端启动方式：

| 选项 | 行为 |
| --- | --- |
| 直接启动 | 不执行 Maven `clean/install`，只校验已有 `wta-system` 产物。产物缺失时失败，需改选清理安装。 |
| 增量安装 | 在聚合根执行 `-pl wta-admin -am install`，不 `clean`。MapStruct Plus 可能因 `target/generated-sources` 中的旧 `AutoMapperConfig` 编译失败。 |
| 清理并重新安装 | 推荐用于生成源或依赖异常。同一聚合根执行 `-pl wta-admin -am -Plocal clean install`，跳过测试。 |

两段后端命令都留在 `backend/`：根 POM 以 import 引入仓内 `wta-common-bom` 和 `wta-profile-bom`，`-am` 不会安装这两个 BOM；进入 `wta-admin` 后 reactor 看不到它们。`spring-boot:run` 不带 `-am`，否则没有主类的依赖模块也会执行该目标；聚合根没有 spring-boot 插件前缀，所以必须带 `-pl wta-admin`。安装使用 Maven profile `local`，启动使用 Maven profile `dev` 和 Spring profiles `dev,local`。

构建或直接启动前，脚本比较 `backend/wta-modules/wta-system/target/classes` 与 target JAR、`wta-admin` 实际 Maven classpath 中已安装 JAR 的 class 集合，并检查 admin 登录链依赖的关键类型。通过后释放构建锁，再以前台方式启动。Windows 上 Maven classpath 使用 `;` 和盘符路径，脚本按平台分隔符解析，不会把 `D:\` 中的冒号当成 Unix classpath 分隔符。该脚本用于启动人工测试环境，不能替代前后端自动测试和质量门禁。

### 前置条件与保护

- 前端需要可用的 Node.js、Corepack 或 pnpm，以及完整的 `frontend/package.json` 和 lockfile。Git Bash 会识别 `pnpm.cmd`。
- 后端需要 Java 21、可执行的 Maven Wrapper，以及非空的
  `backend/wta-admin/src/main/resources/application-local.yml`；该配置允许纳入 Git 跟踪，
  启动脚本不校验其忽略状态。
- 前端端口取所选应用的 `VITE_APP_PORT`。后端只解析本地配置里顶层 `server.port`；没有该键时使用 `38888`，键存在但不是 1–65535 的纯数字时退出。两种情况都不打印该文件的其他内容。
- 启动前检查对应端口：优先 `lsof`，Windows 上回退到 `netstat`；端口被占用时只报告进程并退出，不会自动终止任何现有服务，也不会先删缓存。
- 前端缓存删除只允许 `.vite`、`.cache`、`.unocss`、`dist` 和 `*.tsbuildinfo`。解析后的路径必须仍在 `frontend/` 内；`node_modules` 本身、pnpm store 和源码不删除。
- 脚本不会读取或输出本地配置中的账号、密码等敏感值，也不会删除本机 Maven 仓库。
- 仓库不要求 `.vscode/settings.json` 存在。使用 Java 自动构建的编辑器时，应在自己的工作区设置关闭对 Maven `target/` 的并行写入；不要在 reactor 运行中触发 IDE 编译。
- 同一后端工作区的第二个受管启动会立即失败，并显示持锁 PID；正常退出或 `Ctrl+C`/`TERM` 会清理锁，
  owner PID 已不存在的 stale lock 会被安全替换。含未知内容或元数据不匹配的锁不会被递归删除。
- 锁只协调 `start-dev.sh` 的安装和校验。运行后端启动时不要同时从其他终端或 IDE 对同一工作区执行 Maven
  `clean/package/install`；这些外部进程不获取脚本锁，但构建后的 JAR 完整性门会阻止已发现的半成品继续启动。

所有可执行 Shell 入口都启用了 `set -euo pipefail`：命令失败、使用未定义变量或管道中的任一命令失败时，
脚本都会立即以非零状态退出，使 CI 能够准确判定门禁失败。

### 后端构建锁或 JAR 完整性失败的恢复

1. 停止同一后端工作区中仍在运行的 Maven/IDE build；确认 VS Code 已应用工作区中的
   `"java.autobuild.enabled": false`，不要删除仍有存活 owner PID 的锁。
2. 重新执行 `./scripts/start-dev.sh`，选择后端，再选择「清理 Maven target 并重新安装后启动」。stale lock 会自动清理，reactor 会重新 `clean install`。
3. 若仍提示 class 集合或哨兵缺失，检查错误中显示的 target/installed JAR，确认没有外部构建持续写入；
   然后再次串行启动。脚本不会在产物不完整时进入 Spring Boot。
4. 若极端 `SIGKILL` 留下错误中显示的 `.reclaim` 目录，先核对其 `owner` PID 已不存在，再只删除该 owner 文件
   与已变空的 `.reclaim` 目录；不得递归删除锁根目录或仍有活动 PID 的锁。

## ci/verify-agent-handbooks.mjs

从仓根运行 `node scripts/ci/verify-agent-handbooks.mjs`，检查 backend/frontend 的实际 manifest 是否有最近适用的产品手册、手册是否有标题和中文导读、Markdown inline 本地链接是否指向现存文件或目录，以及产品目录是否出现重复 CLAUDE 入口。允许模块继承父手册，不要求每个 POM/package 复制七段模板。链接检查不覆盖锚点或完整 Markdown 语法，独有硬约束的保留仍需逐文件评审。

回归命令为 `node --test --test-concurrency=1 scripts/ci/verify-agent-handbooks.test.mjs`；夹具仅使用并清理各自临时目录。两条命令已接入仓内 CI 候选，远程执行仍待提交推送后验证。

## ci/verify-dev-build-guard.sh

### 作用

使用临时目录和临时 JAR 验证后端开发构建保护模块，覆盖活动 owner
锁冲突、并发 stale lock 回收、stale lock 恢复、`TERM` 清理、完整 class 集合、残缺 JAR、关键 class
哨兵，以及 Unix/Windows Maven classpath 中 `wta-system` JAR 的唯一定位。测试只清理自己创建的临时目录，
不访问产品配置或本地 Maven 仓库。

### 使用方式

```bash
scripts/ci/verify-dev-build-guard.sh
```

编辑器配置不是验收前置条件。使用自动 Java 编译的编辑器时，应关闭对 Maven `target/` 的并行写入；构建锁和 JAR 检查在没有编辑器的干净 clone 中也必须运行。

## ci/verify-admin-bundle.sh

### 作用

检查后端最终生成的 Spring Boot 可执行 JAR 是否符合 `full` 或 `core` 组装契约，防止 Maven profile
配置错误或上一次构建残留导致模块被漏打包、误打包。

### 参数

| 参数 | 必须包含 | 业务模块要求 |
| --- | --- | --- |
| `full` | `wta-system`、`wta-common-notify`、`wta-common-oss`、`wta-third`、`wta-sso`、`wta-notify`、`wta-profile-person`、`wta-profile-enterprise` | 必须包含 `wta-job`、`wta-ai`、`wta-demo`、`wta-workflow`。 |
| `core` | 同上 | 必须排除上述四个可选业务模块。 |

脚本使用 JDK 的 `jar tf` 读取
`backend/wta-admin/target/wta-admin.jar` 中的 `BOOT-INF/lib/` 条目。
它只校验已经生成的产物，不负责执行 Maven 打包；产物不存在、参数无效或模块集合不符合契约时均会失败。

### 使用方式

```bash
# 先保存完整测试结果；skipTests 仅用于后续打包，不代表测试通过
(cd backend && ./mvnw test)

# 全量业务组合
(cd backend && ./mvnw clean package -DskipTests)
scripts/ci/verify-admin-bundle.sh full

# 核心平台组合
(cd backend && ./mvnw clean package -Pbundle-core -Dmaven.test.skip=true)
scripts/ci/verify-admin-bundle.sh core
```

两次打包都使用 `clean`，用于避免前一种 profile 的产物污染后一种 profile 的校验。必须先验证并保存 full
产物清单，再 clean 构建 core。可用 `ADMIN_ARTIFACT` 指定待检 JAR；按完整 artifact 名匹配，不接受同前缀的替代模块。
该脚本由仓内 GitHub Actions 候选的 `backend` job 和 release-manage 的后端打包步骤调用。

前端在 `frontend/` 执行 `pnpm build:dev` 或 `pnpm build:prod`：先串行按依赖顺序构建 packages/tooling，
再以指定模式分别构建 Admin、Home、SSO 各一次。每个 App 的 `dist/build-mode.json` 记录 Vite 实际模式；
检查开发产物后再运行生产构建，避免覆盖证据。此名单表示源码中的 active App，发布名单由发布配置单独决定。

## ci/run-external-services.sh

### 作用

创建一套一次性的真实外部服务环境，等待服务健康后运行后端指定的集成测试。它用于验证代码确实能够与
Redis、MySQL 和兼容 S3 协议的 MinIO 协作，而不只是通过 mock 或纯单元测试。

### 执行流程

1. 以运行 ID、进程 ID 和随机后缀生成本轮 Docker 资源名，并记录实际创建成功的资源 ID。
2. 启动以下固定版本的容器：

   | 服务 | 镜像 | 默认宿主机端口 | 用途 |
   | --- | --- | --- | --- |
   | Redis | `redis:8.6.3` | 127.0.0.1 随机端口 | 通知幂等存储、OSS 上传票据存储集成测试。 |
   | MySQL | `mysql:8.4.9` | 127.0.0.1 随机端口 | 通知监控、业务菜单退役集成测试。 |
   | MinIO | `pgsty/minio:RELEASE.2026-04-17T00-00-00Z` | 127.0.0.1 随机端口 | OSS/S3 客户端集成测试。 |

3. 分别使用 `redis-cli ping`、`mysqladmin ping` 和 MinIO readiness endpoint 等待服务就绪；超时或健康检查
   失败时脚本退出。
4. 在后端目录中通过 Maven Wrapper 运行以下测试：
   - `RedisNotifyIdempotencyStoreIntegrationTest`
   - `RedisOssUploadTicketStoreIntegrationTest`
   - `NotifyMonitorMySqlIntegrationTest`
   - `MinioOssClientIntegrationTest`
   - `BusinessMenuRetirementMySqlIntegrationTest`
   - `ThirdSchemaMySqlIntegrationTest`
   - `ThirdRedisIntegrationTest`
5. 无论测试成功还是中途失败，`EXIT` trap 都按记录的 ID 删除本轮容器、匿名卷和 Docker 网络；名称冲突时不删除既有资源。

Maven 成功后还由 `verify-external-tests.py` 检查本轮七个测试类的 XML 报告；缺失、旧报告、零测试或任何跳过均失败。
通知监控通过当前 UseCase/Service/DAO/Mapper 读取真实投递表；菜单验证当前基座 DSL-004 的删除与保留行为。

### 可配置端口

| 环境变量 | 默认值 | 含义 |
| --- | --- | --- |
| `NAMEWTA_CI_REDIS_PORT` | 空，自动分配 | Redis 映射到本机的端口。 |
| `NAMEWTA_CI_MYSQL_PORT` | 空，自动分配 | MySQL 映射到本机的端口。 |
| `NAMEWTA_CI_MINIO_PORT` | 空，自动分配 | MinIO API 映射到本机的端口。 |

需要固定端口时，可以在单次命令前覆盖（占用时启动失败，不连接既有服务）：

```bash
NAMEWTA_CI_REDIS_PORT=26379 \
NAMEWTA_CI_MYSQL_PORT=23306 \
NAMEWTA_CI_MINIO_PORT=29000 \
scripts/ci/run-external-services.sh
```

### 前置条件与注意事项

- 需要 Bash、Git、curl、Python 3、Java 21、可用的 Docker CLI/daemon，以及后端 Maven Wrapper 所需的网络和依赖。
- 脚本使用的数据库和对象存储凭据仅属于一次性 CI 容器，不应复用于共享环境或生产环境。
- 镜像版本与 `release-artifacts/docker/docker-compose-infrastructure.yml` 一致，发布合同测试检查漂移。
- 该脚本由仓内 GitHub Actions 候选的 `external-services` job 调用，本地没有 Docker 时无法执行完整验收。

## 与 GitHub Actions 的对应关系

| GitHub Actions job | 脚本 | 门禁目标 |
| --- | --- | --- |
| `release-contracts` | Skill/分层检查、`verify-dev-build-guard.sh`、`verify-release.sh` | 检查monorepo结构、锁、JAR与发布合同。 |
| `frontend` | `frontend/package.json`及OpenAPI工具scripts | 架构、OpenAPI漂移、lint、typecheck、测试、双模式构建及Admin E2E。 |
| `backend` | `verify-admin-bundle.sh full\|core` | 确保两种后端分发包具有正确的模块边界。 |
| `external-services` | `run-external-services.sh` | 使用真实 Redis、MySQL、MinIO 验证关键集成路径。 |

该 workflow 是仓内候选，jobs 通过 needs 串行连接；配置存在不代表远程运行成功或分支保护已启用。SSO 专用 E2E 与整体候选验收由相应任务补充，不能用 Admin 默认 E2E 代替。
