# 前端架构、目录与用户流程审查

> 2026-09-18 已按当前工作树重新核对并修订建议。原报告中的2026-09-14命令属于历史记录；当前命令结果见 [re-review-command-results.json](re-review-command-results.json)，逐票结论见 [re-review.md](re-review.md)。本次单人串行，仅改change；无旧版兼容要求。


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
| platform | contracts、auth、app-runtime 及 navigationRecovery、permission、validation | 保留终端纯投影与权限失败关闭；导航恢复需要 explicit loaded/route transaction；HTTP目前无取消传播，是否扩展由实际资源需求决定 |
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
- 修改意见：按用户要求以HTTPS完成硬切，同步删除浏览器共享privateKey、ECB与无消费者包装/依赖。保留Sa-Token、机器HMAC和数据库加密。本问题不引入AEAD备选协议或HttpOnly会话迁移。当前仅证实可注入代码，生产bundle是否含实际私钥未构建验证。
- 删除复杂性：删掉 browser decrypt 分支、私钥 env、CryptoJS ECB 包装和两端重复配置；减少 interface 深度而增加真实安全 seam。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed. Security boundary and adjacent ADRs require proposed decision.

### F-02 [P1][confirmed] Home 新申请缺少必填材料上传，无法完成自助提交

- 证据：<Path>frontend/packages/web-domains/profile/src/self/PersonVerificationPage.vue</Path> (lines 11-30,83-122)、<Path>frontend/packages/web-domains/profile/src/self/EnterpriseVerificationPage.vue</Path> (lines 11-40,50-61) 只有身份表单及 save/submit，没有 `runtime.fileUpload`、材料树或材料 service。<Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path> (lines 3-15) 也未注入材料能力。对应 domain service 仅调用 application save/submit：<Path>frontend/packages/domains/profile/src/person/application/service.ts</Path> (lines 5-19)、<Path>frontend/packages/domains/profile/src/enterprise/application/service.ts</Path> (lines 5-23)。
- 反证：管理端档案页具备 tagged upload（组件契约测试断言 `runtime.fileUpload`、`materialNodeId`）；故不是后端不支持，而是 self web-domain 纵切片断裂。
- 跨层事实：<Path>backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/PersonApplicationService.java</Path> (lines 134-153) 调 `materials.validateRequired(...)`；企业同名 service:151-175 校验必需材料。基座 DML <Path>release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql</Path> (lines 1008-1036) 登记 PERSON CN_RESIDENT_ID 人像/国徽各 1 件；ENTERPRISE `*` ALWAYS 营业执照、法人身份证明各 1 件，非法人经办还需授权委托书。<Path>backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/ProfileMaterialService.java</Path> (lines 232-242) 按附属材料计数，缺失抛 `MISSING_REQUIRED_MATERIAL:<tag>`。
- 触发与影响：用户填写完整身份并点“提交认证”时，后端返回缺材料错误；当前页面没有上传入口、材料缺失逐项提示或恢复路径，核心用户旅程不可完成。
- 修改意见：把材料目录/当前材料/上传/删除/预览/必填校验作为 Profile self interface；Person/Enterprise 页面按 domain material owner 显式组合。提交前显示缺失 tag，上传完成才允许 submit；服务端错误映射到字段/tag。新增跨层合同测试（空材料、半材料、完整材料、取消上传、过期 OSS）。
- 删除复杂性：不要复制管理端页面；提取最小 self material capability，保留业务 owner 和 App adapter locality。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed. Cross-layer contract is incomplete in self web-domain.

### F-03 [P1][confirmed] 流程办理弹窗在并发打开/加载失败时可能提交旧 task

- 证据：<Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path> (lines 181-203) 每次 `open(taskId)` 清部分字段并并发请求，但没有 generation、AbortSignal 或 taskId guard；失败只写 `failure`，<Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path> (lines 258-285) 的 `complete` 仍以 `task.value` 执行。关闭后再次打开、快速切换任务或第二次请求失败时，旧 `task.value` 未清空。
- 触发与影响：用户先打开任务 A、关闭或快速打开 B，B 加载失败，弹窗仍可显示/提交 A 的审批意见、附件和下一节点；产生错误审批或错误任务操作，属于数据完整性阻塞。权限是否可被越过需以后端合同另行验证，本 finding 不声称越权。
- 修改意见：open清空task/节点与附属动作并递增generation；旧response/catch/finally不能更新新弹窗。提交同步加single-flight并捕获taskId/payload，await确认后复核generation，关闭/卸载使其失效。复用后端现有锁和状态校验，不新增taskVersion协议；HTTP尚无signal，不强制扩展全链取消接口。
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
- 修改意见：以独立loaded/restoring状态表达恢复，不叠加无用状态位；空roles成功恢复也只执行一次。保持getInfo→菜单→注册→replace顺序，失败清理。
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
- 修改意见：在现有runtime/HTTP上传机制上补error/abort终态、动态读取headers与业务错误映射，删除web-domain的env拼URL；如确需新增方法仅提供最小导入能力，不建立新的上传框架。
- 验收：失败、401、取消、重复提交后按钮可恢复；错误消息不回显敏感响应。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed. Upload/import boundary.

