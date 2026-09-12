---
schema_version: 1
artifact: source
change: 2026-09-04-third-party-http-integration
source_type: conversation
canonical_locator: null
captured_at: 2026-09-04T00:00:00+08:00
content_sha256: cadd891b0cdc5cbab5e64aca9cf072afc80f1359ec142e31ab8301e2b5ee63df
remote_state: not-applicable
close_capability: not-applicable
---

# Source: 统一第三方 HTTP 供应商与 URI 接入能力

## Capture Metadata

- **Capture method:** conversation + local-file inspection + URL research
- **Author:** user
- **Created / updated:** 2026-09-04
- **Labels or classification supplied by source:** Spring Boot 4.x, HTTPExchange, 企查查, third-party provider
- **Attachments:** `D:/Document/code/cde-standard/cde-base/cde-modules/cde-third`
- **Redactions:** none

## Original Content

用户要求认真学习 `D:/Document/code/cde-standard/cde-base/cde-modules/cde-third`，背景是未来会接入多个不同的外部 HTTP 接口，每个接口可能有不同的认证授权体系、请求头、传输加密和请求体。希望统一创建一个子模块，集中管理系统对外请求，包括接口启停、baseurl 配置等。

用户举例：接入企查查后，供应商的请求头和 baseurl 通常一致，通过不同 URI 区分接口；不同 URI 的请求体各不相同。整体模型类似“大的第三方接口供应商接入”，供应商下创建多个 URI 接口，分别访问或推送数据；每个接口可独立开关并统计调用次数，供应商也可以整体开关、编辑。系统其他子模块只通过一个通用工具服务，按固定参数调用供应商的对应接口。

用户补充：`HTTPExchange` 访问 HTTP，认为这是不错的方案；同时询问在已经升级到 Spring Boot 4.x 的前提下当前最适合的库。

## Source Comments

- 本 change 使用本地 `cde-third` 作为现状参考，不默认授权修改该外部目录。
- 当前工程后端 POM 的事实版本为 Spring Boot 4.1.0、Java 21；当前没有 `spring-cloud-starter-openfeign`、`WebClient` 或 `RestClient` 的业务调用实现。
