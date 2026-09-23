# T-41 D 首次真实浏览器启动失败只读归因

固定输入：D `d93eb1d8d6095464a9606619f4161b3dd4ef35c7`，运行记录 `/tmp/wta-t41/browser-runs/f2ed71eeedb7b8e2/result.json`，固定运行器 `frontend/e2e/run-inbox-paged-real.py`。只读结果和六 SQL / 初始化器；未启动服务、Docker、MySQL、Maven、Vite 或浏览器，未修改仓库/运行器。本报告不把失败结果写成业务红灯或 T-41 验收。

## 可直接证明的失败边界

`result.json`：六 SQL `init_exit_code=0`，紧随的五项基座查询已得到 `(business_tables,outboxes,external_deliveries,enabled_external_accounts,private_default_oss)=(103,0,0,0,1)`。接下来 `run-inbox-paged-real.py:841-855` 依次查 WTA/test1、查 Admin Client、更新两用户 fresh 库密码哈希；在 `login_passwords_rotated_in_fresh_schema` 写入前抛出 `RuntimeError: Owned Docker operation failed: exec -i`。由于 `docker():122-126` 只向结果抛 `args[:2]` 而丢弃 mysql stderr，**不能证明具体是五次 mysql exec 中哪一次、MySQL 错误号/错误文本，也不能排除罕见容器瞬态故障**。BCrypt 如果自身校验失败会抛另一条错误，因此已记录的异常来自 MySQL/Docker exec 返回非零；但不能据此定位到首次 SELECT。Java/Vite/Chrome 均未启动。source 前后同 clean D/tree，双标签容器、两匿名卷和 32862–32864 三个 loopback 端口清理完毕，`cleanup.errors=[]`，`acceptance=false`。

## 最强静态候选：两种 utf8mb4 collation 混比

初始化器 `release-artifacts/scripts/init-mysql-container.sh:167-169` 明确 `CREATE DATABASE wta-plus CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci`。`10-cde-base-ddl.sql:63-87,352-385` 的 `sys_user.user_name/password` 与 `sys_client.client_id` 未另写 collation，故继承数据库默认；`50-cde-base-dml.sql:21-23,353` 确有启用的 WTA、test1 与 Admin Client 精确种子，缺记录/拼错身份的静态解释较弱。

运行器 `hexsql():366-367` 把所有字符串编码为 `CONVERT(0x… USING utf8mb4)`；`one_user():841-847` 和 `client_rows:848-849` 将此表达式直接与 `user_name` / `client_id` 等值比较。MySQL 8.4 的 `utf8mb4` 默认 collation 为 `utf8mb4_0900_ai_ci`，[官方手册](https://dev.mysql.com/doc/refman/8.4/en/charset.html)如此规定；[官方 CONVERT 手册](https://dev.mysql.com/doc/refman/8.4/en/cast-functions.html)说明转换结果可随后显式 `COLLATE`。MySQL 官方工单 [#108319](https://bugs.mysql.com/bug.php?id=108319) 给出 general_ci 列与 `CONVERT(... USING utf8mb4)` 的 0900_ai_ci **同为隐式**比较时产生 collation mix，并被官方判定为预期规则。因此首个 WTA `one_user` SELECT 是**高可信的静态首发点**，但该次实际错误号/排序规则尚无 stderr 或元数据实测，不可写成已证实根因。`status='0'`/`del_flag='0'` 是普通字面量，列可主导其排序规则；两条 UPDATE 的 `password=CONVERT(...)` 属赋值而非列比较，较不符合该同类错误，但仍不能以丢失 stderr 排除。

同型潜伏点在 `verify_seed():442-445` 的 `notify_message.title LIKE hexsql(prefix)`。若只修用户/Client 两条等值查询，真实浏览器继续执行时可能到此再遇同一错误。`hexsql()` 还用于 BCrypt 哈希赋值与消息/角色 INSERT；不要全局改变其语义来修三个字符串比较。

## 可审查修复与下次诊断条件（尚未实施）

保留首次失败原运行器和 result 不覆盖。在新版本私有运行器中，仅对 `user_name`、`client_id` 与 `title LIKE` 这三处比较值使用 `CONVERT(0x… USING utf8mb4) COLLATE utf8mb4_general_ci`（或根据 fresh 库列实际元数据选择精确 collation）；保留 `hexsql()` 在赋值/插入中的既有写法。先在不输出值的条件下通过 `information_schema.COLUMNS.COLLATION_NAME` 和 `COLLATION()/COERCIBILITY()` 检查三列与转换表达式；如实记录阶段枚举（`lookup_WTA` / `lookup_test1` / `lookup_client` / `rotate_A` / `rotate_B` / `verify_title`）和安全白名单 MySQL 数字错误号，不输出 SQL 值、哈希、env、stderr、完整 Docker inspect。若阶段/编号与推断不同，以实际新证据改判。新版本需另名、另 SHA、离线合成负例与 Lead 固定后才可再跑；不能降低 exact-clean/source/清理和真实浏览器门禁，也不能把数据库初始化成功或脚本工具失败记为产品验收。
