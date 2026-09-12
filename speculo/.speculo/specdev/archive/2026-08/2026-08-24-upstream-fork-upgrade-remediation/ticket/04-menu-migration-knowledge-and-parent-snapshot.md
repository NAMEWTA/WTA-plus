---
schema_version: 3
artifact: ticket
change: 2026-08-24-upstream-fork-upgrade-remediation
id: T-04
title: 占位菜单迁移、维护知识与父快照收口
status: done
planning_depth: deep
planning_depth_reason: 包含 append-only 数据迁移、父仓 submodule 指针和跨仓长期维护知识，影响部署可见性与可复现交付
ready: true
risk: high
blocked_by: [T-01, T-02, T-03]
contract_ids: [AC-004, AC-006]
owner: codex
expected_changes: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/BusinessMenuRetirementMySqlIntegrationTest.java</Path>", "<Path>docs/upstream/customization-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/**</Path>", "<Path>.agents/skills/ruoyi-common-modules-guide/**</Path>", "<Path>ruoyi-vue-plus-namewta</Path>", "<Path>plus-ui-namewta</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/BusinessMenuRetirementMySqlIntegrationTest.java</Path>", "<Path>docs/upstream/customization-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/**</Path>", "<Path>.agents/skills/ruoyi-common-modules-guide/**</Path>", "<Path>README.md</Path>", "<Path>ruoyi-vue-plus-namewta</Path>", "<Path>plus-ui-namewta</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>", "<Path>.gitmodules</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-04: 占位菜单迁移、维护知识与父快照收口

- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-04.md</Path>`

## 1. 战略与来源

- **目标：** 让 fresh/upgrade 数据都不暴露占位入口，知识与实际 26 个 common 子模块/41-project reactor 一致，并准备正确父仓快照。
- **来源：** CR-001 parent/knowledge findings、CR-002 hotspot/product completeness finding；AC-004、AC-006。

## 2. 决策状态

- 追加幂等 DSL 块：先删除三个默认角色菜单关联，再将三个菜单设为停用；不改历史块。
- 管理台没有 SQL 菜单，只删除前端组件并在热点清单记录 deferred 产品域。
- common 口径为 26 个子 artifact（BOM + 25 jar）；默认 Maven reactor 与源码树均为 41 个 POM/project。
- 父仓最终记录两个子仓经验证 result SHA，并由远程 snapshot job 复核。
- **批准点：** 用户已要求修复两份审查，且此前明确基座无历史兼容负担；因此允许下线无功能菜单。生产执行 SQL 仍不在本轮授权内。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| append-only 菜单迁移、知识、热点、父 gitlink candidate | 固定 menu IDs、模块 POM 和 Git refs | 生产执行、commit/push、业务页面实现 |

## 4. 要构建什么

执行最新 NAMEWTA DSL 后，三个未实现工作台不再出现在默认角色菜单中；维护者看到的模块数、通知能力和上游热点与确定性扫描一致；父仓 working tree 指向两端验证后的 SHA。

## 5. 实现契约

- SQL 新块必须幂等、末尾追加，并先删除关系再停用菜单。
- 知识数字来自 POM/reactor 命令，不手工猜测。
- 父仓只改变预期 gitlink candidate，不覆盖其他 active change。

## 6. 执行路线

1. 生成上海时区标识，追加幂等 SQL 并增加迁移断言。
2. 更新 customization hotspot、工程画像/module map 和 common skill。
3. 运行 POM/目录/热点确定性扫描与 SQL 验证。
4. 确认两子仓 SHA，提交父仓 gitlink，并运行本地及远程 snapshot 校验。

## 7. 路径访问契约

- **可写范围：** frontmatter 声明的 SQL、知识、README 与两个 gitlink。
- **只读上下文：** NAMEWTA DDL 和 submodule 配置。
- **共享路径：** 无。
- **保留或不动：** 历史 SQL 块、基线 tag、其他 Speculo change。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| fresh/upgrade | isolated MySQL | 执行新增块两次 | 菜单停用、关联为 0、重复执行无错误 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-04.md</Path>` |
| 知识漂移 | deterministic inventory | POM/module counts | 文档数字和模块表一致 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-04.md</Path>` |
| 热点遗漏 | merge-base diff | name-status scan | 37 个前端 modified hotspot 有维护口径 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-04.md</Path>` |
| 父快照 | Git | submodule status + parent diff | 仅预期 result SHA，无未初始化状态 | `<Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-remediation/evidence/T-04.md</Path>` |

- **E2E disposition：** required；隔离 MySQL 和父仓快照属于跨边界验证。
- **E2E owner/environment：** Lead / current-workspace。

## 9. 发布、迁移与恢复

- **顺序：** 现有 DSL 后追加 `NAMEWTA-BASE-DSL-003`；fresh 环境按既有顺序自动收敛，upgrade 只执行新块。
- **兼容窗口：** 无；占位入口没有可用业务合同。
- **监控：** 查询三项 menu status 和 role-menu count。
- **回滚：** 重新启用菜单并按固定 ID 恢复三条默认角色关联；只有对应业务页面完成后允许。
- **不可逆操作：** 无，全部是可前向补偿的数据更新/删除。
- **收缩条件：** 菜单 status 全为停用、默认角色关联为 0、前端占位组件为 0。

## 10. 验收标准

- [x] AC-004、AC-006 通过。
- [x] SQL 仅末尾追加，具备前置、范围、幂等和回滚说明。
- [x] 所有长期知识不再宣称 25 artifacts/40 Maven modules。
- [x] Evidence 明确父仓结果 SHA、远程 snapshot 与 MySQL 重放结果。
