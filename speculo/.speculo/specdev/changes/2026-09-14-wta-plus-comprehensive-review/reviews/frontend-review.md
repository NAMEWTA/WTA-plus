# 前端架构、目录与用户流程审查

- 审查范围：`frontend/apps/{admin-web,home-web,sso-web}`、`frontend/packages/{domains,web-domains,platform,adapters,web-kit}`、前端依赖/构建配置、动态路由、权限认证、SSO、上传、通知、工作流、档案认证及可静态确认的 UI/UX/a11y。
- 审查方式：只读源码、POM/SQL/规则/ADR 交叉核对；未运行浏览器、E2E 或真实服务。涉及视觉断点、跨 Origin Cookie、OSS CORS、后端响应形态的结论标注 needs-runtime。
- 术语：使用 module/interface/depth/seam/adapter/leverage/locality。以下结论服务于无兼容性的升级方案，均为待用户审核的 proposed target，不代表已接受变更。
- 已确认的正向约束：App 显式组合 manifest；domain 不依赖 Vue/DOM；动态路由未知 componentKey 失败关闭；上传适配器有 AbortSignal/分片重签名；通知/OSS/Profile 后端已有 ownership 设计。以下 finding 不重复这些通过项。

## 覆盖地图与保留决定

| 轴线 | 本轮真实读到的入口 | 结果与边界 |
| --- | --- | --- |
| Admin App | application/http、services、session、sso；permission；user/navigation store；manifest；Navbar | F-01、F-04、F-07、F-09；保留显式 App composition 和当前权限 evaluator |
| Home App | router/homeManifestRegistry、user/navigation store、RegisterPage、HomeShell、SSO callback | F-02、F-05、F-06、F-09、F-13；当前 App 已真实激活，不能按旧画像删除 |
| SSO App | main、ssoApi、AuthorizePage、ssoApi.test | F-09/F-13；生产独立 Origin 是 ADR-0073 已接受契约，不能误合并进 Admin |
| platform | contracts、auth、app-runtime 及 navigationRecovery、permission、validation | 保留终端纯投影与权限失败关闭；导航恢复需要 explicit loaded/route transaction；HTTP 合同缺取消传播见 F-08 |
| adapters | axios-browser、crypto-browser、storage-browser、oss-upload-browser（client/transport/resume-store） | F-01/F-11；保留 storage 外部删除语义、OSS 分片重签名及 gateway 校验；不以包多为由合并 |
| system domain/web-domain | system service、menu、open-api、monitor、User/Role/Menu/Oss 页面、runtime/composables | F-07/F-08/F-12；OpenAPI 生成 transport 与 domain-owned 模型是有 leverage 的 seam |
| profile domain/web-domain | self/person/enterprise/application、self 三页面、管理端创建/审核组件测试、材料后端/SQL | F-02；管理端材料上传存在是重要反证；self 不得仅凭 registration 测试宣称可交付 |
| workflow domain/web-domain | definition/DesignPage/designer、ProcessActionDialog、actions/user-selection/composables/tests | F-03/F-10；DefinitionPage 自定义 http-request 也应在 F-07 票中验收成功/error/abort 回调与返回类型，当前返回 undefined 但宣称 XMLHttpRequest |
| notify domain/web-domain | transport、NoticePage、InboxPage、ConfigPage、NotificationPage、recipient selection | NoticePage 的 generation 与卸载失效、Inbox 的 reading Set/订阅清理可作本地参考；Config/monitor 加入 F-08 同类请求状态清单。未发现足够证据的通知前端安全新问题 |
| ai domain/web-domain | chatSession、AiChatPage、runtime、App probeFrame 接线 | 保留 frameUrl 同源 path 校验、probe AbortController、15s timeout、dispose；trustedCredential query 的日志泄漏由根安全审查统一建票，不另造 UI finding |
| third domain/web-domain | ThirdPage、runtime、credential save/edit/remove、domain 注册入口 | 保留凭据不回显、编辑置空、保存后清 secretJson；错误/列表竞态纳入 F-08，密钥 textarea 可见性/剪贴板策略需人工产品验收 |
| demo / web-kit | demo rich-text、file-upload/image-upload、permission、rich-text 入口 | 保留 rich-text 专用组件及业务资产 port、权限指令 fail-closed；F-11/F-12；不删除有真实消费者的现有包 |

