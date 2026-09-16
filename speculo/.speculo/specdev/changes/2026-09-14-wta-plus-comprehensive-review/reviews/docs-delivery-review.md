# WTA-plus 文档、目录与交付治理审查

审查日期：2026-09-14。范围为仓库根、当前 `docs/`、`scripts/`、`release-artifacts/`、前后端索引与 README、`.agents/skills/` 项目治理规则、Maven/pnpm 构建合同、CI/发布脚本和可达的 SpecDev 当前配置。未读取 Speculo 历史 archive，也未修改产品代码、配置、文档或 Git 状态；持久化写入仅限本 change 下的 review 工件。两个验证脚本曾在系统临时目录创建并清理测试夹具，详见命令 JSON，不应将本次执行描述为绝对纯读取。

本报告使用 `module`、`interface`、`depth`、`seam`、`adapter`、`leverage`、`locality` 术语。`confirmed` 表示由当前工作树静态证据直接证明；`likely` 表示代码路径已指出问题但尚未执行相应 runtime；`needs-runtime` 表示需要环境、服务或远程 CI 才能结论。P1 阻止可重复交付或会使安全/产物合同失真，P2 造成维护和验收漂移，P3 为低风险清理候选。

## 范围与证据基线

* `confirmed`：当前产品是单一 monorepo。存在 <Path>frontend/apps/admin-web/package.json</Path>、<Path>frontend/apps/home-web/package.json</Path>、<Path>frontend/apps/sso-web/package.json</Path> 三个可构建 App；<Path>backend/pom.xml</Path> 下实际跟踪 50 个 POM 描述符、247 个 `src/test/java` Java 源文件。项目画像 <Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path> 第 9 行仍记录“46 projects/176 tests”，第 7 行仍把不存在的 <Path>.github/workflows/quality-gates.yml</Path> 当 high-confidence evidence。这里的 247 是 tracked Java source file 数，不是测试用例数。
* `confirmed`：<Path>.github/workflows/quality-gates.yml</Path>、<Path>plan/update.md</Path>、<Path>.vscode/settings.json</Path> 均不存在；<Path>scripts/ci/verify-submodules.sh</Path> 因仓库无 submodule 必定失败。<Path>scripts/ci/verify-dev-build-guard.sh</Path> 又要求缺失的 `.vscode/settings.json`，因此不能成为当前门禁。
* `confirmed`：<Path>release-artifacts/scripts/verify-release.sh</Path> 的 43 个合同测试中 42 通过，唯一失败由读取不存在的 <Path>.github/workflows/quality-gates.yml</Path> 引起；命令、cwd、退出码、测试夹具临时写入与未执行项见 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/docs-command-results.json</Path>。
* `confirmed`：只读 Markdown 相对链接扫描覆盖 218 个当前 Markdown 文件，未发现支持子集中的失效 literal relative link；此结果不覆盖代码 span、glob/变量路径、runtime URL、历史 archive。`docs/fm/scripts/validate.mjs` 与统一全栈 Skill validator 静态通过，但不等于生成输出、Maven、前端构建或部署通过。
* 排除：不把上游 `org.dromara` 坐标、SnailJob/WarmFlow/AI schema 或第三方文件仅因名称旧就建议删除；它们属于 KEEP/供应商所有权，需由后端安全审查维护。分层 validator namespace 漏检由后端 B finding 负责，本报告只记录其与交付治理的耦合。

## 高置信 findings

### D-01 P1 confirmed：CI 权威入口已删除，遗留子模块门禁仍在文档和画像中

**证据。** <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path> 第 25、41、43 行和 <Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path> 第 7、9 行引用不存在的 workflow 并宣称 active CI。<Path>scripts/ci/verify-submodules.sh</Path> 第 6–11 行执行 `git submodule status --recursive`，无 submodule 时显式 exit 1；根目录无 `.gitmodules`。<Path>scripts/README.md</Path> 第 79–82、141–145 行已承认 monorepo 交付，却仍把 `verify-submodules` 放入 CI 目录和 GitHub Actions job 映射。

