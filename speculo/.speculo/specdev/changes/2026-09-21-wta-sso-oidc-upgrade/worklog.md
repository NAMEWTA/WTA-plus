# WTA SSO OIDC 升级工作记录

## Goal

通过 G 工作流创建独立 change，完整核对用户参考方案、当前实现和协议标准，持久化前后端一致、复用 sys_* 账户的 SSO 改造设计。

## Completion criteria

- 原参考全文有可定位来源及完整性记录，并逐项评审。
- 后端、前端、账户管理、协议、安全、兼容、发布和验收均有当前源码依据及目标合同。
- 四份 G 权威工件一致，全部高影响决策有来源；未决内容不伪装 accepted。
- 执行 grill 校验并回读真实源；用户确认共识后才完成 G 并交接。

## Current status

2026-09-21：当前源码评审和完整方案草案已落盘，G 保留可恢复阻塞状态。外部正文读取受阻，已请求用户粘贴完整内容，尚未收到。后端、前端和协议内核研究已完成只读审查；文档复核的三项修正已纳入。已有未提交改动保留，已有其他 change 未接管。

## Decisions

内外统一 OIDC、允许自定义 UI、复用 sys_*、系统用户可管理、覆盖完整前后端是用户已确定的要求。凭据边界等高影响具体方案需要结合参考全文确认。

## Files changed

新增本 change 的来源快照、设计树、LOG/CONTEXT/ADR、源码评审、完整推荐方案、30 项验收矩阵、研究及验证证据和入口说明；仅向全局状态索引增量登记本 change。产品源码、永久 ADR/context 和已有 change 均未修改。

## Remaining work

1. 取得并保存用户参考全文，形成原方案逐项保留/修正/拒绝/待定对照。
2. 对原文仍未回答的问题启动下一轮完整 frontier：D-005、D-006、D-008、D-009；D-007 待 D-006 决定后进入下一轮。当前请求中的已确定要求不重复询问。
3. 用户确认共识后再次校验并交接 S；目前没有 Ready Spec/Ticket，也未自动执行产品实现。

## Verification

G 校验初次发现 source schema 缺失，修复后 exit 0、0 errors / 0 warnings。所有 Path 引用有效；来源摘要一致；42 个项目引用文件已保存摘要；7 个已有修改/删除路径保持原状；全局状态除添加本 change 外与先前一致；HEAD 未变化。最终复核详情见 <Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/evidence/verification.md</Path>。

当前未修改、构建或验收产品代码，不宣称目标 SSO 已交付。G 结构校验通过不代表原方案 review 或用户共识已经完成。
