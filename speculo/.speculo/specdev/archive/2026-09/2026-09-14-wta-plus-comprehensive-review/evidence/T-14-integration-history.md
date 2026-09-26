# T-14 历史失败与恢复记录

以下为实施阶段历史状态；最终验收以 T-14.md 为准。

已通过：requirements首次6/6；domain12/type/lint；focused58（12domain+36web+10Home）/type/lint；Home生产构建；fixture第二次test-compile（不计测试）；异常优先级红灯7中1失败，修复后7/7。

## Integration failure review and recovery

共同模式：从此前控制面mock提升到实际MVC/数据库/OSS时，fixture装配偏差和真实基座缺口逐层暴露。每次重试前均有具体变化，无重复无变化重跑；全部owned容器/卷回收且库存一致，未访问运行服务。

| 记录 | 结果与原因 | 下一次实质变化 |
|---|---|---|
| T-14-browser-first | 1Java error，Chrome未启动；直接读取有Maven占位符的源码YAML | 读取Maven已过滤的实际classpath配置 |
| T-14-browser-second | 4Chrome失败；fixture将JSON converter放在YAML之后 | 使用Spring7 withJsonConverter保留默认优先级 |
| T-14-browser-third | 4Chrome失败；fixture事务拦截器误拦无注解GET；内部隐藏控件定位；无效信用代码；业务异常被Global吞没 | 使用生产注解advisor/可见Element控件/有效校验码；业务Advice顺序独立红灯后修复 |
| T-14-browser-fourth | 1失败/3未运行；Mockito getArgument在JdbcTemplate varargs处被推断为Object[]；javap确认checkcast | 指定getArgument(0,String.class) |
| T-14-browser-fifth | 1失败/3未运行；已通过真实草稿保存和直接缺材料拒绝，上传登记报UploadTicket状态错误 | 在fixture增加只含类型和代码位置的诊断，不记票据/URL/SQL参数 |
| T-14-browser-sixth | 1失败/3未运行；根因为SQLSyntaxError；SysOss及Mapper需要delete_state但六文件DDL缺列 | revision62追加唯一DDL基座，按真实实体默认ACTIVE补列，再从六文件重建验证 |

恢复入口：run-t14-browser.py T-14-browser-seventh；当前尚未验证完整个人/企业提交，不以部分路径成功替代AC。

Seventh：DDL修复后实际上传、登记、刷新和私有图片decode均通过；提交确认使用Home当前英文OK，测试原定位中文确定失败（1失败/3未运行）。下一轮只修测试为实际可见按钮，未修改产品以迎合测试。

Eighth：3Chrome通过（个人双面、企业法人/非法人条件的真实提交）；越权用例复用个人身份证被真实唯一性约束拒绝保存，故1失败，Java最终DB断言尚未执行。下一轮仅改测试独立有效身份。

## Remaining

补齐DDL后跑真实MySQL/Redis/OSS/Chrome；还需取消/失败/过期/替换与事务回滚场景、完整前后端受影响门禁、证据与hash检查点、治理校验。

第九次：3/4 Chrome 通过，越权 owner 的读/挂/删均被拒绝；缺权限的合同码误为 500（预期 403）。与前次唯一性测试数据失败不同；本次根因是鉴权专用 advice 无优先级。下一步新增全局 advice 在前的 401/403 回归测试，并给 SaToken advice 显式顺序，再回到完整浏览器场景。revision 63 已登记精确公共文件。

第十次：4/4 Chrome、1/1 Java 通过，真实 MySQL 提交快照、OSS 引用和 MinIO 内容通过，资源恢复。扩展故障首轮：前四条通过，第五条在材料查询尚未完成、文件输入仍 disabled 时通过 Playwright setInputFiles 注入文件，组件正确忽略操作；无网络故障实际发生。修正 save 测试等待真实文件输入可操作，再执行故障。状态夹具 count(*) 使用 Integer，避免生产 Long ID 字符串序列化影响计数比较。

默认回归首轮 47 pass/4 fail/1 live Nacos skip：测试构建 runner 使用了源码不消费的 VITE_APP_MESSAGE/VITE_APP_NACOS_CONSOLE_PATH，导致消息驱动的 401 恢复、消息显示及 Nacos frame 不启用。实际源码为 VITE_APP_MESSAGE_ENABLED/VITE_APP_NACOS_ADMIN，已回读 push.ts/env.d.ts；只修复 runner 参数并重建，不修改产品或断言。