### F-08 [P2][confirmed] System 列表请求可被过期结果覆盖，巨型 SFC 与重复 loading helper 放大修复面

- 证据：<Path>frontend/packages/web-domains/system/src/role/RolePage.vue</Path> 1272 行，<Path>frontend/packages/web-domains/system/src/user/UserPage.vue</Path> 1217 行，另有 <Path>frontend/packages/web-domains/workflow/src/definition/DefinitionPage.vue</Path> 904 行、MenuPage 732 行。多个包各自复制 `useLoading/useDialogState/useFormDialog/useSearchReset`（system/composables、workflow/composables、demo/composables、admin hooks）。
- 触发与影响：UserPage.getList、RolePage.getList和MenuPage.getList都直接应用响应，没有generation；筛选A慢/B快会使旧数据覆盖新列表，布尔withLoading还可能提前结束loading。当前未证明这些页面支持不卸载的Client切换，因此不把跨Client泄漏作为已确认后果。
- 修改意见：先在实际query owner加generation保护list/error/loading，关闭/卸载使其失效。只有职责和重复语义清晰时提局部composable，不要求巨型SFC统一拆成query/table/form目录。
- 公共合同补充：HttpRequest目前无signal。generation已足够阻止过期结果写入；只有资源取消确有必要且端口实际透传时才扩展signal，不为本票提前改造全部domain/adapter。
- 验收：代表页面请求成功/空/失败/取消/快速切换/卸载覆盖；architecture check 保持 App -> web-domain -> domain -> platform 方向。
- dependency class: in-process; strength: Strong; certainty: confirmed static (A/B timing needs-runtime). Module depth and request state.

### F-09 [P2][confirmed] SSO 丢失部署子路径与 returnTo，callback/authorize 失败缺可恢复交互

- 证据：<Path>frontend/apps/admin-web/src/views/sso-callback.vue</Path> (lines 14-22)、<Path>frontend/apps/home-web/src/views/SsoCallbackPage.vue</Path> (lines 14-22) catch 后直接 replace('/login')，成功固定跳 `/` 或 `/profile`，不恢复原始目标。两 App <Path>frontend/apps/admin-web/src/application/sso.ts</Path> (lines 45-46)、<Path>frontend/apps/home-web/src/application/sso.ts</Path> (lines 45-46) 固定 `${window.location.origin}/sso/callback`，忽略两 App Vite/router 支持的 VITE_APP_CONTEXT_PATH。<Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path> (lines 49-58) 登录成功先隐藏表单，若后续授权失败只写 status，页面没有重试或返回动作。
- 触发与影响：部署在 `/admin/` 或 `/home/` 时 callback 被送到 origin 根路径，偏离应用历史路由和注册 callback；从深链接进入 SSO 成功后目标丢失。授权临时失败后用户可能停在无操作状态。后端负责 redirect_uri 白名单/PKCE，不把前端 parse 函数缺校验臆断为开放重定向漏洞。
- 修改意见：从 origin + 规范化 App base 生成并验证 callback；pending SSO state 记录安全站内 returnTo、创建时间和目标 Client；callback 使用服务端结构化错误码并提供重新发起按钮，authorize 失败给重试/换账号。日志层禁止记录 code_verifier、access_token、authorization code 与会话值明文；Admin/Home application/sso.ts:17-27 是真实 token POST 消费路径，可作为根日志票的验证 fixture。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed UX (cross-Origin/subpath needs-runtime). SSO protocol seam.

### F-10 [P2][confirmed] Warm-Flow iframe 接收任意 origin/source 的 close 消息

- 证据：<Path>frontend/packages/web-domains/workflow/src/definition/DesignPage.vue</Path> (lines 17-28) 把任意 `window.message` 的 data 交给 controller；<Path>frontend/packages/web-domains/workflow/src/designer.ts</Path> (lines 9-17) 只判断 `data.method === 'close'`，无 event.origin、event.source、nonce 或 iframe WindowProxy 比对。
- 触发与影响：同页面任意 iframe/扩展/恶意脚本可发送 `{method:'close'}`，关闭当前流程设计标签；若未来加入保存/发布消息，边界会升级为业务操作注入。
- 修改意见：在iframe owner校验event.source===iframe.contentWindow和由designUrl得到的精确origin，只接受close payload；保留现有第三方消息协议，不新增nonce握手或未来save/publish动作。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed. Browser security seam.

