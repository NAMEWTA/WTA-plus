---
artifact: architecture-review
change: 2026-09-14-wta-plus-comprehensive-review
status: draft
---

# 全面架构复核

2026-09-18基于当前工作树核对原change。完整逐票结论见复核矩阵：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>，源码细节见四份专项报告。本轮由一个执行者串行完成，仅改文档。

## 当前结构

- 前端为Admin/Home/SSO三个App；依赖方向是App → web-domain → domain → platform，App装配adapter/web-kit。platform定义端口，不反向依赖adapter。
- 后端50个POM描述符；业务按classic或layered登记，不把common基础设施强套五层。Third已按layered实现，但登记表遗漏。
- MySQL初始化只有六份完整基座；10-cde-base-ddl.sql结构、50-cde-base-dml.sql数据是当前owner。
- 发布脚本自动构建三App，但Compose/Nginx缺SSO；已有文件digest，缺完整来源约束和整套stage一致性。远程CI文件不存在。

## 结论与取舍

真实问题集中于凭据副本、认证前无界读体、SSO随机源/回调、页面异步状态、通知结果事务、企业转移排队语义和发布组合。31票保留可追溯编号，但实现目标收敛为现有owner内的最小修改。

| 范围 | 最小改造 | 不纳入的冗余设计 |
|---|---|---|
| 日志/正文/IP/防重 | 共享副本脱敏、有界原始字节、可信peer、owner比较删除 | 新安全框架、所有异常兜底继续执行、业务exactly-once |
| SSO/浏览器传输 | CSPRNG、安全Cookie、精确state/base、HTTPS硬切 | 哈希存储重构、新AEAD协议、共享父域Cookie、兼容窗口 |
| 页面/流程/上传 | 局部generation、当前taskId、finally teardown、明确预览状态 | 新taskVersion协议、所有层统一parse、通用取消/上传框架 |
| iframe | 精确origin/source及close消息形状 | 供应商nonce握手、未实现save/publish动作 |
| Notify/Profile | 短事务+fence、持久receipt、公开query核验排队结果 | Profile投影Outbox、事件总线、验证码持久副本 |
| Third/树 | 现有可过期permit、稳定额度key、同树串行校验 | 每版本新额度、通用lease/树服务、任意depth上限 |
| 发布/文档 | 同源完整版本目录、单指针、现有配置来源和准确导航 | 多容器原子热切换承诺、额外多套manifest/生成器 |
| 类型/代码组织 | 实际触及边界和真实重复职责 | 全仓strict硬切前置、按文件行数机械拆分 |

## 保留的设计

保留PKCE S256、精确redirect白名单、单次consume、Client隔离、机器HMAC、Outbox claim/fence、OSS引用保护、Workflow现有任务锁、classic/layered目录边界及第三方schema。Demo broad POM未证明冗余，B-16继续拒绝。

源码可证不等于生产复现：代理白名单影响、Cookie/TLS、WarmFlow读取授权、事件线程、Redis崩溃与真实浏览器均需运行验证。strict关闭、大文件、通用AGENTS是工程债或整理候选，不自动上升为漏洞。

## 授权与执行

用户已允许全面重构本change，无需逐票设计访谈；基座无兼容要求。具体产品实现未在本轮执行，T/P已补齐31票：29票计划Ready、T-03/T-23 blocked，Goal执行Gate关闭。唯一单人串行顺序见tickets-map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>。提交/部署和重要数据操作仍需其实际授权。

## 当前验证

已执行9条文档/分层/发布检查；通过与失败均保留原始结果。release为43项、41通过、2失败。命令、退出码及未运行项见verification：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>。本报告不宣称全仓编译、单测、浏览器或线上验收通过。
