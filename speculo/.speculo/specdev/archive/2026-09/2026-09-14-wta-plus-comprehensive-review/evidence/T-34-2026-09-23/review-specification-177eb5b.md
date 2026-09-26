# T-34 规范轴审查：pass（静态审查）

固定 base `6fcbfeb50747b67bcaf4bf9af1d964cc7d760c04`、head `177eb5bd889afd2ab54f4a8e162dc8358d80f140`；两 SHA 均可解析，merge-base 为 base，`git diff base...head -- frontend` 非空，固定区间六个 commit。审查依照 SpecDev `code-review/SKILL.md` 及 source-discovery、risk-review、reviewer-contracts；规范来源为本 change `spec.md:97` AC-034、`ticket/34-inbox-without-realtime.md` 第 5/6/8/10 节和恢复补充、`verification.md:15,38,52`、`tickets-map.md:77,152,217-225`、适用 ADR。未读取标准轴结果。相对上一固定 head `7c98383`，只有 `frontend/e2e/run-inbox-real.py` 与 `test_run_inbox_real.py` 改变，产品代码、浏览器断言、配置和其他测试源码未变。

**结论：规范轴 pass；未发现需要继续修改的规范偏差。** 这是固定源码的静态 verdict，不代表最终 required E2E 已通过。本审查未运行服务、构建或测试，也未修改仓库。Lead 正独占执行真实 E2E；其命令、退出码、count/skip、clean HEAD/tree、实际容器/进程/端口清理和 result digest 须另行核验，才可给 T-34 完整验收。

前次 S34-4 已关闭：`run-inbox-real.py:161-165` 现在明确 `docker ps -aq --no-trunc`，使双标签发现的 ID 与 `docker run -d` 保存的完整 ID 同型；`cleanup_resources:218-244` 在删除后检查已捕获和漏取两类容器并要求无残留，`gate_exit_code:246-248` 将任何清理错误判失败。`test_run_inbox_real.py:100-126` 对漏取与已捕获完整 ID 两种路径断言精确 `--no-trunc` 参数、实际删除、空残留和无清理错误。上个 head 的真实运行显示浏览器 1/1 通过而此 ID 比较引起 exit 1，该历史失败仍应保留，不得转写为本 head 的通过证据。本机 `docker ps --help` 确认 `--no-trunc` 为关闭默认截断的选项；本次没有执行 Docker 服务命令。

前次 S34-2/S34-3 也保持关闭：驱动固定测试源路径、配置和标题，传绝对测试路径给 Playwright，并核查 JSON reporter 的唯一文件/标题及 1 expected/0 unexpected/0 skipped/0 flaky (`run-inbox-real.py:26-30,135-159,295-311,404-431`)；早退进程组、未捕获容器和各清理失败均作精确 owner/run 标签核验与失败 verdict (`:99-133,172-248,435-459`)。`inbox-real.e2e.ts:9-47` 真实 Admin 登录并读取本人公告，断言摘要/详情单份正文及零 push ticket/stream 请求；`push.ts`、Navbar 和 notice 组件保留关闭实时仍 REST、会话隔离、打开时新鲜请求、加载/错误/空态与重试。五个受控浏览器场景覆盖失败、A/B 切换、卸载、空态、打开时旧请求竞争。T-41 的完整分页仍是后续票，不以本票摘要代替。

历史 `7c98383` request-changes 报告保留于 `/tmp/wta-t34/review-specification-7c98383.md`。本报告不预填 ticket status、Skill Evidence、真实测试结果或归档结论；原始历史日志的空白字符不是产品 finding。
