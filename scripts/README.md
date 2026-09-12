# scripts 目录说明

该目录存放父仓库级别的开发与质量门禁脚本，可以在满足前置条件时从仓库根目录手动执行。

## 目录结构

```text
scripts/
├── README.md
├── start-dev.sh
├── ci/
│   ├── run-external-services.sh
│   ├── verify-admin-bundle.sh
│   ├── verify-dev-build-guard.sh
│   └── verify-submodules.sh
└── lib/
    ├── backend-build-guard.sh
    └── dev-runtime.sh
```

| 目录 | 作用 |
| --- | --- |
| `scripts/` | 父仓库自动化脚本的统一入口及说明文档。 |
| `scripts/ci/` | CI 质量门禁脚本，负责子模块快照、后端打包内容和真实外部服务集成测试的验收。 |
| `scripts/lib/` | 开发脚本复用模块；当前提供后端构建互斥、Maven 模块 JAR 完整性校验，以及 Windows/Unix classpath 与端口探测。 |

## start-dev.sh

### 作用

开发完成后，从父仓库根目录通过两个选项启动本地人工测试环境：

1. 启动前端；
2. 启动后端。

脚本不创建后台进程，也不重定向服务日志。依赖准备完成后，Vite 或 Spring Boot 会直接接管脚本进程，
持续在当前终端输出实时日志；按 `Ctrl+C` 停止服务后返回调用脚本的终端。

### 使用方式

```bash
./scripts/start-dev.sh
```

Windows PowerShell 或 CMD 需要 Git for Windows 提供的 `bash`：

```powershell
bash scripts/start-dev.sh
```

