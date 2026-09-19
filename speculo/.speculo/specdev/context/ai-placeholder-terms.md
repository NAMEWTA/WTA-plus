# AI Maven 占位模块术语

- Status: Current
- Graduated: 2026-09-19
- Source change: 2026-09-19-remote-issues-phone-ai
- Source: <Path>{roots.state}/specdev/archive/2026-09/2026-09-19-remote-issues-phone-ai/CONTEXT.md</Path>；<Path>{roots.state}/specdev/archive/2026-09/2026-09-19-remote-issues-phone-ai/evidence/T-03.md</Path>
- Verified product result: 723e8514cbaeba13094415ba5f1071de31b2241c

**AI 业务占位模块**：wta-ai保留Maven构建身份与业务边界，仅依赖wta-common-ai，当前不提供聊天、模型、知识库、Snail AI用户注册或其他AI服务接入。
_Avoid_: cde-ai（来源旧称，不是重命名要求）

**AI 公共占位模块**：wta-common-ai保留Maven构建身份，无生产依赖、源码、Snail AI starter、自动配置或替代模型SDK。
_Avoid_: cde-common-ai（来源旧称）
