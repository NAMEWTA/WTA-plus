---
schema_version: 1
artifact: triage
change: 2026-08-24-upstream-fork-upgrade-assessment
mode: intake
source: <Path>{roots.state}/specdev/changes/2026-08-24-upstream-fork-upgrade-assessment/source.md</Path>
classification: review
risk: medium
route: specdev/code-review
ready_for_implementation: false
external_action: not-applicable
updated_at: 2026-08-24T01:03:01+08:00
---

# Triage: NAMEWTA 上游 Fork 二次开发升级评估

## 当前判定

- **影响：** 覆盖父聚合仓库、后端与前端两个产品仓库，形成二开流程、业务逻辑、文件级差异、架构、持续同步适配度和后续升级优先级的决策依据；本 change 不修改产品代码。
- **紧急度：** normal
- **当前证据：** 后端当前 HEAD 为 `58aaf342100a2cfc2988e01b257f7468bb2bbad9`，缓存上游 `upstream/6.X` 为 `2933badb9182aaecfd5a45ce09444b8ac59576bb`；前端当前 HEAD 为 `f7d116f6e2b6b61239afc86cbcb860a07530abad`，缓存上游 `upstream/6.X-Vue` 为 `0870ce17514895854ccff03600e102546d8c5046`。2026-08-24 上游 fetch 因 GitHub 443 连接超时失败，报告必须保留时效性限制。
- **相关代码/工件：** `<Path>ruoyi-vue-plus-namewta/**</Path>`、`<Path>plus-ui-namewta/**</Path>`、`<Path>docs/upstream/customization-map.md</Path>`、`<Path>{roots.state}/specdev/adr/</Path>`、`<Path>{roots.state}/specdev/context/</Path>`

## 未知项

- **可发现事实：** 两个 fork 的提交谱系、模块与文件差异、上下游重叠热点、现行业务与架构合同、测试与交付门禁、未来同步风险和升级欠账。
- **需要用户决定：** 无；本轮只读评估不改变产品行为。报告提出的升级建议需要在后续独立实现 change 中审批。
- **低影响实现细节：** 报告章节组织、统计聚合方式和风险评分说明。

## 路由

- **下一 Work：** `<Path>{roots.workflows}/specdev/C-code-review/C-code-review.md</Path>`
- **理由：** 请求核心是当前 fork 与指定 upstream 分支的不可变固定点 diff；代码审查可以冻结 SHA、提交和差异证据，再由总评工件综合业务流程、架构与演进路线。

## 外部动作

- **远程目标：** 无
- **关闭能力：** not-applicable
- **当前状态：** not-applicable
- **授权记录：** 无
- **尝试与结果：** 仅执行只读 `git fetch upstream --prune --tags`；2026-08-24 因网络连接超时失败，没有远程写入。

外部动作只投影最终完成，不替代本地状态、Ticket、Map 或 Evidence。
