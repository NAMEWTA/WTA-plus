import { inspectWorkspace } from '@namewta/architecture';
import { mkdtemp, mkdir, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';

const fixtures: string[] = [];
const gateScripts = { build: 'true', lint: 'true', test: 'true', typecheck: 'true' };

async function fixtureRoot() {
  const root = await mkdtemp(join(tmpdir(), 'namewta-t05-'));
  fixtures.push(root);
  await mkdir(join(root, 'packages/domains/demo/src'), { recursive: true });
  await writeFile(
    join(root, 'package.json'),
    JSON.stringify({ name: 'fixture-app', private: true, scripts: gateScripts }, null, 2)
  );
  await writeFile(
    join(root, 'pnpm-workspace.yaml'),
    "packages:\n  - '.'\n  - 'apps/*'\n  - 'packages/*'\n  - 'packages/*/*'\n  - 'tooling/*'\n"
  );
  await writeFile(join(root, 'pnpm-lock.yaml'), "lockfileVersion: '9.0'\nimporters:\n  .: {}\n");
  return root;
}

afterEach(async () => {
  await Promise.all(fixtures.splice(0).map(path => rm(path, { recursive: true, force: true })));
});

describe('T-05 architecture reverse fixtures', () => {
  it('rejects a headless domain that imports Vue', async () => {
    const root = await fixtureRoot();
    await writeFile(
      join(root, 'packages/domains/demo/package.json'),
      JSON.stringify(
        {
          name: '@fixture/domain-demo',
          private: true,
          exports: { '.': './src/index.ts' },
          scripts: gateScripts,
          dependencies: { vue: 'catalog:' }
        },
        null,
        2
      )
    );
    await writeFile(join(root, 'packages/domains/demo/src/index.ts'), "import { ref } from 'vue';\nexport { ref };\n");

    const result = await inspectWorkspace({ root });

    expect(result.detected).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          rule: 'terminal-purity',
          source: '@fixture/domain-demo',
          target: 'vue'
        })
      ])
    );
  });

  it('rejects an import outside another package public exports', async () => {
    const root = await fixtureRoot();
    await mkdir(join(root, 'packages/platform/example/src'), { recursive: true });
    await writeFile(
      join(root, 'packages/domains/demo/package.json'),
      JSON.stringify(
        {
          name: '@fixture/domain-demo',
          private: true,
          exports: { '.': './src/index.ts' },
          scripts: gateScripts,
          dependencies: { '@fixture/platform-example': 'workspace:*' }
        },
        null,
        2
      )
    );
    await writeFile(
      join(root, 'packages/platform/example/package.json'),
      JSON.stringify(
        {
          name: '@fixture/platform-example',
          private: true,
          exports: { '.': './src/index.ts' },
          scripts: gateScripts
        },
        null,
        2
      )
    );
    await writeFile(join(root, 'packages/domains/demo/src/index.ts'), "import '@fixture/platform-example/internal';\n");
    await writeFile(join(root, 'packages/platform/example/src/index.ts'), 'export {};\n');

    const result = await inspectWorkspace({ root });

    expect(result.detected).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          rule: 'public-entry',
          source: '@fixture/domain-demo',
          target: '@fixture/platform-example',
          detail: expect.stringContaining('is not exported')
        })
      ])
    );
  });
});