前端选项优先使用本机可直接调用的 `pnpm`，没有 `pnpm` 时回退到 `corepack pnpm`；依赖安装严格遵循 lockfile，随后运行固定端口且不自动打开浏览器的 `pnpm dev`。
后端选项先取得按 canonical backend path 隔离的原子构建锁，再通过 Maven Wrapper 执行跳过自动测试的
本地 reactor install。构建完成后，脚本会比较 `wta-system/target/classes` 与 target JAR、
`wta-admin` 实际 Maven classpath 中已安装 JAR 的 class 集合，并检查 admin 登录链依赖的关键类型；全部
通过后释放构建锁，再以 `dev,local` profiles 启动 `wta-admin`。Windows 上 Maven classpath 使用 `;`
和盘符路径，脚本按平台分隔符解析，不会把 `D:\` 中的冒号当成 Unix classpath 分隔符。该脚本用于启动
人工测试环境，不能替代前后端自动测试和质量门禁。

### 前置条件与保护

- 前端需要可用的 Node.js、Corepack，以及完整的 `plus-ui-namewta/package.json` 和 lockfile。
- 后端需要 Java 21、可执行的 Maven Wrapper，以及非空的
  `wta-vue-plus-namewta/wta-admin/src/main/resources/application-local.yml`；该配置允许纳入 Git 跟踪，
  启动脚本不校验其忽略状态。
- 启动前检查前端 `80` 或后端 `8080` 端口：优先 `lsof`，Windows 上回退到 `netstat`；端口被占用时只报告
  进程并退出，不会自动终止任何现有服务。
- 脚本不会读取或输出本地配置中的账号、密码等敏感值。
- 父工作区只版本化 `.vscode/settings.json` 并关闭 Red Hat Java 的自动构建，其他 `.vscode` 本地文件仍被
  忽略；这可避免 JDT language server 与 Maven 同时写入 `target/generated-sources`。需要 IDE 编译时请
  显式执行一次 Java build，不要在后端 reactor 运行中触发。
- 同一后端工作区的第二个受管启动会立即失败，并显示持锁 PID；正常退出或 `Ctrl+C`/`TERM` 会清理锁，
  owner PID 已不存在的 stale lock 会被安全替换。含未知内容或元数据不匹配的锁不会被递归删除。
- 锁只协调 `start-dev.sh` 启动。运行后端启动时不要同时从其他终端或 IDE 对同一工作区执行 Maven
  `clean/package/install`；这些外部进程不获取脚本锁，但构建后的 JAR 完整性门会阻止已发现的半成品继续启动。

所有可执行 Shell 入口都启用了 `set -euo pipefail`：命令失败、使用未定义变量或管道中的任一命令失败时，
脚本都会立即以非零状态退出，使 CI 能够准确判定门禁失败。

### 后端构建锁或 JAR 完整性失败的恢复

1. 停止同一后端工作区中仍在运行的 Maven/IDE build；确认 VS Code 已应用工作区中的
   `"java.autobuild.enabled": false`，不要删除仍有存活 owner PID 的锁。
2. 重新执行 `./scripts/start-dev.sh` 并选择后端。stale lock 会自动清理，reactor 会重新 `clean install`。
3. 若仍提示 class 集合或哨兵缺失，检查错误中显示的 target/installed JAR，确认没有外部构建持续写入；
   然后再次串行启动。脚本不会在产物不完整时进入 Spring Boot。
4. 若极端 `SIGKILL` 留下错误中显示的 `.reclaim` 目录，先核对其 `owner` PID 已不存在，再只删除该 owner 文件
   与已变空的 `.reclaim` 目录；不得递归删除锁根目录或仍有活动 PID 的锁。

## ci/verify-dev-build-guard.sh

### 作用

验证父工作区已关闭 Java 自动构建，并使用临时目录和临时 JAR 验证后端开发构建保护模块，覆盖活动 owner
锁冲突、并发 stale lock 回收、stale lock 恢复、`TERM` 清理、完整 class 集合、残缺 JAR、关键 class
哨兵，以及 Unix/Windows Maven classpath 中 `wta-system` JAR 的唯一定位。测试只清理自己创建的临时目录，
不访问产品配置或本地 Maven 仓库。

### 使用方式

```bash
scripts/ci/verify-dev-build-guard.sh
```

## ci/verify-submodules.sh

### 作用

校验父仓库记录的 Git Submodule 快照与当前检出的子模块完全一致，避免 CI 使用错误、未初始化、
发生冲突或带有本地修改的前后端代码。

### 校验内容

1. 执行 `git submodule status --recursive`，确认仓库配置了子模块。
2. 拒绝以下状态标记：
   - `-`：子模块尚未初始化；
   - `+`：检出的提交与父仓库记录的 gitlink 不一致；
   - `U`：子模块存在合并冲突。
3. 分别比较父仓库 gitlink、`git submodule status` 输出和子模块当前 `HEAD`。
4. 检查每个子模块工作树，存在已跟踪修改或未跟踪文件时失败。
5. 全部通过后输出子模块状态。

### 使用方式

```bash
scripts/ci/verify-submodules.sh
```

该脚本被 GitHub Actions 的 `snapshot` job 调用。它要求在父仓库的 Git 工作树内执行；脚本会自动定位
仓库根目录。开发中的本地子模块通常包含未提交修改或尚未更新父仓库指针，此时失败属于预期行为。

## ci/verify-admin-bundle.sh

### 作用

检查后端最终生成的 Spring Boot 可执行 JAR 是否符合 `full` 或 `core` 组装契约，防止 Maven profile
配置错误或上一次构建残留导致模块被漏打包、误打包。

### 参数

| 参数 | 必须包含 | 业务模块要求 |
| --- | --- | --- |
| `full` | `wta-system`、`wta-common-notify`、`wta-common-oss` | 必须包含 `wta-job`、`wta-ai`、`wta-demo`、`wta-workflow`。 |
| `core` | `wta-system`、`wta-common-notify`、`wta-common-oss` | 必须排除上述四个业务模块。 |

脚本使用 JDK 的 `jar tf` 读取
`wta-vue-plus-namewta/wta-admin/target/wta-admin.jar` 中的 `BOOT-INF/lib/` 条目。
它只校验已经生成的产物，不负责执行 Maven 打包；产物不存在、参数无效或模块集合不符合契约时均会失败。

### 使用方式

```bash
# 全量业务组合
(cd wta-vue-plus-namewta && ./mvnw clean package -DskipTests)
scripts/ci/verify-admin-bundle.sh full

