---
schema_version: 3
plan_contract_version: 1
plan_revision: 1
requested_deliverables: [{"name":"architecture-review","count":1},{"name":"专项审查报告","count":4},{"name":"spec","count":1},{"name":"ADR提案集","count":1},{"name":"tickets-map","count":1},{"name":"计划型Ticket","count":31},{"name":"清理清单","count":1},{"name":"审查证据与交接说明","count":1}]
deliverable_policy: "用户明确要求完整review报告、方案、ticket/tickets-map/spec/ADR等必要文档；数量按实际工件核对"
artifact: tickets-map
change: 2026-09-14-wta-plus-comprehensive-review
status: draft
---

# Tickets Map：WTA-plus 全面升级审查提案

- **Map：** <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>
- **Spec：** <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>
- **Ticket 目录：** <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/</Path>
- **Evidence 目录：** <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/</Path>
- **状态：** draft；所有Ticket均ready=false；本Map不授权实现。

## 1. 总体目标与拆分策略

把静态审查发现转换为可独立验收的垂直改造：先恢复事实/gate与安全边界，再按SSO/Profile/Workflow/Upload/Notify/Third/System树与CRUD分域，最后收敛发布、文档和整体验收。每票拥有完整行为、失败、路径、Skill和Evidence契约；不按“前端/后端/测试”制造空价值票。Spec中的AC-001–AC-031分别由同号Ticket覆盖。

## 2. 项目 Skill 读取矩阵

| Applies To | Project Skill | 触发 | 时机 |
|---|---|---|---|
| ALL | <Path>.agents/skills/engineering-standards/SKILL.md</Path> | 所有代码/目录/测试/交付路径 | Map后、Ticket前 |
| frontend | <Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path> | App/domain/web-domain/API/权限/上传/SSO/UI | 规划与实现前 |
| backend common | <Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path> | common、Redis、日志、OSS/OpenAPI入口 | 规划与实现前 |
| backend modules | <Path>.agents/skills/wta-module-guide/SKILL.md</Path> | Profile/Notify/Workflow/Third/System事实 | 规划与实现前 |
| public Java contracts | <Path>.agents/skills/java-api-compatibility/SKILL.md</Path> | B-08/B-11/CRUD/API删除 | 兼容影响与实现前 |

每票frontmatter绑定为最低集合；实际命中新的scope/reference时由Lead更新Map并重验。部署/远程写入仍需另行激活部署入口，不由本Map授予。

## 3. 执行清单与依赖 DAG

