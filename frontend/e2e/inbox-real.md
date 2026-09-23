# 实时关闭时的真实消息盒子验收

`run-inbox-real.py` 启动自有 MySQL、Redis、当前 core JAR、Admin Vite 和 Chrome，导入六份基座并为样本用户创建一条已读公告。浏览器真实登录后检查本人收件箱、单份摘要/详情和零实时连接请求，不模拟 API。

先完成前端类型检查和构建，再独占测试资源运行本验收。默认 90 秒浏览器预算不会因主机负载而自动加长。失败日志、快照和精确容器清理结果写入随机私有运行目录；驱动不修改源文件或部署环境。

从仓库根执行，传入已记录的构建提交和 JAR SHA256：

```bash
python3 frontend/e2e/run-inbox-real.py \
  --expected-head <当前干净提交SHA> \
  --jar-build-head <core构建提交SHA> \
  --expected-jar-sha256 <core产物SHA256>
```

使用仓库现有 core 构建命令 `./mvnw clean package -Pdev,bundle-core -Dmaven.test.skip=true`；打包不代表后端测试。脚本校验构建提交至当前提交的后端及 SQL 输入一致，若不一致必须重建。默认输出 `/tmp/namewta-inbox-e2e/<随机ID>/result.json`，可用 `--run-root` 指定私有目录。需本机 Docker socket、MySQL 8.4.9 / Redis 8.6.3 镜像、Java、已安装前端依赖及 Chrome；缺少条件会失败，不跳过。

只回收本轮创建的容器 ID 和进程组；不会操作已有数据库、修改桶权限或导入部署凭据。成功必须同时满足精确 clean HEAD、真实浏览器一项通过且零 skipped、资源清理和端口关闭。该场景验证读取，不代替公告发布/取消或附件投递的验收。
