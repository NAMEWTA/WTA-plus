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
│   └── verify-external-tests.mjs
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

开发完成后，从父仓库根目录前台启动一个本地人工测试进程。无参数时按菜单选择：先选前端或后端，再选启动方式；前端还会选择应用。选完就启动，清理和重新安装不会停在半路。

数字参数按同一菜单顺序补齐。文字子命令 `start`、`build`、`doctor`、`repair` 仍然可用。省略模式的 `start` 不执行 Maven `clean`、不重装已有前端依赖、不跑 JAR 哨兵，也不把端口写成 `--server.port`。`SERVER_PORT` 或外部 `application-local.yml` 仍由 Spring 自己读取。`repair`、`build`、`doctor` 只准备或检查，不启动。

脚本不创建后台进程，也不重定向服务日志。依赖或缓存准备完成后，Vite 或 Spring Boot 会直接接管脚本进程，
持续在当前终端输出实时日志；按 `Ctrl+C` 停止服务后返回调用脚本的终端。

### 使用方式

```bash
./scripts/start-dev.sh                      # 交互选择：先选前端或后端，再选启动方式；前端还会选择应用。选完就启动
./scripts/start-dev.sh 1 2 admin-web        # 前端不清理，直接启动 admin-web。依赖已安装则跳过 install
./scripts/start-dev.sh 2 1                  # 后端清理 target、重新安装并校验产物，然后启动
./scripts/start-dev.sh 2 3                  # 后端不清理、不重新安装，直接启动。可能仍使用已安装的旧依赖
./scripts/start-dev.sh start backend        # 与「后端不清理直接启动」相同，不校验 JAR
./scripts/start-dev.sh start backend clean  # 与「后端清理安装后启动」相同
./scripts/start-dev.sh repair backend       # 只做后端清理安装和产物校验，不启动
./scripts/start-dev.sh --help               # 打印完整用法，不启动任何服务
```

Windows PowerShell 或 CMD 需要 Git for Windows 提供的 `bash`：

```powershell
bash scripts/start-dev.sh
```

交互菜单和数字参数使用同一套序号。没写全时，标准输入是终端就只问剩余步骤；不是终端就直接失败，不会等待输入。

| 顺序 | 前端 `1` | 后端 `2` |
| --- | --- | --- |
| 第 2 个参数 | `1` 完全清理后重新安装并启动；`2` 不清理，直接启动 | `1` 清理 Maven target 并重新安装后启动；`2` 增量安装后启动；`3` 不清理，直接启动 |
| 第 3 个参数 | 应用序号或目录名，例如 `admin-web`。只有一个可启动应用时可以省略 | 无 |

前端应用按目录名排序，菜单显示包名和 development 环境的 `VITE_APP_PORT`。端口按 Vite 顺序读取 `.env`、`.env.local`、`.env.development`、`.env.development.local`，后读到的非空值覆盖先前的值；脚本只取端口和 `VITE_APP_CONTEXT_PATH`，不打印其他键。要在脚本里稳定指定应用时写目录名，不要依赖序号。

前端启动方式：

| 选项 | 行为 |
| --- | --- |
| 完全清理后启动 | 删除该应用以及工作区根上的 `node_modules/.vite`、`node_modules/.cache`、`node_modules/.unocss`、应用内 `.vite`、`*.tsbuildinfo` 和该应用 `dist`，总是 `pnpm install --frozen-lockfile`，再用 `vite --force` 启动。不删除 `node_modules` 本身或 pnpm store。 |
| 直接启动 | 保留 Vite 预构建缓存和 `dist`。`frontend/node_modules` 已存在时跳过 install，否则 `pnpm install --frozen-lockfile`。 |

后端启动方式：

| 选项 | 行为 |
| --- | --- |
| 清理并重新安装后启动 | 在聚合根执行 `-pl wta-admin -am -Plocal clean install`，跳过测试，校验 `wta-system` 产物，释放构建锁，然后前台启动。 |
| 增量安装后启动 | 同一范围执行不带 `clean` 的 `install`，再校验并启动。MapStruct Plus 可能因 `target/generated-sources` 中的旧 `AutoMapperConfig` 编译失败；这时改用清理安装。 |
| 直接启动 | 不执行 Maven `clean/install`，也不校验已有 JAR。依赖模块若已改过但没重新安装，启动的仍可能是已安装的旧代码。 |

文字子命令：

| 命令 | 行为 |
| --- | --- |
| `start frontend [应用] [direct\|clean]` | 省略模式时等于直接启动。`clean` 等于前端完全清理后启动。应用和模式两个词不要求固定顺序。 |
| `start backend [direct\|install\|clean]` | 省略模式时等于直接启动。`install` 和 `clean` 准备成功后会启动。 |
| `build backend` | 只做增量 install，不校验，不启动。不等于菜单里的后端 `2`。 |
| `doctor backend` | 只校验已有产物，不安装，不启动。 |
| `repair frontend [应用]` | 做前端完全清理的安装步骤，不启动。 |
| `repair backend` | 做后端清理安装和产物校验，不启动。 |

