# T-34 规范轴审查：request-changes

固定点：base `6fcbfeb50747b67bcaf4bf9af1d964cc7d760c04`，head `7c983838c1de52aa086d8335b1d33684da1cb83e`，merge-base 等于 base。输入为 `git diff base...head -- frontend` 及固定区间五个 commit；新 head 相对 `e78d886` 只改 `frontend/e2e/run-inbox-real.py`、`inbox-real.md` 并新增 `test_run_inbox_real.py`。按 SpecDev `code-review/SKILL.md`、`source-discovery.md`、`risk-review.md`、`reviewer-contracts.md` 独立运行规范轴；未读取标准轴结果。规范来源为本 change `spec.md:97` AC-034、`ticket/34-inbox-without-realtime.md` 第 5/6/8/10 节及末尾 T-34 恢复补充、`verification.md:15,38,52`、`tickets-map.md:77,152,217-225`、`ADR.md` 中 inbox/T-41 边界。无远程 Issue/PR 作为可变权威。只静态审查；未运行服务、测试或构建，真实 E2E 结果仍由 Lead 单独验收。

## Finding S34-4 — medium — 完整/截断容器 ID 不同使成功清理仍判失败

位置：`frontend/e2e/run-inbox-real.py:161-165,224-244,319-325,360-365,458-459`；回归替身 `frontend/e2e/test_run_inbox_real.py:84-122`。

`docker run -d` 返回完整容器 ID，脚本把它保存在 `containers`；但 `owned_container_ids()` 调用 `docker ps -aq`，未指定 `--no-trunc`，Docker CLI 默认截断显示 ID（本机 `docker ps --help` 明示 `--no-trunc` 为“Don't truncate output”）。清理循环用缩写 ID 核验 labels 并执行 `docker rm -fv` 通常可成功，第二次查询也为空；随后 `for cid in containers: if cid not in discovered` 比较完整 ID 与缩写 ID，给每个实际创建的容器添加 `captured_container_not_found_with_exact_labels`。`gate_exit_code` 因非空 `cleanup_errors` 返回 1。正常真实验收必建 MySQL 和 Redis，因此即使浏览器通过且容器已回收也不能得到成功 verdict，与 T-34 要求的实际 required E2E 可验收证据冲突。当前九个无 Docker 测试使用 64 字符假 ID 模拟 `ps` 输出，没覆盖 Docker 默认短 ID；这只是静态可达推断，尚未读取本轮真实运行结果。

修复条件：令发现接口显式返回完整 ID（例如 `docker ps -aq --no-trunc`）再做同型比较；或规范化/映射 ID 并确保 **两枚精确标签**核验、删除、残余查询继续 fail-closed。补测试使 `docker run` 返回完整 ID 而 `docker ps -aq` 返回真实默认缩写，先复现错误，再验证修复后成功清理时 verdict=0；另保留 ID 未返回、标签不符、删除失败和进程停止失败的失败 verdict。

## 已关闭的前次规范发现与其边界

- S34-2 测试来源绑定已修：`run-inbox-real.py:26-30,135-159,295-311,404-431` 把固定测试绝对路径传给 Playwright，固定配置/标题/count，且独立核查 JSON reporter 唯一执行项的文件与标题；`inbox-real.e2e.ts:9-47` 执行真实登录、GET inbox、标题/正文、零 push 请求断言。若 reporter 身份异常或 count/skip 不符，驱动失败。此为静态路径确认，非本轮真实 E2E 通过结论。
- S34-3 早退进程和漏取容器回收主体已修：`run-inbox-real.py:99-133,172-239` 即使进程组 leader 已退出也扫描/停止本组，按随机 run + owner 标签发现、再次核验后删除容器，并在残留/异常时失败；`test_run_inbox_real.py:25-83,84-195` 有 leader 早退、漏取容器、标签不符、日志/stop 失败替身。S34-4 是其新增的 ID 表示边界，未修前不能把资源清理门禁判 pass。
- 既有产品行为未被新提交改动：`push.ts` 将 REST inbox 读取与 `VITE_APP_MESSAGE_ENABLED` 下实时连接分离，以 token/请求代次和会话版本过滤 A/B 迟到结果；`Navbar.vue:133` 打开时强制新鲜刷新；`notice/index.vue` 提供加载、错误重试、成功空态和单份摘要/详情；五个受控浏览器场景保留。`OssPage.vue` 两处表列泛型是已事前登记的 T-34 typecheck 前置写集，OSS 竞态仍归 T-47。上述仅确认 base...head 源码/测试合同，未声明当前 head 测试绿色。

本次审查结果为 **request-changes**，仅因 S34-4。真实 E2E、九个无 Docker 回归及其他门禁的命令、退出码、运行环境、count/skip、clean HEAD/tree、清理实际结果须由执行者单独提供；本报告不预填 T-34 Evidence 或票据状态。原始历史日志的输出空白不作为产品缺陷。