**问题与后果。** 一个已经废弃的 snapshot interface 仍出现在“质量门禁”说明，实际不存在的 workflow 成为错误证据。新成员可能按旧步骤初始化 submodule，CI 若恢复引用会在所有分支稳定失败；画像的 high confidence 使审查和自动化都信任错误事实。

**代码 judo / 删除测试。** 删除 <Path>scripts/ci/verify-submodules.sh</Path> 及其 snapshot 映射后，submodule 复杂性真正消失；将 CI 事实收敛到当前可执行命令清单（或一个只读 manifest）后，任何不存在 workflow 的声明不能再被引用。若组织确实需要检查仓库拓扑，应改为断言 `git ls-files` 中不存在 submodule，而不是保留 checkout/SHA 逻辑。

**建议。** P1，`D-T01-delivery-gates`：重写 project profile、module map、<Path>scripts/README.md</Path> 和 review checklist 的 active/not-active 表；删除遗留脚本或明确 `retired/` 并从所有 gate 映射移除；由真实 CI 文件（若重新建立）和本地命令矩阵共同成为唯一 interface。验证：`git ls-files .github/workflows .gitmodules`、逐条 shell `bash -n`、CI dry parse；不得声称远程 required check，直到提交后的 Actions 记录与分支保护可见。

**架构卡。** `dependency class: local-substitutable`；`strength: Strong`；`deleted complexity: submodule checkout state, stale snapshot branch and duplicated CI claim`；`ADR conflict: ADR-0053 monorepo`，不是与产品行为冲突。前后：`旧 parent -> submodule status -> snapshot workflow`；`目标 parent -> canonical gate manifest -> local/CI same command`。

### D-02 P1 confirmed/needs-runtime：SSO App 已进入源码与构建发现，却断在发布 interface

**证据。** <Path>frontend/apps/sso-web/package.json</Path> 提供 build/lint/test/typecheck；<Path>release-artifacts/scripts/release-manage.sh</Path> 第 155–166、230–270 行通过 `frontend_apps()` 自动发现所有带 `package.json` 的 App，并对 `sso-web` 执行构建。可是 <Path>release-artifacts/docker/docker-compose-frontend.yml</Path> 只定义 `namewta-nginx-admin-web` 与 `namewta-nginx-home-web`（第 48、65 行）；<Path>release-artifacts/skills/wta-namewta-nginx-config/references/port-registry.md</Path> 仅登记 Admin；<Path>release-artifacts/docker/frontend/nginx/apps/</Path> 里没有 `nginx-sso-web.conf.template`。发布脚本 <Path>release-artifacts/scripts/release-manage.sh</Path> 第 405–420 行对每个构建目录强制要求该模板，因而发现 SSO 后 stage 必定失败。<Path>frontend/apps/sso-web/src/ssoApi.ts</Path> 使用 `/sso/*` 和 HttpOnly cookie；<Path>frontend/apps/sso-web/vite.config.ts</Path> 只提供本地 `/sso` proxy。ADR-0073 明确生产是同进程后端 + 独立 `sso-web` Web Origin，需环境 callback 矩阵。

**问题与后果。** “源码有三个 App”“发布只包含两个 App”“发布脚本自动发现三个 App”互相矛盾。即使跳过 stage，SSO 的独立 origin、`/sso` API 反代、cookie domain、callback 和 health route 没有可发布合同；构建通过不能证明登录能用。

**代码 judo / 删除测试。** 不要让目录扫描决定发布面。删除隐式 `find apps/*/package.json` 作为 release interface，改为显式、版本化、按环境审查的发布 App 清单；清单中的每个 App 必须同时声明 package、prefix、port、Nginx template、API route、callback/origin 与是否 shipped。若 SSO 尚未发布，清单明确排除并使 release 脚本拒绝未登记 App；若发布，补齐 Compose/Nginx/port/env/ADR 矩阵并做真实浏览器验收。删除自动发现后，未登记目录不会悄悄改变发布产物。

