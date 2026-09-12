# NAMEWTA 文档导航

本目录只维护当前有效的产品架构、增强边界与上游治理信息。实现细节以对应前后端仓库源码和测试为准；历史设计与变更通过 Git 日志查看。

## 项目说明

- [NAMEWTA 增强说明](./namewta-enhancements.md)：相较上游的当前能力、实现位置和前后端协作方式。
- [OSS 登录与浏览器直传排障手册](./error/oss-login-and-direct-upload-troubleshooting.md)：登录、预签名上传、MinIO CORS、重启顺序和故障验证。
- [上游能力治理](./upstream/README.md)：产品分支、上游镜像、评估和集成规则。
- [定制边界](./upstream/customization-map.md)：前端、后端及跨端必须保持的不变量。

## 子仓库文档

- [前端 README](../frontend/README.md)：多 App monorepo、领域分层、开发命令与复用规则。
- [前端架构基线](../frontend/docs/architecture-baseline.md)：认证、动态路由、权限和包依赖边界。
- [后端 README](../backend/README.md)：模块结构、增强能力、构建、HTTP 与 SQL 规则。
- [发布资产](../release-artifacts/README.md)：MySQL 8.4 六文件基座、初始化、构建与部署规则。

各 App、domain、web-domain、platform、adapter 和 web-kit 的局部职责由其目录 README 说明，不在聚合文档中重复维护文件清单。
