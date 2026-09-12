# Plus UI 共享导航与权限运行时架构决策

永久 ADR-0012、ADR-0013 与 ADR-0015 作为只读基线。

## ADR-001: Vue 权限宿主与跨终端权限算法分层

**Status:** accepted
**Source:** LOG-004 / LOG-008
**Supersedes:** none

### Context
多个 Web Domain 页面已经使用全局 `v-hasPermi` 与 `v-hasRoles`，但当前实现位于 Admin 私有目录并直接读取 Admin Store。跨终端权限算法已经由 `platform-permission` 提供，Vue 指令不适用于移动端或小程序运行时。

### Decision
建立独立 Web Kit 权限指令包，通过注入 `AccessEvaluator` 创建和注册两个 Vue 权限指令。`platform-permission` 保持无 Vue、无 Store、跨终端；具体 App 只负责从自己的会话状态提供求值器并选择是否注册指令。

### Trade-off
继续在 Admin 内保留约二十行指令代码文件更少，但会让 Web Domain 的宿主要求指向一个 App 私有实现；把 Vue 指令放入 Platform 则会污染跨终端边界。

### Consequences
未来 Vue Web App 可直接安装同一权限宿主合同；移动端和小程序只复用权限算法。新增一个小型 Web Kit 包及其测试是接受的维护成本。

### Verification / Migration
特征测试覆盖非法绑定、任一权限、任一角色、超管和失败关闭；Admin 迁移后删除私有权限指令实现，并验证所有 Web Domain 页面仍受控。

## ADR-002: 共享菜单纯流程，导航状态继续归 App

**Status:** accepted
**Source:** LOG-005 / LOG-006 / LOG-008
**Supersedes:** none

### Context
现有 permission Store 同时包含服务端菜单树处理和 Admin 的 Pinia、Router、侧栏、顶栏、Layout 状态。前者属于同一后台 RouterVo 合同，后者是具体 App 产品导航。

### Decision
将无 Pinia、无 Router 单例的 ParentView 展平、空 children 清理、组件键装配和重复 route name 诊断保留或下沉到 `platform-app-runtime` 的纯函数公共入口。Admin Store 改名为 navigation Store，继续拥有 Admin 导航投影与 Router 注册，不建立公共 Store 或公共 Router。

### Trade-off
让每个 App 自己复制菜单纯流程会造成协议漂移；抽取完整 Store 又会迫使所有终端采用 Admin 导航形态。选择纯流程共享会保留少量 App 接线代码。

### Consequences
共享层得到确定性输入输出和独立测试，Admin Store 职责与名称一致；未来 App 可选择自己的状态容器和导航形态。

### Verification / Migration
先为纯菜单转换建立输入输出测试，再迁移 Store；验证侧栏、顶栏、默认路由、动态注入顺序和重复名称诊断行为不变。

## ADR-003: 动态页面严格使用 Manifest

**Status:** accepted
**Source:** LOG-002 / LOG-007
**Supersedes:** none

### Context
当前 `dynamicRoutes` 为空，Admin 自有页面已有显式静态路由；App 本地 `views` glob 兜底会绕过 WebDomainManifest 公开注册边界，并使未知组件键可能被隐式解析。

### Decision
删除 local dynamicRoutes 权限分支和 App 本地动态 views 兜底。服务端动态页面只能解析为特殊宿主组件或当前 App 已选择的 WebDomainManifest；未知键保持稳定、可见且失败关闭的诊断。

### Trade-off
Manifest 注册需要显式维护，但能保持编译期领域选择、包边界、诊断和可追踪性。零兼容基座不承担旧动态入口迁移层。

### Consequences
新增动态页面必须进入相应 Web Domain 或明确的 App-owned manifest；Admin 静态登录、错误、首页和个人中心路由不受影响。

### Verification / Migration
源码检索证明遗留入口无消费者；单元测试覆盖特殊组件、已选 manifest 与未知键；Playwright 验证服务端菜单到页面的完整链路。
