# OIDC / SSO 增量升级验证

本次仅在任务自有的 MySQL 8.4 容器 `wta-external-auth-test-mysql` 内演练，未连接已有业务库。源版本为 `28f841a22ff007fd154cfeef94c27be35cb9f297`，目标为本次工作树的 canonical SQL。升级库先执行源版本基座，然后只执行本次增量；没有向已有库重放完整基座。

## 执行结果

| 检查 | 结果 |
| --- | --- |
| 旧库升级 | 108 张业务表 → 116 张业务表 |
| 新增结构 | 8 张表、7 列、`sys_social.identity_key` 唯一索引 |
| 旧数据 | 108 张旧表中的 581 条记录，按升级前全部列逐值保留 |
| 专门保留样本 | 自定义用户、历史 JustAuth 绑定、第三方 OIDC 应用、人工修改的菜单备注 |
| 新菜单 | 13 个菜单、13 条管理员角色关系，与 fresh 安装一致 |
| 结构对比 | 表引擎/排序规则/注释，全部列顺序/类型/默认值/可空性/生成表达式/注释，全部索引均与 fresh 一致 |
| DML 重试 | 再执行一次增量 DML，菜单与角色关系数不变 |
| 退出码 | 演练 Python 进程 `0` |

新增表为 `sso_service_config`、`oidc_service_config`、`oidc_key_material`、`sso_session`、`sso_business_session`、`oidc_logout_outbox`、`sys_auth_provider`、`sys_auth_registration`。新增列为：

- `sys_social`：`issuer`、`subject`、`identity_key`。
- `sso_authorization_code`：`session_hash`。
- `oidc_application`：`backchannel_logout_uri`、`backchannel_logout_session_required`。
- `oidc_authorization`：`session_closed`。

本次实际执行库为 `namewta_oidc_upgrade_old_v2` / `namewta_oidc_upgrade_fresh_v2`。临时证据位于 `/tmp/oidc-upgrade-verification/`：`summary.json`、`before-upgrade.sql`、`delta-ddl.sql`、`delta-dml.sql`、结构 TSV 和菜单 TSV；执行日志为 `/tmp/oidc-upgrade-rehearsal-v2.log`。目录权限 `0700`，文件权限 `0600`，均未提交。

| 输入/产物 | SHA-256 |
| --- | --- |
| 源 10-DDL | `1d8e0dd7ac7c871c7327246e362cfd7a2f7e717b051af1bdbd96e48ae28b8f95` |
| 源 50-DML | `ff20556a0a8696c584a2f57c36e0e1d37970a367d524ceb045dc2e4a42e65746` |
| 目标 10-DDL | `9ef208dd01825db2ae369354085372e599f1970f169cab5ea80fea38dfe33272` |
| 目标 50-DML | `d788e8a790027b1f53288022c4d8699c55e7f3790896218309dc1007dfa8cdd1` |
| 增量 DDL | `a08a2e8676ee379cabdd19ffcdead05cfc82c1ce7f1224036905d243f9fe711b` |
| 增量 DML | `de5b56204c69b2f0230cb7b7eab5d8f3624c364bdede609cb74b6b14e789b0b8` |

目标基座发生变化时，重新生成和演练，不复用以上散列作为新版本证据。

## 从唯一事实源重新生成差异

以下命令在仓库根目录执行，只在 `/tmp` 生成 SQL。生成器针对本次“仅新增列/索引/表，DML 在末尾新增区块”的差异；发现删除、修改旧列或非追加 DML 时会拒绝继续，不可作为任意版本通用迁移器。