| ID | 主题 | Findings | Blocked By | Depth | Risk | Ready | Status |
|---|---|---|---|---|---|---|---|
| T-01 | 恢复可信的仓库门禁与治理入口 | D-01, D-07, D-11, B-09 | — | standard | medium | no | draft |
| T-02 | 消除HTTP与操作日志中的凭据副本 | R-01 | — | standard | high | no | draft |
| T-03 | 统一有界请求体采集与验签缓存 | R-02 | T-02 | deep | high | no | draft |
| T-04 | 建立可信代理来源IP合同 | R-03 | — | deep | high | no | draft |
| T-05 | 修复防重键过期后的所有权竞态 | R-04 | — | standard | high | no | draft |
| T-06 | 强化SSO令牌随机性与Cookie安全 | B-01, B-03 | — | deep | high | no | draft |
| T-07 | 修复SSO回调编码与可恢复登录旅程 | B-02, F-09 | T-06 | deep | high | no | draft |
| T-08 | 补齐SSO独立Origin发布合同 | D-02 | T-07, T-09, T-10 | deep | high | no | draft |
| T-09 | 统一App模式、bundle与依赖服务验证矩阵 | D-04, D-08, D-09 | T-01 | deep | high | no | draft |
| T-10 | 删除混源局部发布并实现原子stage | D-03, D-10 | T-09 | deep | high | no | draft |
| T-11 | 删除浏览器共享私钥与ECB传输包装 | F-01 | — | deep | high | no | draft |
| T-12 | 统一幂等会话清理与导航恢复状态 | F-04, F-05 | T-07 | standard | high | no | draft |
| T-13 | 完成Home注册开关与验证码重试交互 | F-06 | T-12 | standard | medium | no | draft |
| T-14 | 补齐个人与企业自助认证材料闭环 | F-02 | T-18 | deep | high | no | draft |
| T-15 | 移除Profile过渡接口与测试构造桥 | B-08 | T-14 | deep | high | no | draft |
| T-16 | 保证流程任务读取和办理对象一致 | F-03, B-10 | — | deep | high | no | draft |
| T-17 | 收紧流程设计器消息来源 | F-10 | — | standard | medium | no | draft |
| T-18 | 明确上传完成、引用移除和导入失败生命周期 | F-07, F-11 | — | deep | high | no | draft |
| T-19 | 收敛System大页面中的异步状态与重复封装 | F-08 | T-18 | standard | medium | no | draft |
| T-20 | 按边界落实完整TypeScript严格目标 | F-12 | T-12, T-14, T-19 | deep | high | no | draft |
| T-21 | 统一公开页面的可访问交互基线 | F-13 | T-13, T-14 | standard | medium | no | draft |
| T-22 | 原子提交通知投递结果与lease fence | B-04 | — | deep | high | no | draft |
| T-23 | 将供应商回调幂等纳入持久事务 | B-05 | T-22 | deep | high | no | draft |
| T-24 | 恢复Third并发租约并明确限额热更新 | B-06, B-07 | — | deep | high | no | draft |
| T-25 | 保证部门移动无环且并发一致 | B-12 | — | deep | high | no | draft |
| T-26 | 按资源合同清除旧CRUD方法并同步客户端 | B-11 | T-02, T-16, T-25 | deep | high | no | draft |
| T-27 | 修复Demo树样例并删除误导占位实现 | B-14 | T-26 | standard | medium | no | draft |
| T-28 | 让通知提交后唤醒保持短路径 | B-13 | T-22 | deep | high | no | draft |
| T-29 | 清理过时文档与重复AGENTS权威 | D-05, D-06, B-17 | T-01 | deep | medium | no | draft |
| T-30 | 完成升级整体验收与可审查交付 | 整体验收 | T-01, T-02, T-03, T-04, T-05, T-06, T-07, T-08, T-09, T-10, T-11, T-12, T-13, T-14, T-15, T-16, T-17, T-18, T-19, T-20, T-21, T-22, T-23, T-24, T-25, T-26, T-27, T-28, T-29, T-31 | deep | high | no | draft |
| T-31 | 修复企业转移发码的同步/排队合同阻断 | B-15 | T-22, T-23 | deep | critical | no | draft |


依赖以单票frontmatter为权威，以上是投影。主要顺序：T-01 gate事实 → T-02/T-03日志与正文；T-06/T-07 SSO → T-08/T-09/T-10发布；T-12/T-13会话注册；T-14/T-15 Profile；T-16/T-17 Workflow；T-18/T-19/T-20/T-21前端；T-22/T-23/T-24/T-28/T-31通知与第三方；T-25/T-26/T-27树/CRUD/Demo；T-29文档；T-30整体验收。存在跨资源交集时Lead必须串行化，不能依据表格并行写 shared semantic resource。

## 4. 合同覆盖矩阵

