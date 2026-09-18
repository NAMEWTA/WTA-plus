# WTA-plus 清理计划：REMOVE / REWRITE / KEEP / VERIFY

状态：draft；2026-09-18已复核；本文件只规划后续实施，不授权现在删除任何产品文件。执行顺序与主 tickets-map 一致，T-01处理失效门禁，T-29处理文档；T-09/T-10/T-08分别拥有构建、发布原子性与SSO发布改造。

## 1. 判定规则和不可删除边界

| 分类 | 含义 | 转换门槛 |
|---|---|---|
| REMOVE | 已证实失效且没有独有合同的脚本/正文 | 当前SHA一致、全调用者扫描、有效内容已承接、删除后验证通过；未通过任一项退回VERIFY |
| REWRITE | 文件仍是合法入口，内容/命令/来源已漂移 | 对照当前POM/package/source，保留MUST/禁止与真实边界，写明owner、cwd、验证状态 |
| KEEP | 历史证据、许可证、供应商合同或仍有独立职责 | 不因旧名、行数或目录不美观删除；触及规则必须另有明确设计与批准 |
| VERIFY | 存在疑点但尚未证实可删除 | 逐文件diff、独有规则清点、入口/引用/测试扫描完成后，由owner选择KEEP/REWRITE/REMOVE |

证据来源：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/docs-cleanup-inventory.json</Path>。该历史清单仅作溯源，当前SHA和全文见re-review-cleanup-inventory.json：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review-cleanup-inventory.json</Path>。旧SHA与当前文件已不同，不能据旧SHA执行删除。它记录原始SHA-256、原文snippet、父owner与处置条件；下表是可读投影。41份只是具有相同31行形状，不是41份均可删除：其中4份聚合导航REWRITE，37份VERIFY后才可能REMOVE。8份特殊手册KEEP独有合同。15份当前文档REWRITE。

每项删除必须保存：原路径及SHA、owner、独有MUST/例外清单、承接路径、调用者/链接更新结果、实际命令与退出码、删除前后diff。存在未承接硬约束即停止该文件删除，其他独立文件可继续。

## 2. 明确失效脚本：REMOVE候选

| 路径 | owner / Ticket | 证据与删除门槛 |
|---|---|---|
| <Path>scripts/ci/verify-submodules.sh</Path> | T-01 / Lead | ADR-0053已明确monorepo；脚本对无submodule稳定exit 1。删除前移除所有真实调用、README snapshot映射，验证仓库不再依赖gitlink。不保留一个永远失败的兼容脚本。 |

不存在的<Path>.github/workflows/quality-gates.yml</Path>、<Path>plan/update.md</Path>、<Path>.vscode/settings.json</Path>无需执行删除。应重写引用它们的当前说明；CI与Java构建保护是否恢复由T-01裁决，不能为匹配旧文案而无理由创建占位文件。

## 3. 41份generic AGENTS逐文件处置

