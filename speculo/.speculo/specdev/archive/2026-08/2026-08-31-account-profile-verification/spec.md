---
schema_version: 3
artifact: spec
change: 2026-08-31-account-profile-verification
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:grill-consensus-2026-08-31
  - DESIGN-TREE:consensus-round-14
  - ADR:change-local-001-through-027
  - CODE-BASELINE:ruoyi-profile-workspace-observation-2026-08-31
---

# Spec: 账户个人与企业档案及实名认证闭环

- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/{change}/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/{change}/CONTEXT.md</Path>`

## 1. 问题与目标

### 问题陈述

当前系统账户没有一套独立、可审计的个人与企业主体档案。业务模块无法可靠判断某个账户是否已经完成个人或企业实名认证；运营人员也缺少统一的申请快照、材料、审核、版本、绑定和注销历史管理面。若直接把待审表单写入当前档案，驳回、重提或重新认证会污染仍然有效的身份事实；若把登录账户与法律主体合并，又无法表达解绑、换绑、企业负责人转移和同一账户同时拥有两类档案。

### 目标用户与场景

- **已登录申请人：** 通过任一取得相应接口权限的 Client，创建、编辑和提交自己的个人或企业实名认证申请，管理当前工作副本材料并查询本人申请和绑定状态。
- **个人重新认证申请人：** 对未绑定个人档案重新认领，或在完整身份精确匹配和人工审核后把已绑定个人档案换绑到当前账户。
- **企业认证负责人：** 在认证有效后管理自己的唯一企业绑定，并可本人解绑或通过目标实名信息与短信挑战即时转移负责人。
- **审核与运营人员：** 在 `admin-web` 的档案管理中查看个人、企业、材料、版本、绑定和流程轨迹，办理审核及执行有原因、有审计的高风险覆盖操作。
- **其他后端业务模块：** 通过公开只读服务判断指定账户的个人或企业有效认证状态并取得非敏感摘要。
- **认证供应商实现者：** 通过个人或企业验证 SPI 接入人工、支付宝、微信、微警等策略，而不改变档案核心状态机。

### 成功标准

- 一个账户可同时拥有个人和企业有效档案，但每类最多一条有效绑定；一个个人档案最多绑定一个账户；一个企业档案首期只有一个认证负责人。
- 待审、退回、撤销、驳回和重新认证内容不改写当前有效档案；每次提交冻结不可变字段与材料快照，审核通过才原子发布版本和绑定。
- 个人和企业使用两套独立 workflow；工作流不可用时查询和草稿仍可用，提交失败且不产生伪待审状态或有效绑定。
- 普通绑定探测不回显任何已有主体资料；个人换绑的手机号掩码只在登录申请人完整身份字段精确匹配后返回。
- 已挂接材料、申请快照、档案版本、绑定历史和操作审计均不物理删除；档案注销为终态，再认证创建带前后继关系的新档案。
- 管理后台交付个人档案、企业档案和材料标签三个可用管理面；申请人自助首期只交付受权限保护的 API。
- 其他业务模块无需查询 profile 内部表即可批量判断认证资格，且公共摘要不泄露证件号、材料或申请历史。

### 非目标

- 不改变登录、注册、Client、角色、菜单授予或登录域规则；认证成功只建立档案绑定。
- 不建设 applicant 端 Web、移动端或小程序页面；这些 Client 只消费本次公开的申请 API。
- 不接入全球证件目录，不支持中国大陆、香港、澳门、台湾之外签发的证件或护照。
- 不要求 OCR、人脸活体、权威工商数据源或供应商自动通过；首期最终决定仍来自人工 workflow 或管理员覆盖。
- 不提供个人或企业档案批量导出，不增加离线明文证件文件。
- 不实现企业多负责人、成员角色、组织权限或法人账户自动匹配。
- 不实现物理删除、已注销档案恢复、普通账户一键恢复解绑关系或供应商自动降级/组合。

## 2. 解决方案与外部行为

### 解决方案摘要

后端在 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/</Path>` 下建立 `ruoyi-profile` 聚合父模块、`ruoyi-profile-bom` 以及 `ruoyi-profile-person`、`ruoyi-profile-enterprise` 两个业务叶子模块。BOM 统一管理两个叶子模块的 `${revision}` 版本；两个叶子模块分别拥有个人和企业规则，并通过 `ruoyi-api` 的公开合同复用 system 与 workflow 能力，不依赖其他业务模块的 mapper、entity 或内部 service。profile 属于平台基础业务能力，在 `bundle-full` 与 `bundle-core` 均装配；core 不装配 workflow 时仍满足查询和草稿可用、提交失败关闭的合同。

领域模型分离档案主体、可编辑申请工作副本、不可变提交快照、档案版本、绑定及绑定事件、材料引用、供应商验证尝试和操作审计。个人与企业分别配置 VerificationProvider SPI，默认策略为 `manual`；一次申请固定一个服务端已启用的 provider，供应商结果只形成证据，不直接发布档案。

`ruoyi-workflow` 继续拥有流程实例与任务。档案申请 ID 作为 `businessId`，两类业务只监听各自精确 `flowCode` 的 `ProcessEvent`。`WorkflowService` 增加按 `businessId` 携带原因终止活动实例的引擎中立门面，用于管理员覆盖决定。

前端新增与后端一级领域对齐的 profile domain/web-domain，由 `admin-web` 显式组合。档案管理下提供个人档案、企业档案和材料标签菜单；个人与企业从列表进入独立详情页，详情承载当前档案、提交快照、材料、绑定历史、流程轨迹与操作审计。

### 个人实名认证与换绑

