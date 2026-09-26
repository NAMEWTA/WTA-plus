# T46 Dispatch02 — 404缺失证明

## revision216 — 有界缺失证明与Dispatch02

revision216：T46候选4f8c4b4a定向174例1error（Region空值误拒轮换）；窄修f6f8dd8后174例全通过零skip。独立审查发现源桶404可误归对象缺失，Dispatch02补有界桶存在核对。完整候选attempts0，16done/2cancelled/1in_progress/31ready，Goal active。

只强化新Duration版headObject：对象HEAD404之后在同一剩余总预算内HEAD Bucket；桶可达才将对象404作为OBJECT_NOT_FOUND。桶不存在、拒绝、超时或未知均保守报告PROVIDER_ERROR，迁移保留UNKNOWN且不finalize/重DELETE。SDK两请求各以剩余预算限制总/attempt timeout并有界await；普通旧OSS调用语义保持。不增加接口/schema/路径，14根写集不变。补受控404/403/timeout单元负例和owned真实缺桶负例；更新运行限制说明。cors_audit仅获上述窄修产品写锁；Lead继续独占构建/服务/提交。原失败与后续绿灯分开保留，均不代表真实集成验收。