| # | 路径 | 当前分类 | 承接owner | 具体动作与门槛 |
|---|---|---|---|---|
| 1 | <Path>backend/AGENTS.md</Path> | REWRITE | <Path>backend/AGENTS.md</Path> | Rewrite aggregate navigation; retain meaningful module entries and correct Wrapper/cwd. |
| 2 | <Path>backend/wta-admin/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 3 | <Path>backend/wta-api/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 4 | <Path>backend/wta-common/AGENTS.md</Path> | REWRITE | <Path>backend/AGENTS.md</Path> | Rewrite aggregate navigation; retain meaningful module entries and correct Wrapper/cwd. |
| 5 | <Path>backend/wta-common/wta-common-ai/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 6 | <Path>backend/wta-common/wta-common-bom/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 7 | <Path>backend/wta-common/wta-common-core/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 8 | <Path>backend/wta-common/wta-common-doc/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 9 | <Path>backend/wta-common/wta-common-elasticsearch/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 10 | <Path>backend/wta-common/wta-common-encrypt/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 11 | <Path>backend/wta-common/wta-common-excel/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 12 | <Path>backend/wta-common/wta-common-job/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 13 | <Path>backend/wta-common/wta-common-json/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 14 | <Path>backend/wta-common/wta-common-liteflow/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 15 | <Path>backend/wta-common/wta-common-log/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 16 | <Path>backend/wta-common/wta-common-mail/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 17 | <Path>backend/wta-common/wta-common-mcp/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 18 | <Path>backend/wta-common/wta-common-mqtt/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 19 | <Path>backend/wta-common/wta-common-mybatis/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 20 | <Path>backend/wta-common/wta-common-nacos/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 21 | <Path>backend/wta-common/wta-common-notify/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 22 | <Path>backend/wta-common/wta-common-openapi/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 23 | <Path>backend/wta-common/wta-common-oss/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 24 | <Path>backend/wta-common/wta-common-push/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 25 | <Path>backend/wta-common/wta-common-redis/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 26 | <Path>backend/wta-common/wta-common-satoken/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 27 | <Path>backend/wta-common/wta-common-security/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 28 | <Path>backend/wta-common/wta-common-sensitive/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 29 | <Path>backend/wta-common/wta-common-sms/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 30 | <Path>backend/wta-common/wta-common-social/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 31 | <Path>backend/wta-common/wta-common-translation/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 32 | <Path>backend/wta-common/wta-common-web/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-common/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 33 | <Path>backend/wta-extend/AGENTS.md</Path> | REWRITE | <Path>backend/AGENTS.md</Path> | Rewrite aggregate navigation; retain meaningful module entries and correct Wrapper/cwd. |
| 34 | <Path>backend/wta-extend/wta-monitor-admin/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-extend/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 35 | <Path>backend/wta-extend/wta-snailai-server/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-extend/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 36 | <Path>backend/wta-extend/wta-snailjob-server/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-extend/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 37 | <Path>backend/wta-modules/AGENTS.md</Path> | REWRITE | <Path>backend/AGENTS.md</Path> | Rewrite aggregate navigation; retain meaningful module entries and correct Wrapper/cwd. |
| 38 | <Path>backend/wta-modules/wta-ai/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-modules/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 39 | <Path>backend/wta-modules/wta-job/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-modules/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 40 | <Path>backend/wta-modules/wta-profile/wta-profile-bom/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-modules/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |
| 41 | <Path>backend/wta-modules/wta-workflow/AGENTS.md</Path> | VERIFY -> REMOVE | <Path>backend/wta-modules/AGENTS.md</Path> | Compare current SHA/content; migrate unique rules before deleting generic-only text. |

## 4. 8 special AGENTS: KEEP / targeted REWRITE

| Path | Disposition |
|---|---|
| <Path>backend/wta-modules/wta-demo/AGENTS.md</Path> | KEEP unique module contracts; only correct verified entry/cwd errors. |
| <Path>backend/wta-modules/wta-notify/AGENTS.md</Path> | KEEP unique module contracts; only correct verified entry/cwd errors. |
| <Path>backend/wta-modules/wta-profile/AGENTS.md</Path> | KEEP unique module contracts; only correct verified entry/cwd errors. |
| <Path>backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md</Path> | KEEP unique module contracts; only correct verified entry/cwd errors. |
| <Path>backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md</Path> | KEEP unique module contracts; only correct verified entry/cwd errors. |
| <Path>backend/wta-modules/wta-sso/AGENTS.md</Path> | KEEP unique module contracts; only correct verified entry/cwd errors. |
| <Path>backend/wta-modules/wta-system/AGENTS.md</Path> | KEEP unique module contracts; only correct verified entry/cwd errors. |
| <Path>backend/wta-modules/wta-third/AGENTS.md</Path> | KEEP unique module contracts; only correct verified entry/cwd errors. |

## 5. 15 current documentation targets: REWRITE

