import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const checker = fileURLToPath(new URL('./validate-module-mode.mjs', import.meta.url));

function fixture(t, additions = {}) {
  const root = mkdtempSync(join(tmpdir(), 'wta-layered-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  mkdirSync(join(root, 'src/main/resources'), { recursive: true });
  writeFileSync(join(root, 'pom.xml'), '<project/>');
  const files = {
    'controller/ExampleController.java': 'import org.namewta.example.usecase.ExampleUseCase;\nclass ExampleController {}',
    'usecase/ExampleUseCase.java': 'import org.namewta.example.service.ExampleService;\nclass ExampleUseCase {}',
    'service/ExampleService.java': 'import org.namewta.example.dao.ExampleDao;\nclass ExampleService {}',
    'dao/ExampleDao.java': 'import org.namewta.example.mapper.ExampleMapper;\nclass ExampleDao {}',
    'mapper/ExampleMapper.java': 'interface ExampleMapper {}',
    ...additions,
  };
  for (const [path, body] of Object.entries(files)) {
    const file = join(root, 'src/main/java/org/namewta/example', path);
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, `package org.namewta.example.${dirname(path).replaceAll('/', '.')};\n${body}\n`);
  }
  return root;
}

function check(root) {
  return spawnSync(process.execPath, [checker, root, '--mode', 'layered'], { encoding: 'utf8' });
}

test('actual layered chain and third-party dromara service imports pass', t => {
  const root = fixture(t, {
    'service/ProviderService.java': 'import org.dromara.sms4j.api.service.SmsBlend;\nimport org.dromara.warm.flow.core.service.TaskService;\nclass ProviderService {}',
    'domain/Example.java': 'import org.namewta.common.mybatis.core.domain.BaseEntity;\nclass Example extends BaseEntity {}',
    'usecase/TxUseCase.java': 'import com.baomidou.dynamic.datasource.annotation.DSTransactional;\n@DSTransactional class TxUseCase {}',
  });
  const result = check(root);
  assert.equal(result.status, 0, result.stderr);
});

test('Javadoc, comments, string literals and text blocks cannot create imports or annotations', t => {
  const root = fixture(t, {
    'support/Documentation.java': String.raw`
/** @DSTransactional @Transactional @Component
import org.namewta.common.redis.utils.RedisUtils;
*/
// @Service
class Documentation {
  String value = "https://example.test/\" @Transactional /*";
  char quote = '\'';
  String sample = """
@Component @DSTransactional
import org.namewta.common.redis.utils.RedisUtils;
""";
}`,
  });
  const result = check(root);
  assert.equal(result.status, 0, result.stderr);
});

const forbidden = [
  ['controller/Invalid.java', 'org.namewta.example.mapper.ExampleMapper'],
  ['controller/Invalid.java', 'org.namewta.system.api.UserService'],
  ['controller/Invalid.java', 'org.namewta.common.json.utils.JsonUtils'],
  ['usecase/Invalid.java', 'org.namewta.common.mybatis.core.mapper.BaseMapperPlus'],
  ['usecase/Invalid.java', 'org.namewta.common.redis.utils.RedisUtils'],
  ['usecase/Invalid.java', 'org.namewta.workflow.api.WorkflowService'],
  ['usecase/Invalid.java', 'com.baomidou.mybatisplus.core.conditions.Wrapper'],
  ['service/InvalidService.java', 'org.namewta.example.mapper.ExampleMapper'],
  ['service/InvalidService.java', 'org.namewta.common.mybatis.core.service.BaseService'],
  ['dao/Invalid.java', 'org.namewta.system.api.UserService'],
  ['mapper/Invalid.java', 'org.namewta.example.service.ExampleService'],
  ['port/Invalid.java', 'org.namewta.common.redis.utils.RedisUtils'],
  ['adapter/Invalid.java', 'org.namewta.example.dao.ExampleDao'],
  ['adapter/api/Invalid.java', 'org.namewta.example.service.ExampleService'],
  ['support/Invalid.java', 'org.springframework.http.ResponseCookie'],
  ['domain/Invalid.java', 'org.namewta.common.mybatis.core.mapper.BaseMapperPlus'],
];
for (const [path, dependency] of forbidden) {
  test(`${path} rejects ${dependency}`, t => {
    const result = check(fixture(t, { [path]: `import ${dependency};\nclass Invalid {}` }));
    assert.equal(result.status, 1);
    assert.ok(result.stderr.includes(`层禁止依赖 ${dependency}`), result.stderr);
  });
}

test('indented static imports cannot bypass infrastructure boundary', t => {
  const result = check(fixture(t, {
    'usecase/Invalid.java': '  import static org.namewta.common.redis.utils.RedisUtils.getCacheObject;\nclass Invalid {}',
  }));
  assert.equal(result.status, 1);
  assert.match(result.stderr, /层禁止依赖 org\.namewta\.common\.redis/);
});

for (const annotation of ['DSTransactional', 'com.baomidou.dynamic.datasource.annotation.DSTransactional', 'Transactional', 'org.springframework.transaction.annotation.Transactional']) {
  test(`real @${annotation} on Service fails`, t => {
    const result = check(fixture(t, { 'service/InvalidService.java': `@${annotation}\nclass InvalidService {}` }));
    assert.equal(result.status, 1);
    assert.match(result.stderr, /事务/);
  });
}

test('Spring transaction annotation on UseCase also fails', t => {
  const result = check(fixture(t, { 'usecase/Invalid.java': '@Transactional\nclass Invalid {}' }));
  assert.equal(result.status, 1);
  assert.match(result.stderr, /Spring @Transactional/);
});

test('missing actual entry-to-usecase dependency is not supplied by a comment', t => {
  const result = check(fixture(t, {
    'controller/ExampleController.java': '/* import org.namewta.example.usecase.ExampleUseCase; */\nclass ExampleController {}',
  }));
  assert.equal(result.status, 1);
  assert.match(result.stderr, /未发现 entry -> usecase/);
});

test('Notify wake helper in Service and Spring Bean in support remain invalid', t => {
  const result = check(fixture(t, {
    'service/WakePublisher.java': 'class WakePublisher {}',
    'support/Invalid.java': '@Component\nclass Invalid {}',
  }));
  assert.equal(result.status, 1);
  assert.match(result.stderr, /service 目录只能放/);
  assert.match(result.stderr, /不得声明 Spring Bean/);
});