1. 取得个人 `apply` 能力的当前登录账户创建或恢复自己的可编辑申请工作副本，填写支持证件的姓名、证件号、性别、出生日期和有效期限，并选择启用的材料标签挂接材料。
2. 中国大陆居民身份证必须包含人像面和国徽面；其他首期证件按服务端证件目录与稳定必传标签 code 校验。申请人不能通过历史档案自动填充字段或复用旧材料。
3. 普通身份探测只返回最小绑定状态，不返回 profileId、主体字段、材料、旧账户或手机号。
4. 当申请人已经填写完整个人身份字段并精确命中其他账户的 active 绑定时，系统可返回旧账户当前 system 手机号掩码；任一字段不匹配时使用不披露具体字段的失败行为。
5. 申请人明确确认换绑后，提交快照冻结 `rebindIntent`、匹配档案及旧 `bindingVersion`。确认和提交均不解除旧绑定，也不要求控制旧手机号。
6. workflow 完成或管理员直接批准时重新锁定并校验身份和绑定版本：未绑定且未注销的个人档案复用原档案并发布新版本；已绑定档案原子追加旧 `unbound` 与新 `active` 事件；已注销身份建立新档案并记录 `previousProfileId`。
7. 驳回、退回、撤销、终止、提交失败或并发版本变化都不切换旧绑定。成功换绑后向旧账户当前 system 手机号发送不含身份信息的安全通知，并在站内消息能力可用时同步发送；通知失败不回滚绑定，但保留可重试结果。
8. 当前个人绑定账户可即时本人解绑，解绑后认证立即失效但档案不注销。任一正常账户再次取得绑定都必须提交完整个人认证；普通账户不能注销档案。

### 企业实名认证与负责人管理

1. 取得企业 `apply` 能力的当前账户填写企业名称、统一社会信用代码、单位性质、法定代表人、成立日期、营业期限、注册地址和经营范围；邮箱、资本、行业、网站为可选字段。
2. 营业执照和法定代表人证件材料必传；申请人不是法定代表人时授权书必传。普通统一社会信用代码探测只返回最小绑定状态，不预填企业资料或旧材料。
3. workflow 通过后，申请账户成为该企业唯一认证负责人；法定代表人只作为档案自然人字段，不自动匹配或绑定系统账户。
4. 已存在 active/suspended 负责人时，普通企业申请不能顶替。企业重新绑定只能由当前负责人专用转移、当前负责人先解绑后新申请重新认证，或管理员覆盖完成。
5. 当前 active 负责人可即时本人解绑。解绑后不存在普通自助恢复命令；原负责人或其他账户必须重新认证，管理员可直接指定合格负责人。
6. 当前 active 负责人可发起专用转移挑战：输入目标账户当前有效个人档案的准确姓名、认证证件号后四位和 system 绑定手机号。服务端精确匹配后，只向该目标账户的当前 system 手机号发送验证码。
7. 验证码为 6 位数字、5 分钟有效、60 秒发送间隔、最多错误 5 次、成功即失效，并绑定当前负责人、企业、目标账户和完整转移上下文。目标匹配失败返回模糊结果。
8. 确认转移时重新校验当前负责人、绑定版本和目标资格。目标必须是正常启用账户、拥有 active 个人认证，且没有其他 active/suspended 企业绑定；成功后原子追加旧 `unbound` 与新 `active` 事件，不进入 workflow。

### 申请、审核与管理员覆盖

1. `draft`、`back`、`cancel` 状态允许编辑工作副本并重新提交；每次提交创建递增、不可变的 submission snapshot 和材料版本。`waiting` 只读，`finish` 不可修改该申请，`invalid`、`termination` 关闭本次申请。
2. 同一账户每类、同一活动身份键每类最多一个进行中申请；数据库约束、事务锁和幂等处理共同保证并发提交不会生成两个有效结果。
3. 工作流关闭、Bean 缺失、流程未发布或启动失败时，应用仍可启动且查询和草稿可用；提交明确失败，工作副本不进入 `waiting`，也不建立绑定。
4. workflow `finish` 以当前不可变快照发布档案版本和绑定；`back`、`cancel`、`invalid`、`termination` 只更新申请生命周期，不改写当前有效档案或绑定。
5. 具备 `review` 能力的人员可以读取完成审核所需的快照与材料并从 workflow 待办办理；业务命令不要求额外分配内部 query/material 权限。
6. 具备 `override` 能力的管理员可带原因直接通过或拒绝申请、从零建立档案并可选绑定、修订核心字段、暂停/恢复/解绑绑定、指定企业负责人或注销档案。所有动作追加审计，不改写不可变来源。
7. 管理员覆盖 waiting 申请时先锁定申请、递增 `decisionVersion` 并记录 override-pending，再通过公开 WorkflowService 终止活动实例；终止失败则覆盖命令整体失败。成功后 ADMIN_OVERRIDE 为最终决定，迟到或低版本流程事件只能审计，不能覆盖结果。
8. ADMIN_CREATE 不伪造用户申请或流程历史，必须满足普通首次认证同样的核心字段和必传材料规则，并冻结独立的不可变来源快照、材料版本、操作者、原因与时间。个人可绑定到正常启用且无 active/suspended 个人绑定的账户；企业负责人还必须拥有 active 个人认证且无其他 active/suspended 企业绑定。管理员直建/指定不发送短信。
9. 管理员修改核心字段创建新的 ADMIN_OVERRIDE 档案版本并原子替换 current，旧版本变为 superseded；普通申请人修改核心字段或材料必须重新认证。
10. 档案注销逻辑终结主体并解除有效绑定。已注销档案默认不出现在列表，按状态筛选可进入全量只读详情；不能编辑、绑定或恢复。再次认证创建新档案 ID，并通过 `previousProfileId` 形成历史链。

### 材料目录与文件合同

