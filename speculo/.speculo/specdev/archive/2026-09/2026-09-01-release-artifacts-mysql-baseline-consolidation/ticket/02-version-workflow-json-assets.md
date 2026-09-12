---
schema_version: 3
artifact: ticket
change: 2026-09-01-release-artifacts-mysql-baseline-consolidation
id: T-02
title: 在发布资产中版本化工作流 JSON
status: done
planning_depth: standard
planning_depth_reason: 跨父仓库和后端子仓库迁移六个运行参考资产，需要内容等价、解析和跟踪合同
ready: true
risk: medium
blocked_by: []
contract_ids: [AC-005]
owner: codex:/root
expected_changes:
  - "<Path>release-artifacts/workflow/leave/*.json</Path>"
  - "<Path>release-artifacts/tests/workflow-assets-contract.test.mjs</Path>"
writable_paths:
  - "<Path>release-artifacts/workflow/leave/*.json</Path>"
  - "<Path>release-artifacts/tests/workflow-assets-contract.test.mjs</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/script/leave/*.json</Path>"
shared_paths:
  - "<Path>release-artifacts/workflow/leave/*.json</Path>"
shared_path_owners:
  - "<Path>release-artifacts/workflow/leave/*.json</Path> => T-02"
---

# Ticket T-02：在发布资产中版本化工作流 JSON

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/02-version-workflow-json-assets.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 把 `leave1.json` 至 `leave6.json` 等价放入父仓库发布资产，作为可复用、可跟踪的工作流模板。
- **可观察产出：** 新目录中恰有六个可解析 JSON，逐文件摘要与旧目录一致，并被 Git 跟踪。
- **来源：** `US-005`、`AC-005`、`USER-DECISION:后端 script 整体退出`。
- **当前事实：** 六个 JSON 只存在于即将删除的后端 `script/leave/`。
- **Planning Depth 原因：** 跨 Git 仓库搬迁资产，必须先扩展再收缩，避免删除时丢失流程模板。

## 2. 决策状态

### 已锁定决策

- 新位置固定为 `<Path>release-artifacts/workflow/leave/</Path>`。
- T-02 只建立等价新副本；旧文件由 T-04 在所有消费者迁移后统一删除。
- 文件名、字节内容和 JSON 结构不得借迁移机会改写。

### 已采用的低影响假设

- 使用独立 Node 合同测试检查清单、JSON 解析、摘要和 Git 跟踪。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 六文件等价复制、新目录和合同测试 | 旧 JSON 作为只读来源、Node 测试基建 | 删除旧目录、修改流程定义、部署或导入流程 |

## 4. 要构建什么

维护者从发布资产目录即可找到六个请假流程参考文件，测试会保证文件完整、可解析、已跟踪且迁移期间内容没有变化。后端旧文件在本 Ticket 后仍短期存在，只作为 T-04 收缩前的兼容副本。

## 5. 实现契约

- **入口或接缝：** 发布目录文件清单、JSON 解析和 Git 跟踪合同。
- **输入与输出：** 输入为六个旧 JSON；输出为六个同名、同摘要的新文件。
- **公共接口变化：** 无运行时 API 变化。
- **不变量：** 文件数量为 6，名称为 `leave1.json` 至 `leave6.json`，逐文件摘要一致。
- **状态或数据流：** 后端旧资产只读 -> 父仓库发布资产 -> 后续文档与部署消费者。
- **错误与失败行为：** 缺文件、重名、不可解析、摘要不同或未跟踪时测试失败并指明文件。
- **兼容要求：** T-04 前保留旧副本；T-05 负责更新活动文档引用。
- **安全与隐私要求：** JSON 不得新增环境账密或现场标识；测试只读取仓库文件。

## 6. 执行路线

1. 建立失败测试，声明六文件清单、解析和跟踪要求。
2. 创建新目录并等价复制六个文件。
3. 比对旧/新逐文件摘要并解析全部 JSON。
4. 证明新文件被 Git 跟踪候选接管，且未修改旧来源。
5. 运行 Node 回归并记录迁移清单。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅新发布目录和独立合同测试。
- **只读上下文：** 旧 JSON 只读，不在本 Ticket 删除或编辑。
- **共享路径：** 新 Workflow 资产由 T-02 唯一拥有。
- **保留或不动：** 工作流 Java、数据库表和已部署流程实例均不改动。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Node 文件合同 | 运行工作流资产测试 | 六文件存在、可解析、被跟踪、摘要一致 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-02.md</Path>` |
| 失败路径 | 隔离夹具 | 缺失或破坏一个 JSON | 测试失败并指出目标文件 | 同上 |
| 回归 | Git diff 与 JSON 解析 | 比对旧/新 SHA-256、解析全部文件 | 旧文件未变，新文件无格式损坏 | 同上 |

- **Workspace checks：** Node 定向测试、Git 跟踪检查、`git diff --check`。
- **E2E disposition：** `not-required`：仅做静态模板等价迁移，不导入运行中流程引擎。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** 实现提交、父分支结果、六份摘要清单。

## 9. 发布、迁移与恢复

- **迁移顺序：** 新增新位置 -> 摘要与解析验证 -> 更新消费者 -> T-04 删除旧位置。
- **兼容窗口：** T-02 完成至 T-04 收缩期间允许两份字节等价资产，父仓库新位置为目标 owner。
- **监控信号：** 文件清单、JSON 解析和摘要差异。
- **回滚或前向恢复：** 新位置有误时只修正新副本；旧位置在 T-04 前仍可用于恢复比对。
- **不可逆操作与批准点：** 无删除、无流程导入；实现提交需授权。
- **收缩条件：** 新位置 6 文件全部有效、被跟踪且所有活动引用已能迁移。

## 10. 验收标准

- [ ] `AC-005` 的 Workflow JSON 部分有完整证据。
- [ ] 六个文件逐个摘要一致、可解析并被 Git 跟踪。
- [ ] 未修改或删除旧来源，收缩交给 T-04。
- [ ] 验证记录到 `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-02.md</Path>`。
- [ ] 形成非空实现提交和父分支验证结果。