此覆盖是风险与调用链抽样，不是逐行审计全部文件。没有浏览器运行证据的外观、焦点和响应式问题仅作为验收条件；AI/Third/Notify 没有为凑数添加低置信 finding。

## Findings

### F-01 [P1][confirmed] 浏览器 bundle 持有响应私钥，且请求加密使用 AES-ECB

- 证据：<Path>frontend/apps/admin-web/src/application/http.ts</Path> (lines 23-31)、<Path>frontend/apps/home-web/src/application/http.ts</Path> (lines 9-12) 将 `import.meta.env.VITE_APP_RSA_PRIVATE_KEY` 传给 <Path>frontend/packages/adapters/crypto-browser/src/index.ts</Path> (lines 7-16,34-43)；同文件 30、41 行以 AES-ECB 解密/加密。
- 触发与影响：启用该加密模式时，任何访问 Admin/Home 静态资源的人可读取 Vite 注入的响应私钥；该密钥不能提供对浏览器之外观察者的独立保密边界。ECB 无随机 IV、缺认证标签，泄漏块模式。此处不等同于已经获得服务器私钥，也不证明攻击者能越过后端授权。SSO token、PKCE code_verifier 请求显式 `isEncrypt:false`，普通登录请求会走该路径；跨端日志仍需与根安全审查联动。
- 反证/边界：公钥放客户端是可接受的；真正的边界问题是 privateKey 和 ECB 组合。若生产构建始终关闭 `VITE_APP_ENCRYPT`，需通过构建矩阵证实，当前源码仍允许误开。
- 修改意见：建议在本轮 proposed ADR 裁决后删除浏览器固定 privateKey 和 `decryptResponse` 责任，明确 HTTPS 是传输保密边界；若确有应用层加密需求，先建威胁模型再采用能验证消息完整性的协议，禁止把共享响应解密私钥描述为秘密。HttpOnly Cookie 是可选会话方案，需要一并裁决 CSRF、同源/SameSite、OAuth 既有 token 合同，不能由此 finding 直接切换。配置 schema/构建检查禁止误注入服务端私钥；保留 public key 仅在有明确协议消费者时。
- 删除复杂性：删掉 browser decrypt 分支、私钥 env、CryptoJS ECB 包装和两端重复配置；减少 interface 深度而增加真实安全 seam。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed. Security boundary and adjacent ADRs require proposed decision.

### F-02 [P1][confirmed] Home 个人/企业认证页没有必填材料上传，提交必然被后端材料门禁拒绝

- 证据：<Path>frontend/packages/web-domains/profile/src/self/PersonVerificationPage.vue</Path> (lines 11-30,83-122)、<Path>frontend/packages/web-domains/profile/src/self/EnterpriseVerificationPage.vue</Path> (lines 11-40,50-61) 只有身份表单及 save/submit，没有 `runtime.fileUpload`、材料树或材料 service。<Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path> (lines 3-15) 也未注入材料能力。对应 domain service 仅调用 application save/submit：<Path>frontend/packages/domains/profile/src/person/application/service.ts</Path> (lines 5-19)、<Path>frontend/packages/domains/profile/src/enterprise/application/service.ts</Path> (lines 5-23)。
- 反证：管理端档案页具备 tagged upload（组件契约测试断言 `runtime.fileUpload`、`materialNodeId`）；故不是后端不支持，而是 self web-domain 纵切片断裂。
- 跨层事实：<Path>backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/PersonApplicationService.java</Path> (lines 134-153) 调 `materials.validateRequired(...)`；企业同名 service:151-175 校验必需材料。基座 DML <Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path> (lines 1008-1036) 登记 PERSON CN_RESIDENT_ID 人像/国徽各 1 件；ENTERPRISE `*` ALWAYS 营业执照、法人身份证明各 1 件，非法人经办还需授权委托书。<Path>backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/ProfileMaterialService.java</Path> (lines 232-242) 按附属材料计数，缺失抛 `MISSING_REQUIRED_MATERIAL:<tag>`。
- 触发与影响：用户填写完整身份并点“提交认证”时，后端返回缺材料错误；当前页面没有上传入口、材料缺失逐项提示或恢复路径，核心用户旅程不可完成。
- 修改意见：把材料目录/当前材料/上传/删除/预览/必填校验作为 Profile self interface；Person/Enterprise 页面按 domain material owner 显式组合。提交前显示缺失 tag，上传完成才允许 submit；服务端错误映射到字段/tag。新增跨层合同测试（空材料、半材料、完整材料、取消上传、过期 OSS）。
- 删除复杂性：不要复制管理端页面；提取最小 self material capability，保留业务 owner 和 App adapter locality。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed. Cross-layer contract is incomplete in self web-domain.