1. profile 自有一棵受限材料目录，一级固定为个人、企业、通用；一级可直接挂标签，也可增加一层二级分类后挂标签。节点以 `nodeType`、`parentId` 表达，每个标签只有一个父分类。
2. 每份材料必须通过独立绑定关系选择恰好一个启用标签叶子，分类节点不可选择。申请人凭对应 `apply` 能力可读取适用标签，无需后台目录权限。
3. 已存在子节点、必传规则或材料引用的节点不能物理删除；已引用的普通标签只能停用，历史材料保存标签名称快照。
4. 必传标签由 DML 预置稳定 code 并标记 `systemRequired`。管理员只能修改显示名称和排序，不能改 code、停用或删除；校验按 code 而不是显示名称或节点 ID。
5. 一个当前申请工作副本或 ADMIN_CREATE 材料集合最多挂接 10 个文件；每个文件最大 10 MiB，首期只接受 JPEG、PNG、PDF。服务端根据 OSS 元数据同时校验扩展名和 MIME，并在提交时复验。
6. 草稿或退回的工作副本可以解除当前材料关系并释放名额，但保留 detached 审计且不删除 OSS。进入 submission snapshot、ADMIN_CREATE 或档案版本的材料永久不可移除。
7. profile 只拥有已挂接 OSS 引用的业务生命周期，并在业务事务中协调 `OssService` 引用；从未挂接的孤立对象继续遵守 system OSS 生命周期。任何档案、申请、绑定或标签状态变化都不得由 profile 删除底层对象。

### 外部供应商与跨模块查询

1. 个人和企业分别提供 VerificationProvider SPI 与 `providerCode` 注册表；`manual` 为默认策略。服务端只允许从已启用策略中选择，一次申请固定一个 provider，不自动组合或降级。
2. 每次显式重试新增不可变验证尝试。适配器验证供应商回调签名和时间窗；领域层以 `(providerCode, providerRequestId)` 幂等：同内容重复成功，冲突或终态后迟到结果只记安全审计。
3. 供应商结果仅作为人工审核证据，不直接修改申请终态、发布档案或切换绑定。
4. `ruoyi-api` 公开只读 `ProfileService`，提供按 `userId` 单个及批量查询个人/企业有效绑定状态与非敏感主体摘要的能力。它不返回证件号、材料或申请历史，消费者不依赖 profile 持久化模型。

### 管理后台

1. `admin-web` 在档案管理下提供个人档案、企业档案和材料标签三个动态菜单页面，由 profile WebDomainManifest 注册并由 App 显式选择。
2. 个人和企业列表支持按必要业务字段与状态查询；默认排除已注销档案，可显式筛选。首期不提供批量导出。
3. 独立详情页显示当前主体、版本与来源、申请提交快照、材料、绑定历史、流程轨迹及操作审计。有权用户看到明文结构化字段和完整材料，不叠加部门数据权限。
4. workflow `formPath` 进入相应个人或企业申请审核上下文；高风险操作只从详情页发起，要求确认并填写原因。
5. 个人和企业列表分别提供受 `override` 保护的新建档案入口，可选直接绑定合格账户。材料标签页面采用树表与父节点选择交互，并对系统必传标签显示不可变约束。

### 边界、失败与稳定错误行为

- 未登录或没有对应能力时由后端拒绝；前端隐藏或禁用只作为授权投影，不能替代服务端检查。
- 一个业务命令只校验其完整能力，不因调用内部查询、材料或工作流步骤而要求隐藏的伴随权限。
- 普通个人/企业绑定探测对存在、不存在和不匹配场景保持最小、不披露响应；只有个人完整字段精确命中 active 绑定后可返回手机号掩码。
- 身份键、账户绑定基数、进行中申请或版本围栏冲突时，命令失败且不留下部分版本、部分绑定或伪 waiting 状态。
- 不支持证件、缺少核心字段/必传标签、材料数量/大小/类型不合规、标签停用或 OSS 元数据不一致时，保存或提交在对应边界失败。
- 未知/禁用 provider、供应商鉴权失败、重复冲突或迟到回调不能改变档案、申请和绑定。
- workflow 不可用、流程未发布、启动或管理员终止失败时失败关闭；不得静默转为人工成功或遗留相反终态。
- 企业转移目标匹配失败、验证码无效/过期/超限/已消费、目标资格变化或并发绑定变化时不转移，并使用不泄露目标账户存在性的响应。
- 个人换绑通知失败不回滚已经提交的档案与绑定事务；通知结果必须可审计和重试。
- 所有写 API 使用 POST 并带安全、准确的 `@Log`，但请求体中的姓名、证件号、手机号、证件后四位、验证码、材料和供应商原始敏感值不得进入持久日志。

### 状态转换与不变量

申请：`draft/back/cancel -> waiting -> finish|back|cancel|invalid|termination`；只有可编辑状态可生成下一提交快照，旧快照永不覆盖。

档案版本：首次发布或 ADMIN_CREATE 产生 `current`；重新认证或 ADMIN_OVERRIDE 产生新 `current` 并把旧版本置为 `superseded`。档案注销是主体级终态，不把历史版本恢复为 current。

绑定：发布或转移追加 `active`；管理员停用追加 `suspended`；本人解绑、换绑、注销或管理员解绑追加 `unbound`。恢复只适用于未注销主体的 suspended 绑定并追加历史，不覆盖旧事件。

始终成立：

- 同一账户可各有一条个人和企业 active/suspended 绑定；同类不能超过一条。
- 一个未注销个人档案最多一个 active/suspended 账户绑定；一个未注销企业档案最多一个 active/suspended 负责人。
- 身份证件号或统一社会信用代码只在未注销档案和进行中申请范围保持活动唯一；已注销历史可复用身份键，但新旧档案 ID 不同。
- 待审重新认证期间旧 current 档案和 active 绑定继续有效。
- 个人换绑只在最终通过且绑定版本未变化时原子完成；企业普通认证不能顶替已有负责人。
- 申请快照、ADMIN_CREATE/ADMIN_OVERRIDE 来源、档案版本、绑定事件、材料引用和验证尝试均不可变或只追加，不提供物理删除业务入口。
- 认证不自动授予、撤销角色、菜单、登录域或 Client 权限。

