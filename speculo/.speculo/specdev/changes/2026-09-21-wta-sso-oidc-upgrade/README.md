# WTA SSO 统一 OIDC 升级

Change：`2026-09-21-wta-sso-oidc-upgrade`。

本 change 覆盖后端 SSO、默认/自定义认证界面、Admin/Home、外部接入、sys_* 账户复用、管理面和交付验收。当前已完成源码静态评审与完整建议草案；Claude artifact 正文未取得，G 决策尚未收口。**不是最终 Spec，也不是已完成产品实现。**

## 先读内容

| 工件 | 用途 |
|---|---|
| <Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/review.md</Path> | 当前能力、12 项发现、证据和风险 |
| <Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/proposal.md</Path> | 完整前后端推荐方案、协议/UI/数据/生命周期/迁移与实施切片 |
| <Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/acceptance.md</Path> | 30 项候选验收与真实 System/外部 RP 验证要求 |

## G 权威工件

- <Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/design-tree.json</Path>：已确认需求、未决问题、推荐答案和依赖。
- <Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/LOG.md</Path>：逐项来源及决策轨迹。
- <Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/CONTEXT.md</Path>：已确认的统一认证术语。
- <Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/ADR.md</Path>：仅包含用户已明确的账户/协议架构决定。

## 来源和恢复

会话原始要求：<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/source.md</Path>。
链接读取结果：<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/evidence/source-access.json</Path>。
官方协议/框架研究：<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/evidence/protocol-research.md</Path>。
工作与验证记录：<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/worklog.md</Path>。

下一步取得参考全文并逐条映射，再处理尚未被原文回答的设计树问题。用户明确共识且 grill 校验通过后，才交接 <Path>{roots.workflows}/specdev/S-spec/S-spec.md</Path>；本次保留 G 恢复状态，不自动进入实现。
