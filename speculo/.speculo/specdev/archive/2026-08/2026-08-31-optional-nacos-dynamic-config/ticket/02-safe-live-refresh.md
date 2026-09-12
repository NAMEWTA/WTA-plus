---
schema_version: 3
artifact: ticket
change: 2026-08-31-optional-nacos-dynamic-config
id: T-02
title: 让三组安全配置即时生效
status: done
planning_depth: deep
planning_depth_reason: 跨 common-web、common-notify、ruoyi-system 三个运行路径建立并发安全的两阶段配置切换，错误会影响登录、幂等和 OSS 签名行为。
ready: true
risk: high
blocked_by: [T-01]
contract_ids: [AC-009, AC-010, AC-011, AC-012, AC-013, AC-022]
owner: unassigned
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-nacos/src/main/java/org/dromara/common/nacos/NacosConfigParticipant.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-nacos/src/main/java/org/dromara/common/nacos/NacosConfigManager.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-nacos/src/test/java/org/dromara/common/nacos/NacosConfigManagerTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/src/main/java/org/dromara/common/notify/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/attachment/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/CaptchaController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/nacos/refresh/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/idempotency/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-nacos/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application*.yml</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: 让三组安全配置即时生效

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/02-safe-live-refresh.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 把已由源码证明按调用读取的三组配置接入 T-01 两阶段刷新合同，使行为在下一次业务调用中即时变化。
- **可观察产出：** 合法发布后验证码、通知幂等和 OSS 下载 TTL 行为无需重启即变化；删除键回本地；混合文档只即时切换清单内配置。
- **来源：** `US-003`、`AC-009` 至 `AC-013`、`AC-022`、`ADR-006`、`ADR-009`、`CODE`。
- **当前事实：** `CaptchaController` 每次请求读取 `CaptchaProperties`；`NotifyIdempotencyCoordinator` 每次操作读取幂等属性；`OssLifecycleManager`/附件快照读取 `OssLifecycleProperties.downloadTtl`。端口、数据源、条件 Bean 和普通构造期字段没有同等安全接缝。
- **Planning Depth 原因：** 同一版本跨三个业务域切换，必须在校验失败时保持全部上一行为且不能泄露配置。

## 2. 决策状态

### 已锁定决策

- 即时生效范围严格为 `captcha.*`、`notify.idempotency.*`、`oss.lifecycle.download-ttl`；新增前缀必须另补 Spec 与行为证据。
- 每组属性先绑定到新的不可变候选并完成 Bean Validation/领域约束，再以原子引用或等价非抛出 commit 切换。
- 业务调用读取当前快照，不在请求路径访问 Nacos，也不以全 ApplicationContext refresh 实现。
- 混合文档中非清单键只进入等待重启；不能因其存在拒绝合法即时键，也不能报告其已即时生效。

### 已采用的低影响假设

- 保留现有 `@ConfigurationProperties` 外部键名和默认值；内部 accessor 形态按各模块现有风格选择。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 三组刷新参与者、并发安全快照、真实业务调用与删除/非法测试 | T-01 prepare/commit SPI、现有 Captcha/Notify/OSS 行为接缝 | 端口、数据源、邮件客户端、条件 Bean 或任意其他配置即时刷新 |

## 4. 要构建什么

运维发布合法候选后，每个参与者在 prepare 阶段生成不影响当前请求的候选快照。全部 prepare 成功后统一 commit，后续验证码生成/开关判断、通知幂等判断以及 OSS presign TTL 使用新值。任意参与者校验失败时三组均保持上一快照；删除远程键则重新绑定本地基线并即时回退。

## 5. 实现契约

- **入口或接缝：** T-01 刷新参与者 SPI与 Captcha/Notify/Oss 的业务入口。
- **输入与输出：** 已展平的完整候选 effective properties -> 三个已验证快照与即时分类；业务调用 -> 新行为。
- **公共接口变化：** 仅模块内配置读取接缝和 common SPI 实现；外部 HTTP/Java API 与配置键保持不变。
- **不变量：** prepare 无副作用；commit 不抛出；同一业务调用只看到一个完整快照；清单外不热更。
- **状态或数据流：** T-01 candidate -> 三 participant prepare -> all-success commit -> next calls；any-failure -> no commit。
- **错误与失败行为：** 非法布尔、duration、范围或约束拒绝整份版本；错误只含分类和 digest。
- **兼容要求：** Nacos 关闭时使用原配置对象行为；现有调用方和测试无需感知 Nacos。
- **安全与隐私要求：** 不记录键值；并发测试不得使用真实凭据或业务数据。

## 6. 执行路线

1. 为三组属性建立合法/非法/删除和并发读取行为测试。
2. 将配置读取改为可原子替换的快照，同时保持 Nacos 关闭路径与现有构造方式。
3. 分别注册 prepare/commit 参与者并声明稳定前缀与类型约束。
4. 覆盖混合文档、任一参与者失败和删除回本地的跨参与者原子性。
5. 运行验证码、通知、OSS 原有测试与 full/core 回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅三个消费域、必要 POM、CaptchaController 和定向测试。
- **只读上下文：** T-01 的配置快照与参与者合同；不得修改其实现。
- **共享路径：** 无跨 Ticket 共同写路径。
- **T-01 接缝修正：** OSS 清单项是精确键而非前缀；允许为参与者 SPI 增加向后兼容的 `exactKeys()` 默认方法并修正分类测试，除此之外 `ruoyi-common-nacos` 保持只读。
- **保留或不动：** Nacos 核心状态机、application YAML、其他配置属性和公共业务 API。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | Spring/模块行为测试 | 发布三组合法候选并再次调用业务入口 | 无重启观察到新行为 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| 失败 | 两阶段原子测试 | 让任一组类型或约束失败 | 三组均保持上一快照且不回显值 | 同上 |
| 回归 | 既有 JUnit + bundle | 运行 Captcha/Notify/Oss 测试并 package full/core | 关闭路径与既有行为不回归 | 同上 |

- **Workspace checks：** current workspace 串行执行三个模块的定向测试、格式检查与 Maven bundle 回归。
- **E2E disposition：** not-required：本 Ticket 的可观察业务行为可由 Spring/模块集成测试稳定证明；真实推送由 T-06 统一验证。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** implementation commit、direct-parent before/result SHA、父分支包含关系和测试报告。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01 合同先合入，再逐域接入，最后一次性启用清单声明。
- **兼容窗口：** 接入期间未注册的域继续报告等待重启；最终提交前不得宣称部分清单完成。
- **监控信号：** 每次候选的即时/重启分类计数、参与者拒绝类别和 digest。
- **回滚或前向恢复：** 移除参与者注册可降级为重启生效，保持配置键和本地读取。
- **不可逆操作与批准点：** 无生产操作；实现 commit/父分支推进需授权。
- **收缩条件：** 所有业务入口不再直接缓存旧构造期值，并由扫描与行为测试证明。

## 10. 验收标准

- [x] AC-009..AC-013 的合法、删除、混合和原子拒绝行为成立。
- [x] 即时清单没有扩展到三组之外，Nacos 关闭回归通过。
- [x] 并发调用只观察完整旧/新快照，输出扫描无值泄露。
- [x] Evidence、implementation commit 与 direct-parent 结果记录完整。
