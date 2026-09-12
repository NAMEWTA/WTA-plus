---
schema_version: 3
artifact: ticket
change: 2026-08-24-notify-eli5
ticket: T-01
status: in_progress
ready: true
blocked_by: []
owner: codex
paths:
  - ruoyi-vue-plus-namewta/ruoyi-api/**
  - ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/**
  - ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml
  - ruoyi-vue-plus-namewta/pom.xml
  - ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml
---

# T-01 API 与模块

新增通知公共 records、枚举和 `InAppNotificationPort`，建立 `ruoyi-notify` Maven 模块及 `AGENTS.md`，完成 modules/root/admin 装配。公共方法写中文 Javadoc。验证 API 编译、模块编译和依赖方向。
