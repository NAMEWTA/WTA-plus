# T-29 AGENTS 逐文件裁决

全部41个generic当前全文经只替换标题模块名、Scope路径和旧编译命令后，规范化正文完全一致；原文与原SHA见T-29-originals.json，历史SHA对照及硬约束清单见T-29-agents-dispositions.json。37个叶手册移除前引用扫描为零；4个父导航先改写，公开API/SPI MUST由backend/AGENTS.md承接，完整模块导航由Module Map承接。8份special原有硬约束逐行保留。

| 路径 | 裁决 | Owner/承接 | 原SHA-256 |
|---|---|---|---|
| backend/AGENTS.md | REWRITE | backend/AGENTS.md | 80046026a390491bc48bfaddbf5e30590d0fde0d3633b411107fcf0462252654 |
| backend/wta-admin/AGENTS.md | REMOVE | backend/AGENTS.md | 629e3376abf61230581e99100a9d6792af7fb5490d3bfa147d0d4e580f2ef21c |
| backend/wta-api/AGENTS.md | REMOVE | backend/AGENTS.md | 0d003ceb18189e8bff029475f6fab942d32796a5e5e70be996534766e15f9c53 |
| backend/wta-common/AGENTS.md | REWRITE | backend/AGENTS.md | 36380d8cdbe6ec9520143b434369a777e60514392805b2a82a1e07d263fb7987 |
| backend/wta-common/wta-common-ai/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 2671f5c66fd871a4e12574ef7a5022c5910d36a3d2c1baaaa470e4efb9e35b64 |
| backend/wta-common/wta-common-bom/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | fbdc9047cf85ba7bba1bd6d6162841fd930fb1dd5ffbf798eecad0d9b921d7dc |
| backend/wta-common/wta-common-core/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 986dc24280d1610b84f7c5d57ff348fc93219330625264ea809f5cfe423cbbcc |
| backend/wta-common/wta-common-doc/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 51bce11116d4cf3f9b4d042c8e91727772c312cd3ae3f7869304cdb87faf47b6 |
| backend/wta-common/wta-common-elasticsearch/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 4a4ea8750ab3d38d6d2e81f5da17edb328c968765518d133516d23badd980bf1 |
| backend/wta-common/wta-common-encrypt/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 4026515f294d1852abf1ed6aacc4dfd3524b892d6e1a2f0d154401fbfdda78c6 |
| backend/wta-common/wta-common-excel/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | f7475591769d9a256c2c141090553d64052723468afb362a7e2ad5fcbfc9cc58 |
| backend/wta-common/wta-common-job/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | c7c59981e9d766fe9b136638417750ac09e9ab141e6179b4c53ad3af47af7394 |
| backend/wta-common/wta-common-json/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | c3f5cbeb7e5033d8eff47a409db04c7e627474b821bcf7af592781747750670e |
| backend/wta-common/wta-common-liteflow/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 2182d6838101bcf977cce0c71a6ea272d414f9c72d55f990294369f2fe2f307b |
| backend/wta-common/wta-common-log/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | fc1a3d0f5379aa0d985159034a9f1ee215aa1caef2d3c1d0ee7cb1689bb88cb5 |
| backend/wta-common/wta-common-mail/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 2be3686552bc1249a44638e1752614020cf278fc46d0c6af82b445ed6d9adc1a |
| backend/wta-common/wta-common-mcp/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | de8fbba9b9680a2b949c9394cb9b50b423e0935bf8806ddb87781fe0b0b1470f |
| backend/wta-common/wta-common-mqtt/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 1c55a7ee908a980d35cde41518653ee9f88301c21a958eccd9dbaabf43f4efab |
| backend/wta-common/wta-common-mybatis/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 04140edf40e6a3cbfa6f4448cec80861adbe74dc6e7dd3a10cc829d08ecc5a5d |
| backend/wta-common/wta-common-nacos/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 091288779ed79d033b9a6393a858b4e0723553b59622166a082d1169574f3525 |
| backend/wta-common/wta-common-notify/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | caf6bafca625a141fc9ce85b2d12ebdb66dfc872286a8c07c8681ff53f46defa |
| backend/wta-common/wta-common-openapi/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | fc7eb45ea0e3870d2be14c440ffd5f85792a6b6055b3d9f1698d10f61ada0802 |
| backend/wta-common/wta-common-oss/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 4dd0fd9dd005e311bc7f0db433b75ba1e0e986a14a29e0b1825c73546c9e8ee1 |
| backend/wta-common/wta-common-push/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 6e41cadb041003b6a9dd725a4e18f1638f8cccc1f8e48ebbb117421cee493cb8 |
| backend/wta-common/wta-common-redis/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | a83cc598859328222b74794425c4f4d82f0ce392ee8b68d89bce9e264716af2c |
| backend/wta-common/wta-common-satoken/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 1accda06873ef8d33cb17207e4c43ef947bcdb71742ae0727f0d7a2439a08d9f |
| backend/wta-common/wta-common-security/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | f1a38aa28a7a2424ae4ec35086a0e8a06091687611b8bb9e830cde8dd553af6c |
| backend/wta-common/wta-common-sensitive/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 63074f5dfc33d895e51702f9e4ee840707b76241f5fe23dba468cee9a9a104a3 |
| backend/wta-common/wta-common-sms/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | dc70f5675e06c2b8d73368aa25e600e2ff06efb0e0af9bfb685e862277dafbf4 |
| backend/wta-common/wta-common-social/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | e83357276f02ae7cbf3dcaacd5e84a11aa17a0a8b28a501da281faa151aedff3 |
| backend/wta-common/wta-common-translation/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | 329e67acba8c31aec29e2a3898ea5538e961ab8e030a9a2a284559dd2897edab |
| backend/wta-common/wta-common-web/AGENTS.md | REMOVE | backend/wta-common/AGENTS.md | e812cd5e5bd0dc388236e6b9094d0cb52371a6a7727289d7ecb980512516c6e0 |
| backend/wta-extend/AGENTS.md | REWRITE | backend/AGENTS.md | 19968c100aab48f3cfb85cd1b040f376c46e60340374403819c4841445d056a8 |
| backend/wta-extend/wta-monitor-admin/AGENTS.md | REMOVE | backend/wta-extend/AGENTS.md | 4014a69abe0bd942072cccbe1df9148a4591adf878a7ab73366d1b01b936b446 |
| backend/wta-extend/wta-snailai-server/AGENTS.md | REMOVE | backend/wta-extend/AGENTS.md | e6a3d0e84f1e26b97afa697b28e507efa8dbce44bdb3ef816869bc0f8247b6ce |
| backend/wta-extend/wta-snailjob-server/AGENTS.md | REMOVE | backend/wta-extend/AGENTS.md | 6e08be30fe42a574e34029d160aff65308dc4ba8e43cb593073f23202713d9d6 |
| backend/wta-modules/AGENTS.md | REWRITE | backend/AGENTS.md | 4f160ae40d8d9b725ab91c503db5e7070d1447ba35765c1ce1fa6ba2a9fd78e9 |
| backend/wta-modules/wta-ai/AGENTS.md | REMOVE | backend/wta-modules/AGENTS.md | fef3bef3ecd0897dd50777eb43e62e7cc1f549e4ea737b442d38df070a5a7625 |
| backend/wta-modules/wta-job/AGENTS.md | REMOVE | backend/wta-modules/AGENTS.md | 6e9b18d47385c55fdba1cee585a19d92fd694bf27151255f85d18a8087096ce1 |
| backend/wta-modules/wta-profile/wta-profile-bom/AGENTS.md | REMOVE | backend/wta-modules/AGENTS.md | b6d128b9f8ead8c8d011f27e982479da9b7adac6cc2c1a93a6955fb90534018e |
| backend/wta-modules/wta-workflow/AGENTS.md | REMOVE | backend/wta-modules/AGENTS.md | 7e69fee4fb081de3e72c43e0efc4232dc9395cda9cdb2ee7d569c9665d79ccfb |
| backend/wta-modules/wta-demo/AGENTS.md | KEEP_WITH_PATH_CWD_CORRECTION | backend/wta-modules/wta-demo/AGENTS.md | 650e566b1461d74d32b9235ff13db7d89eb0e21c2f691052fe17d80796cf4476 |
| backend/wta-modules/wta-notify/AGENTS.md | KEEP_WITH_PATH_CWD_CORRECTION | backend/wta-modules/wta-notify/AGENTS.md | be0d873bbc6b67db40f3b04b58b7e0468df7a8941695ca941ac30c534ae0661d |
| backend/wta-modules/wta-profile/AGENTS.md | KEEP_WITH_PATH_CWD_CORRECTION | backend/wta-modules/wta-profile/AGENTS.md | 760cc4497af1c504b4bad4a64ca478c16051538c0fa455ca59770ba8506104bd |
| backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md | KEEP_WITH_PATH_CWD_CORRECTION | backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md | 156f92c3e644a9cde70b0c3ea8d0a59cd29cd42059cad80c020dff73e3e35c57 |
| backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md | KEEP_WITH_PATH_CWD_CORRECTION | backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md | acdb7d8379bfebdeba3d3ecb5479de4e7858b243d20eda62bc95eb8d7bc2edb8 |
| backend/wta-modules/wta-sso/AGENTS.md | KEEP | backend/wta-modules/wta-sso/AGENTS.md | 5cf276bc68a8a5eea9bf6e4b5ac809972df066b95a773f4b19446d072c78d8c8 |
| backend/wta-modules/wta-system/AGENTS.md | KEEP | backend/wta-modules/wta-system/AGENTS.md | ffc9a8f6a3b87a8e093f2265260c867957601f3fcde52d2cc85c342e1abb2079 |
| backend/wta-modules/wta-third/AGENTS.md | KEEP_WITH_PATH_CWD_CORRECTION | backend/wta-modules/wta-third/AGENTS.md | 2c724d9042791bd1ae3aff3b15f315626315cecf7f99ee00226cc3baf56bc88b |
