# T-01 固定点审查与 Lead 裁决

初审 base `dea1754fb1cae6d8d32fcc1ed19a91d6610ea5cd`，head `241a96a3234ddd8a70eeb3e644ffa048d3ef2d3c`。两轴独立只读；均未运行测试、E2E 或写文件。

- 标准轴 packet `phone-ai-T01-standards-1`：request-changes，5 项如下。
- 规范轴 packet `phone-ai-T01-specification-1`：pass，逐项核对 AC-001–005、权限/开关、null 兼容、导入、事务和 IN/OUT，无 finding。

| ID | 标准轴结论 | Lead 裁决与处理 |
|---|---|---|
| S1 / P1 | MySQL URL 包含判断可绕过，清理会删除非自有数据 | 接受并修复。完整解析本机地址/端口/schema/允许参数；本轮随机 schema 与 owner token 由独占 fixture 创建，实际 database 与标记匹配后才接管，且每次清理前复核。错误 URL、错误 owner 和 sentinel 保存均验证。 |
| S2 / P2 | 当前 OpenAPI 仍将注册号码标为可选 | 确认整 change 待办，归 T-03。已确认 Ticket 写集明确禁止 T-01/T-02 修改生成物，当前源码并未对外发布；G-contract 必须从最终后端更新来源并检查 required phoneNumber。旧 openapi:check 不计为最终合同一致证据。 |
| S3 / P2 | 导入标准化后没有唯一性检查 | 不采纳为本票缺陷。Spec/ADR 明确保留各入口既有唯一性策略、不扩张导入唯一性和数据库索引；base 的导入也允许重复标准号码。收口全局唯一/增加数据库原子约束会改变已批准合同及存量数据要求。本票只确保已有检查的入口用同一标准化号码检查/持久化。既有导入重复与短信单用户查询风险仍存在，不能声称本票解决。 |
| S4 / P2 | 初始化 URL 失败会将 Spring 全局状态清空 | 接受并修复。显式 capturedSpringState 标志仅恢复已接管快照；sentinel factory/context 测试验证早期失败不改变原引用。 |
| S5 / P3 | 关闭验证码的测试无法保护消费顺序 | 接受并修复。单元路径启用验证码；另在隔离 MySQL/Redis 通过真实注册/RedisUtils 检查非法号保留 challenge、合法号消费并提交。临时错误顺序 mutation 会使验证失败，测试后恢复。 |

修复复审和最终 SHA 待 Lead 补录。标准轴结论不被改写为初审 pass；S2 必须在 T-03 闭合，S3 作为已有合同边界保留。