| Path | Required revision |
|---|---|
| <Path>scripts/README.md</Path> | 重写真实CI状态和dev-build-guard前置条件；删除submodule snapshot映射。 |
| <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path> | 更新三App、50 POM、247 backend tracked test-source和失效plan/CI来源；动态数字派生。 |
| <Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path> | 补齐POM模块和真实测试面；Source/Status不宣称不存在CI已启用。 |
| <Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path> | 更新Current事实；兼容等待按用户本次无兼容要求撤销，安全/事务/数据owner规则保留。 |
| <Path>README.md</Path> | 保留产品说明和图片，校正获取构建示例cwd和可发布状态；历史预览不当当前验证。 |
| <Path>frontend/README.md</Path> | 更新Admin/Home/SSO编译与发布状态；删除第二个App尚待激活的旧条件。 |
| <Path>frontend/AGENTS.md</Path> | 增加SSO导航并指向canonical module facts。 |
| <Path>frontend/apps/README.md</Path> | 三个App真实边界与SSO独立Origin；激活门槛仍保留。 |
| <Path>frontend/docs/architecture-baseline.md</Path> | 保留权限依赖不变量，校正跨App验证与OpenAPI准确cwd。 |
| <Path>backend/README.md</Path> | 将独立前端仓库/独立检出默认事实改为monorepo工作区；修正Notify/SSO导航。 |
| <Path>docs/README.md</Path> | 补入OSS双桶、Nacos运行、静态模板导航，子仓库改工作区。 |
| <Path>docs/namewta-enhancements.md</Path> | 分清三App的compiled/preview/shipped，不重复模块数字。 |
| <Path>docs/runtime-nacos-hard-cut.md</Path> | 当前规则并入发布Nacos说明；核对历史change链接，保留hard-cut合同。 |
| <Path>docs/oss-public-private-operations.md</Path> | 保留双桶、安全/迁移批准边界并加入docs导航。 |
| <Path>release-artifacts/README.md</Path> | 与发布manifest对齐三App和artifact provenance，保留MySQL唯一基座及回滚规则。 |

## 6. KEEP：历史、许可证、供应商和生成物

| 对象 | 保护和理由 | 后续可变门槛 |
|---|---|---|
| <Path>CLAUDE.md</Path> | 仅指向AGENTS的薄导航adapter，没有复制规则，KEEP | 仅入口协议改变时修导航，不因工具名称删除 |
| <Path>AGENTS.md</Path> | 当前授权与工程路由入口，KEEP | 用户明确要求且完整承接硬约束后才变更 |
| <Path>backend/LICENSE</Path>、<Path>frontend/LICENSE</Path> | KEEP许可证与版权来源；本轮未做法律所有权鉴定 | 权利人/许可证证据明确，不能按旧品牌批量改写 |
| 第三方org.dromara坐标、WarmFlow/SnailJob/AI数据库与Nacos schema | KEEP供应商版本/协议/所有权，不是自有旧代码 | 版本迁移票证明兼容和schema owner接管后才改；不按namespace grep删除 |
| <Path>release-artifacts/docker/infrastructure/mysql/init/</Path> | KEEP六份完整业务基座和独立Nacos初始化；ADR-0042唯一owner | 业务DDL/DML在所属ticket按现有规则修改；已有库不得重放基座 |
| <Path>{roots.state}/specdev/archive/</Path>、<Path>{roots.state}/specdev/adr/</Path>、<Path>{roots.state}/specdev/context/</Path> | KEEP历史原文与永久知识；本轮未全量审查，不是清理目标 | 仅通过明确归档/知识提升流程修订，不能把旧事实伪装成当前记录 |
| <Path>speculo/workflows/</Path>、<Path>speculo/skills/</Path>、供应商研究快照 | KEEP工作流/可复用模板/历史研究与示例所有权 | 只有明确消费链和新workflow任务时处理，不能因出现旧仓名一律删除 |
| <Path>docs/fm/</Path> | KEEP32模板及catalog/context契约；当前静态validator通过，未渲染/编译 | 代表性普通/树资源渲染、编译、测试完成后由模板ticket升级 |
| <Path>frontend/packages/api-contracts/</Path>与OpenAPI版本快照 | KEEP生成来源链；禁止手工修generated类型以掩盖漂移 | 通过tooling/openapi fetch/generate/check，确认调用方和snapshot owner |
| <Path>frontend/apps/*/src/types/auto-imports.d.ts</Path>、<Path>frontend/apps/*/src/types/components.d.ts</Path> | KEEP生成声明，不手改 | 修改生成器配置后重新生成并核对diff |
| <Path>frontend/tooling/generators/README.md</Path>、<Path>frontend/packages/adapters/taro-request/README.md</Path>、<Path>frontend/packages/adapters/taro-storage/README.md</Path> | KEEP明确的README-only占位及激活门槛 | 只有真实新终端规格接受后激活，不能创建空包冒充实现 |
| <Path>backend/**/target/</Path>、<Path>frontend/**/dist/</Path>、<Path>frontend/**/node_modules/</Path>、缓存与本地env | 非本次清理对象；既不当源码审查，也不执行磁盘清理 | 用户另行授权且解析绝对目标在允许workspace后才清理；产品构建按工具生命周期处理 |