### F-03 [P1][confirmed] 流程办理弹窗在并发打开/加载失败时可能提交旧 task

- 证据：<Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path> (lines 181-203) 每次 `open(taskId)` 清部分字段并并发请求，但没有 generation、AbortSignal 或 taskId guard；失败只写 `failure`，<Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path> (lines 258-285) 的 `complete` 仍以 `task.value` 执行。关闭后再次打开、快速切换任务或第二次请求失败时，旧 `task.value` 未清空。
- 触发与影响：用户先打开任务 A、关闭或快速打开 B，B 加载失败，弹窗仍可显示/提交 A 的审批意见、附件和下一节点；产生错误审批或错误任务操作，属于数据完整性阻塞。权限是否可被越过需以后端合同另行验证，本 finding 不声称越权。
- 修改意见：open 开始立即 `task.value=undefined`；为每次打开分配 generation 与 AbortController，所有 response 仅在 generation/taskId 匹配时写入；加载失败禁用所有 footer action；提交 payload 绑定当前 task id/version 并由后端做 optimistic check。为 complete/back/operation 加重复提交锁和取消语义。
- 验收：A→B 快速切换、B 失败、卸载中响应、重复点击提交均不得调用 A；成功路径仍只提交 B。
- dependency class: in-process; strength: Strong; certainty: confirmed. Workflow state seam.

### F-04 [P1][likely] Admin 登出/401 恢复不是原子会话清理，网络失败时残留 token 与动态路由

- 证据：<Path>frontend/apps/admin-web/src/store/modules/user.ts</Path> (lines 48-64) 在 `await identityAccessService.logout()` 成功后才清 token；<Path>frontend/apps/admin-web/src/permission.ts</Path> (lines 33-58) 恢复 catch 再调用同一 logout。<Path>frontend/apps/admin-web/src/layout/components/Navbar.vue</Path> (lines 146-160) 只有 logout 成功才 replace 登录、closeAllPage。Home <Path>frontend/apps/home-web/src/store/user.ts</Path> (lines 23) 已用 finally 清 token，是应保留的反证；但 <Path>frontend/apps/home-web/src/layout/HomeShell.vue</Path> (lines 26) 与 router catch 仍在 await logout 抛错后跳过导航。两 App 的 addRoute 没有对应 remove/reset。
- 触发与影响：Admin 的远端 logout 在服务端不可达、401 或超时时会提前抛出，当前 user store 没有本地 finally teardown，因而可能继续持有旧 token；已注册动态路由与权限投影仍在内存，用户看到旧菜单并可能继续发起请求。Home store 已有 finally 清理，是保留的反证；需要 runtime 验证具体刷新/路由行为。
- 修改意见：本地会话清理、Push/SSE 关闭和导航 reset 放在 finally/幂等 teardown；远端 logout best-effort；清空 navigation store 并移除动态 route，再 replace 登录。保留已有 requestRelogin 合并提示机制，明确取消后的本地会话合同；不要重新建立第二套恢复 wrapper。
- dependency class: ports & adapters; strength: Strong; certainty: likely (browser recovery needs-runtime). Session lifecycle.