## 3. 用户故事

- **US-001**：作为已登录用户，我希望通过有权限的任一 Client 申请个人实名认证，以便把个人档案绑定到当前账户。
- **US-002**：作为已登录用户，我希望申请企业实名认证，以便成为该企业唯一认证负责人。
- **US-003**：作为申请人，我希望保存草稿、退回后修改并重新提交，以便修正资料且保留每次审核证据。
- **US-004**：作为已认证用户，我希望重新认证核心字段和材料，以便新资料通过前旧认证仍然有效。
- **US-005**：作为个人申请人，我希望通过完整重新认证认领未绑定档案，以便无需恢复或删除历史档案。
- **US-006**：作为失去旧账户控制权的个人，我希望通过完整重新认证把档案换绑到新账户，以便审核通过后恢复本人认证关系。
- **US-007**：作为旧绑定账户，我希望在个人档案被换绑后收到不泄密的通知，以便发现异常变更。
- **US-008**：作为个人档案当前账户，我希望即时解绑自己，以便主动终止当前认证关系。
- **US-009**：作为企业负责人，我希望即时解绑或通过短信挑战转移负责人，以便无需新审核处理账户交接。
- **US-010**：作为企业负责人，我希望目标账户资格在确认时重新校验，以便并发状态变化不会产生无效负责人。
- **US-011**：作为审核人员，我希望在 workflow 中看到准确的提交快照和材料，以便作出可追溯决定。
- **US-012**：作为运营管理员，我希望直接审批或拒绝申请，以便在必要时绕过活动流程且不产生双重决定。
- **US-013**：作为运营管理员，我希望从零建立个人或企业档案并可选绑定账户，以便录入没有用户申请的存量身份事实。
- **US-014**：作为运营管理员，我希望修订核心字段和处置档案/绑定状态，以便纠错且不覆盖历史。
- **US-015**：作为运营管理员，我希望查询已注销档案及完整历史链，以便审计但不能恢复旧主体。
- **US-016**：作为材料目录管理员，我希望管理受限树形分类与标签，以便申请材料使用统一、稳定的归属。
- **US-017**：作为申请人或管理员，我希望材料数量、大小、类型和必传标签由服务端校验，以便审核证据完整一致。
- **US-018**：作为安全维护者，我希望普通绑定探测不回显已有档案，以便避免个人和企业资料枚举。
- **US-019**：作为认证供应商实现者，我希望通过策略 SPI 接入并提交幂等证据，以便新增供应商不改写档案核心。
- **US-020**：作为其他业务模块，我希望批量查询账户有效认证状态和非敏感摘要，以便不直接读取 profile 内部表。
- **US-021**：作为管理后台用户，我希望从个人/企业列表进入完整详情并办理操作，以便集中管理档案、材料、流程和历史。
- **US-022**：作为权限管理员，我希望按完整业务能力授权，以便角色拿到一项操作后不会因缺少内部伴随权限而卡住。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 当前账户有个人 apply 能力且无进行中个人申请 | 保存草稿并提交完整个人资料 | 草稿可重复保存；提交生成不可变 snapshot 并启动个人流程 | person application service + MVC contract |
| AC-002 | 当前账户有企业 apply 能力且无进行中企业申请 | 保存并提交完整企业资料 | 提交生成独立 snapshot 并启动企业流程，申请人成为候选负责人 | enterprise application service + MVC contract |
| AC-003 | 申请处于 back 或 cancel | 修改字段/材料后重新提交 | 产生递增新 snapshot；旧字段和材料版本保持不变 | snapshot persistence test |
| AC-004 | 账户已有有效同类档案 | 提交重新认证 | waiting 期间旧 current 版本和 active 绑定仍是资格查询结果 | publication transaction test |
| AC-005 | workflow finish 指向当前 snapshot | 处理流程事件 | 原子发布 current 版本和 active 绑定；重复事件不重复发布 | ProcessEvent integration test |
| AC-006 | 流程 back/cancel/invalid/termination | 处理流程事件 | 申请按状态更新；current 档案和绑定不变化 | ProcessEvent state matrix |
| AC-007 | workflow 关闭、Bean 缺失、流程未发布或启动异常 | 启动应用、保存草稿并提交 | 应用/查询/草稿可用；提交失败且不进入 waiting 或建立绑定 | conditional context + fault test |
| AC-008 | 同一账户或同一活动身份并发提交 | 同时提交两次 | 最多一个进行中申请成功，不产生双档案/双绑定 | DB concurrency integration test |
| AC-009 | 普通个人或企业身份探测 | 命中任意已有档案/绑定状态 | 仅返回最小绑定状态，不含主体、profileId、userId、手机号或材料 | transport schema + enumeration test |
| AC-010 | 登录用户填写完整个人身份且精确命中他人 active 绑定 | 请求换绑确认信息 | 只返回 system 手机号掩码；任一字段不匹配时不指出具体字段 | person rebind privacy matrix |
| AC-011 | 申请人确认个人换绑 | 提交申请 | snapshot 冻结 rebindIntent、目标档案和旧 bindingVersion；旧绑定继续 active | snapshot contract test |
| AC-012 | 个人换绑审核通过且版本未变化 | 发布决定 | 同一事务追加旧 unbound、新 active 和新 current 版本 | locking + transaction integration test |
| AC-013 | 个人换绑被拒/撤销/终止或 bindingVersion 已变化 | 处理终态 | 不切换绑定；并发变化得到冲突结果且不部分发布 | race/fault injection test |
| AC-014 | 个人档案 unbound 且未注销 | 任一正常账户完整重新认证通过 | 复用档案 ID、发布新版本并建立新 active 绑定 | person reclaim integration test |
| AC-015 | 个人档案已注销 | 同身份重新认证通过 | 创建新档案 ID、previousProfileId 和独立历史；旧档案保持只读 | profile succession test |
| AC-016 | 个人换绑事务已成功 | 发送旧账户通知时某渠道失败 | 绑定不回滚；通知安全、可审计、可重试且不含身份/新账户信息 | after-commit notify fault test |
| AC-017 | 当前账户拥有 active 个人绑定 | 本人解绑 | 立即追加 unbound，资格失效；无恢复命令且历史/材料保留 | person unbind service test |
| AC-018 | 企业已有 active/suspended 负责人 | 其他账户提交普通企业认证以顶替 | 命令拒绝且不泄露企业详情；原负责人不变化 | enterprise ownership matrix |
| AC-019 | 当前 active 企业负责人提供正确目标三要素 | 请求发送转移验证码 | 验证码只发送到目标账户当前 system 手机号，挑战绑定完整上下文 | transfer challenge service test |
| AC-020 | 转移挑战存在 | 验证过期、60 秒限发、5 次失败、成功消费和重放 | 每条边界符合协议，失败不泄露目标是否存在 | Redis challenge clock test |
| AC-021 | 目标账户正常、个人 active、无其他 active/suspended 企业绑定 | 输入正确验证码确认 | 原子追加旧 unbound 和目标 active，不启动 workflow | enterprise transfer transaction test |
| AC-022 | 发送挑战后双方资格或绑定版本变化 | 确认转移 | 重新校验后拒绝，不产生部分切换 | enterprise transfer concurrency test |
| AC-023 | 当前 active 企业负责人 | 本人解绑 | 即时 unbound；再次绑定必须重新认证或管理员指定 | enterprise unbind/rebind test |
| AC-024 | 管理员有 review 能力但没有额外 query/material | 办理审核 | 能完整读取审核所需快照/材料并完成任务 | authorization capability matrix |
| AC-025 | 管理员有 override 且申请 waiting | 直接审批/拒绝 | 活动实例按 businessId 终止，ADMIN_OVERRIDE 成为唯一最终决定 | workflow termination integration test |
| AC-026 | workflow 终止失败或迟到事件并发 | 执行管理员覆盖 | 终止失败则整体失败；高 decisionVersion/管理员来源不被迟到事件覆盖 | decision fence concurrency test |
| AC-027 | 管理员有对应 override 并填写原因 | 直建个人或企业档案 | 相同核心/必传规则通过后生成 ADMIN_CREATE 快照、首版和可选绑定，无伪申请/流程 | admin-create transaction test |
| AC-028 | 直建绑定目标不合格或身份键冲突 | 执行 ADMIN_CREATE | 整体拒绝，不留下档案、版本、材料引用或绑定残片 | admin-create rollback test |
| AC-029 | 管理员修订未注销档案核心字段 | 提交带原因修改 | 创建 ADMIN_OVERRIDE current 版本，旧版本 superseded 且快照不变 | version history test |
| AC-030 | 管理员暂停/恢复/解绑绑定 | 执行状态命令 | 追加状态事件并按当前状态改变资格，不覆盖或删除旧事件 | binding state test |
| AC-031 | 管理员注销档案 | 查询默认列表与注销筛选并尝试编辑/绑定 | 默认排除；筛选可全量只读查看历史链；写操作拒绝 | MVC + admin page test |
| AC-032 | 材料目录写入 | 创建一级、二级、标签及非法深度/父类型 | 只接受约定三层语义和单父标签，非法结构拒绝 | material tree service test |
| AC-033 | 标签已有引用/规则/子节点 | 删除、停用或改码 | 按生命周期拒绝；系统必传标签只能改名/排序 | material tag invariant test |
| AC-034 | 工作副本或 ADMIN_CREATE 添加材料 | 超过 10 个、单个超过 10 MiB、类型/MIME 不匹配或缺必传 code | 服务端拒绝；合法 JPEG/PNG/PDF 通过且提交时复验 | OSS metadata + material matrix test |
| AC-035 | draft/back 工作副本含材料 | 解除挂接 | 当前名额释放，detached 审计保留，OSS 不删除；不可变快照材料不可移除 | OSS reference lifecycle test |
| AC-036 | provider 已启用/禁用/未知 | 创建申请和显式重试 | 固定已启用 provider；每次重试新增 attempt；无自动降级或组合 | provider registry test |
| AC-037 | 测试 provider 回调 | 发送合法、伪造、过期、重复、冲突和迟到回调 | 适配器鉴权；同内容幂等；冲突/迟到只审计且不发布档案 | provider callback contract test |
| AC-038 | 其他模块调用 ProfileService | 单个/批量查询各种档案与绑定状态 | 只以未注销 current + active 为有效，并返回非敏感摘要 | ruoyi-api compatibility test |
| AC-039 | 用户分别只有 apply/query/material/review/manage/override 某项能力 | 调用对应正反接口矩阵 | 已授权命令闭合完成；未授权命令在事务前拒绝 | Spring Security/Sa-Token matrix |
| AC-040 | admin-web 已组合 profile manifest | 访问三菜单、列表、详情、审核及标签树 | 动态路由可解析，按钮按能力投影，详情/表单状态符合后端合同 | Vitest + Playwright admin flow |
| AC-041 | 有权/无权管理用户请求明文字段或材料 | 查询列表、详情、预览/下载 | 有权返回合同内全量内容；无权后端拒绝；不应用部门数据范围 | MVC authorization + download test |
| AC-042 | 任意驳回、解绑、换绑、注销、版本替换或标签停用 | 检查业务表和 OSS | 历史行与已挂接对象未物理删除，引用和审计仍可追溯 | SQL invariant + OSS spy test |
| AC-043 | 任意写 API 或失败路径包含敏感输入 | 检查操作、运行时、通知和异常日志 | 有调用审计但无姓名、证件号、手机号、后四位、验证码、材料或供应商敏感原文 | log capture/redaction test |
| AC-044 | 个人和企业认证完成 | 检查 system 用户角色、菜单、Client 和登录域 | 除档案绑定外均无自动变化 | system regression contract |

