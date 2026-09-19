# G Grounding — 2026-09-19

两名只读调查者分别覆盖手机号、AI 运行面，Lead 汇总。证据是当前工作树静态阅读；未运行产品测试、数据库查询或真实服务。本文件不替代行为 Spec 或实现 Evidence。

## 手机号

| 事实 | 证据 |
|---|---|
| RegisterBody 明确手机号可选，仅格式注解；公共格式校验对空值放行 | <Path>backend/wta-api/src/main/java/org/namewta/system/api/model/RegisterBody.java</Path>；<Path>backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/validation/ValidationUtils.java</Path> |
| Admin、Home 注册表单没有手机号；domain 注册输入与显式 HTTP 字段映射也没有手机号 | <Path>frontend/apps/admin-web/src/views/register.vue</Path>；<Path>frontend/apps/home-web/src/views/RegisterPage.vue</Path>；<Path>frontend/packages/domains/admin/src/index.ts</Path> |
| 管理端用户表单只校验格式，SysUserBo 无手机号必填或格式注解 | <Path>frontend/packages/web-domains/system/src/user/UserPage.vue</Path>；<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/bo/SysUserBo.java</Path> |
| Excel 新增/覆盖导入把行转为同一 SysUserBo 后校验，写入绕过 Controller 的手机号唯一检查 | <Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/listener/SysUserImportListener.java</Path>；<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysUserServiceImpl.java</Path> |
| 个人资料 UI 必填，但 BO 只有可空格式校验；API 省略手机号保留原值，传空字符串可以写空 | <Path>frontend/apps/admin-web/src/views/system/user/profile/userInfo.vue</Path>；<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/bo/SysUserProfileBo.java</Path>；<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysProfileController.java</Path>；<Path>backend/wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/core/mapper/LambdaCrudChainWrapper.java</Path> |
| 当前 Controller 唯一检查是全局手机号，不按 Client 分组；DB 列可空、默认空串，仅普通索引，无并发唯一保证 | <Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysUserServiceImpl.java</Path>；<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path> |
| 初始化 test/test1 手机号为空；实际环境还有多少空值未知，未查运行库 | <Path>release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql</Path> |
| 密码登录没有手机号补录门槛；socialRegister 仅绑定已存在用户，不创建新 sys_user | <Path>backend/wta-admin/src/main/java/org/namewta/web/service/impl/PasswordAuthStrategy.java</Path>；<Path>backend/wta-admin/src/main/java/org/namewta/web/service/SysLoginService.java</Path> |

可复用验证接缝：<Path>backend/wta-admin/src/test/java/org/namewta/web/service/SysRegisterServiceRegistrationUnitTest.java</Path>、<Path>backend/wta-admin/src/test/java/org/namewta/test/password/write/PasswordImportUnitTest.java</Path>、<Path>frontend/packages/domains/admin/src/index.test.ts</Path>、<Path>frontend/e2e/recoverable-registration.spec.ts</Path>、<Path>frontend/e2e/browser-https-transport.spec.ts</Path>。导入旧测试注入空 validator，不能充当真实 Bean Validation 证据。

产品未知：必填切换的存量兼容政策，以及更新 API 是否保留省略字段的语义。邮箱、短信登录与新第三方自动注册不从本 issue 派生。

## AI 移除与新运行面