### F-05 [P2][confirmed] Home 用空 roles 作为导航恢复哨兵，零角色用户可能无限恢复/replace

- 证据：<Path>frontend/apps/home-web/src/router/index.ts</Path> (lines 20-39) 在每次受保护导航以 `user.roles.length===0` 判断未初始化；<Path>frontend/apps/home-web/src/store/user.ts</Path> (lines 14-21) 将服务端 roles 原样赋值，允许合法空数组。Admin 特意补 `ROLE_DEFAULT`（<Path>frontend/apps/admin-web/src/store/modules/user.ts</Path> (lines 35-42)），Home 没有等价状态位。
- 触发与影响：合法但无角色的用户访问 /profile 时，每次 replace 后 guard 再次 getInfo/getMenus；可产生重复请求、菜单重建或死循环。需要浏览器/后端零角色账户确认次数。
- 后端反查：<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysPermissionServiceImpl.java</Path> (lines 37-44) 从空 HashSet 加载当前 Client 角色，普通无角色用户结果仍为空；<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysUserController.java</Path> (lines 126-137) 原样返回会话 rolePermission。没有找到“角色非空才允许 getInfo”的合同，不能依赖这个隐式不变量。
- 修改意见：使用 `identityLoaded/navigationLoaded/restoreAttempt` 独立状态，而不是业务 roles；恢复成功即使 roles 空也只执行一次；失败清理并失败关闭。
- dependency class: in-process; strength: Strong; certainty: confirmed static (loop count needs-runtime). Navigation state.

### F-06 [P2][confirmed] Home 注册验证码没有刷新/重试状态，注册入口不按 registrationEnabled 失败关闭

- 证据：<Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path> (lines 1-3) 将页面压成单行实现；只在 onMounted `prepareLogin` 一次，captcha img 无 click/refresh 控件，submit 复用旧 uuid；<Path>frontend/apps/home-web/src/router/index.ts</Path> (lines 14-17) 无条件暴露 `/register`。
- 反证：domain 已解析 `context.registerEnabled` 并在 register 时抛 `registration-disabled`（<Path>frontend/packages/domains/admin/src/index.ts</Path> (lines 413-417)），但页面没有在 prepare 后隐藏/禁用入口。
- 后端一次性验证码证据：<Path>backend/wta-admin/src/main/java/org/namewta/web/service/SysRegisterService.java</Path> (lines 115-124) 先取 captcha 再 deleteObject，连输错也会消费 uuid；因此第二次更正验证码仍失败，必须刷新挑战。
- 触发与影响：验证码过期/输错后用户无法获取新验证码，只能刷新整页；客户端显示可注册但服务端关闭，反馈晚且不可恢复。
- 修改意见：提供 refresh captcha action、提交失败按错误码自动刷新并清空 code；依据 client context 在路由/页面失败关闭注册入口；为 loading/网络失败/验证码过期提供可操作状态。拆分单行 SFC，加入 label/aria-live。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed. Auth UX.

### F-07 [P2][confirmed] 用户导入流程绕过上传 adapter，失败回调不复位 isUploading

- 证据：<Path>frontend/packages/web-domains/system/src/user/UserPage.vue</Path> (lines 481-508) 直接把 `upload.url + ?updateSupport` 和 headers 交给 Element Upload；<Path>frontend/packages/web-domains/system/src/user/UserPage.vue</Path> (lines 1010-1031) 只在 progress/success 设置状态，没有 `on-error` 或 finally reset。`globalHeaders` 在 595 行正确来自 `runtime.uploadHeaders`，这一点应保留；但 629 行仅在组件创建时读取一次，631 行仍在 web-domain 读取 env 构造 URL，请求/业务错误不经统一 adapter。
- 触发与影响：导入网络/业务失败后对话框永久禁用；凭据、重试、错误语义与 OSS/HTTP adapter 分散，无法统一取消/401/错误映射。
- 修改意见：定义 system import port（上传 URL、headers、错误/响应解析、AbortSignal），由 App 注入；失败/取消统一复位并保留可重试文件；不要在 web-domain 读取 env 或构造全局 headers。
- 验收：失败、401、取消、重复提交后按钮可恢复；错误消息不回显敏感响应。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed. Upload/import boundary.