## 5. 范围

### IN

- 新 profile Maven 聚合、领域 BOM、个人与企业两个叶子模块及 `ruoyi-admin`/根 reactor 装配。
- 个人与企业申请、快照、档案版本、绑定、材料、验证尝试、审计和活动唯一性数据模型。
- 两套 workflow 接入、流程事件回写和 WorkflowService 终止公开合同。
- 个人认领、个人审核后换绑、个人本人解绑与旧账户安全通知。
- 企业本人解绑、负责人短信挑战转移、重新认证与管理员指定。
- 管理员审核覆盖、ADMIN_CREATE、ADMIN_OVERRIDE、绑定处置和档案注销。
- 受限树形材料目录、系统必传标签、OSS 引用生命周期和文件校验。
- 个人/企业 VerificationProvider SPI、人工默认策略、验证尝试和回调幂等框架。
- `ruoyi-api` ProfileService 单个/批量只读查询合同。
- `admin-web` 个人档案、企业档案、材料标签管理页面及显式 profile domain/web-domain 组合。
- NAMEWTA MySQL DDL/DML、菜单权限、字典/配置和必传材料种子数据。

### REUSE

- 复用 `<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/workflow/api/WorkflowService.java</Path>`、`StartProcessDTO`、`ProcessEvent` 与现有 WarmFlow businessId/formPath 模式。
- 复用 `ruoyi-api` 的 UserService、OssService、MessageService 以及 `ruoyi-common-notify` 的 NotifyClient，不读取 system 内部 mapper。
- 复用 system OSS 直传、短时下载、业务引用与未挂接对象生命周期，不在 profile 代理文件字节或保存永久 URL。
- 复用项目 `BusinessStatusEnum`、`R` 响应、Sa-Token 权限、`@Log`、`@DSTransactional`、基础实体字段和 MyBatis 查询惯例。
- 材料目录复用 system 菜单/部门的树构建和前端树表交互范式，但拥有 profile 自身的表、模型和权限。
- 前端复用现有 domain -> web-domain -> admin App 显式组合、WebDomainManifest、动态菜单和权限宿主合同。
- 参考用户指定的外部 `cde-person` 与 `cde-enterprise` 模块源码快照中的领域概念；实现必须适配当前仓库模块、工作流、OSS、权限与版本模型，不能直接复制其内部耦合。