**建议。** P1，`D-T02-sso-release-surface`，与前端 SSO ticket 合并；只在用户选定“发布 SSO”或“继续源码-only”后执行。验收：`release-manage build/stage` 在干净 workspace；每个 manifest App 生成且仅生成一套 Nginx/Compose 资产；独立 origin 的 authorize/token/PKCE、cookie、callback、刷新/失败关闭由 Playwright + 双后端 runtime 验证。`needs-runtime`：DNS/TLS、跨 origin cookie、实际 `/sso` upstream。

**架构卡。** `dependency class: ports & adapters`；`strength: Strong`；`deleted complexity: implicit directory scan and partial release ownership`；`ADR conflict: ADR-0073/0074`，当前发布文件没有兑现已接受的独立 origin 决策。前后：`旧 apps scan -> build -> stage(template missing)`；`目标 release manifest -> build -> verified App adapter -> atomic stage`。

### D-03 P1 confirmed：partial build 会混合旧产物，release manifest 仍宣称当前 HEAD

**证据。** <Path>release-artifacts/scripts/release-manage.sh</Path> 第 169–183 行在 `target != all` 时复制 `current_<env>` 到临时 staging；第 230–270 行只覆盖请求的 frontend 或 backend。第 273–318 行的 `write_manifest` 无论 staging 中文件来自哪次构建，都把当前 parent/backend/frontend HEAD、environment 与 bundle 写入同一 manifest。第 350–360 行对 `frontend`/`backend` 单目标同样写完整清单。

**问题与后果。** 先用 `bundle-core` 构建 backend，再只构建 frontend，新的 manifest 可能标记请求的 `bundle` 参数，而 staging 仍含旧 backend；反向亦然。旧产物没有 commit/digest 对照，partial build 失去可追溯性，回滚和发布批准会看到“当前 HEAD”但部署了其他 revision。<Path>release-artifacts/README.md</Path> 第 61–66 行要求 manifest 可追溯，却没有声明 partial 组合规则。

**代码 judo / 删除测试。** 删除“按目录复制 current 后覆盖一部分”的隐式组合；每次可交付 release 只接受同一 parent/backend/frontend revision 的完整 artifact set。若保留单目标开发命令，它应输出不可发布的局部 cache，不得进入 `current_<env>`、stage 或 bundle。manifest 应记录每个 artifact 的 source SHA 和完整清单，promotion 在全部校验通过后一次完成。

**建议。** P1，`D-T03-release-provenance`。先建立负测试：backend-only 后不得产生 deployable current；frontend-only 只能更新独立 cache；两端完整构建的 manifest source SHA 与产物 digest 一一匹配；stage 对缺失/混源立即失败且不覆盖已发布目录。不要通过复制更多 metadata 来掩盖混源，核心是删除隐式 partial promotion。`dependency class: local-substitutable`；`strength: Strong`；`ADR conflict: ADR-0042` 的“可复现构建”与发布 manifest 合同。

### D-04 P2 confirmed/likely：bundle-core 构建参数与必需 artifact 校验漂移

**证据。** <Path>scripts/ci/verify-admin-bundle.sh</Path> 第 20–21 行要求 `wta-system,wta-common-notify,wta-common-oss,wta-third,wta-sso`，第 24 行将 job/ai/demo/workflow 作为 optional。<Path>backend/wta-admin/pom.xml</Path> 第 147–205 行 `bundle-full` 同时包含 `wta-profile-person` 和 `wta-profile-enterprise`，`bundle-core` 也显式包含 profile 两个子模块；验证脚本没有断言 profile 两个 artifact。<Path>backend/pom.xml</Path> 第 431–488 行显示实际 platform/业务依赖集合；<Path>backend/wta-admin/src/test/java</Path> 仍有 workflow/demo 直接 import（例如 <Path>backend/wta-admin/src/test/java/org/namewta/test/profile/contract/WorkflowTerminationContractTest.java</Path> 第 9–13 行）。

