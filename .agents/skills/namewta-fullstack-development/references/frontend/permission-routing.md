# 动态菜单、路由与权限

## 正式恢复链路

```text
router guard / restoreProtectedNavigation
  -> getInfo 恢复 identity
  -> navigation Store 获取当前 Client 的服务端菜单
  -> @namewta/platform-app-runtime 生成导航投影
  -> Admin 所选 web-domain manifest 解析页面组件
  -> Vue Router addRoute
  -> replace 原目标
```

该顺序是动态路由合同，实施时不得跳过菜单投影或 manifest 解析。`addRoute` 只接受当前 App 已显式注册的组件；身份、Client 菜单、投影、组件解析或注册任一步失败时必须失败关闭，不得通过 catch-all、页面点击时注册或跨 App 组件补偿继续导航。

后端菜单已经按 Client 和服务端权限裁剪。前端只组合当前 App 已选择的 domain/web-domain，不重新授权或补偿跨 Client 菜单。身份、菜单、投影或注册任一步失败时，不得继续替换到未注册目标。

## 源码地图

- 导航恢复守卫：`apps/admin-web/src/permission.ts`
- 认证与菜单服务：`apps/admin-web/src/application/services.ts`
- App 导航状态：`apps/admin-web/src/store/modules/navigation.ts`
- 菜单纯投影与重复名称诊断：`packages/platform/app-runtime`
- Admin manifest 组合：`apps/admin-web/src/router/adminManifestRegistry.ts`
- 缺失组件与重复名称呈现：`apps/admin-web/src/router/manifestDiagnostic.ts`
- 终端无关权限语义：`packages/platform/permission`
- Vue 权限指令宿主：`packages/web-kit/permission`
- Admin evaluator/provider：`apps/admin-web/src/application/access.ts`、`apps/admin-web/src/directive/index.ts`

## 所有权

- Platform App Runtime 只处理不可变菜单投影、特殊组件解析接缝和结构化诊断，不依赖 Vue、DOM、Store、Router 或 UI。
- Web Kit Permission 只注册 `v-hasPermi`、`v-hasRoles` 并调用注入的 evaluator provider，不读取 App Store。
- Admin 拥有 manifest 选择、navigation Store、Router 注册、诊断呈现和会话 evaluator 装配。
- web-domain manifest 拥有领域页面 registration；未知或未选择的组件键失败关闭。
- web-domain registration 创建时必须先校验注入 runtime；不能把缺失 runtime 延迟到页面点击后才暴露，也不能在模块加载时自动注册自身。
- 后端仍是最终授权者，前端路由和按钮权限只控制可见性与交互。

非空权限/角色数组之外的指令绑定直接报错；权限或角色不匹配时移除元素。evaluator provider 在指令执行时读取当前会话，不缓存权限快照；页面需要命令式判断时也复用同一 evaluator。

## 修改检查

- `getInfo -> getRouters -> addRoute -> replace` 顺序是否保持，失败时是否停止。
- 当前 Client 的服务端菜单是否先经过 navigation Store 和 App manifest 投影，再调用 `addRoute`；是否没有 domain/web-domain 直接操作全局 Router。
- 每个菜单 `componentKey` 是否能被当前 App manifest 精确解析；未知、未选择或重复组件键是否失败关闭并保留结构化诊断。
- 菜单 `icon` 是否遵循 `local-name`、`tabler:name` 或显式外部 `prefix:name` 协议；空值、`#` 和未知值是否有离线 fallback。
- 新组件键是否由所属 manifest 公开并由 App 显式选择。
- 新资源是否先产出局部 registration/permission contribution，再由包级 manifest 显式汇总；组件键是否保持后端菜单合同而非机械跟随文件移动。
- navigation Store 是否只维护导航投影，不扫描本地页面或复制共享算法。
- 权限指令和命令式判断是否使用同一实时 evaluator。
- Platform 是否保持无 Vue/DOM，Web Kit 是否保持无 App Store/Router 单例。
- 涉及认证、菜单或权限时是否运行对应 Vitest 与 Playwright。
