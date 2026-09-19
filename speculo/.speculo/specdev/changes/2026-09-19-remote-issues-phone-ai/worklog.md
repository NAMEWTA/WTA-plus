# Worklog

## Goal

获取当前 remote 全部待处理 Issue 并完成来源冻结。按用户最新决定，#1 手机号必填与 #2 移除 Snail AI 在本 change 内完成 T-triage → G → S → T → P → I；#3 Go/Python 已另建暂缓 change，等待用户想清楚。

完成标准：3 条来源分别完整冻结并可校验 hash；G 高影响决策有用户答案和明确共识；S 的可观察验收合同 Ready；T 的票据、依赖、Skill 绑定和 Map Ready；P 的目标、Gate、workspace 策略和恢复入口完整。各阶段运行实际校验并回读真实工件。规划工件不代表产品实现已完成。

## Current status

用户已按 LOG-011 批准基线修复和本地提交。T-01 done，固定 result ccd9d98，双轴和 Lead 验收通过；T-02 正在实施，随后 T-03 严格串行。产品与状态唯一 writer=codex-root；无部署、推送或远程 Issue 回写。

## Decisions

- 本次所有 3 条 remote open Issue 共用一个 change，为用户明确要求。
- roots 由 <Path>{roots.state}/workspace.json</Path> 解析；使用嵌套安装的 state。
- 每条 Issue 独立冻结 locator/hash；聚合 source 保存用户此次指令。
- 保留已有 comprehensive-review change，不继承其旧授权。Lead 为 codex-root，调查子代理只读。
- 现有未提交文件在开始时为 <Path>AGENTS.md</Path>、<Path>{roots.state}/specdev/config.json</Path>、<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>，以及已删除的 validator test；不覆盖这些修改。

## Files changed

本 change 的 Source / Sources / Issue Index / Triage / 状态 / 工作记录 / Grounding，以及 G 的 design tree / LOG / CONTEXT / ADR；S 的 Spec、T 的三票/Map、P 的 Goal及规划评审证据；另建 Go/Python 暂缓change。全局 active 索引新增两个新 change；capture 三行消费状态与计数更新，原 locator/marker/hash 保留。

## Remaining work

继续 T-02→T-03，完成 Snail AI 退出、当前 OpenAPI、六份 SQL 与旧数据保留验证、同源发布候选和最终 SpecDev 门。#3 继续独立暂缓。

## Verification

- Git remote：origin 为 NAMEWTA/WTA-plus；当前 main，起始 HEAD 86154daa4c67fa02ac13439c038a09caf5e6308a。
- GitHub REST `issues?state=open&per_page=100` / `state=closed` 全分页：exit 0；3 open / 0 closed，排除 PR。
- github-npm-ops issue-read #1 / #2 / #3：均 exit 0，全部 0 评论。
- 来源完整性：聚合 source 与三份 Issue source 的 SHA-256 全部通过；每份仅一个 canonical_locator。
- `node <Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path> --stage triage --repo <project-root> <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/</Path>`：exit 0，0 errors / 0 warnings。
- `node <Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path> --capture <Path>{roots.state}/specdev/capture.md</Path>`：exit 0，0 errors / 0 warnings。
- `node <Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path> --stage grill --repo <project-root> <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/</Path>`：exit 0，0 errors / 0 warnings；仅结构有效，尚未 consensus。
- 工件叙述中的 69 处实际路径引用存在性检查通过；G 状态保留 current_work，works_run 仅包含已完成的 triage。
- 产品测试、构建和部署：本次规划阶段尚未执行。

### 当前规划验证

- stage spec首次exit 1：未决问题章节格式已修正；当前validator在S提前要求Ticket覆盖，保留已有用户改动并用真实T草稿补齐，重跑exit 0。
- stage tickets（真实draft）：exit 0，0 errors / 0 warnings；Ready发布仍等待计划质量评审。
- 手机号AC-001–005只读独立review：pass，无高影响缺口；未运行产品测试。
- 环境只读探测：Node24.21.0，Java21.0.12.1，Docker29.7.2；系统默认pnpm9.15.9，frontend cwd的corepack pnpm实测10.34.5（exit 0），所有实施命令使用后者。
- 唯一后端基线HEAD仍86154daa4c67fa02ac13439c038a09caf5e6308a；用户原有四处修改保持。

### 最终规划验收

三票与Goal独立review pass；Ready tickets校验exit0；当前goal-plan stage因既有validator误判单change而exit1/2errors，已提交HEAD原版对同一工件stage goal-plan exit0，当前无stage工件检查exit0，controller exit0且无errors。详细命令、摘要和限制在 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/planning-review.md</Path>；不把规划验证视为产品通过。

### I 实施进展（T-01）

- Map revision 5；Lead 唯一产品 writer，研究代理只读。
- 新建/更新 policy、公开 RegisterBody、管理 BO 格式、标准化后唯一性校验、双 App 表单/domain、管理页面必填已落地。
- 实际红绿：注册必填 DTO 4、注册服务缺失/非法 5、持久化新建 2、资料写入 12、BO 格式 3、Controller 标准化重复号 3；定向汇总 36 pass/0 skip。
- 临时专属 MySQL 四场景 4 pass/0 skip（含真实 Service/Mapper/动态事务、原空/原有效写入和实际 validator/listener）；受控移除注册 DSTransactional 后授权失败保留了 1 个用户，反向验证失败符合预期，随后恢复生产注解。
- 前端全量 test、typecheck、lint exit 0；两端注册 UI 2 pass；HTTPS 注册恢复 10 pass；个人资料 UI pass，管理新增/编辑浏览器夹具定位已修正待重跑。
- 无本地提交；无远程、部署或运行库操作。任务临时 MySQL 属于本任务，收尾须销毁。

### 当前恢复点

T-01 本地实现/验证全部通过，Ticket 与 current workspace 记录为 blocked，仅等待本地提交及受保护基线处理方案确认。34 个浏览器用例、默认后端 772 executed / 121 gated skip、真实 MySQL 4、前端 729、full/core 打包均有真实记录；详见 T-01 Evidence。临时 MySQL 已销毁。待确认后先应用可审查 validator 修复和基线提交，取得 T-01 implementation commit、固定点双轴 review/direct-parent，再按 T-02→T-03；没有开始其他产品票。

## 2026-09-19 授权后进展

用户已批准 preflight 方案和本 change main 本地提交；基线 dea1754、T-01 初始实现 241a96a 均已提交。固定点双轴初审完成；产品规范通过，测试安全修复与复审进行中，后续 T-02/T-03 尚未开工。current 串行 Git 校验缺陷以独立治理修复处理。仍无 push、部署或真实数据修改。

T-01 已验收：ccd9d98，双轴通过，最终同tree28真实服务/安全/登录及10浏览器均通过；result不可变。接着串行T-02；OpenAPI S2留T-03必闭合。

T-02 产品退出及非E2E通过：907后端发现/785执行/122属性skip，前端594+101+7（另补1导航边界），full/core产物验证，最终Client7+退役2浏览器通过。下一步不可变提交、隔离双轴审查、Lead固定点复核；独立server/发布/SQL/OpenAPI尚待T-03。
