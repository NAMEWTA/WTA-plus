import assert from 'node:assert/strict';
import { mkdtemp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { afterEach, test } from 'node:test';
import { parse as parseYaml, stringify as stringifyYaml } from 'yaml';
import { createBaseline, runArchitectureCheck } from '../src/index.mjs';

const temporaryRoots = [];
const fixtureManifests = new Map();
const catalog = {
  '@types/node': '^26.1.1',
  '@vue/compiler-sfc': '3.5.40',
  axios: '1.18.1',
  'crypto-js': '4.2.0',
  'element-plus': '2.14.3',
  jsencrypt: '3.5.4',
  oxlint: '^1.75.0',
  pinia: '4.0.2',
  typescript: '^6.0.3',
  vue: '3.5.40',
  'vue-router': '5.2.0',
  yaml: '2.9.0'
};
const gateScripts = {
  build: 'node --check ./src/index.js',
  lint: 'node --check ./src/index.js',
  test: 'node --test',
  typecheck: 'node --check ./src/index.js'
};

afterEach(async () => {
  await Promise.all(temporaryRoots.splice(0).map(root => rm(root, { recursive: true, force: true })));
});

async function writeJson(path, value) {
  await mkdir(dirname(path), { recursive: true });
  await writeFile(path, `${JSON.stringify(value, null, 2)}\n`);
}

async function writeFixtureFile(root, relativePath, content) {
  const path = join(root, relativePath);
  await mkdir(dirname(path), { recursive: true });
  await writeFile(path, content);
}

async function createFixture() {
  const root = await mkdtemp(join(tmpdir(), 'namewta-architecture-'));
  temporaryRoots.push(root);
  const manifest = {
    name: 'fixture-root',
    private: true,
    scripts: { ...gateScripts },
    type: 'module'
  };
  await writeJson(join(root, 'package.json'), manifest);
  fixtureManifests.set(root, new Map([['.', manifest]]));
  await writeJson(join(root, 'tooling/architecture/baseline.json'), {
    schemaVersion: 1,
    maximumViolations: 0,
    violations: []
  });
  return root;
}

async function addPackage(root, relativePath, name, dependencies = {}, imports = []) {
  const manifest = {
    name,
    version: '0.0.0',
    private: true,
    scripts: { ...gateScripts },
    type: 'module',
    exports: { '.': './src/index.js' },
    dependencies
  };
  await writeJson(join(root, relativePath, 'package.json'), manifest);
  fixtureManifests.get(root).set(relativePath, manifest);
  await writeFixtureFile(
    root,
    join(relativePath, 'src/index.js'),
    `${imports.map(specifier => `import '${specifier}';`).join('\n')}\nexport const active = true;\n`
  );
}

async function configureFixture(root) {
  const manifests = fixtureManifests.get(root);
  const importers = {};
  for (const [relativePath, manifest] of manifests) {
    const importer = {};
    for (const field of ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies']) {
      const values = manifest[field] ?? {};
      if (Object.keys(values).length === 0) continue;
      importer[field] = Object.fromEntries(
        Object.entries(values).map(([name, specifier]) => [name, { specifier, version: specifier }])
      );
    }
    importers[relativePath] = importer;
  }
  await writeFixtureFile(
    root,
    'pnpm-workspace.yaml',
    stringifyYaml({ packages: ['.', 'apps/*', 'packages/*', 'packages/*/*', 'tooling/*'], catalog })
  );
  await writeFixtureFile(
    root,
    'pnpm-lock.yaml',
    stringifyYaml({
      lockfileVersion: '9.0',
      catalogs: {
        default: Object.fromEntries(
          Object.entries(catalog).map(([name, specifier]) => [name, { specifier, version: specifier }])
        )
      },
      importers
    })
  );
}

async function check(root, { configure = true } = {}) {
  if (configure) await configureFixture(root);
  return runArchitectureCheck({ root });
}

function assertFailure(result, rule, source, target, path) {
  assert.equal(result.exitCode, 1, result.output);
  assert.match(result.output, new RegExp(`\\[${rule}\\]`));
  assert.match(result.output, new RegExp(`source=${source.replaceAll('/', '\\/')}`));
  assert.match(result.output, new RegExp(`target=${target.replaceAll('/', '\\/')}`));
  assert.match(result.output, new RegExp(`path=${path.replaceAll('/', '\\/')}`));
}

test('accepts a valid public dependency graph without reading environment files', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/platform/http', '@namewta/platform-http');
  await addPackage(
    root,
    'packages/domains/admin',
    '@namewta/domain-admin',
    { '@namewta/platform-http': 'workspace:*' },
    ['@namewta/platform-http']
  );
  await addPackage(
    root,
    'packages/web-domains/admin-web',
    '@namewta/web-domain-admin',
    { '@namewta/domain-admin': 'workspace:*' },
    ['@namewta/domain-admin']
  );
  await addPackage(root, 'packages/adapters/browser-request', '@namewta/adapter-browser-request', {
    '@namewta/platform-http': 'workspace:*'
  });
  await addPackage(
    root,
    'apps/admin-web',
    '@namewta/admin-web',
    {
      '@namewta/adapter-browser-request': 'workspace:*',
      '@namewta/web-domain-admin': 'workspace:*'
    },
    ['@namewta/adapter-browser-request', '@namewta/web-domain-admin']
  );
  await writeFixtureFile(root, '.env', 'ARCHITECTURE_SENTINEL=must-not-be-read\n');

  const result = await check(root);

  assert.equal(result.exitCode, 0, result.output);
  assert.doesNotMatch(result.output, /ARCHITECTURE_SENTINEL|must-not-be-read/);
});