### F-08 [P2][confirmed] System 列表请求可被过期结果覆盖，巨型 SFC 与重复 loading helper 放大修复面

- 证据：<Path>frontend/packages/web-domains/system/src/role/RolePage.vue</Path> 1272 行，<Path>frontend/packages/web-domains/system/src/user/UserPage.vue</Path> 1217 行，另有 <Path>frontend/packages/web-domains/workflow/src/definition/DefinitionPage.vue</Path> 904 行、MenuPage 732 行。多个包各自复制 `useLoading/useDialogState/useFormDialog/useSearchReset`（system/composables、workflow/composables、demo/composables、admin hooks）。
- 触发与影响：<Path>frontend/packages/web-domains/system/src/user/UserPage.vue</Path> (lines 889-894)、<Path>frontend/packages/web-domains/system/src/role/RolePage.vue</Path> (lines 859-869)、<Path>frontend/packages/web-domains/system/src/menu/MenuPage.vue</Path> (lines 547-558) 的 getList 每次请求成功后直接覆盖列表，没有 generation/取消；快速切换 Client A→B 后 A 慢响应可覆盖 B 列表。<Path>frontend/packages/web-domains/system/src/composables.ts</Path> (lines 12-24) 的布尔 withLoading 在任一并行请求结束即关闭。角色编辑/菜单操作若随后针对旧行进行，会给用户造成 Client/数据不一致。审查准则将 1k lines 视为拆分压力，但真正 finding 是可证明的 async owner 缺失。
- 修改意见：按领域职责拆为 query/table/form/permission/upload composables，保留页面编排；只提取有真实消费者的纯 platform/web-kit state primitive，删除三份 identity wrapper；为每个 async owner 加 generation/AbortSignal。禁止为凑行数创建无语义组件。
- 公共合同补充：<Path>frontend/packages/platform/contracts/src/index.ts</Path> (lines 21-29) 的 HttpRequest 目前没有 signal，domain 无法把取消交给 axios-browser；先增加终端中立的 cancellation seam 或与现有 AbortSignal 方案一致的明确合同，避免每页加一个不下传的假取消参数。Generation 防止旧结果写入，取消释放资源，两者不能互相替代。
- 验收：代表页面请求成功/空/失败/取消/快速切换/卸载覆盖；architecture check 保持 domain->web-domain->App 方向。
- dependency class: in-process; strength: Strong; certainty: confirmed static (A/B timing needs-runtime). Module depth and request state.

### F-09 [P2][confirmed] SSO 丢失部署子路径与 returnTo，callback/authorize 失败缺可恢复交互

- 证据：<Path>frontend/apps/admin-web/src/views/sso-callback.vue</Path> (lines 14-22)、<Path>frontend/apps/home-web/src/views/SsoCallbackPage.vue</Path> (lines 14-22) catch 后直接 replace('/login')，成功固定跳 `/` 或 `/profile`，不恢复原始目标。两 App <Path>frontend/apps/admin-web/src/application/sso.ts</Path> (lines 45-46)、<Path>frontend/apps/home-web/src/application/sso.ts</Path> (lines 45-46) 固定 `${window.location.origin}/sso/callback`，忽略两 App Vite/router 支持的 VITE_APP_CONTEXT_PATH。<Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path> (lines 49-58) 登录成功先隐藏表单，若后续授权失败只写 status，页面没有重试或返回动作。
- 触发与影响：部署在 `/admin/` 或 `/home/` 时 callback 被送到 origin 根路径，偏离应用历史路由和注册 callback；从深链接进入 SSO 成功后目标丢失。授权临时失败后用户可能停在无操作状态。后端负责 redirect_uri 白名单/PKCE，不把前端 parse 函数缺校验臆断为开放重定向漏洞。
- 修改意见：从 origin + 规范化 App base 生成并验证 callback；pending SSO state 记录安全站内 returnTo、创建时间和目标 Client；callback 使用服务端结构化错误码并提供重新发起按钮，authorize 失败给重试/换账号。日志层禁止记录 code_verifier、access_token、authorization code 与会话值明文；Admin/Home application/sso.ts:17-27 是真实 token POST 消费路径，可作为根日志票的验证 fixture。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed UX (cross-Origin/subpath needs-runtime). SSO protocol seam.