**问题与后果。** “core 排除可选模块”的文字与测试 classpath、profile 依赖和校验列表不是同一 interface。`release-manage.sh` 第 198–200 行用 `-DskipTests` 构建 full/core；core 阶段跳过测试本身合法，但此前是否运行过覆盖排除组合的 compile/test 没有由 release script 强制。遗漏 profile artifact 会使 bundle gate 在错误组合上给绿灯；直接把 admin tests 当 core 运行又可能依赖被排除的 workflow/demo。

**建议。** P2，`D-T04-bundle-contract`：从 effective POM 与实际 JAR 生成唯一 bundle manifest，分别记录 full/core 的 required/forbidden/allowed artifact；补齐 `wta-notify`、profile person/enterprise 的必需项，并保留已存在的 third、sso 校验。项目画像已有 core `-Dmaven.test.skip=true` 规则，而 release 脚本 core 仍使用 `-DskipTests`，后者仍进入 test-compile；admin tests 的 workflow/demo 直接 import 在 core classpath 下的编译失败为 `likely`，本次未运行 Maven。先按当前合同在完整测试通过后用正确 core 打包参数；长期将排除组合测试 scope 独立到能验证该组合的 test module/fixture。验收为两种 clean package、`jar tf` manifest、未跳过的受影响 tests；不以删除 tests 或扩大 core 依赖获得通过。

### D-05 P2 confirmed：项目画像、模块地图与前后端 README 的数字/拓扑事实过时

**证据。** <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path> 第 5、14、19、24–25 行仍写 `wta-vue-plus-docs`、唯一 Admin、46 POM/176 test、`plan/update.md` 和 CI workflow；<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path> 第 7–9 行重复这些数字。当前 git inventory 为 50 POM、247 Java test source、3 App。<Path>frontend/README.md</Path> 第 9、38 行和 <Path>frontend/AGENTS.md</Path> 第 7 行仍只描述 Admin/Home；<Path>docs/namewta-enhancements.md</Path> 第 21 行也把 SSO 从发布面排除。<Path>release-artifacts/README.md</Path> 第 27 行同样只发布 Admin/Home。

**问题与后果。** 这是重复权威：源码/POM、根 README、project profile、module map 和 release README 各自维护 App 与数量。reviewer 无法知道哪个数字可依赖，文档变更会继续漂移。

**代码 judo / 删除测试。** 保留短 README 作为导航，把数量、激活状态、构建命令和 owner 收敛到一个可验证的 project profile/module manifest；删除各 README 中重复的模块清单和过去时描述。对每个动态数字增加验证脚本输出或明确“不得手工统计”，使错误事实在文档 gate 失败而非静默通过。

**建议。** P2，`D-T05-facts-convergence`：以当前 POM/package/Compose 为 source，修正 project profile、module map、<Path>frontend/README.md</Path>、<Path>frontend/AGENTS.md</Path>、<Path>docs/namewta-enhancements.md</Path>、<Path>release-artifacts/README.md</Path>；保留历史 ADR/变更链接但不把 archive 当 current fact。验收为 inventory diff、README link scan、命令矩阵一致性；明确 3 个 App 的 shipped/compiled/preview 状态分别是什么。

### D-06 P2 confirmed：41 份后端 AGENTS 是低深度复制，且验证命令多数错误/不完整

**证据。** git inventory 发现 49 个 <Path>backend/**/AGENTS.md</Path>，其中 41 个完全呈 31 行通用索引形状，只把 Scope、路径和命令替换。<Path>backend/AGENTS.md</Path> 第 27 行甚至给出字面占位 `mvn -pl 后端工作区`；<Path>backend/wta-common/AGENTS.md</Path> 第 27 行使用 `mvn -pl wta-common`（聚合 POM，不是实际 compile target）；<Path>backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md</Path> 第 29 行从后端 cwd 调用 `node .agents/skills/...`，相对路径无法指向仓库根 `.agents`。很多索引只说“以源码为准”，没有本模块 public entry、owner、dependency class 或最小正确验证。

**问题与后果。** 这些文件形成 shallow module/interface：复制文本增加维护面，却没有 locality 或导航价值。命令复制错误会使新人得到稳定失败的验证步骤。

