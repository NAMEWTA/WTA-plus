import assert from 'node:assert/strict';
import { access, readdir, readFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

const workspaceRoot = join(dirname(fileURLToPath(import.meta.url)), '../../..');
const canonicalDomains = ['admin', 'ai', 'demo', 'notify', 'profile', 'system', 'third', 'workflow'];
const removedDomains = ['identity-access', 'system-admin', 'devtools', 'operations', 'gen'];
const backendModules = {
  admin: 'wta-admin',
  ai: 'wta-ai',
  demo: 'wta-demo',
  notify: 'wta-notify',
  profile: 'wta-profile',
  system: 'wta-system',
  third: 'wta-third',
  workflow: 'wta-workflow'
};

const domainResources = {
  admin: {
    auth: ['AuthController', '/auth'],
    captcha: ['CaptchaController', '/auth/code']
  },
  ai: {
    'snail-ai': ['SnailAiController', '/snail-ai']
  },
  demo: {
    'test-demo': ['TestDemoController', '/demo/demo'],
    'test-tree': ['TestTreeController', '/demo/tree']
  },
  notify: {
    monitor: ['NotificationMonitorController', '/notify/monitor'],
    notification: ['NotificationController', '/notify/notification'],
    notice: ['NotifyNoticeController', '/notify/notice'],
    inbox: ['NotifyInboxController', '/notify/inbox'],
    callback: ['ProviderCallbackController', '/notify/callback']
  },
  profile: {
    'material-tags': ['MaterialTagController', '/profile/material-tags'],
    'person/application': ['PersonApplicationController', '/profile/person/application'],
    'person/rebind': ['PersonRebindController', '/profile/person/rebind'],
    'person/materials': ['PersonMaterialController', '/profile/person/materials'],
    'person/archive': ['PersonAdminController', '/profile/person/archive'],
    'enterprise/application': ['EnterpriseApplicationController', '/profile/enterprise/application'],
    'enterprise/transfer': ['EnterpriseTransferController', '/profile/enterprise/transfer'],
    'enterprise/materials': ['EnterpriseMaterialController', '/profile/enterprise/materials'],
    'enterprise/archive': ['EnterpriseAdminController', '/profile/enterprise/archive']
  },
  system: {
    client: ['SysClientController', '/system/client'],
    config: ['SysConfigController', '/system/config'],
    dept: ['SysDeptController', '/system/dept'],
    'dict-data': ['SysDictDataController', '/system/dict/data'],
    'dict-type': ['SysDictTypeController', '/system/dict/type'],
    menu: ['SysMenuController', '/system/menu'],
    oss: ['SysOssController', '/resource/oss'],
    'oss-config': ['SysOssConfigController', '/resource/oss/config'],
    'oss-upload': ['SysOssUploadController', '/resource/oss/uploads'],
    post: ['SysPostController', '/system/post'],
    profile: ['SysProfileController', '/system/user/profile'],
    role: ['SysRoleController', '/system/role'],
    social: ['SysSocialController', '/system/social'],
    user: ['SysUserController', '/system/user'],
    'user-type': ['SysUserTypeController', '/system/userType'],
    'monitor/cache': ['CacheController', '/monitor/cache'],
    'monitor/login-info': ['SysLoginInfoController', '/monitor/loginInfo'],
    'monitor/online': ['SysUserOnlineController', '/monitor/online'],
    'monitor/operlog': ['SysOperlogController', '/monitor/operlog']
  },
  workflow: {
    category: ['FlwCategoryController', '/workflow/category'],
    definition: ['FlwDefinitionController', '/workflow/definition'],
    instance: ['FlwInstanceController', '/workflow/instance'],
    leave: ['TestLeaveController', '/workflow/leave'],
    spel: ['FlwSpelController', '/workflow/spel'],
    task: ['FlwTaskController', '/workflow/task']
  }
};

const webResources = {
  admin: ['auth'],
  ai: ['snail-ai'],
  demo: ['test-demo', 'test-tree'],
  notify: ['monitor', 'notice', 'inbox'],
  profile: ['material-tag', 'person', 'enterprise'],
  system: [
    'client',
    'config',
    'dept',
    'dict-type',
    'menu',
    'oss',
    'oss-config',
    'post',
    'role',
    'user',
    'user-type',
    'monitor/cache',
    'monitor/login-info',
    'monitor/online',
    'monitor/operlog'
  ],
  workflow: ['category', 'definition', 'instance', 'leave', 'spel', 'task']
};

async function exists(path) {
  try {
    await access(path);
    return true;
  } catch {
    return false;
  }
}

async function packageDirectories(group) {
  const entries = await readdir(join(workspaceRoot, 'packages', group), { withFileTypes: true });
  const result = [];
  for (const entry of entries) {
    if (!entry.isDirectory()) continue;
    if (await exists(join(workspaceRoot, 'packages', group, entry.name, 'package.json'))) result.push(entry.name);
  }
  return result.toSorted();
}

test('keeps domain package names aligned one-to-one with backend modules', async () => {
  assert.deepEqual(await packageDirectories('domains'), canonicalDomains);
  assert.deepEqual(await packageDirectories('web-domains'), canonicalDomains);

  for (const name of canonicalDomains) {
    const domain = JSON.parse(await readFile(join(workspaceRoot, 'packages/domains', name, 'package.json'), 'utf8'));
    const webDomain = JSON.parse(
      await readFile(join(workspaceRoot, 'packages/web-domains', name, 'package.json'), 'utf8')
    );
    assert.equal(domain.name, `@namewta/domain-${name}`);
    assert.equal(webDomain.name, `@namewta/web-domain-${name}`);

    const source = await readFile(join(workspaceRoot, 'packages/domains', name, 'src/index.ts'), 'utf8');
    assert.match(source, new RegExp(`id: ['"]${name}['"]`));
    assert.match(
      source,
      new RegExp(`backendModules: (?:Object\\.freeze\\()?\\[['"]${backendModules[name]}['"]\\](?:\\))?`)
    );
  }

  for (const name of removedDomains) {
    assert.equal(await exists(join(workspaceRoot, 'packages/domains', name)), false);
    assert.equal(await exists(join(workspaceRoot, 'packages/web-domains', name)), false);
  }
});

test('keeps consumed Controller resources traceable through typed domain entries', async () => {
  for (const [domain, resources] of Object.entries(domainResources)) {
    const packageJson = JSON.parse(
      await readFile(join(workspaceRoot, 'packages/domains', domain, 'package.json'), 'utf8')
    );
    for (const [resource, [controller, basePath]] of Object.entries(resources)) {
      const directory = join(workspaceRoot, 'packages/domains', domain, 'src', resource);
      const source = await readFile(join(directory, 'index.ts'), 'utf8');
      assert.equal(await exists(join(directory, 'types.ts')), true, `${domain}/${resource} needs types.ts`);
      assert.match(source, new RegExp(`controller: ['"]${controller}['"]`));
      assert.match(source, new RegExp(`basePath: ['"]${basePath.replaceAll('/', '\\/')}['"]`));
      assert.equal(packageJson.exports[`./${resource}`], `./src/${resource}/index.ts`);
      if (domain === 'profile') {
        assert.equal(await exists(join(directory, 'service.ts')), true, `${domain}/${resource} needs service.ts`);
        assert.doesNotMatch(source, /export\s+\*/u, `${domain}/${resource} must use explicit exports`);
        assert.doesNotMatch(source, /HttpClient|\.request\s*\(/u, `${domain}/${resource} index must stay thin`);
      }
    }
  }
});

test('keeps the profile root as an explicit compatibility facade', async () => {
  const source = await readFile(join(workspaceRoot, 'packages/domains/profile/src/index.ts'), 'utf8');
  const service = await readFile(join(workspaceRoot, 'packages/domains/profile/src/service.ts'), 'utf8');
  const types = await readFile(join(workspaceRoot, 'packages/domains/profile/src/types.ts'), 'utf8');
  assert.doesNotMatch(source, /export\s+\*/u);
  assert.doesNotMatch(source, /HttpClient|\.request\s*\(/u);
  assert.doesNotMatch(service, /HttpRequest|\.request\s*\(/u);
  assert.doesNotMatch(types, /(?:import|export).*from\s+['"]\.\//u);
  assert.match(source, /createProfileService/u);
  assert.match(source, /profilePermissions/u);
  assert.match(source, /profileDomainModule/u);
});

test('keeps web pages reachable through explicit page-owner entries', async () => {
  for (const [domain, resources] of Object.entries(webResources)) {
    const packageJson = JSON.parse(
      await readFile(join(workspaceRoot, 'packages/web-domains', domain, 'package.json'), 'utf8')
    );
    for (const resource of resources) {
      assert.equal(
        await exists(join(workspaceRoot, 'packages/web-domains', domain, 'src', resource, 'index.ts')),
        true,
        `${domain}/${resource} needs a web entry`
      );
      assert.equal(packageJson.exports[`./${resource}`], `./src/${resource}/index.ts`);
    }
  }
});