### F-11 [P2][confirmed] OSS 上传的本地预览 URL 没有释放 owner，长会话可持续保留 File

- 证据：<Path>frontend/packages/web-kit/file-upload/src/FileUpload.vue</Path> (lines 131-138)、<Path>frontend/packages/web-kit/file-upload/src/ImageUpload.vue</Path> (lines 134-143) 删除 UI 行时直接调用 `client.remove(ossId)`；<Path>frontend/packages/adapters/oss-upload-browser/src/client.ts</Path> (lines 76-82) 下载 URL 不可用时使用 `URL.createObjectURL(file)`，未保存/撤销 URL。
- 触发与影响：上传完成但下载 URL 请求失败时，每次 fallback 新建 object URL；FileUpload/ImageUpload 没有 revoke 或归属记录，replace/remove/unmount 均不能释放它，SPA 长会话上传大文件会保留对应 Blob。内存量需浏览器测量；创建/未释放链静态确认。
- 修改意见：优先删除adapter隐式createObjectURL fallback，上传成功但预览不可用时保留文件名并提示/重取URL，不能误报上传失败。确需本地预览才由预览组件创建并释放URL；不新增通用dispose合同。detach/delete仅为新self材料接入时的owner约束，不是已确认误删缺陷。
- dependency class: ports & adapters; strength: Strong; certainty: confirmed (leak scale needs-runtime). Resource lifecycle.

### F-12 [P3][confirmed] 前端 strict 类型约束被关闭，边界 cast/any 形成长期 contract 漂移

- 证据：<Path>frontend/tsconfig.json</Path> (lines 10-24) 开启 strict 却关闭 `noImplicitAny`、`strictFunctionTypes`、`strictNullChecks`；<Path>frontend/apps/admin-web/vite/plugins/index.ts</Path> (lines 11-12)、<Path>frontend/apps/admin-web/vite/plugins/check-transition.ts</Path> (lines 16-107) 大量 `any`；<Path>frontend/apps/home-web/src/store/user.ts</Path> (lines 17-21) 将 unknown user 强 cast 为 UserVO；<Path>frontend/apps/home-web/src/store/navigation.ts</Path> (lines 18-22) 将菜单强 cast。
- 修改意见：先运行受影响包的真实严格诊断，收紧本change已触及的公共边界。全仓三开关硬切没有工作量与必要性证据，不作为所有安全修复的验收前提。不以重复parser、双cast、ignore或移出检查消除诊断。
- dependency class: in-process; strength: Worth exploring; certainty: confirmed. Type boundary ratchet.

### F-13 [P3][confirmed] Home 登录缺所需主题 token，SSO 动态错误未播报，注册页压成单行 SFC

- 证据：Identity 登录页面依赖 `--client-*` token（<Path>frontend/packages/web-domains/admin/src/auth/LoginPage.vue</Path> (lines 119-190)），Home 的 main/App/Shell 与该 web-domain 未声明这些属性；<Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path> (lines 1-3) 将 template/script/style 分别压成单行，妨碍逐项错误状态与合同审查。App 自有品牌可以不同，这不构成统一品牌的要求。
- 静态 UX 结论：Home 已有 nav、aside aria-label，SSO 使用包裹 input 的 label，均应保留，不能误判为无可访问名称。SSO <Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path> (lines 7) 的动态 status 没有 live region；Home login 的 `--client-*` 自定义属性没有在 Home main/App/Shell 或该 web-domain 声明，依赖这些 token 的 border/background/color 会回退。视觉对比、键盘焦点和真实移动端断点需浏览器确认。
- 修改意见：为 Home 注入登录页实际需要的主题 token 或在组件提供明确 fallback；SSO status 采用适当的 `aria-live`/`role=alert` 并将重试动作纳入键盘顺序；格式化注册页为可审阅 SFC。仅在实际重复交互证实后提取表单 primitive。视觉对比/焦点是否不合格必须实测，不能从静态差异臆断。
- dependency class: in-process; strength: Worth exploring; certainty: confirmed static (visual/focus needs-runtime). UI accessibility.

## 实施与验证

修订后的逐票写集、步骤和验收以 [tickets-map](../tickets-map.md) 及对应Ticket为准，不在专项报告中重复维护另一套计划。2026-09-18复核矩阵见 [re-review](re-review.md)，本次运行记录见 [re-review-command-results.json](re-review-command-results.json)。

confirmed表示源码链成立，不代表线上事故已复现。未运行Maven/pnpm/浏览器或真实数据库、Redis、Provider；不得将计划验收写成通过。仅修改本change，产品实现未开始。