### OUT

- **OOS-001**：申请人 Web/移动端/小程序页面；首期只提供 API，由各 Client 自行组合。
- **OOS-002**：全球证件、E.164 或 system 手机号合同改造；只沿用当前 system 手机号和短信能力。
- **OOS-003**：OCR、活体、权威实名/工商自动核验和供应商自动通过。
- **OOS-004**：企业多负责人、成员角色、组织关系和认证后自动授权。
- **OOS-005**：个人/企业批量导出、批量下载和离线明文证件文件。
- **OOS-006**：物理删除、清理已挂接材料、恢复已注销档案或覆盖不可变历史。
- **OOS-007**：按部门、Client 或登录域的数据行隔离；首期只有功能权限。
- **OOS-008**：申请端企业资料预填或任意已有主体信息回显。
- **OOS-009**：企业普通认证直接顶替现有负责人，或个人换绑使用旧手机号验证码。
- **OOS-010**：改变现有角色、菜单、Client、登录域、注册、登录或普通用户资料合同。

## 6. 已锁定实现约束

- **DEC-001**：使用 `ruoyi-profile` 聚合、`ruoyi-profile-bom`、`ruoyi-profile-person` 与 `ruoyi-profile-enterprise`，BOM 统一 `${revision}`；不新增平行业务聚合。来源：`ADR-001`。
- **DEC-002**：个人和企业审核使用现有 ruoyi-workflow、独立 flowCode、申请 ID businessId 和 ruoyi-api 门面；业务叶子不依赖 workflow 实现类型。来源：`ADR-002`、`ADR-011`。
- **DEC-003**：档案主体、工作副本、不可变提交/来源快照、档案版本、绑定和材料引用分离，待审不污染 current。来源：`ADR-003`、`ADR-015`、`ADR-022`。
- **DEC-004**：个人与企业验证供应商分别采用 providerCode 策略 SPI，默认 manual，结果只作为证据。来源：`ADR-004`、`ADR-013`。
- **DEC-005**：结构化证件字段首期明文持久化；有权管理接口返回全量字段，无权拒绝，日志和非敏感公共摘要不得泄露。来源：`ADR-005`、`ADR-006`。
- **DEC-006**：workflow 与管理员覆盖是两个可审计决定通道；管理员覆盖通过 decisionVersion 和流程终止门面防止迟到事件覆盖。来源：`ADR-007`、`ADR-017`。
- **DEC-007**：workflow 能力可选装配，提交失败关闭，查询和草稿不随 workflow 缺失失效。来源：`ADR-008`。
- **DEC-008**：企业负责人转移使用绑定完整上下文的专用短信挑战；目标资格在确认时重检。来源：`ADR-009`、`ADR-019`。
- **DEC-009**：权限按个人/企业 apply、query、material、review、manage、override 及共享 material-tag query/manage 的完整业务能力分组。来源：`ADR-012`。
- **DEC-010**：跨模块有效认证查询只通过 ruoyi-api ProfileService，公共 DTO 不含敏感字段。来源：`ADR-014`。
- **DEC-011**：解绑不提供普通恢复；个人重新认证可认领未绑定档案，企业重新绑定仍走企业认证或管理员指定。来源：`ADR-016`、`ADR-027`。
- **DEC-012**：证件目录只覆盖大陆及港澳台相关证件和这些地区签发的护照/旅行证件，稳定 code 的既有含义不可改写。来源：`ADR-018`。
- **DEC-013**：已挂接材料由 profile 作为 Business OSS Owner 永久保留；孤立对象继续归 system OSS 生命周期。来源：`ADR-020`、永久 `ADR-0005`、`ADR-0010`。
- **DEC-014**：材料目录为受限节点树和单标签绑定，必传规则引用不可停用/改码/删除的 systemRequired 稳定 code。来源：`ADR-023`、`ADR-025`。
- **DEC-015**：档案注销是不可恢复的逻辑终态；再次认证创建新档案并保留 previousProfileId 链。来源：`ADR-024`。
- **DEC-016**：普通个人与企业绑定探测均不披露主体资料；个人手机号掩码仅是完整字段精确匹配后的受限换绑提示。来源：`ADR-026`、`ADR-027`。
- **DEC-017**：前端能力按 profile domain/web-domain 与 admin App 显式组合落位，动态页面只由 manifest 注册。来源：永久 `ADR-0012`、`ADR-0014`、`ADR-0015`。
- **DEC-018**：所有 CRUD 查询使用 GET，业务变更使用 POST 且具备安全 `@Log`；新业务事务使用 `@DSTransactional`。来源：工程 `DEC-006`、`MIG-BE-DS-TX`。