### F-10 [P2][confirmed] Warm-Flow iframe 接收任意 origin/source 的 close 消息

- 证据：<Path>frontend/packages/web-domains/workflow/src/definition/DesignPage.vue</Path> (lines 17-28) 把任意 `window.message` 的 data 交给 controller；<Path>frontend/packages/web-domains/workflow/src/designer.ts</Path> (lines 9-17) 只判断 `data.method === 'close'`，无 event.origin、event.source、nonce 或 iframe WindowProxy 比对。
- 触发与影响：同页面任意 iframe/扩展/恶意脚本可发送 `{method:'close'}`，关闭当前流程设计标签；若未来加入保存/发布消息，边界会升级为业务操作注入。
- 修改意见：runtime.designUrl 返回允许 origin/nonce；记录 iframe ref；仅接受 `event.source===iframe.contentWindow`、origin 精确匹配、schema/nonce 匹配的消息；未知消息静默丢弃并可一次性诊断。设计器消息测试覆盖伪造 source/origin。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed. Browser security seam.

### F-11 [P2][confirmed] OSS 上传的本地预览 URL 没有释放 owner，长会话可持续保留 File

- 证据：<Path>frontend/packages/web-kit/file-upload/src/FileUpload.vue</Path> (lines 131-138)、<Path>frontend/packages/web-kit/file-upload/src/ImageUpload.vue</Path> (lines 134-143) 删除 UI 行时直接调用 `client.remove(ossId)`；<Path>frontend/packages/adapters/oss-upload-browser/src/client.ts</Path> (lines 76-82) 下载 URL 不可用时使用 `URL.createObjectURL(file)`，未保存/撤销 URL。
- 触发与影响：上传完成但下载 URL 请求失败时，每次 fallback 新建 object URL；FileUpload/ImageUpload 没有 revoke 或归属记录，replace/remove/unmount 均不能释放它，SPA 长会话上传大文件会保留对应 Blob。内存量需浏览器测量；创建/未释放链静态确认。
- 修改意见：adapter 不隐式创建无人接管的 object URL；预览 component 获得 File 后创建并维护 URL，在替换/移除/unmount 后 revoke，或 UploadResult 返回明确 dispose 责任。业务对象删除仍遵循 ADR-0010：现有调用者多为新附件/OSS 管理，未证实“有引用对象删除”在当前业务编辑中可达，因此不把 detach/delete 风险计为 confirmed bug；若新 self material 页面复用该组件，要先由业务 Owner 明确移除引用语义。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed (leak scale needs-runtime). Resource lifecycle.

### F-12 [P3][confirmed] 前端 strict 类型约束被关闭，边界 cast/any 形成长期 contract 漂移

- 证据：<Path>frontend/tsconfig.json</Path> (lines 10-24) 开启 strict 却关闭 `noImplicitAny`、`strictFunctionTypes`、`strictNullChecks`；<Path>frontend/apps/admin-web/vite/plugins/index.ts</Path> (lines 11-12)、<Path>frontend/apps/admin-web/vite/plugins/check-transition.ts</Path> (lines 16-107) 大量 `any`；<Path>frontend/apps/home-web/src/store/user.ts</Path> (lines 17-21) 将 unknown user 强 cast 为 UserVO；<Path>frontend/apps/home-web/src/store/navigation.ts</Path> (lines 18-22) 将菜单强 cast。
- 修改意见：分期 Ratchet：先在 platform/domain/public adapter 边界开启 strictNullChecks/noImplicitAny，新增 parser 类型守卫；再逐包收紧。禁止通过 `as unknown as` 穿透动态菜单、用户、OSS 和 SSO 合同。存量例外需到期清单，不全仓无关重写。
- dependency class: in-process; strength: Worth exploring; certainty: confirmed. Type boundary ratchet.