两段后端命令都留在 `backend/`：根 POM 以 import 引入仓内 `wta-common-bom` 和 `wta-profile-bom`，`-am` 不会安装这两个 BOM；进入 `wta-admin` 后 reactor 看不到它们。`spring-boot:run` 不带 `-am`，否则没有主类的依赖模块也会执行该目标；聚合根没有 spring-boot 插件前缀，所以必须带 `-pl wta-admin`。安装使用 Maven profile `local`，启动使用 Maven profile `dev` 和 Spring profiles `dev,local`。

增量安装或清理安装完成、前台启动之前，脚本比较 `backend/wta-modules/wta-system/target/classes` 与 target JAR、`wta-admin` 实际 Maven classpath 中已安装 JAR 的 class 集合，并检查 admin 登录链依赖的关键类型。通过后释放构建锁，再启动。直接启动和 `build backend` 不跑这道校验。`doctor` 和 `repair` 会校验，但都不启动。Windows 上 Maven classpath 使用 `;` 和盘符路径，脚本按平台分隔符解析，不会把 `D:\` 中的冒号当成 Unix classpath 分隔符。该脚本用于启动人工测试环境，不能替代前后端自动测试和质量门禁。

### 前置条件与保护

- 前端需要可用的 Node.js、Corepack 或 pnpm，以及完整的 `frontend/package.json` 和 lockfile。Git Bash 会识别 `pnpm.cmd`。
- 后端需要 Java 21、可执行的 Maven Wrapper，以及非空的
  `backend/wta-admin/src/main/resources/application-local.yml`。从同目录的
  `application-local.example.yml` 复制并通过环境注入必需凭据；本机文件被 Git 忽略，不能提交。
  Maven 资源复制及 JAR 打包均排除本地配置，脚本通过 `SPRING_CONFIG_ADDITIONAL_LOCATION`
  显式加载磁盘文件；操作者已设置该变量时保持原值。完整变量与轮换流程见 [后端说明](../backend/README.md#本地配置与凭据)。
- 前端端口取所选应用的 `VITE_APP_PORT`。后端只解析本地配置里顶层 `server.port`；没有该键时使用 `38888`，键存在但不是 1–65535 的纯数字时退出。两种情况都不打印该文件的其他内容。
- 启动前检查对应端口：优先 `lsof`，Windows 上回退到 `netstat`；端口被占用时只报告进程并退出，不会自动终止任何现有服务，也不会先删缓存。
- 前端缓存删除只允许 `.vite`、`.cache`、`.unocss`、`dist` 和 `*.tsbuildinfo`。解析后的路径必须仍在 `frontend/` 内；`node_modules` 本身、pnpm store 和源码不删除。
- 脚本不会读取或输出本地配置中的账号、密码等敏感值，也不会删除本机 Maven 仓库。
- 仓库不要求 `.vscode/settings.json` 存在。使用 Java 自动构建的编辑器时，应在自己的工作区设置关闭对 Maven `target/` 的并行写入；不要在 reactor 运行中触发 IDE 编译。
- 同一后端工作区里，安装和校验使用的构建锁被占用时会立即失败，并显示持锁 PID。省略模式的直接启动不取这把锁。
  正常退出或 `Ctrl+C`/`TERM` 会清理锁，owner PID 已不存在的 stale lock 会被安全替换。含未知内容或元数据不匹配的锁不会被递归删除。
- 锁只协调 `start-dev.sh` 的安装和校验。运行后端启动时不要同时从其他终端或 IDE 对同一工作区执行 Maven
  `clean/package/install`；这些外部进程不获取脚本锁。JAR 完整性门只拦增量安装、清理安装、`doctor` 和 `repair`；直接启动不跑这道门。

所有可执行 Shell 入口都启用了 `set -euo pipefail`：命令失败、使用未定义变量或管道中的任一命令失败时，
脚本都会立即以非零状态退出，使 CI 能够准确判定门禁失败。

### 后端构建锁或 JAR 完整性失败的恢复

1. 停止同一后端工作区中仍在运行的 Maven/IDE build；确认 VS Code 已应用工作区中的
   `"java.autobuild.enabled": false`，不要删除仍有存活 owner PID 的锁。
2. 只想重建、先不启动时，执行 `./scripts/start-dev.sh repair backend`。要清理并立刻启动，执行 `./scripts/start-dev.sh 2 1` 或 `./scripts/start-dev.sh start backend clean`。stale lock 会自动清理。省略模式的 `start backend` 不会 `clean install`。
3. 若 `install`、`clean`、`doctor` 或 `repair` 提示 class 集合或哨兵缺失，检查错误中显示的 target/installed JAR，确认没有外部构建持续写入；
   然后再次串行启动。这些路径不会在产物不完整时进入 Spring Boot。直接启动不检查产物。
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
   | MinIO | `quay.io/minio/minio:RELEASE.2025-04-22T22-12-26Z` | 127.0.0.1 随机端口 | OSS/S3 客户端集成测试；钉死最后一个完全开源（AGPL）社区版，不要升到更新的官方标签。 |

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

Maven 成功后还由 `verify-external-tests.mjs` 检查本轮七个测试类的 XML 报告；缺失、旧报告、零测试或任何跳过均失败。
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
