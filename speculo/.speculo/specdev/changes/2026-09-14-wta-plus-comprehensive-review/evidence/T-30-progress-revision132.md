# T-30 整体验收记录

Revision128：T-30进入同一工作树候选整体验收。30个前置票已完成本地review，648累计路径全部核对；计划T/P与ticket-control均0error，T-03/T-23原未知关闭。源码88a5a9a25f9c3d88def978ac6aac64d60522638cfa43150e0cfb3f4924a9fee0，重建正式门禁/环境/浏览器矩阵，旧准备日志保留不覆盖。30review/1in_progress/0Done；提交/推送/部署全部暂缓，正式交付出口仍未完成。

历史准备报告见T-30-preparation-report.md；其中589/610路径、配置list与未决说明均保留为历史，不作为当前验收。完成标准：18条核心门禁及追加真实服务/专用浏览器/失败恢复在同一源码通过，零测试或required skip不得通过；验证前后源码与owned资源核对，所有命令/退出码/用例数/未验证边界可回读。

## 当前进度

当前源码1dff1a345e1979d809bb547f3060645d86b4508f3df3aad5fd227de53ab2c504，649累计路径。18条核心命令全部有通过报告；默认浏览器仍有1项live Nacos条件skip，须专门补验。初始化补修前后的输入等价关系见T-30-initializer-input-equivalence.json，原始报告指纹不改写。

| 已执行组 | 实际结果 | 证据 |
|---|---|---|
| 前端静态/单元及手册/OpenAPI | 8命令exit0，Vitest614通过 | T-30-v2-static.json |
| 三App dev/prod | 两模式通过，逐App标记/完整dist哈希保存 | T-30-v2-frontend-builds.json |
| 默认浏览器 | 53通过/1 live Nacos环境skip | T-30-v2-default-e2e.json/log |
| 根Maven | 740通过/117环境skip/0失败 | T-30-v1-backend-tests.json |
| 后端full/core | clean构建/包清单4命令exit0，先full后core，JAR哈希保存 | T-30-v1-backend-bundles.json |
| 发布 | 117通过/0skip | T-30-v2-release.json |
| 既有真实服务CI | 8通过/0skip，资源恢复 | T-30-v2-external.json |
| Notify回执/派发原子性 | 41通过/0skip | T-30-notify-v1.json |
| Notify跨进程wake/丢wake恢复 | 2通过/0skip | T-30-wake-v1.json |
| 企业转移与Notify联动 | 13通过/0skip | T-30-queued-v1.json |
| 部门树/初始数据审计 | 14通过/0skip，审计0异常 | T-30-department-v1.json |
| Third崩溃/Redis故障/限额 | 16通过/0skip | T-30-third-v2.json |
| Demo树 | 15通过/0skip | T-30-demo-tree-v1.json |
| CRUD HTTP/MySQL/上传合同 | 6通过/0skip | T-30-crud-http-v1.json |
| 实际模板渲染/编译/MySQL | 21通过/0skip | T-30-template-v1.json |

所有专用环境结果均核对source_unchanged/resources_restored；部分用例与根测试或外部CI重叠，不把上述数量相加当唯一覆盖数。SSO/三Origin/Nacos、Profile/OSS、可信代理/入口容量/审计、其余环境门控类、专用浏览器和最终覆盖索引仍待执行。

## Skill Execution Records

engineering-standards、namewta-fullstack-development、wta-module-guide按既有绑定执行本地验收矩阵；本票完整verify仍pending。

首次核心门禁：architecture:check通过，architecture:test的内层pnpm误用了系统9.15.9（项目要求10.x），exit1。使用本轮独立临时PATH shim统一调用Corepack固定版本，不升级全局工具、不改产品package/lockfile。失败日志T-30-v1-static保留，修复driver后重跑。

核心静态门禁8条通过（含全前端typecheck/lint、614项Vitest、OpenAPI）；三App dev/prod首次构建均exit0。driver首次未在production覆盖前保存development产物标记，不能只用构建命令替代AC-009产物检查；补入每模式三App build-mode与全部dist文件哈希保存后重跑两种构建，源码不变。

默认浏览器首次54项：48通过/5失败/1 live Nacos条件skip。复用未启用消息/Nacos/Monitor配置的普通production产物导致5项夹具预期不满足：两个401由消息查询触发，消息功能关闭时没有该请求；另有消息内容、Nacos及Monitor入口断言。保留原结果，恢复既有隔离driver的构建步骤，使fixture配置在编译时生效，重跑整套用例；不改产品默认值/测试断言。

Revision129：T-30真实依赖门禁发现T-23新增receipt表未同步受保护初始化器：实际126表/预期125，测试尚未开始即exit1，owned资源已恢复。回到T-23补修，先登记初始化脚本及两份发布合同测试精确写集；T-30暂停为ready，29review/1in_progress/1ready/0Done。保留旧源码88a5a9a的通过与失败证据；修正后重新冻结输入并运行受影响发布/真实服务门禁。全部提交继续暂缓。

Revision130：T-23初始化补修完成review：受保护六文件初始化实际126表，发布117项及真实MySQL/Redis/MinIO八项均零skip通过，资源恢复；仅初始化脚本和两处旧计数断言变化。T-23-checkpoint-v2共50路径，累计649路径；源码1dff1a345e1979d809bb547f3060645d86b4508f3df3aad5fd227de53ab2c504。T-30继续整体验收，30review/1in_progress/0Done；未受影响的前端/后端源树逐文件相同，旧验证证据按明确输入等价关系关联，受影响release/external已重跑。全部提交暂缓。

Third专项首轮16项中1项未进入故障注入：旧测试先断言受控容器owner=T-24，适配driver改为T-30触发安全前置失败。保留失败T-30-third-v1；新driver保留T-24安全标签并追加run=T-30，容器名/ID仍本轮独占，产品与测试断言未变，重跑16项。

Revision131：T-30补跑默认环境skip发现5个夹具错误：Admin菜单使用失效裸图标，两个OSS测试从旧标记截取至EOF导致重复建表，Profile以分号直接切SQL破坏坐标字面量且截取后续无关域。在本票既有admin测试写集内登记4测试及1共用SQL执行工具：复用真实基座DDL、限定片段、使用Spring SQL脚本解析，Profile在owned空数据库完整初始化五份业务基座并清理全部所建表；保留所有权限/数据/失败关闭断言，生产SQL不放宽。保留T-30-extra-services-v1的15项/5错误，修复后重跑受影响闭包；30review/1in_progress/0Done，全部提交暂缓。

补验v2：15项中14通过，Profile完整基座包含原14个Admin权限及T-14新增4个Home本人申请/材料权限，旧总数断言不再适用。保持Admin精确14，追加Home精确4及不得包含其他Profile权限；不改变生产菜单/权限，v2失败原样保留。

Revision132：T-30补查全部环境门控/Tag发现9个Profile e2e类未被默认Maven选择；真实MySQL补跑15项，13通过/2失败/零skip。企业申请夹具credit(suffix)拼接任意末位，不满足当前统一社会信用代码校验码合同，save在业务入口即被拒绝。先追加该EnterpriseApplicationMySqlE2ETest.java精确写集，仅修正合成合法身份数据并增加响应code断言，保留发布/重新认证/唯一约束/工作流回滚断言及生产校验。浏览器163项及额外10工作流弹窗已实际通过；30review/1in_progress/0Done，全部提交暂缓。