### F-13 [P3][confirmed] Home 登录缺所需主题 token，SSO 动态错误未播报，注册页压成单行 SFC

- 证据：Identity 登录页面依赖 `--client-*` token（<Path>frontend/packages/web-domains/admin/src/auth/LoginPage.vue</Path> (lines 119-190)），Home 的 main/App/Shell 与该 web-domain 未声明这些属性；<Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path> (lines 1-3) 将 template/script/style 分别压成单行，妨碍逐项错误状态与合同审查。App 自有品牌可以不同，这不构成统一品牌的要求。
- 静态 UX 结论：Home 已有 nav、aside aria-label，SSO 使用包裹 input 的 label，均应保留，不能误判为无可访问名称。SSO <Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path> (lines 7) 的动态 status 没有 live region；Home login 的 `--client-*` 自定义属性没有在 Home main/App/Shell 或该 web-domain 声明，依赖这些 token 的 border/background/color 会回退。视觉对比、键盘焦点和真实移动端断点需浏览器确认。
- 修改意见：为 Home 注入登录页实际需要的主题 token 或在组件提供明确 fallback；SSO status 采用适当的 `aria-live`/`role=alert` 并将重试动作纳入键盘顺序；格式化注册页为可审阅 SFC。仅在实际重复交互证实后提取表单 primitive。视觉对比/焦点是否不合格必须实测，不能从静态差异臆断。
- dependency class: in-process; strength: Worth exploring; certainty: confirmed static (visual/focus needs-runtime). UI accessibility.

## 建议 ticket 分组

- T-SEC-FRONTEND：F-01、F-09、F-10（浏览器密钥/SSO/iframe 安全；先冻结 public contract，再更新 proposed ADR）。
- T-PROFILE-SELF-VERTICAL-SLICE：F-02（材料目录、上传、引用、认证提交与 E2E）。
- T-WORKFLOW-ACTION-INTEGRITY：F-03（task generation、取消、版本绑定、重复提交）。
- T-SESSION-NAVIGATION-LIFECYCLE：F-04、F-05（本地 teardown、动态 route reset、独立 loaded 状态）。
- T-AUTH-REGISTRATION-UX：F-06、F-09（验证码 refresh、注册开关、SSO 可恢复错误）。
- T-UPLOAD-ADAPTER-BOUNDARY：F-07、F-11（导入 port、OSS detach/delete、object URL 资源）。
- T-FRONTEND-MODULE-RATCHET：F-08、F-12、F-13（SFC 拆分、重复 helper、类型收紧、a11y/design tokens）。

## 删除复杂性与实现交接矩阵