**代码 judo / 删除测试。** 删除 41 份无增量事实的正文，保留每个真正有边界/例外/验证差异的模块 README；统一以模板生成短索引，但模板必须输出相对仓根正确的命令（或显式 `cd <Path>backend</Path>`）。不删除有 Profile、Notify、SSO、Third 特殊边界的 6–8 份真实手册，先比较内容和 owner。

**建议。** P2，`D-T06-agents-consolidation`：逐文件 hash/语义 diff；generic-only 文件合并到父级导航或删除，特例保留并补齐入口、依赖、验证和 Read Next。验收：所有 README 命令在声明 cwd 下可解析；索引不重复工程规范、不制造第二套 module mode 名单。`dependency class: local-substitutable`；`strength: Strong`；`deleted complexity: 41 duplicate handbooks`。

### D-07 P2 confirmed：SpecDev 全局 verification 仍指向删除的后端目录

**证据。** <Path>{roots.state}/specdev/config.json</Path> 第 24–27 行将 build 写为 `cd ruoyi-vue-plus-namewta && sh mvnw package ... && cd ../frontend && pnpm build`，test 同样进入删除的 `ruoyi-vue-plus-namewta`。当前仓库后端路径是 <Path>backend</Path>，wrapper 入口是 <Path>backend/mvnw</Path>；正确的 frontend build interface 来自 <Path>frontend/package.json</Path>。config 的 lint/typecheck 只覆盖旧式 `pnpm --dir frontend ...`，且 build 未声明 architecture/openapi gate。

**问题与后果。** SpecDev Ready/verification 会在执行前失败，或者执行的命令与项目画像不一致；报告若照 config 记录会产生假阴性/假阳性。

**建议。** P2，`D-T07-specdev-verification-sync`：在用户批准后将 global config 的 verification 与当前 project profile 的真实命令矩阵对齐，按 command/cwd/exitCode 记录；本次不修改 global status/config。`pnpm build` 当前存在且是 <Path>frontend/package.json</Path> 的 `build:prod` 别名，问题是旧 `ruoyi-vue-plus-namewta` cwd 和缺少 architecture/OpenAPI 语义，而不是该别名不存在。验收必须逐条从仓根/目标目录运行，区分 build、test、typecheck、architecture、openapi、external services；不得把 planned/not-run 写成 passed。

### D-08 P2 confirmed：`frontend` build:dev 会随后用 production build 覆盖 Home 产物

**证据。** <Path>frontend/package.json</Path> 第 18 行：`build:dev` 先运行 Admin 与 Home 的 `build:dev`，随后第 21 行 `build:workspace:non-admin` 执行 `pnpm -r --filter '!@namewta/admin-web' --if-present build`。`build` 在 <Path>frontend/apps/home-web/package.json</Path> 第 10 行固定为 `vite build --mode production`，SSO 也被该递归命令 production build。于是 `pnpm build:dev` 结束时 Home/SSO 不是 dev mode，且同一 workspace 其他包可能重复执行 build。

**问题与后果。** 开发构建的 `VITE_APP_CONTEXT_PATH`、API proxy、source map 和环境语义可能被 production build 覆盖；命令名与最终产物不一致，release/debug 排障会误判。该判断基于脚本组合，尚未执行 Vite 产物检查。

**建议。** P2，`D-T08-frontend-build-matrix`：每个 environment 对每个 shipped App 只调用一次对应 `build:<env>`；递归 workspace build 只能用于生产或显式 all matrix，不能从 dev script 隐式调用 package `build`。增加静态命令 contract 测试，断言 build:dev 输出的 index 资源和 env marker 与 development，build:prod 与 production 一致。需要 runtime：Vite 产物路径和浏览器加载验证。

### D-09 P2 confirmed：外部服务镜像版本在 CI 与发布 Compose 漂移