```bash
export REHEARSAL_DIR="$(mktemp -d /tmp/namewta-oidc-upgrade.XXXXXX)"
chmod 700 "$REHEARSAL_DIR"
python3 - <<'PY'
import os, pathlib, re, subprocess
out = pathlib.Path(os.environ['REHEARSAL_DIR'])
root = pathlib.Path('release-artifacts/docker/infrastructure/mysql/init')
source = '28f841a22ff007fd154cfeef94c27be35cb9f297'
def old(name):
    return subprocess.check_output(['git', 'show', f'{source}:{root}/{name}'], text=True)
def tables(ddl):
    return {m[1]: m[0] for m in re.finditer(
        r'create table\s+`?(\w+)`?\s*\([\s\S]*?;(?=\s*(?:\n|$))', ddl, re.I)}
def columns(block):
    kinds = r'bigint|int|tinyint|smallint|mediumint|varchar|char|text|longtext|mediumtext|datetime|timestamp|json|decimal|double|float|date|time|blob|bit'
    return [(m[1], line.strip().removesuffix(',')) for line in block.splitlines()
            if (m := re.match(r'\s+`?(\w+)`?\s+(?:' + kinds + r')\b', line, re.I))]
def indexes(block):
    return {m[1]: line.strip().removesuffix(',') for line in block.splitlines()
            if (m := re.match(r'\s+(?:unique\s+)?key\s+`?(\w+)`?', line, re.I))}
before, after = old('10-cde-base-ddl.sql'), (root / '10-cde-base-ddl.sql').read_text()
source_tables, target_tables = tables(before), tables(after)
assert source_tables.keys() <= target_tables.keys()
statements = []
for table, block in source_tables.items():
    old_columns = dict(columns(block))
    new_columns = dict(columns(target_tables[table]))
    assert old_columns.keys() <= new_columns.keys()
    assert all(new_columns[c] == definition for c, definition in old_columns.items())
    previous = None
    for column, definition in columns(target_tables[table]):
        if column not in old_columns:
            position = 'FIRST' if previous is None else f'AFTER `{previous}`'
            statements.append(f'ALTER TABLE `{table}` ADD COLUMN {definition} {position};')
        previous = column
    for key, definition in indexes(target_tables[table]).items():
        if key not in indexes(block):
            statements.append(f'ALTER TABLE `{table}` ADD {definition};')
new_tables = [t for t in target_tables if t not in source_tables]
ddl = 'SET NAMES utf8mb4;\n' + '\n'.join(statements) + '\n\n' + '\n\n'.join(target_tables[t] for t in new_tables) + '\n'
old_dml, new_dml = old('50-cde-base-dml.sql'), (root / '50-cde-base-dml.sql').read_text()
assert new_dml.startswith(old_dml)
dml = 'SET NAMES utf8mb4;\n' + new_dml[len(old_dml):]
assert not re.search(r'\b(?:DROP|TRUNCATE|DELETE)\b', ddl + dml, re.I)
for name, text in [('source-10.sql', before), ('source-50.sql', old_dml),
                   ('delta-ddl.sql', ddl), ('delta-dml.sql', dml)]:
    file = out / name
    file.write_text(text)
    file.chmod(0o600)
PY
sha256sum "$REHEARSAL_DIR"/*.sql
```

在另一个明确为空的任务自有库复现时，先用 `source-10.sql`、未变化的 `20/30/40`、`source-50.sql` 建立旧基座；另建 fresh 库执行当前 `10/20/30/40/50`。Nacos 的 `60` 属于独立库，不混入业务库。向旧库加入可识别的现有用户、绑定和应用样本，再备份。

```bash
# MYSQL_PWD 从本次隔离测试的私密环境取得，不写入仓库或命令输出。
# old_db 必须是本次新建的空白演练库；不得替换为线上库。
export UPGRADE_CONTAINER=wta-external-auth-test-mysql
export old_db=namewta_oidc_upgrade_old_v2

docker exec -e MYSQL_PWD="$MYSQL_PWD" "$UPGRADE_CONTAINER" \
  mysqldump -uroot --single-transaction --skip-comments --hex-blob "$old_db" \
  > "$REHEARSAL_DIR/before-upgrade.sql"
chmod 600 "$REHEARSAL_DIR/before-upgrade.sql"

docker exec -i -e MYSQL_PWD="$MYSQL_PWD" "$UPGRADE_CONTAINER" \
  mysql -uroot --default-character-set=utf8mb4 "$old_db" < "$REHEARSAL_DIR/delta-ddl.sql"
docker exec -i -e MYSQL_PWD="$MYSQL_PWD" "$UPGRADE_CONTAINER" \
  mysql -uroot --default-character-set=utf8mb4 "$old_db" < "$REHEARSAL_DIR/delta-dml.sql"
```

DDL 仅执行一次；重试先核对已经成功的语句。MySQL DDL 隐式提交，不能用事务回滚替代备份恢复。上面固定库名是本次已完成的样本，复现时应新建另一组 owned 库，不能再次向该样本执行 DDL。

本次校验程序 `/tmp/oidc-upgrade-rehearsal.py` 的执行命令为：

```bash
# 从环境注入隔离库口令，不在文档保存口令。
python3 /tmp/oidc-upgrade-rehearsal.py > /tmp/oidc-upgrade-rehearsal-v2.log 2>&1
```

它读取 `UPGRADE_TEST_MYSQL_PASSWORD`，拒绝覆盖已存在的目标库。再次运行需换用新的 owned 库名。逐表数据检查先按旧版 `information_schema.columns` 顺序生成 `JSON_ARRAY(全部旧列)`，保存每行的多重集；升级后使用相同旧列投影，断言原多重集全部存在，允许新增菜单记录。该方式也覆盖二进制值；客户端明确使用 `utf8mb4` 与 `--binary-as-hex`，比较阶段不以替换字符丢弃字节。结构检查对两个库的 `information_schema.tables/columns/statistics` 排序投影逐字比较，不比较统计基数和数据量。菜单检查比较 `210091…` 与 `210092…` 的全部业务字段和角色关联，排除初始化时间戳。

## 运行配置与协议升级边界