| Finding | 应删除/替换 | 必须保留 | 最小可执行验收 |
| --- | --- | --- | --- |
| F-01 | browser privateKey、ECB 分支与误导性安全表述（待 ADR） | TLS、服务端最终鉴权、Client 隔离、OpenAPI HMAC 相邻合同 | dev/prod bundle 不含服务端私钥；token/业务请求授权不退化；必要加密协议篡改失败关闭 |
| F-02 | 只有身份字段却声称可提交的断裂流程 | 材料 tag/requirement、OSS owner 事务、版本校验 | 新普通用户上传两份身份证或企业必填材料后完成提交；缺失 tag 明确阻止；非法人经办额外授权委托书 |
| F-03 | 上次 task 遗留状态、无归属异步响应、读取可变 task 的提交闭包 | 服务端任务参与人权限和动作语义 | A/B 交错返回、B 拒绝、关闭后重开、重复提交均只可能处理当前已加载 task |
| F-04/F-05 | roles 充当 initialized、logout 成功才清理、残留动态路由 | Home 已有 finally、requestRelogin 单次提示、manifest-only 注册 | logout 超时/401仍清 token；A退出B登录时A独有路由不存在；零角色只恢复一次并显示无授权入口 |
| F-06 | 固定一次 captcha、注册已关闭仍呈现有效表单 | 后端验证码一次性消费、密码策略与 registerEnabled | 首次输错→刷新→成功，失败不清用户名/密码以外的必要草稿；registerEnabled=false 无提交动作 |
| F-07 | 原生上传旁路状态、snapshotted headers、声明 XHR 却返回 undefined | 后端导入验证、操作权限、安全结果摘要 | 用户导入/流程导入成功、业务错误、HTTP500、401、abort 每条路径进入终态并可重试 |
| F-08 | 各页重复的旧响应覆盖/布尔 loading、无语义拆分 | 领域 ownership、App 私有机制、真实有用的资源 slice | 手动控制 promise A慢/B快，B列表/Client不被A覆盖；unmount 后零状态写入 |
| F-09 | origin根 callback、固定首页跳转、失败死路 | 后端 exact redirect/PKCE S256/state、独立 SSO Origin | /admin/和/home/子路径；深链接query/hash；state错/过期/授权暂时失败可安全重试 |
| F-10 | 不区分来源的 data-only message interface | iframe 正常 close action、onBeforeUnmount 清理 | 外域/同域错误 iframe/source/null origin 消息均零导航；真实 designer close 一次 |
| F-11 | 无 owner 的 createObjectURL fallback | OSS 直接字节交换、短期 URL、临时对象回收、业务引用事务 | 故意让 downloadUrl 失败；替换/删除/卸载后 spy revokeObjectURL 每 URL 恰一次；长会话 heap 回落 |
| F-12 | 新边界 any/unknown 双 cast、无期限 strict 例外 | 生成 transport 不手改、纯 domain model、类型安全 parser | 以真实 nullable/错误 transport fixture证明拒绝；受影响包 strict 开启且 public consumer编译通过 |
| F-13 | 缺失 token 依赖、静默 status、单行 SFC | App 独立品牌、已有 label/nav landmark | Home login computed style有明确边框/背景；屏幕阅读器播报SSO错误；Tab顺序/重试/移动视口人工验收 |

矩阵中的候选改名或搬文件只有在删掉实际分支/重复状态后才成立。F-12/F-13 的具体共享 primitive 仍为 Worth exploring；不得为了统一目录创建新的 shallow module。

## 建议 spec/ADR 与验收顺序

1. 先写 proposed ADR：浏览器不持有私钥；TLS/HttpOnly 或 AEAD server boundary；SSO/iframe origin 与日志脱敏。
2. 冻结 Profile material self 合同和 OSS owner 事务，再实现 F-02。
3. 冻结 session/navigation teardown 与 workflow task version contract，再实现 F-03/F-04/F-05。
4. 为 upload/import 分离业务引用与对象删除，补失败/取消/过期测试。
5. 最后按 Ratchet 拆大 SFC、删重复 composables、逐包收紧 TypeScript 与 a11y/design token，并运行 architecture/lint/typecheck/unit/build/E2E。

## 未验证项

- 未运行 pnpm/Vitest/Playwright，未连接浏览器、Redis/MySQL/MinIO、SSO 独立 Origin 或 Warm-Flow iframe；上述 needs-runtime 条目必须在真实环境验收。
- 未修改任何代码、依赖、生成文件或构建输出。
- 已执行只读证据命令：`rg --files`、`rg -n`、UTF-8 Get-Content、Python pathlib 统计 SFC 行数与 Path 标签存在性，成功命令 exit 0；若候选文件路径不存在，先用 rg 定位真实路径后再审阅。一次报告文本替换 PowerShell 解析失败、未产生写入，已通过 apply_patch 完成修订。最终 Path 存在性检查通过。没有把静态阅读报告成编译、单测或 UI 通过。
