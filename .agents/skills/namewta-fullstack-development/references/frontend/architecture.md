# 架构边界

## 目录职责

| 目录 | 职责 | 不负责 |
|---|---|---|
| `apps/*` | 独立终端入口、ClientContext、布局、品牌、路由装配、运行时适配器、部署 | 可复用业务规则 |
| `packages/domains/*` | 领域模型、服务、映射器、端口和无界面工具 | Vue 页面、DOM、具体请求库 |
| `packages/web-domains/*` | 对应领域的 Vue 页面、组件、组合式函数、语言资源和 manifest | App 布局、全局单例、终端选择 |
| `packages/platform/*` | 跨领域最小合同和组合运行时 | 业务领域模型、浏览器实现 |
| `packages/adapters/*` | HTTP、存储、加密等运行时实现 | 领域策略和页面 |
| `packages/web-kit/*` | 被多个真实消费者证明稳定的 Web 共享机制 | 领域流程、App 品牌 |
| `packages/api-contracts` | OpenAPI 生成的传输合同 | 领域模型和页面模型 |
| `tooling/*` | 架构、OpenAPI 和仓库工具 | 产品运行时代码 |

## 依赖方向

```text
apps
  -> web-domains -> domains -> platform
  -> web-kit      -> platform（仅需要时）
  -> adapters     -> platform

api-contracts -> 在 domain 边界映射后使用
tooling       -> 不进入产品运行时
```

领域之间默认不直接依赖。共享语义必须属于某个领域公开合同，或已经小到足以成为稳定的 `platform` 合同；不要建立无 owner 的公共业务包。

## 后端定位

- 当前一级 domain/web-domain 与后端 owner 一一对应：`admin`、`ai`、`demo`、`profile`、`notify`、`third`、`system`、`workflow`。`gen` 已物理删除，不得恢复为运行时领域。
- 资源目录对应实际消费的 Controller 稳定 base path，使用 kebab-case，不复制 `Sys`、`Flw` 等 Java 实现前缀。
- `/system/dict/data` 对应 `packages/domains/system/src/dict-data`；存在页面时对应 `packages/web-domains/system/src/dict-data`。
- `/monitor/loginInfo` 等监控资源归 `system/src/monitor/*`。
- `profile` 的个人、企业、材料能力必须按 Controller owner 保持可追溯；自服务或回调没有管理页面时只保留 domain 资源，不创建空 Web 目录。
- domain 资源拥有领域类型、transport 映射、服务和资源元数据；web-domain 资源拥有页面、局部状态、runtime port 和 registration。可选文件按真实职责创建，不靠空文件追求目录对称。
- 跨模块流程通过公开端口注入组合，各 domain 只拥有本模块 HTTP。

## App 显式组合

- App 的领域选择分布在三个必须同步的编译期入口：`package.json#dependencies` 声明可用包，`src/application/services.ts` 注入 adapter 并创建所选 service，`src/router/*ManifestRegistry.ts` 注入 runtime 并组合所选 domain/web-domain manifest。
- `admin-web` 当前选择 `admin`、`ai`、`demo`、`profile`、`notify`、`third`、`system`、`workflow`；不存在 `gen` service、manifest 或依赖。
- 三个入口的集合必须一致。只安装依赖、只创建 service 或只注册 manifest 都是不完整组合；未选择、缺少 domain/runtime、重复 registration 和未知组件键失败关闭。
- 未激活的未来终端尚未进入工作区，只保留 README，不参与当前 App 组合。
- `taro-request`、`taro-storage` 只保留 README，尚无包清单、依赖和实现。

未来终端必须拥有独立领域选择、ClientId、会话命名空间和组合入口，不继承 Admin 全量能力。