**证据。** <Path>scripts/ci/run-external-services.sh</Path> 第 32 行使用 `pgsty/minio:RELEASE.2026-08-04T00-00-00Z`；<Path>release-artifacts/docker/docker-compose-infrastructure.yml</Path> 第 111 行使用 `pgsty/minio:RELEASE.2026-04-17T00-00-00Z`。同一 CI/部署 contract 的 Redis、MySQL、MinIO 行为未由共享版本 manifest 驱动。

**问题与后果。** 集成测试通过的 API/签名/CORS 语义不一定是发布运行时语义；未来镜像更新会只改一侧，供应链 provenance 也无法从 manifest 追踪。

**建议。** P2，`D-T09-runtime-image-matrix`：建立单一受审查 image/version manifest，CI 和四类 Compose 仅引用它生成或校验；若 CI 必须与发布镜像不同，明确 compatibility class、差异原因和额外测试。验收：静态版本一致性、镜像 digest（若环境可得）、MinIO OSS readiness/私有匿名拒绝和 Nginx/API smoke。`needs-runtime`：无法在本机 Docker 缺失时证明兼容。

### D-10 P2 confirmed：发布脚本是非原子、多阶段写入，失败恢复依赖目录副作用

**证据。** <Path>release-artifacts/scripts/release-manage.sh</Path> 第 137–153、217–228、405–420 行使用 `find -delete` 清目录并直接复制 frontend HTML/JAR 到 Docker context；第 462–478 行 `stage_release` 依次写 backend images、frontend html、MySQL staging。任一 app 缺模板、产物损坏或 SQL 校验失败时，前置目录可能已经被清空/部分覆盖。<Path>release-artifacts/scripts/verify-release.sh</Path> 主要验证脚本和合同测试，没有对“失败后目标目录字节不变”作普遍断言。

**问题与后果。** 可复现 release 需要全量成功后再 promotion；当前 stage 的 seam 泄漏中间状态给 Nginx/Docker 和人工操作者，发布失败可能留下混合 current/context。

**建议。** P2，`D-T10-atomic-staging`：所有产物、Nginx、Compose、SQL、manifest 先在受保护临时 staging 完整验证，最后单次 promotion；失败只清理本次 staging，目标 context digest 保持不变。增加失败注入测试（缺 App template、坏 SQL、坏 JAR）和前后 SHA-256 对比。不要以恢复脚本掩盖非原子 seam。

## P3 清理与保留清单

逐文件候选、原始 snippet、SHA-256 和父 owner 见 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/docs-cleanup-inventory.json</Path>；可读表见 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/docs-cleanup-inventory.md</Path>。

