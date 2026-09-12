# ELI5 图解索引

| 编号 | 文件 | 主题 | 简介 |
| --- | --- | --- | --- |
| 01 | 01_ruoyi-ai架构与个人档案接入.md | ruoyi-ai 架构、SnailAI Server 与个人档案接入 | 图解 AI 模块分层、真实对话数据流、SnailAI Server 职责，以及个人档案安全接入路线。 |
| 02 | 02_个人档案Tool-Skill与提示词分层.md | 个人档案 Tool、Skill 与系统提示词分层 | 图解个人档案业务、AI Tool、SnailAI Skill 和系统提示词的代码位置、安全边界与实施顺序。 |
| 03 | 03_三类代码归属与ruoyi-ai身份.md | 三类代码归属与 ruoyi-ai 身份 | 直接区分 SnailAI 现成平台能力、必须自建的个人档案能力，以及 ruoyi-ai 和 Server 各自运行在哪里。 |
| 04 | 04_动态系统提示词组合.md | 动态系统提示词组合 | 图解如何用 ruoyi-ai 聊天网关、sid、Redis 和自定义 PromptFactory，在不修改独立 Server 的情况下组合每次不同的授权业务上下文。 |