test('ignores Speculo worktree copies outside the active workspace', async () => {
  const root = await createFixture();
  await writeFixtureFile(
    root,
    'specdev-worktree/archived-change/packages/domains/demo/src/index.ts',
    "import axios from 'axios';\nexport const browserWindow = window;\n"
  );

  const result = await check(root);

  assert.equal(result.exitCode, 0, result.output);
  assert.doesNotMatch(result.output, /specdev-worktree|terminal-purity/);
});

test('rejects retired Admin navigation owners and fallback algorithms', async () => {
  const cases = [
    {
      path: 'apps/admin-web/src/store/modules/permission.ts',
      source: 'export const usePermissionStore = () => undefined;\n',
      target: '@namewta/platform-app-runtime'
    },
    {
      path: 'apps/admin-web/src/directive/permission/index.ts',
      source: 'export const hasPermi = { mounted() {} };\n',
      target: '@namewta/web-kit-permission'
    },
    {
      path: 'apps/admin-web/src/router/index.ts',
      source: 'export const dynamicRoutes = [];\nexport const filterDynamicRoutes = () => [];\n',
      target: '@namewta/platform-app-runtime'
    },
    {
      path: 'apps/admin-web/src/store/modules/navigation.ts',
      source: "const modules = import.meta.glob('../../views/**/*.vue');\nexport { modules };\n",
      target: '@namewta/platform-app-runtime'
    }
  ];

  for (const fixture of cases) {
    const root = await createFixture();
    await addPackage(root, 'apps/admin-web', '@namewta/admin-web');
    await writeFixtureFile(root, fixture.path, fixture.source);

    assertFailure(await check(root), 'admin-navigation-boundary', '@namewta/admin-web', fixture.target, fixture.path);
  }
});

test('rejects an internal deep import with a localized public-entry diagnostic', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await addPackage(root, 'apps/admin-web', '@namewta/admin-web', { '@namewta/domain-demo': 'workspace:*' }, [
    '@namewta/domain-demo/internal.js'
  ]);

  const result = await check(root);
  assertFailure(result, 'public-entry', '@namewta/admin-web', '@namewta/domain-demo', 'apps/admin-web/src/index.js');
  assert.equal(result.violations.filter(item => item.rule === 'public-entry').length, 1, result.output);
  assert.doesNotMatch(result.output, /source=fixture-root/);
});