* **删除/合并候选（执行前逐条确认 owner）：** <Path>scripts/ci/verify-submodules.sh</Path> 及其 CI/README 映射；41 份 generic-only <Path>backend/**/AGENTS.md</Path>（保留真实特殊边界文件）；README 中重复的 module/file inventory；任何把 `plan/update.md`、缺失 workflow 或唯一 Admin 当 current fact 的段落。
* **必须保留并重写：** <Path>docs/README.md</Path> 作为导航；<Path>docs/namewta-enhancements.md</Path> 作为产品增强叙述但改为 current/target 分栏；<Path>docs/fm/**</Path> 静态模板（当前 validator 通过）；<Path>release-artifacts/README.md</Path> 的 MySQL owner、安全批准和 rollback 规则；<Path>frontend/tooling/generators/README.md</Path>、<Path>frontend/packages/adapters/taro-request/README.md</Path> 这类 README-only placeholder（它们有明确激活门槛，不应伪装成源码）。`<Path>release-artifacts/skills/wta-namewta-nginx-config/**</Path>` 是发布配置专用 Skill，不与项目开发 Skill 混作第二套后端规则；其 App 台账需与 release manifest 收敛。
* **不要删除：** `org.dromara` 第三方坐标、WarmFlow/SnailJob/AI schema、Nacos 独立初始化、`frontend/LICENSE`、`backend/LICENSE`、Notify/Profile/SSO/Third 的真实 module handbook；先以 owner 和 ADR 判断。
* **命名与路径：** 生产自有 Java package 使用 `org.namewta`；分层 validator 旧 `org.dromara` predicate 是 B finding。不要为了“清理旧名”改供应商接口或历史 ADR。

* **校验器前置条件：** `validate-skill-facts.mjs` 第 50–51 行把被 `.gitignore` 忽略且当前不存在的 <Path>temp/release</Path> 当作 PASS 必备目录，同时检查旧拼写 `temp/relase`。这使干净 clone 的治理校验先于任何 Skill 事实检查失败。

### D-11 P2 confirmed：Skill 事实校验把私密运行目录的存在性错误地当作仓库健康条件

**证据。** <Path>.agents/skills/engineering-standards/scripts/validate-skill-facts.mjs</Path> 第 50 行要求 `existsSync(join(root, 'temp', 'release'))`，第 51 行检查旧拼写；<Path>.gitignore</Path> 已忽略 `temp/`。当前目录不存在，命令 exit 1。私密发布报告目录按 <Path>release-artifacts/README.md</Path> 和项目安全规则不应被 Git 跟踪。

**问题与后果。** validator 将“部署后可选的本地私密输出目录”与“Skill 事实正确”耦合，干净 clone 和 CI 无法通过；开发者可能为绿色状态创建无内容目录，增加不必要的工作树副作用。

**代码 judo / 删除测试。** 删除存在性硬门禁；只在写入发布报告的命令中按需 `mkdir`，或检查 parent ignore rule/路径策略而不要求目录存在。保留旧拼写检测和敏感文件 ignore 测试。

**建议。** P2，合入 `D-T01-delivery-gates` 或独立 `D-T11-skill-validator-preconditions`；增加 clean-clone fixture，验证不创建目录也可通过。`dependency class: local-substitutable`；`strength: Strong`；`ADR conflict: 无`。

## 提案到 tickets / spec / ADR 的写集

这些是供用户审核的 draft 候选，不表示已接受，也不应在本次 review 阶段直接实现：

全部候选的 `interview state = not-started`、`user conclusion = pending-user-review`。所有改造保留当前已接受的安全、MySQL owner 和独立 SSO Origin 契约；用户要求无兼容升级只表示可提硬切方案，不表示已批准上线或删除数据。

| Finding | code-judo / 真正删除的复杂性 | dependency class / strength | ADR 与删除测试 |
|---|---|---|---|
| D-01 | 删除 submodule 快照分支与虚假 CI 状态 | local-substitutable / Strong | 兑现 ADR-0053；删除后 monorepo 无需 checkout/SHA 双源 |
| D-02 | 删除目录扫描隐式激活发布面，收敛显式 release manifest | ports & adapters / Strong | 保留 ADR-0073 独立 Origin；缺 template 在写产物前失败 |
| D-03 | 删除从 current 复制另一半旧产物的 implicit promotion | local-substitutable / Strong | 与可追溯交付一致；混源不再可产生 deployable current |
| D-04 | 删除 README/script/POM 三份手工 bundle 名单 | local-substitutable / Strong | 不改变 full/core 产品契约；manifest 派生并检查真实 JAR |
| D-05 | 删除重复事实数字与失效入口叙述 | local-substitutable / Strong | 与 DEC-000、DOC-003 一致；实际 inventory 一变即可发现漂移 |
| D-06 | 删除无模块事实的复制手册正文 | local-substitutable / Strong | 保留特例规则；删后 owner/入口仍可在 canonical 导航找到 |
| D-07 | 删除旧目录和旁路 verification 命令 | local-substitutable / Strong | ADR-0053；执行范围直接来自当前 command matrix |
| D-08 | 删除 dev 后递归 production 二次构建 | in-process / Strong | 无 ADR 冲突；每 App 每 mode 一次构建 |
| D-09 | 删除 CI/Compose 两处独立镜像版本选择 | ports & adapters / Strong | 无 ADR 冲突；允许差异须显式记录 compatibility class |
| D-10 | 删除逐目录原地清空与部分投放分支 | local-substitutable / Strong | 与发布回滚契约一致；失败时旧 context 字节不变 |
| D-11 | 删除 Skill facts 与可选私密输出目录的耦合 | local-substitutable / Strong | 无 ADR 冲突；clean clone 无需创建任何发布目录 |

| Ticket 候选 | 内容与写集 | 门槛/验收 |
|---|---|---|
| `D-T01-delivery-gates` | `spec.md`：CI/local gate matrix；`tickets-map`：命令 owner；ADR 候选：canonical gate interface；重写 profile/module map/scripts README | 先确认是否恢复 `.github` workflow；远程 required check 需 Actions + branch protection 证据 |
| `D-T02-sso-release-surface` | SSO shipped/compiled/preview scope、origin/callback/cookie/反代矩阵、Compose/Nginx/env 清单；引用 ADR-0073/0074 | 用户决定发布 SSO 或明确源码-only；Playwright + 双后端 runtime |
| `D-T03-release-provenance` | release manifest source SHA、artifact digest、禁止混源 partial promotion、full staging；更新 `release-artifacts` spec/ADR | backend/frontend 同 revision；失败注入后目标目录 hash 不变 |
| `D-T04-bundle-contract` | full/core effective POM 与 JAR artifact allow/deny manifest；测试 scope 与 `verify-admin-bundle` | clean 两 profile、受影响 tests 未跳过；不扩大 bundle 以逃避验证 |
| `D-T05-facts-convergence` | project profile/module map 为唯一事实，README 只导航；删除失效路径描述 | inventory、link、命令矩阵一致 |
| `D-T06-agents-consolidation` | generic AGENTS 删除/合并，保留真实 boundary docs；模板命令 cwd 修复 | 每份保留文件有独立 owner/dependency/verification |
| `D-T07-specdev-verification-sync` | 全局 config 命令更新与 evidence schema；本次 review 只记录，不改 config | 用户批准后逐命令执行并记录 exit code |
| `D-T08-frontend-build-matrix` | build dev/prod 单次 App matrix、静态 mode contract | 产物环境标记、浏览器 smoke |
| `D-T09-runtime-image-matrix` | CI/Compose 镜像 manifest 与 digest/差异策略 | Docker、OSS readiness、供应链审查 |
| `D-T10-atomic-staging` | release staging promotion 与 failure injection | 失败不污染目标 context |
| `D-T11-skill-validator-preconditions` | 与 D-T01 合并：只检私密路径策略，不要求目录存在 | clean-clone fixture；不创建目录也能验证事实 |

## 当前没有问题或尚未验证的面

* literal Markdown 相对链接支持子集无失效项；FreeMarker 清单/SQL/CRUD method/Javadoc 静态 validator 通过。
* <Path>release-artifacts/docker/infrastructure/mysql/init/</Path> 六份 SQL 的 owner、顺序和 MySQL-only 规则已有多处一致描述，未发现应因“旧内容”删除的第三方 schema。
* 未验证 Maven effective dependency、Java compile/test、pnpm install/lint/typecheck/unit/build、Playwright、Docker Compose/Nginx、真实 MinIO/MySQL/Redis、SSO callback/cookie、远程 GitHub Actions/branch protection、覆盖率/依赖漏洞扫描、FreeMarker 代表性渲染。它们均是 `needs-runtime` 或 `not-run`，不能写 passed。
* UI/UX 本轮只审查交付与文档可达性，没有把图片预览当作当前交互通过证据；Admin/Home/SSO 真实登录、动态菜单、权限负向和 API error state 需要独立浏览器票据。

## 最佳推荐

唯一最佳推荐是先审查 `D-T01-delivery-gates`。随后处理 `D-T03-release-provenance`，再决定 `D-T02-sso-release-surface`。这条顺序删除隐式 gate、混源产物和重复事实，提升 interface depth、leverage 与 locality；其余文档合并、镜像矩阵和 UI/runtime 验证在前两个基础合同稳定后按 tickets-map 排期。当前没有对产品代码的改写，也没有把任何候选视为用户已接受。
