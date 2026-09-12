# Plus UI 多应用领域化架构决策

## ADR-001: 采用编译期模块化单体 Monorepo

**Status:** accepted
**Source:** LOG-002
**Supersedes:** none

### Context
多个前端 App 共享同一后台合同、团队和大部分领域能力，但需要独立构建、部署、Client 配置和表现层定制。运行时微前端会引入版本协议、远程加载、故障隔离和部署编排成本。

### Decision
`plus-ui-namewta` 演进为 pnpm workspace 管理的编译期模块化单体。App 通过 workspace 私有包显式组合能力并独立产出，首期不使用微前端、Module Federation 或运行时远程插件。

### Trade-off
相比微前端，编译期组合不能让多个团队任意独立发布领域模块，但换取统一类型检查、原子依赖升级、更简单的部署和更低的运行时故障面。相比多仓库复制，它要求严格维护包边界。

### Consequences
所有 App 和共享包留在同一前端子模块；依赖方向、公开 exports 和循环依赖必须自动或可重复审查。只有组织和发布独立性成为真实瓶颈时才重新评估微前端。

### Verification / Migration
迁移后每个激活 App 可独立构建；共享包不存在对具体 App 的导入；架构检查能够拒绝禁止依赖。

## ADR-002: 分离跨终端 Domains 与 Web Domains

**Status:** accepted
**Source:** LOG-003, LOG-006
**Supersedes:** none

### Context
当前业务请求、认证、UI 消息、Router、Pinia、浏览器存储和加密实现相互耦合。未来移动端与小程序无法直接复用绑定 Vue Router、Element Plus 和 DOM 的代码。

### Decision
跨终端业务能力进入 `domains`，Vue Web 表现能力进入 `web-domains`。`platform` 定义运行时无关端口，`adapters` 实现浏览器及未来终端能力；依赖方向为 App/Web Domain/Adapter 指向 Domain/Platform，Platform 不反向依赖业务领域。

### Trade-off
分层增加包数量、接口设计和装配成本，但避免每个终端复制 API、模型、认证与权限语义。把所有内容放入单一领域包更简单，却会让未来终端被 Web 依赖锁定。

### Consequences
domains 不得依赖 Vue Router、Element Plus、DOM、浏览器存储或 Axios 具体实现。web-domains 可以提供 Vue 页面、组件和 composables；运行时副作用必须经平台端口注入。

### Verification / Migration
增加依赖边界检查；对 domain 包执行静态 import 审查；用第二 App 或替代 adapter 验证领域逻辑不依赖当前 Web 壳层。

## ADR-003: 前端按能力领域建模而非镜像 Maven 模块

**Status:** accepted
**Source:** LOG-004
**Supersedes:** none

### Context
前端用户流程会跨越后端部署和 Maven 边界，例如登录同时消费 `/auth/**` 与 `/system/user/getInfo`，workflow 页面也合法复用 system 用户能力。一一镜像后端模块会拆散前端内聚流程。

### Decision
首批前端领域采用 identity-access、system-admin、workflow、ai、demo、devtools、operations。每个领域通过元数据记录一个或多个 `backendModules`，但后端模块名不决定前端包边界。

### Trade-off
该设计需要维护前后端映射，不再能只靠同名目录导航；换取更符合前端用例的内聚性，并允许一个后端模块被多个前端能力以公开合同消费。

### Consequences
跨领域关系必须通过公开入口表达；`backendModules` 用于追踪和影响分析，不形成自动依赖。新增领域必须证明独立生命周期、依赖边界或导航价值。

### Verification / Migration
每个激活领域 README 和 manifest 记录职责、非职责、允许依赖和后端来源；依赖图不得出现循环。

## ADR-004: 以 WebDomainManifest 组装动态路由

**Status:** accepted
**Source:** LOG-007
**Supersedes:** none

### Context
当前 `import.meta.glob('./../../views/**/*.vue')` 假定所有页面位于单一 `src/views`。拆成多个包后，App 必须只打包自己选择的页面，同时继续解析后端菜单中的 component key。

### Decision
每个 web-domain 通过公开入口导出 WebDomainManifest，App 在编译期显式汇总 component key、懒加载视图、消息和权限相关贡献。保留后端 component 字符串作为稳定业务键，不把它改成包物理路径。