test('rejects a self deep import that bypasses the package public entry', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo', {}, ['@namewta/domain-demo/internal.js']);

  assertFailure(
    await check(root),
    'public-entry',
    '@namewta/domain-demo',
    '@namewta/domain-demo',
    'packages/domains/demo/src/index.js'
  );
});

test('rejects a platform to domain reverse edge', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await addPackage(
    root,
    'packages/platform/http',
    '@namewta/platform-http',
    { '@namewta/domain-demo': 'workspace:*' },
    ['@namewta/domain-demo']
  );

  assertFailure(
    await check(root),
    'dependency-direction',
    '@namewta/platform-http',
    '@namewta/domain-demo',
    'packages/platform/http/package.json#dependencies'
  );
});

test('rejects an undeclared public internal import', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await addPackage(root, 'packages/platform/http', '@namewta/platform-http', {}, ['@namewta/domain-demo']);

  assertFailure(
    await check(root),
    'internal-dependency-declaration',
    '@namewta/platform-http',
    '@namewta/domain-demo',
    'packages/platform/http/src/index.js'
  );
});

test('rejects a concrete adapter to domain reverse edge', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await addPackage(
    root,
    'packages/adapters/browser-request',
    '@namewta/adapter-browser-request',
    { '@namewta/domain-demo': 'workspace:*' },
    ['@namewta/domain-demo']
  );

  assertFailure(
    await check(root),
    'dependency-direction',
    '@namewta/adapter-browser-request',
    '@namewta/domain-demo',
    'packages/adapters/browser-request/package.json#dependencies'
  );
});

test('rejects an internal dependency cycle', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/a', '@namewta/domain-a', { '@namewta/domain-b': 'workspace:*' }, [
    '@namewta/domain-b'
  ]);
  await addPackage(root, 'packages/domains/b', '@namewta/domain-b', { '@namewta/domain-a': 'workspace:*' }, [
    '@namewta/domain-a'
  ]);

  const result = await check(root);
  assertFailure(result, 'dependency-cycle', '@namewta/domain-a', '@namewta/domain-a', 'dependency graph');
  assert.match(result.output, /@namewta\/domain-a -> @namewta\/domain-b -> @namewta\/domain-a/);
});

test('rejects growth of the reviewed baseline', async () => {
  const root = await createFixture();
  await writeJson(join(root, 'tooling/architecture/baseline.json'), {
    schemaVersion: 1,
    maximumViolations: 0,
    violations: [
      {
        rule: 'public-entry',
        source: '@namewta/example',
        target: '@namewta/internal',
        path: 'apps/example/src/main.ts'
      }
    ]
  });

  assertFailure(
    await check(root),
    'baseline-growth',
    'tooling/architecture/baseline.json',
    'reviewed baseline',
    'tooling/architecture/baseline.json'
  );
});

test('rejects activation of an explicitly inactive placeholder', async () => {
  const root = await createFixture();
  await addPackage(root, 'apps/mobile-web', '@namewta/mobile-web');

  assertFailure(
    await check(root),
    'placeholder-activation',
    '@namewta/mobile-web',
    'inactive placeholder',
    'apps/mobile-web/package.json'
  );
});

test('rejects duplicated API facades inside activated Apps', async () => {
  const root = await createFixture();
  await addPackage(root, 'apps/admin-web', '@namewta/admin-web');
  await writeFixtureFile(root, 'apps/admin-web/src/api/login.ts', 'export const login = () => undefined;\n');

  assertFailure(
    await check(root),
    'app-api-facade',
    '@namewta/admin-web',
    'domain public service',
    'apps/admin-web/src/api/login.ts'
  );
});

test('rejects an activated manifest outside the supported package layout', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/misc', '@namewta/misc');

  assertFailure(
    await check(root),
    'package-layout',
    '@namewta/misc',
    'recognized workspace layer',
    'packages/misc/package.json'
  );
});

