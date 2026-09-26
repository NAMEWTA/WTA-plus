# T-29 私有目录移动工具（供 Lead 审查执行）

脚本：`/tmp/wta-t29-private-move.py`。固定仓库 `/srv/WTA-plus`；仅处理 `temp/relase` → 同父 `temp/release`。本次只创建此脚本与本说明；**没有对真实私有目录运行 prepare、move、备份、Git 检查或读取内容**，也没有替换 Skill/配置中的旧路径引用。真实执行前须由 Lead 停止报告生成器、部署、轮换等所有源目录写入者，并确认 T-29 的精确写集、现场 owner 和后续文档修复顺序。

## 两阶段命令与结果

在唯一 owner 且源树静止时，先运行：

```bash
python3 /tmp/wta-t29-private-move.py prepare
```

只在输出 `success:true`、`exit_code:0` 时使用返回的 `backup_root` 作为下一步唯一参数。先由受限操作者私下检查该根下 `manifest.json`、`backup/`、`restore-rehearsal/`、`recovery.txt` 和 `prepared.json` 均存在；不得把 manifest、哈希、私有文件名或内容写入公开证据。然后运行：

```bash
python3 /tmp/wta-t29-private-move.py move /srv/WTA-plus/temp/.t29-private-backup/<prepare返回的随机ID>
```

CLI 的公开输出仅是 JSON `success`、`count`、`backup_root`、`exit_code`；异常详情、内部文件名、哈希和内容不输出。`prepare` 成功不等于目录已移动；只有 `move` 成功及 Lead 后续独立事实门禁才可进入 T-29/T-01 验收。脚本本身不改产品、Skill、私有路径指针或服务器侧报告。

## 安全门禁

- 前置：仓库和 `temp` 是可信共享父目录，固定现场身份 `1000:1000/0755`，拒绝 symlink、所有者/组/模式漂移及 group/other 写权限；脚本绝不对二者 chmod/chown。源根为当前有效 UID（本现场 euid 0）的私有目录，源及其内部目录/文件均无 group/other 权限；源与 `temp` 同设备。`prepare` 完成时及 `move` 原子改名前后重验共享父目录身份和 inode，改名使用的目录 FD 也须对应最初 inode。递归使用 `O_NOFOLLOW` 与描述符锚定，拒绝 symlink、FIFO、socket、设备、跨设备挂载及多链接常规文件；目标必须不存在。若顶层 `namewta-deployment.md` 存在，要求其为普通文件且 `0600`。Git `check-ignore --no-index` 要确认旧、目标、备份路径均被忽略；`git ls-files -z` 要确认三处零跟踪，结果只在进程内判断。
- 备份：新建 `temp/.t29-private-backup/<128-bit随机ID>/`，备份父目录和随机根为 `0700`。`backup/` 是全树、同文件系统、不跟随链接的保 uid/gid/mode/mtime 复制；`restore-rehearsal/` 是从备份重新复制的独立恢复演练。每个常规文件比较 SHA-256/字节数/type/mode/mtime/uid/gid；目录比较类型、直接子项数、mode/mtime/uid/gid（目录磁盘 `st_size` 不是可移植内容大小）。源、备份、演练的详细清单仅存 `0600` 私有 manifest。文件和目录执行 fsync；空间不足或比较不一致即停止，不创建可移动标记。
- 移动：`move` 再验源树与不可变 manifest、备份和演练，重做 Git/目标检查，写 `0600` 的 `moving.json` 后仅调用 Linux `renameat2(RENAME_NOREPLACE)`，使用同一个打开的 `temp` 目录 FD；没有普通 rename、复制删除或覆盖回退。之后检查旧路径不存在、目标每个原 inode/dev 与移动前完全一致、内容与元数据和备份一致、Git 仍忽略且零跟踪，才写 `moved.json` 并返回成功。
- 保留：脚本从不删除或自动反向覆盖源、目标、备份、演练。`move` 已写 `moving.json` 后如返回失败，真实 rename **可能已经完成**；先私下检查两侧和 manifest，停止任何重试/新写入，再设计单独授权的恢复。若目标已出现，不合并也不覆盖。`prepare` 中途失败可能保留不完整随机根，输出会给出该安全根；不得把它当已验证备份使用。备份清理须另行裁决。

## 本次合成验证

只在 `tempfile.TemporaryDirectory(prefix='t29-synthetic-')` 创建合成普通文件与目录，内部函数注入空的 Git guard，**没有调用真实仓库 Git，也没有碰真实私有目录**。夹具将共享父目录设为 `1000:1000/0755`、私有源设为 `root:root/02700`。`ast.parse` 通过；九组断言 `9/9`、命令退出 `0`：prepare→备份/演练→move 保留源 inode 且共享父目录身份、模式、inode 不变；仓库或 `temp` group-writable 均拒绝；仓库或 `temp` owner 漂移均拒绝；prepare 后 `temp` owner 漂移阻止 move；源 symlink 被拒；prepare 后源文件改变阻止 move；目标预存在阻止 prepare。合成测试不替代 Lead 在实际现场的备份检查和后验。

后续人工步骤：目录移动成功后再由 T-29 的单一产品/治理 owner 修复 Deploy/Customization Skill 的 `temp/release` 引用及工程事实检查器，运行其正负 fixture、handbook 与当前 facts 门禁；T-01 再独立复验。服务器 `<server-root>/deployment/namewta-deployment.md` 本工具不读取也不修改。