### Trade-off
显式清单需要注册和重复键校验，新增页面比全局 glob 多一步；换取清晰的包边界、可控打包范围和可诊断的缺失映射。

### Consequences
不同 App 可注册不同 web-domain。后端继续按 Client 裁剪菜单，前端只负责把返回路由加入当前 Router；缺失或重复 component key 必须 fail visibly，并保留 keep-alive 组件名语义。

### Verification / Migration
以 demo 领域验证懒加载、重复 key、缺失 key、404、keep-alive 和动态 `addRoute`；相关 E2E 覆盖服务端裁剪后的菜单注册。

## ADR-005: 共享认证授权核心并由 App 注入 Client 上下文

**Status:** accepted
**Source:** LOG-008
**Supersedes:** none

### Context
所有 App 都需要登录、注册、社交回调、Token、401、用户信息、动态菜单和按钮权限，但登录页面和错误展示可能不同。永久 ADR 已规定 Client 是认证授权上下文而不是 tenant。

### Decision
identity-access 与 platform auth 共同承载共享认证授权流程；App 注入 ClientContext、NavigationPort 和终端 UI 适配器。保持服务端授权权威以及 `getInfo -> getRouters -> addRoute` 流程，前端不重复跨 Client 菜单过滤。

### Trade-off
集中核心状态机会约束 App 自由修改认证流程，但避免安全语义漂移。允许 App 覆盖页面和反馈，不允许绕过共享 Client、Token、权限与失败策略。

### Consequences
Client 缺失或非法时 fail-close；UI 权限只控制呈现，不能替代后端鉴权。后端规范超管键 `superadmin` 是目标合同，存量 `admin` 差异须先经运行时验证再以显式兼容迁移。

### Verification / Migration
特征测试和 E2E 覆盖登录、注册、社交回调、401、Client 严格真值、菜单注册和按钮权限；任何迁移不得削弱 `<Path>docs/upstream/customization-map.md</Path>` 的认证授权不变量。

## ADR-006: 采用兼容入口驱动的渐进迁移

**Status:** accepted
**Source:** LOG-010
**Supersedes:** none

### Context
现有单体的请求、Store、Router、页面和组件存在真实耦合；一次性目录迁移会把结构变化与行为回归混合，尤其威胁登录、Client 和动态路由。

### Decision
保留现有 `<Path>plus-ui-namewta/src/</Path>` 作为兼容入口，按“特征测试、占位、workspace、platform/adapters、demo 试点、第二 App、identity/shell、workflow、其余领域、入口迁移、后期生成能力”的波次推进。替代路径通过对应门禁前不得删除旧入口。

### Trade-off
渐进迁移会产生短期双结构和适配代码，整体历时长于一次性移动；换取每个波次可运行、可回滚、可定位问题的落点。

### Consequences
迁移 Ticket 必须是垂直切片并明确旧入口与新入口的唯一 owner。兼容层必须有删除条件，不能成为永久双写接口。

### Verification / Migration
每波至少运行适用的 lint、typecheck、unit、E2E 和生产构建；demo 和 client-web 是进入高风险认证迁移前的架构证明 Gate。

## ADR-007: 上游前端作为选择性能力来源

**Status:** accepted
**Source:** LOG-011
**Supersedes:** none

### Context
本地前端将围绕多 App、跨终端和能力领域进行较大改造，继续把目录同构和低差异作为第一目标会阻止本地架构形成。与此同时，上游仍持续提供修复和能力参考。

### Decision
产品 main 以本地架构为权威，上游镜像用于查看新增功能、修复和优化。值得吸收的变化先映射到 platform、domain、web-domain、web-kit 或 app，再在本地对应边界增量实现；不要求整包合并或文件一一对应。

### Trade-off
选择性吸收降低直接合并便利性并增加评估成本，但避免上游单 App 结构反向支配本地多 App 架构。完全放弃上游会丢失安全修复和产品灵感，因此仍保留镜像和差异审查。

### Consequences
上游同步从“代码合并优先”转为“能力评估优先”。认证、Client、权限和菜单的 NAMEWTA 定制合同仍是任何吸收动作的硬边界。

### Verification / Migration
后续更新上游映射文档，使热点能定位到新包边界；每次吸收记录上游能力、目标本地边界、保留的不变量和实际验证。
