# I 执行前具体准备事项

用户已在 LOG-011 明确批准本方案；校验器修复已应用，11 场景回归通过。基线及逐票提交按后续 Evidence 记录执行。

1. <Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>：恢复已提交 HEAD 的四处单/多change路由判断，保留其余内容；不只添加一行guard，以免产生缺Goal假绿。
2. 恢复 <Path>{roots.workflows}/specdev/common/tools/validate-specdev.test.mjs</Path> 的既有218行/11场景回归；运行原fixture命令，不删门禁。
3. <Path>AGENTS.md</Path> 的新增空行及 <Path>{roots.state}/specdev/config.json</Path> 的键重排按现状保留，没有语义变化。
4. 准备事项、上述保留改动、capture/status及本轮两个change的规划产物形成明确基线提交；随后逐票仅提交本change的实现与证据。全仓干净才执行真实release归档，不能stash、删除用户内容或放宽clean_source。

精确修复差异：<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/preflight-validator-proposal.patch</Path>。不涉及推送、部署、远程Issue关闭或Go/Python实现。