test('does not hide a second deep import behind one reviewed baseline detail', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await addPackage(root, 'apps/admin-web', '@namewta/admin-web', { '@namewta/domain-demo': 'workspace:*' }, [
    '@namewta/domain-demo/first.js'
  ]);
  await configureFixture(root);
  const baseline = await createBaseline({ root });
  await writeJson(join(root, 'tooling/architecture/baseline.json'), baseline);
  await writeFixtureFile(
    root,
    'apps/admin-web/src/index.js',
    "import '@namewta/domain-demo/first.js';\nimport '@namewta/domain-demo/second.js';\n"
  );

  const result = await check(root);
  assertFailure(result, 'public-entry', '@namewta/admin-web', '@namewta/domain-demo', 'apps/admin-web/src/index.js');
  assert.match(result.output, /second\.js/);
  assert.doesNotMatch(result.output, /detail=.*first\.js/);
});

test('consumes reviewed baseline identities as a multiset', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await addPackage(root, 'apps/admin-web', '@namewta/admin-web', { '@namewta/domain-demo': 'workspace:*' }, [
    '@namewta/domain-demo/private.js'
  ]);
  await configureFixture(root);
  const baseline = await createBaseline({ root });
  await writeJson(join(root, 'tooling/architecture/baseline.json'), {
    ...baseline,
    maximumViolations: 2,
    violations: [baseline.violations[0], baseline.violations[0]]
  });

  assertFailure(
    await check(root),
    'baseline-stale',
    '@namewta/admin-web',
    '@namewta/domain-demo',
    'apps/admin-web/src/index.js'
  );
});

for (const [title, sourcePath, sourceName, targetPath, targetName] of [
  [
    'platform to api-contracts',
    'packages/platform/http',
    '@namewta/platform-http',
    'packages/api-contracts',
    '@namewta/api-contracts'
  ],
  ['domain to tooling', 'packages/domains/demo', '@namewta/domain-demo', 'tooling/generator', '@namewta/generator'],
  [
    'adapter to api-contracts',
    'packages/adapters/browser-request',
    '@namewta/adapter-browser-request',
    'packages/api-contracts',
    '@namewta/api-contracts'
  ],
  [
    'adapter to tooling',
    'packages/adapters/browser-request',
    '@namewta/adapter-browser-request',
    'tooling/generator',
    '@namewta/generator'
  ]
]) {
  test(`rejects ${title} outside the explicit dependency allowlist`, async () => {
    const root = await createFixture();
    await addPackage(root, targetPath, targetName);
    await addPackage(root, sourcePath, sourceName, { [targetName]: 'workspace:*' }, [targetName]);

    assertFailure(
      await check(root),
      'dependency-direction',
      sourceName,
      targetName,
      `${sourcePath}/package.json#dependencies`
    );
  });
}

for (const [layer, path, source, terminal] of [
  ['domain', 'packages/domains/demo', '@namewta/domain-demo', 'vue'],
  ['platform', 'packages/platform/http', '@namewta/platform-http', 'axios'],
  ['platform', 'packages/platform/crypto', '@namewta/platform-crypto', 'crypto-js'],
  ['domain', 'packages/domains/secure', '@namewta/domain-secure', 'jsencrypt']
]) {
  test(`rejects ${terminal} terminal imports from ${layer}`, async () => {
    const root = await createFixture();
    await addPackage(root, path, source, {}, [terminal]);

    assertFailure(await check(root), 'terminal-purity', source, terminal, `${path}/src/index.js`);
  });
}

test('rejects terminal runtime dependencies from platform manifests', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/platform/http', '@namewta/platform-http', { axios: 'catalog:' });

  assertFailure(
    await check(root),
    'terminal-purity',
    '@namewta/platform-http',
    'axios',
    'packages/platform/http/package.json#dependencies'
  );
});

test('rejects source assets under an inactive README-only placeholder', async () => {
  const root = await createFixture();
  await writeFixtureFile(root, 'apps/mobile-web/README.md', '# Mobile Web\n');
  await writeFixtureFile(root, 'apps/mobile-web/src/main.ts', 'export const active = true;\n');

  assertFailure(
    await check(root),
    'inactive-placeholder-content',
    'apps/mobile-web',
    'README-only placeholder',
    'apps/mobile-web/src/main.ts'
  );
});

