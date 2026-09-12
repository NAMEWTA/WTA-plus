---
schema_version: 3
artifact: ticket
change: 2026-08-24-upstream-fork-upgrade-remediation
id: T-02
title: 后端构建、上游补丁与支持合同
status: done
planning_depth: standard
planning_depth_reason: 修改 Maven 默认行为和部署 profile，并吸收单文件非重叠上游补丁；无业务 API 或 schema 变化
ready: true
risk: medium
blocked_by: []
contract_ids: [AC-002, AC-005]
owner: codex
expected_changes: ["<Path>ruoyi-vue-plus-namewta/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/mvnw</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mybatis/src/main/java/org/dromara/common/mybatis/interceptor/SqlLogInterceptor.java</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/README.md</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/mvnw</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mybatis/src/main/java/org/dromara/common/mybatis/interceptor/SqlLogInterceptor.java</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/README.md</Path>", "<Path>ruoyi-vue-plus-namewta/README.md</Path>"]
read_only_paths: ["<Path>docs/upstream/customization-map.md</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: 后端构建、上游补丁与支持合同

- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 让 Wrapper、默认测试、full/core bundle、MySQL-only 支持口径和缓存上游补丁成为真实合同。
- **来源：** CR-001 standards P1/P2、specification P1/P2；AC-002、AC-005。

## 2. 决策状态

- 根 Maven 默认执行测试；需要跳过时调用者必须显式声明。
- `bundle-full` 继续 activeByDefault；显式 `bundle-core` 只组装系统基础能力并自动关闭 full。
- MySQL-only 写入后端 README 和 SQL README，不新增方言。
- 产品源文件按上游 `2933bad` 的精确非重叠 diff 更新；本地 6.X 只 fast-forward。
- 未决问题：无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| Maven、Wrapper、单文件上游补丁、支持文档 | 现有测试和模块依赖 | 业务 API、其他数据库方言、远程 push |

## 4. 要构建什么

维护者直接运行 Wrapper 时可得到真实测试结果，并能显式选择 full/core 部署组合；产品源代码包含固定上游 SQL 日志优化，文档不会再暗示未经验证的多数据库支持。

## 5. 实现契约

- `./mvnw` 在 Git 中为 executable。
- 默认 test 实际运行且不能靠删除/skip 获绿；full/core 都完成 package。
- core bundle 不装入 job、AI、demo、workflow、gen；基础 system、notify、OSS 能力保留。
- SQL 日志优化不得改变格式，只移除进程内 console lock 并使用单次 print。

## 6. 执行路线

1. 固定 wrapper 权限、默认 skip、profile 和上游 diff 的 red 证据。
2. 调整 POM/文件模式并应用精确上游补丁。
3. 文档化 bundle 与 MySQL-only 支持矩阵。
4. 执行定向测试、默认 test、full/core package 和 git diff 检查。

## 7. 路径访问契约

- **可写范围：** 仅 frontmatter `writable_paths`。
- **只读上下文：** 现有测试和 customization map。
- **共享路径：** 无。
- **保留或不动：** 基线标签、认证/权限/Client 源码和锁文件。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 默认测试 | Maven | `./mvnw test` | 129+ 测试执行，不由根属性跳过 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-02.md</Path>` |
| bundle | Maven profiles + jar inventory | `./mvnw clean package -DskipTests`、`./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`、`scripts/ci/verify-admin-bundle.sh` | 两种组装成功且无跨 profile 产物污染；full 含、core 不含 job/ai/demo/workflow/gen | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-02.md</Path>` |
| 上游 | source diff | `git diff 2933bad -- SqlLogInterceptor.java` | 无该补丁差异 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-02.md</Path>` |

- **E2E disposition：** not-required；外部依赖 E2E 由 T-03 负责。
- **E2E owner/environment：** Lead / current-workspace；本 Ticket 不要求跨边界 E2E。
- **集成出口：** 后端结果已提交并推送，父仓最终快照已通过本地与远程门禁。

## 9. 发布、迁移与恢复

- **迁移顺序：** 默认测试和 profiles 同步落地，文档随后更新。
- **回滚或前向恢复：** 恢复 POM/模式/单文件补丁；无数据迁移。
- **不可逆操作与批准点：** 无。

## 10. 验收标准

- [x] AC-002、AC-005 通过。
- [x] 6.X 基线标签不移动，产品安全热点不受影响。
- [x] Evidence 记录命令、退出码和测试数量。