| 事实 | 证据 |
|---|---|
| Snail AI 与 SnailJob 分别管理依赖；common-ai 使用 3 个 vendor starter 和自动配置 | <Path>backend/pom.xml</Path>；<Path>backend/wta-common/wta-common-ai/pom.xml</Path>；<Path>backend/wta-common/wta-common-ai/src/main/java/org/namewta/common/ai/config/SnailAiConfig.java</Path> |
| AI 业务模块仅现有 SnailAI 用户注册桥；暴露 vendor VO | <Path>backend/wta-modules/wta-ai/src/main/java/org/namewta/ai/controller/SnailAiController.java</Path> |
| Snail AI 独立 server 由 extend 聚合，vendor starter 提供服务与静态管理面 | <Path>backend/wta-extend/pom.xml</Path>；<Path>backend/wta-extend/wta-snailai-server/pom.xml</Path>；<Path>backend/wta-extend/wta-snailai-server/src/main/resources/application.yml</Path> |
| full bundle 包含 AI artifact，core 不包含；删除 vendor 实现不意味着必须删掉 AI artifact | <Path>backend/wta-admin/pom.xml</Path>；<Path>scripts/ci/verify-admin-bundle.sh</Path> |
| 前端有 AI domain 注册桥、带凭据 URL 的旧聊天 iframe、Admin AI manifest 与 monitor/snailai 管理入口 | <Path>frontend/packages/domains/ai/src/index.ts</Path>；<Path>frontend/packages/web-domains/ai/src/chatSession.ts</Path>；<Path>frontend/packages/web-domains/ai/src/snail-ai/AiChatPage.vue</Path>；<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>；<Path>frontend/packages/domains/system/src/monitor/index.ts</Path> |
| 原初始化基座有 23 张 sai_* 表，含会话/模型/智能体/RAG/MCP 等；旧功能不自动成为新服务范围 | <Path>release-artifacts/docker/infrastructure/mysql/init/40-cde-ai.sql</Path>；菜单在 <Path>release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql</Path> |
| Compose、Nginx、Admin 回调端口、外部 OCR/Docling、env 和日志采集都消费 Snail AI | <Path>release-artifacts/docker/docker-compose-backend.yml</Path>；<Path>release-artifacts/docker/frontend/nginx/lb/nginx-lb-http.conf.template</Path> |
| release 固定 4 个 Java backend JAR、6 份 SQL，每 backend 需要 app.jar；当前仅有 Maven 构建复制路径 | <Path>release-artifacts/scripts/release-state.py</Path> |
| backend 当前没有 Go/Python 应用 manifest/代码，Python 运维脚本不等于 Python AI 服务 | 定向枚举 go.mod/go.sum/pyproject.toml/requirements.txt/poetry.lock/uv.lock 和 Go/Python 源码，无应用匹配；不含缓存/构建物 |

协议区别：现有 <Path>backend/wta-common/wta-common-openapi/src/main/java/org/namewta/common/openapi/gateway/OpenApiGatewayFilter.java</Path> 是外部机器调用 Java 的入站 HMAC，不是 Java 调 Go 的出站接口。<Path>frontend/tooling/openapi/README.md</Path> 管理文档快照/TS 生成，也不是运行时协议。

Java 出站可复用 <Path>backend/wta-api/src/main/java/org/namewta/third/api/ThirdPartyGateway.java</Path>，但当前 <Path>backend/wta-modules/wta-third/src/main/java/org/namewta/third/adapter/gateway/ThirdGatewayAdapter.java</Path> 完整读入 byte[]，不能声称现成支持 SSE 流式出口。

当前六份 SQL 是项目硬约束。移除 AI 源码基座须在设计中明确：保留无 vendor 表的合法占位，或经明确决定正式演进基座合同；不得简单放宽测试。运行库历史数据的保留/迁移/丢弃是另一个决定，本次未执行真实数据删除。

## 访谈分类

- 当前 frontier：手机号存量兼容、AI 第一版可观察终点、旧 Snail AI 历史数据处置目标。
- 后续依赖 AI 终点：管理面/用户面、模型与工具范围、鉴权/租户隔离、流式与取消、数据生命周期、构建/发布验收。
- 低影响实现选择：延续当前 wta artifact 与 @namewta 包命名；局部文件组织由 Skill/Ticket 决定。
- #2 的清空占位与 #3 的接入可以作为同 change 的顺序切片；最终是否已有接入能力由 AI 首版答案决定，不能同时声称最终完全空占位且已集成新服务。