test('ignores import examples inside comments and strings', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await addPackage(root, 'apps/admin-web', '@namewta/admin-web');
  await writeFixtureFile(
    root,
    'apps/admin-web/src/index.ts',
    "// import '@namewta/domain-demo/private.js';\nconst example = \"import '@namewta/domain-demo/private.js'\";\nexport { example };\n"
  );

  const result = await check(root);
  assert.equal(result.exitCode, 0, result.output);
});

test('parses Vue SFC scripts and rejects their deep imports', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await addPackage(root, 'apps/admin-web', '@namewta/admin-web', { '@namewta/domain-demo': 'workspace:*' });
  await writeFixtureFile(
    root,
    'apps/admin-web/src/View.vue',
    "<template><main>import '@namewta/domain-demo/example.js'</main></template>\n<script setup lang=\"ts\">\nimport '@namewta/domain-demo/private.js';\n</script>\n"
  );

  assertFailure(
    await check(root),
    'public-entry',
    '@namewta/admin-web',
    '@namewta/domain-demo',
    'apps/admin-web/src/View.vue'
  );
});

test('rejects a relative import that crosses into another workspace', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await addPackage(root, 'apps/admin-web', '@namewta/admin-web');
  await writeFixtureFile(root, 'packages/domains/demo/src/private.js', 'export const hidden = true;\n');
  await writeFixtureFile(
    root,
    'apps/admin-web/src/index.js',
    "import '../../../packages/domains/demo/src/private.js';\n"
  );

  assertFailure(
    await check(root),
    'cross-workspace-relative-import',
    '@namewta/admin-web',
    '@namewta/domain-demo',
    'apps/admin-web/src/index.js'
  );
});

test('accepts a relative import owned by the same workspace', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(root, 'packages/domains/demo/src/local.js', 'export const local = true;\n');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.js',
    "import './local.js';\nexport const active = true;\n"
  );

  const result = await check(root);
  assert.equal(result.exitCode, 0, result.output);
});

test('rejects pnpm workspace configuration drift', async () => {
  const root = await createFixture();
  await configureFixture(root);
  await writeFixtureFile(root, 'pnpm-workspace.yaml', "packages:\n  - '.'\n");

  assertFailure(
    await check(root, { configure: false }),
    'workspace-config',
    'pnpm-workspace.yaml',
    'required workspace globs',
    'pnpm-workspace.yaml'
  );
});

test('rejects a shared framework version outside the catalog', async () => {
  const root = await createFixture();
  await addPackage(root, 'apps/admin-web', '@namewta/admin-web', { vue: '3.5.40' }, ['vue']);

  assertFailure(
    await check(root),
    'catalog-reference',
    '@namewta/admin-web',
    'vue',
    'apps/admin-web/package.json#dependencies'
  );
});

test('allows only the reviewed OpenAPI generator TypeScript peer outside the catalog', async () => {
  const root = await createFixture();
  await addPackage(root, 'tooling/openapi', '@namewta/tooling-openapi');
  const manifest = fixtureManifests.get(root).get('tooling/openapi');
  manifest.devDependencies = { typescript: '5.9.3' };
  await writeJson(join(root, 'tooling/openapi/package.json'), manifest);

  const result = await check(root);
  assert.equal(result.exitCode, 0, result.output);
});

test('rejects the OpenAPI TypeScript peer exception in another package or dependency field', async () => {
  const otherRoot = await createFixture();
  await addPackage(otherRoot, 'apps/admin-web', '@namewta/admin-web');
  const otherManifest = fixtureManifests.get(otherRoot).get('apps/admin-web');
  otherManifest.devDependencies = { typescript: '5.9.3' };
  await writeJson(join(otherRoot, 'apps/admin-web/package.json'), otherManifest);
  assertFailure(
    await check(otherRoot),
    'catalog-reference',
    '@namewta/admin-web',
    'typescript',
    'apps/admin-web/package.json#devDependencies'
  );

  const fieldRoot = await createFixture();
  await addPackage(fieldRoot, 'tooling/openapi', '@namewta/tooling-openapi', { typescript: '5.9.3' }, ['typescript']);
  assertFailure(
    await check(fieldRoot),
    'catalog-reference',
    '@namewta/tooling-openapi',
    'typescript',
    'tooling/openapi/package.json#dependencies'
  );
});

