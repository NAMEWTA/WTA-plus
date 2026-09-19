# App 目录

## 当前状态

- `admin-web` 是管理端，`home-web` 是用户门户，`sso-web` 是独立 Origin 的认人页；三者均有真实源码、构建脚本和发布登记。
- 构建由 [frontend/package.json](../package.json) 串行运行；发布由 [apps.json](../../release-artifacts/apps.json) 检查配套。预览成功、清单登记与目标环境已部署分别记录，不互相替代。

## 目录职责

每个 App 拥有自己的入口、ClientContext、环境变量、会话命名空间、领域选择、路由装配、布局、品牌、主题、静态资源和部署配置。

App 可从 `packages/**` 的公开入口组合所需能力，但不得导入其他 App、深层导入包内部，也不得重新拥有可复用领域规则。

相较上游单 App 结构，这里只保存“这个终端如何组装和交付”，不保存可复用的后端 API、领域类型或管理页面。Admin/Home 按需复用 domain/web-domain/platform，保持 Client、会话和动态菜单隔离；SSO 使用专属页面与 host-only Cookie，不取得业务 App 的菜单或会话。

## 激活新终端

必须先通过独立规格明确产品范围、Client、安全合同、所选领域、技术栈、构建和部署方式，再创建真实包与源码。占位阶段不得添加空 `package.json` 或虚假构建脚本。