## 7. 数据、接口与兼容

- **公共接口变化：** 新增当前用户个人/企业申请、材料、探测、解绑和企业转移挑战 API；新增管理端个人/企业档案、申请审核上下文、材料下载、ADMIN_CREATE/OVERRIDE、状态处置和材料目录 API；新增供应商回调适配入口。`ruoyi-api` 新增 ProfileService 及非敏感 DTO，并扩展 WorkflowService 的按 businessId 终止合同。接口继续使用现有 `R` envelope 和全局异常映射，不在本 Spec 虚构错误码。
- **权限接口：** 个人和企业分别公开 apply、query、material、review、manage、override 能力，共享材料目录公开 query/manage；操作能力隐含完成该命令所需读取。具体权限字符串遵循项目现有 `module:resource:action` 命名并在 Ticket/SQL/OpenAPI/前端合同中一次性固化。
- **数据模型与持久化：** 新增个人/企业档案主体、档案版本、申请工作副本、提交快照、来源快照、绑定与事件、材料目录/绑定、验证尝试和业务审计等 profile 自有数据。所有新项目表包含 version、create_dept、create_time、create_by、update_time、update_by、del_flag 基础字段；业务历史不以 del_flag 物理隐藏代替明确状态。
- **唯一性：** 数据库可判定的唯一键与事务锁共同保证活动身份、进行中申请和 active/suspended 绑定基数。MySQL 具体实现可以采用归一化活动键/守卫行等当前项目可支持方式，但必须证明并发不变量，不能只做应用层先查后写。
- **敏感数据：** 姓名、证件号、手机号及身份字段按已确认合同明文落库/有权展示；材料只保存 ossId 与业务归属，不保存永久下载 URL。日志、错误、通知和 ProfileService 摘要不携带敏感值。
- **兼容要求：** 现有 system 用户手机号格式、登录/RBAC、OSS、通知和 workflow 已有调用方保持兼容。WorkflowService 扩展保持引擎中立；现有实现与禁用装配均需通过编译和合同测试。
- **迁移要求：** 当前无既有 profile 生产数据迁移或 CDE 数据导入。直接修改父仓库 `<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>` 与 `<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>` 的当前完整基座，纳入 fresh-install 数据结构、字典、菜单权限、配置与 systemRequired 标签；后端仓库不得恢复 SQL 副本。
- **发布或运维影响：** 发布前必须完成新模块在 full/core 两种 bundle 的装配、数据库 DDL/DML、菜单权限、两套 workflow 定义/formPath、默认 manual provider 与必要通知/短信配置。workflow 未发布不会阻止应用启动，但会阻止提交；真实短信、OSS 和 workflow E2E 需要对应环境证据。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 所有档案、材料、申请、手机号提示、供应商回调和高风险命令必须服务端鉴权。普通探测不披露，企业目标匹配使用模糊响应；结构化字段虽明文存储，但不得进入操作日志、运行时日志、异常、通知正文、前端持久缓存或 ProfileService 摘要。材料预览/下载必须先验证 profile 权限和归属，再生成短时 OSS URL。
- **NFR-002 性能与容量：** 单文件最大 10 MiB、当前集合最多 10 个；列表使用分页或既有树列表范式，批量 ProfileService 必须一次接收有界 userId 集合并避免逐用户 N+1。除此之外不虚构响应时间、吞吐量或历史保留容量阈值。
- **NFR-003 可用性与可靠性：** 档案发布、换绑、转移、ADMIN_CREATE/OVERRIDE 和绑定处置必须事务原子；流程事件、供应商回调、短信挑战消费和重试命令必须幂等或有版本围栏。workflow 缺失、Redis/通知/OSS/供应商失败按各合同失败关闭或 after-commit best-effort，不能留下部分业务状态。
- **NFR-004 可观测性与运营：** 所有 POST 业务命令记录操作者、能力、业务 ID、结果、耗时和失败类别；高风险操作额外保存原因、来源、decisionVersion 与前后状态。验证码通知使用 `REDACT_SENSITIVE`；个人换绑通知失败有不含敏感值的重试与审计结果。
- **NFR-005 数据保留与审计：** 已挂接材料、提交快照、来源快照、档案版本、绑定事件、验证尝试和注销历史无限期逻辑保留，业务 API 不提供物理删除。底层数据库/备份法定保留或清理政策不在本 Spec 中虚构。
- **NFR-006 国际化与可访问性：** 证件类型使用稳定 code，显示文本可国际化；手机号仍遵守 system 当前合同。admin-web 页面需覆盖桌面管理工作流的加载、空态、错误、只读注销态和键盘可达基本交互，不扩展到其他终端。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| 申请、快照、发布和活动唯一性 | Java 单元 + MySQL 集成 | AC-001 至 AC-008、AC-014/AC-015 | 对照 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/TestLeaveServiceImpl.java</Path>` 的 businessId/Event 接缝；运行 profile reactor 定向测试 | test output + SQL assertions |
| 个人探测、认领与换绑 | Java service/MVC + 并发 | AC-009 至 AC-017 | 登录用户上下文、事务锁、版本故障注入与 transport 字段白名单测试 | test output + serialized payload |
| 企业解绑和负责人转移 | Java service + Redis clock + SMS spy | AC-018 至 AC-023 | 复用 UserService、NotifyClient 与 Redis 测试替身；固定时钟验证 5 分钟/60 秒/5 次/一次消费 | test output + notification spy |
| workflow 与管理员决定围栏 | API compatibility + Spring integration | AC-005 至 AC-007、AC-024 至 AC-026 | 扩展 WorkflowService 合同；使用 ProcessEvent 与同步/迟到事件测试；workflow 启用/禁用双 context | compile/test output + event trace |
| ADMIN_CREATE、版本与状态 | Java service + DB transaction | AC-027 至 AC-031 | 必传矩阵、目标资格、回滚、前后版本和注销链断言 | test output + query transcript |
| 材料树、规则和 OSS 生命周期 | Java service + OssService spy/MySQL | AC-032 至 AC-035、AC-042 | 复用 system 菜单/部门树先例与 OssService.reconcileReferences 合同；验证无删除调用 | test output + OSS spy |
| Provider 策略与回调 | Java contract tests | AC-036/AC-037 | manual/test provider、签名/时间窗 fixture、providerRequestId 并发幂等矩阵 | test output + audit rows |
| ProfileService 公共合同 | ruoyi-api compatibility + consumer test | AC-038、AC-044 | 单个/批量状态矩阵、DTO 序列化字段白名单与跨模块依赖扫描 | compile/test output + scan output |
| 权限、明文展示与日志安全 | Spring MVC/Sa-Token + log capture | AC-024、AC-039、AC-041、AC-043 | 六类能力正反矩阵、材料下载鉴权、跨部门全量查询、`@Log` payload 排除测试 | test output + captured logs |
| Admin profile domain/web-domain | Vitest + typecheck | AC-031、AC-040/AC-041 | 复用现有 domain/web-domain service、manifest 和树表测试范式；运行新增 profile 包定向 test/typecheck | test/typecheck output |
| Admin 关键管理闭环 | Playwright | AC-024 至 AC-035、AC-040/AC-041 | 登录后台验证三菜单、列表到详情、workflow formPath、直建/覆盖确认和注销只读态 | trace/screenshot + test output |
| DDL/DML 与模块装配 | Maven + MySQL fresh install + static scan | AC-008、AC-027、AC-032 至 AC-034、AC-038/AC-044 | cwd `<Path>ruoyi-vue-plus-namewta/</Path>` 运行 `./mvnw test`、full/core 适用 package；执行 NAMEWTA DDL/DML fresh fixture | build output + schema/menu assertions |
| 前端完整门禁 | workspace checks | AC-040/AC-041 | cwd `<Path>plus-ui-namewta/</Path>` 运行 `pnpm architecture:check`、`pnpm lint`、`pnpm typecheck`、`pnpm test`、`pnpm build:prod`，按风险运行定向 E2E | command output + build artifacts |

## 10. 风险、假设与未决问题

### 风险

- 明文证件字段和权限内全量可见显著扩大数据库、备份和有权账户泄露影响面。本次只能通过最小授权、后端鉴权、无导出、日志禁载荷和访问审计降低风险，不能提供静态加密的保护。
- 个人换绑允许不控制旧手机号的新账户在人工审核后替换绑定，审核质量直接成为账户接管防线。手机号掩码、不披露匹配和 bindingVersion 只能降低枚举与竞态风险，不能替代严格材料复核。
- MySQL 对“仅活动记录唯一”的约束需要精心建模；若实现只依赖应用层查询，并发提交、注销重建或换绑可能产生重复 current/active 数据。
- workflow 终止与领域事务不在同一数据库事务时存在故障窗口。decisionVersion、override-pending、幂等重试和迟到事件守卫必须共同验证，不能假设远程/跨模块调用原子。
- 已挂接材料永久保留会持续增长存储；本 Spec 明确不物理清理，但上线前仍需确认存储容量、备份与有权访问治理属于可运营状态。
- 多供应商回调协议各不相同。SPI 只定义领域证据边界；真实供应商上线前仍需为其签名、时钟、重试和敏感数据做独立验收。

### 已采用的低影响假设

- 两套 workflow 的具体 flowCode、表单组件 key 和显示名称由部署配置与菜单命名惯例决定；固定合同是彼此独立、精确监听且 formPath 能进入相应审核详情。
- 具体表名、主键名、内部包结构和共享材料目录/聚合查询实现 owner 由 Tickets 按两个已锁定叶子模块和跨模块 API 边界安排，不新增第三个业务 artifact。
- 权限字符串按现有 `module:resource:action` 形式从已锁定能力矩阵导出；字符串本身不改变能力闭合、敏感字段可见性或验收合同。
- 各地区证件的稳定 code、号码正则和必传标签矩阵由实现时依据本 Spec 的封闭范围形成测试 fixture；不会用一个“港澳台”通用类型合并不同法律证件。
- 站内通知沿用当前 MessageService 可用性；短信通过 NotifyClient。换绑通知属于提交后 best-effort，转移验证码发送失败则挑战不可进入可确认状态。
- full/core 的依赖声明位置按当前 `ruoyi-admin` POM 结构安排；两种 bundle 都必须装配 profile，且 core 的 workflow 缺失场景保持档案查询和草稿可用。

### 未决问题

无。
