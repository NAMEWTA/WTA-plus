# 构建、传输与发布

## 本地门禁

```bash
bash release-artifacts/scripts/verify-release.sh
bash release-artifacts/scripts/release-manage.sh build \
  --target all --env prod --env-file release-artifacts/.env
bash release-artifacts/scripts/release-manage.sh stage --env prod
bash release-artifacts/scripts/release-manage.sh bundle --env prod
```

发布清单必须记录父仓库与子模块修订、环境、文件和 SHA-256。不得打包 `.env`、`application-local.yml`、前端 `*.local`、证书私钥或其他密钥。

前端生产构建必须从 profile 注入 `VITE_APP_CONTEXT_PATH` 与 `VITE_APP_BASE_API`。构建完成后运行 `scripts/verify-frontend-artifact.mjs`，确认 `index.html` 中 JavaScript/CSS 使用预期 `assetPrefix`；错误根 `/` 构建不得进入传输阶段。

## 传输

在校验过的服务器根目录下创建新暂存目录，使用 `.part` 文件或独立 staging 传输不可变发布包、清单和校验值。在服务器端验证大小与 SHA-256 后才能原子重命名/解压，禁止将未验证归档直接流入活动版本。记录最终镜像 ID，不能只记录标签。

## 发布顺序

1. 保存当前盘点和备份。
2. 使用目标 env 解析 Compose 配置。
3. 暂存新版本并构建或加载镜像。
4. 执行向后兼容的数据库迁移。
5. 启动或验证基础设施。
6. 启动可选 Nacos，并验证稀疏配置摘要。
7. 逐个替换后端实例，每次都验证健康。
8. 发布前端资产并重载入口。
9. 验证 API、UI、OSS、监控和日志。
10. 提升活动指针并保留上一版本。

本机缺少 Docker 时只能把 Compose 校验报告为 skipped；目标机必须使用现场精确 project/files/env 补齐 `config --quiet`，否则不能开始滚动。完整 Gate 和逐实例失败恢复见[全栈滚动发布运行手册](rolling-full-stack-release.md)。

四类逻辑 Compose 所有权为 infrastructure、observability、backend、frontend。既有服务器可能使用版本化文件和项目名，必须保留现场命名。不得仅为了匹配仓库样例而覆盖根 Compose。

`fresh-dev` 应初始化依赖并生成被忽略的本地前后端配置。开发者能从源码启动前后端，并访问 MySQL、Redis、MinIO、可选 Nacos 和统一控制台后，开发准备才算完成。

生产环境额外要求同时部署前后端，并记录 DNS、TLS、防火墙决策。内部中间件只绑定回环或可信网络，只暴露预期入口。启用 TLS 时校验证书链、有效期、安全 Cookie 与响应头。