test('rejects a non-workspace internal reference', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/platform/http', '@namewta/platform-http');
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo', { '@namewta/platform-http': '^0.0.0' }, [
    '@namewta/platform-http'
  ]);

  assertFailure(
    await check(root),
    'workspace-reference',
    '@namewta/domain-demo',
    '@namewta/platform-http',
    'packages/domains/demo/package.json#dependencies'
  );
});

test('rejects lockfile importer drift', async () => {
  const root = await createFixture();
  await configureFixture(root);
  await writeFixtureFile(root, 'pnpm-lock.yaml', "lockfileVersion: '9.0'\nimporters: {}\n");

  assertFailure(
    await check(root, { configure: false }),
    'lockfile-parity',
    'fixture-root',
    'active importer',
    'pnpm-lock.yaml#importers'
  );
});

test('rejects lockfile dependency specifier drift', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/platform/http', '@namewta/platform-http');
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo', { '@namewta/platform-http': 'workspace:*' });
  await configureFixture(root);
  const lockPath = join(root, 'pnpm-lock.yaml');
  const lockfile = parseYaml(await readFile(lockPath, 'utf8'));
  lockfile.importers['packages/domains/demo'].dependencies['@namewta/platform-http'].specifier = '^0.0.0';
  await writeFile(lockPath, stringifyYaml(lockfile));

  assertFailure(
    await check(root, { configure: false }),
    'lockfile-parity',
    '@namewta/domain-demo',
    '@namewta/platform-http',
    'pnpm-lock.yaml#importers.packages/domains/demo.dependencies'
  );
});

test('rejects stale inactive placeholder importers', async () => {
  const root = await createFixture();
  await configureFixture(root);
  const lockPath = join(root, 'pnpm-lock.yaml');
  const lockfile = parseYaml(await readFile(lockPath, 'utf8'));
  lockfile.importers['apps/mobile-web'] = {};
  await writeFile(lockPath, stringifyYaml(lockfile));

  assertFailure(
    await check(root, { configure: false }),
    'lockfile-parity',
    'apps/mobile-web',
    'active workspace importer',
    'pnpm-lock.yaml#importers'
  );
});

for (const [layer, path, source, globalName] of [
  ['domain', 'packages/domains/window-check', '@namewta/domain-window-check', 'window'],
  ['domain', 'packages/domains/document-check', '@namewta/domain-document-check', 'document'],
  ['domain', 'packages/domains/storage-check', '@namewta/domain-storage-check', 'localStorage'],
  ['platform', 'packages/platform/session-check', '@namewta/platform-session-check', 'sessionStorage'],
  ['platform', 'packages/platform/navigator-check', '@namewta/platform-navigator-check', 'navigator'],
  ['platform', 'packages/platform/location-check', '@namewta/platform-location-check', 'location']
]) {
  test(`rejects unshadowed ${globalName} browser access from ${layer}`, async () => {
    const root = await createFixture();
    await addPackage(root, path, source);
    await writeFixtureFile(root, `${path}/src/index.js`, `export const browserValue = ${globalName};\n`);

    assertFailure(await check(root), 'terminal-purity', source, globalName, `${path}/src/index.js`);
  });
}

