# T-40 真实公告撤回浏览器验收

`run-notice-retraction-real.py` 使用独占、随机 loopback MySQL/Redis/MinIO 和完整 Admin JAR。控制账号与普通用户 A/B 先分别通过无 User-Agent 的真实 HTTP 登录；运行器在内存中核对各自在线会话，并按登录前 `info_id` 基线等待对应的异步成功审计（浏览器、系统均为 `Unknown`）。控制账号随后以固定非秘密 Chrome User-Agent 再登录，验证正常浏览器/系统审计并使用新令牌保存、发布公告，等待真实 Worker 为 A 送达 V1，再撤回该版本；随后仅为分页和负例插入显式合成消息。两例 Chrome 用例验证 A 的离页本人详情、旧管理链接安全转换，以及 B 对外人/不存在消息的一致拒绝。合成行不能代替真实 V1 发布证据。

离线安全检查：

```bash
python3 -m unittest frontend/e2e/test_run_notice_retraction_real.py
python3 frontend/e2e/run-notice-retraction-real.py --preflight
```

真实运行由验收负责人在固定、干净的源码和成功的完整 `clean package` 后执行：

```bash
python3 frontend/e2e/run-notice-retraction-real.py --execute \
  --expected-head <完整40位提交SHA> \
  --expected-jar-sha256 <完整JAR的SHA256> \
  --package-proof <完整打包证据JSON路径>
```

运行器拒绝缺少任一固定输入、非独占容器、跳过或重试的用例，以及任何清理失败。随机登录口令只留在子进程环境；浏览器 trace、视频、截图、HAR、storageState 均关闭。公开结果只保留案例计数、受白名单限制的源码行列、真实投递计数及源/JAR/资源清理事实，原始 Playwright 报告和运行时凭据在 `finally` 删除。
