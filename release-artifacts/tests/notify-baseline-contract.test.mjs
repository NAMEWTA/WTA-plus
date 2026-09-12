import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const sqlRoot = new URL('../docker/infrastructure/mysql/init/', import.meta.url);
const ddl = fs.readFileSync(new URL('50-namewta-ddl.sql', sqlRoot), 'utf8');
const dml = fs.readFileSync(new URL('60-namewta-dml.sql', sqlRoot), 'utf8');

// 按 SQL 顶层逗号拆分，保留字符串、JSON 数组和函数参数内的逗号。
function splitSqlList(source) {
  const parts = [];
  let start = 0;
  let depth = 0;
  let quoted = false;
  for (let index = 0; index < source.length; index += 1) {
    const character = source[index];
    if (character === "'") {
      if (quoted && source[index + 1] === "'") index += 1;
      else quoted = !quoted;
    } else if (!quoted) {
      if (character === '(') depth += 1;
      else if (character === ')') depth -= 1;
      else if (character === ',' && depth === 0) {
        parts.push(source.slice(start, index).trim());
        start = index + 1;
      }
    }
  }
  assert.equal(quoted, false, 'SQL 字符串必须闭合');
  assert.equal(depth, 0, 'SQL 括号必须配对');
  parts.push(source.slice(start).trim());
  return parts;
}

function noticeSeeds() {
  const insert = dml.match(/insert\s+(?:ignore\s+)?into\s+notify_notice\s*\(([^)]+)\)\s*values\s*([\s\S]*?);/i);
  assert.ok(insert, '通知中心必须有显式列名的初始化公告');
  const columns = splitSqlList(insert[1]);
  return splitSqlList(insert[2]).map((row) => {
    assert.ok(row.startsWith('(') && row.endsWith(')'));
    const values = splitSqlList(row.slice(1, -1));
    assert.equal(values.length, columns.length, '初始化公告的列和值必须一一对应');
    return Object.fromEntries(columns.map((column, index) => [column, values[index]]));
  });
}

function tableColumns(table) {
  const definition = ddl.match(new RegExp(`create table ${table} \\(([\\s\\S]*?)\\) engine`, 'i'));
  assert.ok(definition, `缺少 ${table} 表`);
  return new Map([...definition[1].matchAll(/^\s+(\w+)\s+(.+)$/gm)].map((match) => [match[1], match[2]]));
}

function jsonLiteral(value) {
  assert.equal(typeof value, 'string', '初始化 JSON 字段必须显式赋值');
  assert.ok(value.startsWith("'") && value.endsWith("'"), '初始化 JSON 使用 SQL 字符串字面量');
  return JSON.parse(value.slice(1, -1).replaceAll("''", "'"));
}

test('公告发送范围和站内消息快照字段由唯一 MySQL 基座完整定义', () => {
  const notice = tableColumns('notify_notice');
  assert.match(notice.get('recipient_type') ?? '', /^varchar\(16\) not null default 'ALL'/i);
  for (const field of ['recipient_ids_json', 'user_type_ids_json', 'channels_json']) {
    assert.match(notice.get(field) ?? '', /^json\b/i, field);
  }
  const message = tableColumns('notify_message');
  assert.match(message.get('notice_type') ?? '', /^varchar\(10\)/i);
  assert.match(message.get('channels_json') ?? '', /^json\b/i);
  for (const seed of noticeSeeds()) {
    for (const column of Object.keys(seed)) assert.ok(notice.has(column), `${column} 必须存在于 DDL`);
  }
});

test('初始化公告显式面向全部用户且只选择站内渠道，不产生额外目标或外部投递', () => {
  const seeds = noticeSeeds();
  assert.ok(seeds.length > 0);
  for (const seed of seeds) {
    assert.equal(seed.recipient_type, "'ALL'", seed.notice_id);
    assert.deepEqual(jsonLiteral(seed.recipient_ids_json), []);
    assert.deepEqual(jsonLiteral(seed.user_type_ids_json), []);
    assert.deepEqual(jsonLiteral(seed.channels_json), ['IN_APP']);
    assert.equal(seed.lifecycle, "'PUBLISHED'");
    assert.equal(seed.status, "'0'");
    assert.match(seed.published_at ?? '', /^sysdate\(\)$/i, '已发布初始化公告必须有发布时间');
  }
  assert.doesNotMatch(dml, /insert\s+(?:ignore\s+)?into\s+notify_(?:intent|delivery|outbox|message)\s*\(/i);
  assert.match(dml, /select notice_id, notice_id, 1, notice_title, notice_content, notice_type,\s*concat\([^\n]+\), published_at, create_by, create_time\s*from notify_notice where lifecycle = 'PUBLISHED'/i);
});

test('通知渠道字典与公告可选站内信、短信、邮件合同一致', () => {
  const channels = [...dml.matchAll(/\(\d+,\s*\d+,\s*'[^']+',\s*'([^']+)',\s*'notify_channel'/g)]
    .map((match) => match[1]);
  assert.deepEqual(channels.sort(), ['IN_APP', 'MAIL', 'SMS']);
});

test('通知菜单权限覆盖管理动作与收件箱阅读动作', () => {
  for (const permission of [
    'notify:notice:list', 'notify:notice:query', 'notify:notice:add', 'notify:notice:edit',
    'notify:notice:publish', 'notify:notice:retract', 'notify:notice:remove',
    'notify:notification:submit', 'notify:notification:query', 'notify:notification:retry',
    'notify:notification:cancel', 'notify:monitor:list', 'notify:monitor:query',
    'notify:inbox:list', 'notify:inbox:seen', 'notify:inbox:read'
  ]) {
    assert.match(dml, new RegExp(`['"]${permission}['"]`), permission);
  }
});

test('清理旧通知菜单不会删除当前通知监控路由', () => {
  assert.match(dml, /'notify\/monitor\/index'/, '当前通知监控菜单必须保留');
  const roleCleanup = dml.match(/delete from sys_role_menu where menu_id in \([\s\S]*?\);/i);
  assert.ok(roleCleanup, '必须先清理旧通知菜单的角色关联');
  assert.match(roleCleanup[0], /'system\/notice\/index'/);
  assert.match(roleCleanup[0], /'monitor\/notify\/index'/);
  assert.doesNotMatch(roleCleanup[0], /'notify\/monitor\/index'/,
    '角色关联清理不能按当前通知监控 component 删除有效菜单');
  const cleanup = dml.match(/delete from sys_menu where perms like 'system:notice:%'[\s\S]*?;/i);
  assert.ok(cleanup, '必须有旧通知菜单清理语句');
  assert.match(cleanup[0], /'system\/notice\/index'/);
  assert.match(cleanup[0], /'monitor\/notify\/index'/);
  assert.doesNotMatch(cleanup[0], /'notify\/monitor\/index'/,
    '清理语句不能按当前通知监控 component 删除有效菜单');
});