for (const globalName of [
  'self',
  'history',
  'visualViewport',
  'indexedDB',
  'caches',
  'cookieStore',
  'IDBFactory',
  'CacheStorage',
  'DOMParser',
  'XMLSerializer',
  'Node',
  'HTMLElement',
  'Element',
  'Document',
  'DocumentFragment',
  'customElements',
  'CustomElementRegistry',
  'Storage',
  'MutationObserver',
  'ResizeObserver',
  'IntersectionObserver',
  'PerformanceObserver',
  'ReportingObserver',
  'XMLHttpRequest',
  'WebSocket',
  'EventSource',
  'BroadcastChannel',
  'fetch',
  'Worker',
  'SharedWorker',
  'ServiceWorker',
  'requestAnimationFrame',
  'cancelAnimationFrame',
  'matchMedia',
  'getComputedStyle',
  'screen'
]) {
  test(`rejects reviewed browser global ${globalName}`, async () => {
    const root = await createFixture();
    await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
    await writeFixtureFile(root, 'packages/domains/demo/src/index.js', `export const host = ${globalName};\n`);

    assertFailure(
      await check(root),
      'terminal-purity',
      '@namewta/domain-demo',
      globalName,
      'packages/domains/demo/src/index.js'
    );
  });
}

test('rejects browser host aliases and destructuring through unshadowed globals', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.js',
    'export const storage = self.localStorage;\nexport const database = indexedDB;\nexport const parser = new DOMParser();\nexport const host = globalThis;\nexport const { localStorage } = globalThis;\n'
  );

  const result = await check(root);
  assert.equal(result.exitCode, 1, result.output);
  for (const globalName of ['self', 'indexedDB', 'DOMParser', 'globalThis'])
    assert.match(result.output, new RegExp(`target=${globalName}`));
});

test('accepts browser-global names shadowed by lexical declarations', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.js',
    'export function inspect(window, document, globalThis) {\n  const localStorage = new Map();\n  const sessionStorage = new Map();\n  const navigator = {};\n  const location = {};\n  return [window, document, localStorage, sessionStorage, navigator, location, globalThis.window];\n}\n'
  );

  const result = await check(root);
  assert.equal(result.exitCode, 0, result.output);
});

test('keeps named class-expression bindings local to the class', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.js',
    'export const Host = class window { static current() { return window; } };\nexport const actualHost = window;\n'
  );

  assertFailure(
    await check(root),
    'terminal-purity',
    '@namewta/domain-demo',
    'window',
    'packages/domains/demo/src/index.js'
  );
});

test('accepts class-local and locally shadowed browser host names', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.js',
    'export const Host = class window { static current() { return window; } };\nexport class document { static current() { return document; } }\nexport function inspect(self, globalThis, DOMParser, indexedDB) { return [self, globalThis, DOMParser, indexedDB]; }\n'
  );

  const result = await check(root);
  assert.equal(result.exitCode, 0, result.output);
});

test('does not treat TypeScript value and type declaration names as browser references', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.ts',
    'type location = string;\ninterface Document { value: string }\ninterface Model { window: string }\nexport type Snapshot = location & Document & Model;\n'
  );
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/namespace.ts',
    'namespace location { export const value = 1; }\nexport const namespaceValue = location.value;\n'
  );
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/import-equals.ts',
    "import location = require('node:path');\nexport const separator = location.sep;\n"
  );

  const result = await check(root);
  assert.equal(result.exitCode, 0, result.output);
});

test('does not let a type-only declaration shadow a runtime browser global', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.ts',
    'type location = string;\nexport const actualLocation = location;\n'
  );

  assertFailure(
    await check(root),
    'terminal-purity',
    '@namewta/domain-demo',
    'location',
    'packages/domains/demo/src/index.ts'
  );
});

test('rejects an unshadowed DOM global used only as a type', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.ts',
    'export interface Snapshot { root: Document; element: HTMLElement }\n'
  );

  const result = await check(root);
  assertFailure(result, 'terminal-purity', '@namewta/domain-demo', 'Document', 'packages/domains/demo/src/index.ts');
  assert.match(result.output, /target=HTMLElement/);
});

test('keeps module and class-static var declarations inside their runtime scopes', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.ts',
    'namespace Internal { var window = 1; export const value = window; }\nclass Host { static { var document = {}; void document; } }\nexport const browserValues = [window, document];\n'
  );

  const result = await check(root);
  assertFailure(result, 'terminal-purity', '@namewta/domain-demo', 'window', 'packages/domains/demo/src/index.ts');
  assert.match(result.output, /target=document/);
});