| Contract | Ticket | 主要接缝 | 状态 |
|---|---|---|---|
| AC-001 | T-01 | 见该票验证矩阵 | draft |
| AC-002 | T-02 | 见该票验证矩阵 | draft |
| AC-003 | T-03 | 见该票验证矩阵 | draft |
| AC-004 | T-04 | 见该票验证矩阵 | draft |
| AC-005 | T-05 | 见该票验证矩阵 | draft |
| AC-006 | T-06 | 见该票验证矩阵 | draft |
| AC-007 | T-07 | 见该票验证矩阵 | draft |
| AC-008 | T-08 | 见该票验证矩阵 | draft |
| AC-009 | T-09 | 见该票验证矩阵 | draft |
| AC-010 | T-10 | 见该票验证矩阵 | draft |
| AC-011 | T-11 | 见该票验证矩阵 | draft |
| AC-012 | T-12 | 见该票验证矩阵 | draft |
| AC-013 | T-13 | 见该票验证矩阵 | draft |
| AC-014 | T-14 | 见该票验证矩阵 | draft |
| AC-015 | T-15 | 见该票验证矩阵 | draft |
| AC-016 | T-16 | 见该票验证矩阵 | draft |
| AC-017 | T-17 | 见该票验证矩阵 | draft |
| AC-018 | T-18 | 见该票验证矩阵 | draft |
| AC-019 | T-19 | 见该票验证矩阵 | draft |
| AC-020 | T-20 | 见该票验证矩阵 | draft |
| AC-021 | T-21 | 见该票验证矩阵 | draft |
| AC-022 | T-22 | 见该票验证矩阵 | draft |
| AC-023 | T-23 | 见该票验证矩阵 | draft |
| AC-024 | T-24 | 见该票验证矩阵 | draft |
| AC-025 | T-25 | 见该票验证矩阵 | draft |
| AC-026 | T-26 | 见该票验证矩阵 | draft |
| AC-027 | T-27 | 见该票验证矩阵 | draft |
| AC-028 | T-28 | 见该票验证矩阵 | draft |
| AC-029 | T-29 | 见该票验证矩阵 | draft |
| AC-030 | T-30 | 见该票验证矩阵 | draft |
| AC-031 | T-31 | 见该票验证矩阵 | draft |


## 5. 并行、路径所有权与失败停止

当前只读审查已完成；实施阶段最多按 config 的 implementation agent 上限派遣，Lead独占状态、Evidence与父分支。不同写集可并行，但以下语义资源必须由专票owner：日志凭据规则(T-02)、请求正文(T-03)、SSO身份(T-06/T-07)、Notify状态(T-22/T-23/T-31)、release manifest(T-08/T-09/T-10)、Project facts/Skill规则(T-01/T-29)。任一候选未获用户选择、运行验证失败、父分支漂移、路径越界、数据/兼容决策不明时阻塞对应票和下游。

## 6. Gate 与整体验收

所有票保持draft。未来进入T-30前必须完成适用单元/集成、浏览器、真实MySQL/Redis/MinIO/HTTP、失败注入、构建和发布验证；not-run必须保留原因。T-30不替代各票Evidence；它只汇总最终Map revision、所有AC、跨change合同、artifact digest、E2E disposition、偏差与残余风险。

## 7. 同步规则

Ticket frontmatter是状态/写集/依赖权威；Map只投影。任何Finding编号、路径、强度、ADR或验收改变时递增`plan_revision`，保留原审查证据并重算依赖闭包。用户接受某一架构候选后才调用 Grill；没有用户结论不把Strong候选转Ready。


## 9. 总控与恢复

### 总体实施背景
本 Map 将静态审查发现映射为 31 个待审核 Ticket；所有实现、迁移和发布均须用户逐项接受。

### 项目 Skill 读取矩阵
项目 Skill 绑定见第 2 节；执行前按实际 scope 重新读取并记录版本。

## 2. 执行清单

执行清单见第 3 节表格，Ticket frontmatter 为唯一状态来源。

## 3. 依赖 DAG

依赖关系以各 Ticket 的 blocked_by 为准；本 Map 仅作投影，禁止绕过阻塞条件。

## 5. 并行与路径所有权

共享语义资源（日志、SSO、Notify、release manifest、facts）由专票 owner 串行修改；路径冲突时 Lead 停止并重排。