- 新服务配置以 `enabled=false` 初始化。显式设置 `AUTH_CONFIG_ROOT_KEY` 后，在管理页登记服务地址、导入或生成签名/状态密钥，再启用服务；数据库或缓存中没有启动根密钥。
- 旧 `justauth.type` 通过“身份接入配置”的文件导入先预览再执行。空示例跳过，已有接入不覆盖，密钥加密保存；不恢复运行时 YAML fallback。独立 `stackOverflowKey` 会提示人工核对，不写入公开选项。
- 本次证明旧业务数据保留，不承诺旧授权码、Token、中央会话连续可用。旧授权码缺少 `session_hash`；旧 OIDC 状态密文没有数据库状态密钥版本；密钥导入会生成新的 `kid`。应使用维护窗口，切换后重新登录，不能在未知旧密钥状态下把保留的旧授权记录当作可继续使用的凭据。
- 不将历史 `sys_social` 的重复 `auth_id` 自动合并到某个用户。新增 `identity_key` 对旧记录为 `NULL`，历史冲突仍由精确兼容查找拒绝；用户、密码和旧绑定内容不被升级 SQL 改写。
- 回滚优先恢复相容的应用与配置。不要删除新增表或列来“回滚”；需要恢复数据库时使用核验过的备份，并另行批准具体目标。

## 发布配置独立复核

同域 LB 保留 SSO 静态前缀，同时将 `/sso/` 和固定 OIDC 协议根路径转给 SSO App；管理/用户 App 的 API 仍按各自前缀转发。三个 App 的后端地址可独立覆盖，未设置的实例继续使用公共 `BACKEND_SERVER1/2`。`add_app.mjs` 与发布清单校验使用同一嵌套变量形式。

修复了一处实际链路问题：SSO App 在 LB 后原先覆盖 `X-Forwarded-For`，导致真实客户端地址丢失。HTTP/TLS 模板现在与其他内部 App 代理一样追加实际 peer；公开 LB 仍先清洗外来转发链。

```bash
node --test release-artifacts/tests/app-inventory.test.mjs \
  release-artifacts/tests/release-config.test.mjs \
  release-artifacts/tests/trusted-client-address.test.mjs \
  release-artifacts/tests/release-integration-contract.test.mjs
# 42 项通过，退出码 0；/tmp/oidc-release-review-tests.log

docker compose --env-file /tmp/oidc-compose-review.env \
  -f release-artifacts/docker/docker-compose-frontend.yml --profile tls config --format json
# 退出码 0；另断言各 App 的 SERVER1 独立覆盖、SERVER2 公共回退、SSO TLS 同步、LB SSO 前缀。
```

以上为隔离升级和配置验证，不是线上部署或流量切换记录。

### 真实 Nginx HTTP / TLS 传输验收

使用任务自有临时容器 `wta-oidc-nginx-transport`、实际镜像 `nginx:1.31.1`，加载仓库 HTTP/TLS LB 及三个 App 模板。仅将模板中的上游地址替换为容器回环上的四个 owned stub（Admin、Home、SSO、替代上游），静态文件使用三个各不相同的测试页面。TLS 使用一天有效的临时自签名证书。容器只向 `127.0.0.1` 发布随机测试端口，结束后已移除；共享 MySQL/Redis 容器未清理。

| 实际 curl 路径 | HTTP / HTTPS 结果 |
| --- | --- |
| `/review-admin/nested/page`、`/review-home/nested/page`、`/review-sso/nested/page` | 均 `200`，分别返回正确 App 的 SPA |
| `/review-admin/prod-api/marker?x=1` | `200`，Admin 后端收到 `/marker?x=1` |
| `/review-home/prod-api/marker?x=2` | `200`，Home 后端收到 `/marker?x=2` |
| `/sso/context?request=owned` | `200`，SSO 后端收到原路径与查询串 |
| `/.well-known/openid-configuration`、`/oidc/login-context?request=owned` | `200`，协议根路径与查询串原样到达 SSO 后端 |
| `/oidc/authorize` | `302`，保留 `Location: /review-sso/authorize` |
| `/oidc/token` | `401`，保留 `invalid_client` 响应正文 |
| `/oidc/userinfo` | `503`，保留协议错误正文，没有替换成 LB 维护页 |
| 逐一切换 Admin / Home / SSO 的上游 | 两种传输下均只有选中 App 切换，另外两个 App 的上游不变 |

HTTP 与 TLS 两阶段的 `nginx -t` 均输出 `syntax is ok`、`test is successful`，退出码 `0`。共 40 次真实 curl 检查通过。代理到达 stub 的 `X-Forwarded-Proto` 分别为 `http` / `https`；入口清除了传入的伪造 XFF，内部 App 又追加了实际 peer，SSO 共享入口没有丢失入口客户端地址。

执行命令及证据：

```bash
python3 /tmp/oidc-nginx-transport.py > /tmp/oidc-nginx-transport.log 2>&1
# 退出码 0；脚本通过 docker run 加载实际模板，并通过 docker exec 执行 nginx -t / reload。
# curl 使用 --insecure 仅接受本次临时自签名证书，不改变产品模板。

cat /tmp/oidc-nginx-transport/summary.json
cat /tmp/oidc-nginx-transport/nginx-http-test.log
cat /tmp/oidc-nginx-transport/nginx-tls-test.log
```

渲染后配置、测试页面、临时证书、逐次响应、nginx 日志都在 `/tmp/oidc-nginx-transport/`，未提交。该 stub 只验证传输合同；OIDC 签名、交换、绑定和浏览器会话的正确性由对应后端/浏览器测试证明。