# 核心平台组合
(cd wta-vue-plus-namewta && ./mvnw clean package -Pbundle-core -Dmaven.test.skip=true)
scripts/ci/verify-admin-bundle.sh core
```

两次打包都使用 `clean`，用于避免前一种 profile 的产物污染后一种 profile 的校验。该脚本被 GitHub Actions
的 `backend` job 在两次打包后分别调用。

## ci/run-external-services.sh

### 作用

创建一套一次性的真实外部服务环境，等待服务健康后运行后端指定的集成测试。它用于验证代码确实能够与
Redis、MySQL 和兼容 S3 协议的 MinIO 协作，而不只是通过 mock 或纯单元测试。

### 执行流程

1. 以 `GITHUB_RUN_ID`（本地执行时使用进程 ID）生成唯一的 Docker 网络名和容器名。
2. 启动以下固定版本的容器：

   | 服务 | 镜像 | 默认宿主机端口 | 用途 |
   | --- | --- | --- | --- |
   | Redis | `redis:8.6.3` | `16379` | 通知幂等存储、OSS 上传票据存储集成测试。 |
   | MySQL | `mysql:8.4.9` | `13306` | 通知监控、业务菜单退役集成测试。 |
   | MinIO | `pgsty/minio:RELEASE.2026-08-04T00-00-00Z` | `19000` | OSS/S3 客户端集成测试。 |

3. 分别使用 `redis-cli ping`、`mysqladmin ping` 和 MinIO readiness endpoint 等待服务就绪；超时或健康检查
   失败时脚本退出。
4. 在后端子模块中通过 Maven Wrapper 运行以下测试：
   - `RedisNotifyIdempotencyStoreIntegrationTest`
   - `RedisOssUploadTicketStoreIntegrationTest`
   - `NotifyMonitorMySqlIntegrationTest`
   - `MinioOssClientIntegrationTest`
   - `BusinessMenuRetirementMySqlIntegrationTest`
5. 无论测试成功还是中途失败，`EXIT` trap 都会删除本次创建的三个容器和 Docker 网络。

### 可配置端口

| 环境变量 | 默认值 | 含义 |
| --- | --- | --- |
| `NAMEWTA_CI_REDIS_PORT` | `16379` | Redis 映射到宿主机的端口。 |
| `NAMEWTA_CI_MYSQL_PORT` | `13306` | MySQL 映射到宿主机的端口。 |
| `NAMEWTA_CI_MINIO_PORT` | `19000` | MinIO API 映射到宿主机的端口。 |

端口被占用时，可以在单次命令前覆盖：

```bash
NAMEWTA_CI_REDIS_PORT=26379 \
NAMEWTA_CI_MYSQL_PORT=23306 \
NAMEWTA_CI_MINIO_PORT=29000 \
scripts/ci/run-external-services.sh
```

### 前置条件与注意事项

- 需要 Bash、Git、curl、Java 21、可用的 Docker CLI/daemon，以及后端 Maven Wrapper 所需的网络和依赖。
- 脚本使用的数据库和对象存储凭据仅属于一次性 CI 容器，不应复用于共享环境或生产环境。
- 脚本会删除与本次运行生成名称相同的容器和网络；不要手工复用 `namewta-*-<run-id>` 命名。
- 该脚本被 GitHub Actions 的 `external-services` job 调用，本地没有 Docker 时无法执行完整验收。

## 与 GitHub Actions 的对应关系

| GitHub Actions job | 脚本 | 门禁目标 |
| --- | --- | --- |
| `snapshot` | `verify-submodules.sh` | 确保父仓库与前后端子模块快照一致且工作树干净。 |
| `backend` | `verify-admin-bundle.sh full\|core` | 确保两种后端分发包具有正确的模块边界。 |
| `external-services` | `run-external-services.sh` | 使用真实 Redis、MySQL、MinIO 验证关键集成路径。 |