test('keeps switch-case lexical declarations inside the case block', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.ts',
    'switch (1) { case 1: const window = 1; void window; break; }\nexport const browserWindow = window;\n'
  );

  assertFailure(
    await check(root),
    'terminal-purity',
    '@namewta/domain-demo',
    'window',
    'packages/domains/demo/src/index.ts'
  );
});

test('accepts browser-named mapped and inferred type parameters', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.ts',
    'export type Map<T> = { [Document in keyof T]: T[Document] };\nexport type Infer<T> = T extends infer DOMParser ? DOMParser : never;\n'
  );

  const result = await check(root);
  assert.equal(result.exitCode, 0, result.output);
});

test('does not leak an inferred type parameter into a conditional false branch', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.ts',
    'export type Infer<T> = T extends infer DOMParser ? DOMParser : DOMParser;\n'
  );

  assertFailure(
    await check(root),
    'terminal-purity',
    '@namewta/domain-demo',
    'DOMParser',
    'packages/domains/demo/src/index.ts'
  );
});

test('does not let ambient declarations shadow runtime browser globals', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.ts',
    'declare const window: unknown;\ndeclare function fetch(): unknown;\ndeclare class DOMParser {}\nexport const runtimeValues = [window, fetch(), new DOMParser()];\n'
  );

  const result = await check(root);
  assertFailure(result, 'terminal-purity', '@namewta/domain-demo', 'window', 'packages/domains/demo/src/index.ts');
  assert.match(result.output, /target=fetch/);
  assert.match(result.output, /target=DOMParser/);
});

test('allows an ambient class declaration to satisfy a local type reference', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.ts',
    'declare class DOMParser {}\nexport type Parser = DOMParser;\n'
  );

  const result = await check(root);
  assert.equal(result.exitCode, 0, result.output);
});

test('keeps ambient enum and namespace declarations type-only', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.ts',
    'declare enum Storage { Local }\ndeclare namespace Document { type Root = unknown; }\nexport type Snapshot = Storage | Document.Root;\nexport const runtimeValues = [Storage, Document];\n'
  );

  const result = await check(root);
  assertFailure(result, 'terminal-purity', '@namewta/domain-demo', 'Storage', 'packages/domains/demo/src/index.ts');
  assert.match(result.output, /target=Document/);
});

test('rejects browser access through unshadowed globalThis properties', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.js',
    "export const browserValues = [globalThis.window, globalThis['document']];\n"
  );

  const result = await check(root);
  assertFailure(result, 'terminal-purity', '@namewta/domain-demo', 'window', 'packages/domains/demo/src/index.js');
  assert.match(result.output, /target=document/);
});

test('does not treat JSX attribute names as browser-global references', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/index.tsx',
    'const Widget = (_props) => null;\nexport const view = <Widget location="preview" />;\n'
  );

  const result = await check(root);
  assert.equal(result.exitCode, 0, result.output);
});

test('applies browser-global purity to Vue script blocks', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  await writeFixtureFile(
    root,
    'packages/domains/demo/src/View.vue',
    '<template><main>Safe template text: window</main></template>\n<script setup lang="ts">\nconst width = window.innerWidth;\n</script>\n'
  );

  assertFailure(
    await check(root),
    'terminal-purity',
    '@namewta/domain-demo',
    'window',
    'packages/domains/demo/src/View.vue'
  );
});

test('rejects an activated workspace without every aggregate gate script', async () => {
  const root = await createFixture();
  await addPackage(root, 'packages/domains/demo', '@namewta/domain-demo');
  const manifest = fixtureManifests.get(root).get('packages/domains/demo');
  delete manifest.scripts.build;
  await writeJson(join(root, 'packages/domains/demo/package.json'), manifest);

  assertFailure(
    await check(root),
    'workspace-gate-script',
    '@namewta/domain-demo',
    'build',
    'packages/domains/demo/package.json#scripts'
  );
});