## 7. VERIFY：路径、图像和当前事实

根README图片不因来自历史预览删除。先核对实际引用、当前页面和敏感数据；无引用且无历史/设计价值才列REMOVE候选。不能把旧截图说明改为本轮已运行。当前源码不支持的功能表述先写current/target或指向具体已确认限制，不能虚构新截图。

项目事实按实施后的真实源码重算：基线为50个backend POM描述符、3个App package、247个backend src/test/java Java源文件；这些不是固定通过阈值或测试用例数。库存数字只在一个可验证事实源维护，README只链接；新增模块导致数量变化不应被校验器误判失败。

前后端package/POM的依赖升级、模块删除、API改名、数据库DDL、菜单权限、UI重构由各自Ticket负责，本文件不额外授权。没有调用者/测试证据的thin wrapper不因名称或文件大小进入REMOVE。

## 8. 执行顺序、失败处理与验收

1. 进入产品实施后，由单一执行者将目标文件、SHA、owner与最终分类写入对应Ticket；T-01先恢复可信命令和验证器。
2. 逐文件复核inventory，先REWRITE父级聚合导航，迁移有用内容；37份VERIFY候选只有在无独有规则且引用已迁移时变为REMOVE。
3. 保留8份特殊手册的规则，按实际cwd修Wrapper命令。15份当前文档更新单一事实来源、三App状态、模块与真实CI状态。
4. 所属代码票发生事实变化时由该票owner同步，T-29不提前宣称能力已完成；T-30进行最终文档/源码/发布矩阵一致性检查。
5. 文档验收包括Path与普通code span路径、链接、每个命令cwd/退出码、生成物/历史/许可证前后SHA、硬约束迁移表。删除测试必须证明复杂性消失且信息未丢失。

可用验证命令必须在仓根执行并分别记录：`node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs`、`node .agents/skills/namewta-fullstack-development/scripts/validate-skill.mjs`、`node docs/fm/scripts/validate.mjs`、`git diff --check`。2026-09-18已运行facts、全栈Skill和模板检查，结果见reviews/re-review-command-results.json；实际文档清理尚未实施，不能将检查基线冒充T-29完成。

回滚为恢复本次受控文件diff及原始SHA；不删除历史、许可证、运行数据或共享目录。规则承接不完整、SHA漂移、owner不明或真实命令失败时保持draft，停止受影响项。

## 9. 当前所有权校正

发布测试要求speculo/skills/upstream-fork-sync不存在，但当前它是Speculo提供的内容。本change不删除该目录迎合产品断言；T-01先校正产品检查的所有权范围。41份generic索引归并后不再自动生成41份同义README/AGENTS；保留能增加真实导航价值的最少入口。

基座无需旧版兼容。代码合同直接同步切换；清理历史、许可证或运行数据不由此授权。
