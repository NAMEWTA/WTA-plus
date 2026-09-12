import { parse as parseVueSfc } from '@vue/compiler-sfc';
import { access, readdir, readFile } from 'node:fs/promises';
import { dirname, extname, join, relative, resolve, sep } from 'node:path';
import ts from 'typescript';
import { parse as parseYaml } from 'yaml';

const dependencyFields = ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies'];
const runtimeDependencyFields = new Set(['dependencies', 'peerDependencies', 'optionalDependencies']);
const requiredGateScripts = ['build', 'lint', 'test', 'typecheck'];
const browserGlobals = new Set([
  'globalThis',
  'window',
  'self',
  'document',
  'navigator',
  'location',
  'history',
  'screen',
  'visualViewport',
  'localStorage',
  'sessionStorage',
  'indexedDB',
  'caches',
  'cookieStore',
  'Storage',
  'IDBFactory',
  'CacheStorage',
  'DOMParser',
  'XMLSerializer',
  'Node',
  'Element',
  'HTMLElement',
  'Document',
  'DocumentFragment',
  'customElements',
  'CustomElementRegistry',
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
  'getComputedStyle'
]);
const sourceExtensions = new Set(['.cjs', '.js', '.jsx', '.mjs', '.ts', '.tsx', '.vue']);
const ignoredDirectories = new Set([
  '.git',
  '.output',
  '.vite',
  'coverage',
  'dist',
  'node_modules',
  'specdev-worktree'
]);
const requiredWorkspaceGlobs = ['.', 'apps/*', 'packages/*', 'packages/*/*', 'tooling/*'];
const sharedCatalogPackages = [
  '@types/node',
  '@vue/compiler-sfc',
  'axios',
  'crypto-js',
  'element-plus',
  'jsencrypt',
  'oxlint',
  'pinia',
  'typescript',
  'vue',
  'vue-router',
  'yaml'
];
const catalogReferenceExceptions = new Map([['@namewta/tooling-openapi\0devDependencies\0typescript', '5.9.3']]);
const inactivePlaceholders = [
  'apps/client-web',
  'apps/mobile-web',
  'apps/miniapp-taro',
  'packages/adapters/taro-request',
  'packages/adapters/taro-storage'
];
const retiredAdminNavigationSymbols = [
  'dynamicRoutes',
  'filterAsyncRouter',
  'filterDynamicRoutes',
  'loadView',
  'usePermissionStore'
];
const runtimeLayerAllowlist = {
  app: new Set(['adapter', 'domain', 'platform', 'web-domain', 'web-kit']),
  'web-domain': new Set(['domain', 'platform', 'web-kit']),
  domain: new Set(['api-contracts', 'domain', 'platform']),
  'web-kit': new Set(['platform', 'web-kit']),
  adapter: new Set(['platform']),
  platform: new Set(['platform']),
  'api-contracts': new Set(),
  tooling: new Set()
};

function toPosix(path) {
  return path.split(sep).join('/');
}

function isWithin(path, parent) {
  return path === parent || path.startsWith(`${parent}${sep}`);
}

async function exists(path) {
  try {
    await access(path);
    return true;
  } catch {
    return false;
  }
}

async function readJson(path) {
  return JSON.parse(await readFile(path, 'utf8'));
}

async function readYaml(path) {
  return parseYaml(await readFile(path, 'utf8'));
}

async function childDirectories(path) {
  if (!(await exists(path))) return [];
  return (await readdir(path, { withFileTypes: true }))
    .filter(entry => entry.isDirectory() && !entry.name.startsWith('.'))
    .map(entry => join(path, entry.name))
    .toSorted();
}

async function discoverManifestDirectories(root) {
  const directories = [root];
  const apps = await childDirectories(join(root, 'apps'));
  const packageGroups = await childDirectories(join(root, 'packages'));
  const tooling = await childDirectories(join(root, 'tooling'));
  directories.push(...apps, ...packageGroups, ...tooling);
  for (const group of packageGroups) directories.push(...(await childDirectories(group)));

  const manifests = [];
  for (const directory of new Set(directories.map(path => resolve(path)))) {
    const manifestPath = join(directory, 'package.json');
    if (await exists(manifestPath)) manifests.push({ directory, manifestPath });
  }
  return manifests;
}

function packageLayer(relativeDirectory) {
  if (relativeDirectory === '.' || relativeDirectory.startsWith('apps/')) return 'app';
  if (relativeDirectory.startsWith('packages/platform/')) return 'platform';
  if (relativeDirectory.startsWith('packages/domains/')) return 'domain';
  if (relativeDirectory.startsWith('packages/web-domains/')) return 'web-domain';
  if (relativeDirectory.startsWith('packages/web-kit/')) return 'web-kit';
  if (relativeDirectory.startsWith('packages/adapters/')) return 'adapter';
  if (relativeDirectory === 'packages/api-contracts') return 'api-contracts';
  if (relativeDirectory.startsWith('tooling/')) return 'tooling';
  return 'unknown';
}

function violation(rule, source, target, path, detail) {
  return { rule, source, target, path, detail };
}

function adminNavigationBoundaryViolation(sourcePath, source) {
  if (!sourcePath.startsWith('apps/admin-web/src/')) return undefined;
  if (sourcePath.startsWith('apps/admin-web/src/directive/permission/')) {
    return {
      target: '@namewta/web-kit-permission',
      detail: 'Admin permission directives must be installed from the Web Kit public entry'
    };
  }
  if (sourcePath.startsWith('apps/admin-web/src/store/modules/permission.')) {
    return {
      target: '@namewta/platform-app-runtime',
      detail: 'Admin navigation state must use the navigation Store and shared menu runtime'
    };
  }
  if (/\.directive\(\s*['"]has(?:Permi|Roles)['"]/.test(source)) {
    return {
      target: '@namewta/web-kit-permission',
      detail: 'Admin cannot register private permission directive implementations'
    };
  }
  if (/import\s*\.\s*meta\s*\.\s*glob\s*\(/.test(source) && source.includes('/views/')) {
    return {
      target: '@namewta/platform-app-runtime',
      detail: 'Admin dynamic pages must resolve through selected manifests instead of a local views glob'
    };
  }
  const retiredSymbol = retiredAdminNavigationSymbols.find(symbol => new RegExp(`\\b${symbol}\\b`).test(source));
  if (retiredSymbol) {
    return {
      target: '@namewta/platform-app-runtime',
      detail: `Retired Admin navigation symbol ${retiredSymbol} cannot return`
    };
  }
  return undefined;
}

function formatViolation(item) {
  return `[${item.rule}] source=${item.source} target=${item.target} path=${item.path} detail=${item.detail}`;
}

function fingerprint(item) {
  return `${item.rule}\u0000${item.source}\u0000${item.target}\u0000${item.path}\u0000${item.detail}`;
}

function internalPackageName(specifier) {
  if (!specifier.startsWith('@')) return specifier.split('/')[0];
  return specifier.split('/').slice(0, 2).join('/');
}

function exportedSubpath(manifest, specifier) {
  const suffix = specifier.slice(manifest.name.length);
  const requested = suffix ? `.${suffix}` : '.';
  const exportsField = manifest.exports;
  if (requested === '.') {
    if (typeof exportsField === 'string' || Array.isArray(exportsField)) return true;
    if (!exportsField || typeof exportsField !== 'object') return false;
    return '.' in exportsField || Object.keys(exportsField).some(key => !key.startsWith('.'));
  }
  if (!exportsField || typeof exportsField !== 'object' || Array.isArray(exportsField)) return false;
  return Object.keys(exportsField).some(key => {
    if (key === requested) return true;
    if (!key.includes('*')) return false;
    const [prefix, suffixPattern] = key.split('*');
    return requested.startsWith(prefix) && requested.endsWith(suffixPattern);
  });
}

function allowedInternalEdge(sourceLayer, targetLayer, field) {
  if (sourceLayer === 'tooling' || targetLayer === 'tooling') return field === 'devDependencies';
  return runtimeLayerAllowlist[sourceLayer]?.has(targetLayer) ?? false;
}

function terminalPackage(specifier) {
  const name = internalPackageName(specifier);
  if (['axios', 'crypto-js', 'element-plus', 'jsencrypt', 'pinia', 'vue', 'vue-router'].includes(name)) return name;
  if (name.startsWith('@element-plus/') || name.startsWith('@tarojs/')) return name;
  if (/^(?:axios|taro)(?:$|[-/])/.test(name)) return name;
  return undefined;
}

async function allFiles(directory) {
  if (!(await exists(directory))) return [];
  const result = [];
  async function visit(current) {
    for (const entry of await readdir(current, { withFileTypes: true })) {
      const path = join(current, entry.name);
      if (entry.isDirectory()) await visit(path);
      else if (entry.isFile()) result.push(path);
    }
  }
  await visit(directory);
  return result.toSorted();
}

async function sourceFiles(directory, excludedDirectories) {
  const result = [];
  async function visit(current) {
    for (const entry of await readdir(current, { withFileTypes: true })) {
      if (entry.name.startsWith('.') || ignoredDirectories.has(entry.name)) continue;
      const path = join(current, entry.name);
      if (entry.isDirectory() && !excludedDirectories.has(path)) await visit(path);
      else if (entry.isFile() && sourceExtensions.has(extname(entry.name))) result.push(path);
    }
  }
  await visit(directory);
  return result.toSorted();
}

function scriptKind(path, language) {
  if (language === 'tsx' || path.endsWith('.tsx')) return ts.ScriptKind.TSX;
  if (language === 'jsx' || path.endsWith('.jsx')) return ts.ScriptKind.JSX;
  if (language === 'js' || path.endsWith('.js') || path.endsWith('.mjs') || path.endsWith('.cjs'))
    return ts.ScriptKind.JS;
  return ts.ScriptKind.TS;
}

function collectAstImports(source, fileName, language) {
  const sourceFile = ts.createSourceFile(
    fileName,
    source,
    ts.ScriptTarget.Latest,
    true,
    scriptKind(fileName, language)
  );
  const specifiers = new Set();
  const globals = new Set();
  const declarationIdentifiers = new Set();
  const nodeScopes = new Map();
  const rootScope = {
    parent: undefined,
    type: 'source',
    typeDeclarations: new Set(),
    valueDeclarations: new Set()
  };

  function bindingIdentifiers(name, callback) {
    if (ts.isIdentifier(name)) {
      declarationIdentifiers.add(name);
      callback(name.text);
      return;
    }
    for (const element of name.elements ?? []) {
      if (ts.isBindingElement(element)) bindingIdentifiers(element.name, callback);
    }
  }

  function nearestFunctionScope(scope) {
    let current = scope;
    while (current.parent && !['function', 'module', 'source', 'static-block'].includes(current.type))
      current = current.parent;
    return current;
  }

  function scopeType(node) {
    if (ts.isSourceFile(node)) return 'source';
    if (ts.isFunctionLike(node)) return 'function';
    if (ts.isClassDeclaration(node) || ts.isClassExpression(node)) return 'class';
    if (ts.isInterfaceDeclaration(node) || ts.isTypeAliasDeclaration(node) || ts.isMappedTypeNode(node)) return 'type';
    if (ts.isModuleBlock(node)) return 'module';
    if (ts.isClassStaticBlockDeclaration(node)) return 'static-block';
    if (ts.isCatchClause(node)) return 'catch';
    if (
      ts.isBlock(node) ||
      ts.isCaseBlock(node) ||
      ts.isForStatement(node) ||
      ts.isForInStatement(node) ||
      ts.isForOfStatement(node)
    )
      return 'block';
    return undefined;
  }

  function isAmbient(node) {
    let current = node;
    while (current) {
      if (current.flags & ts.NodeFlags.Ambient) return true;
      current = current.parent;
    }
    return false;
  }

  function declare(scope, name, namespaces = ['value']) {
    bindingIdentifiers(name, identifier => {
      if (namespaces.includes('type')) scope.typeDeclarations.add(identifier);
      if (namespaces.includes('value')) scope.valueDeclarations.add(identifier);
    });
  }

  function declareImports(node, scope) {
    if (!ts.isImportDeclaration(node) || !node.importClause) return;
    const clauseNamespaces = node.importClause.isTypeOnly ? ['type'] : ['type', 'value'];
    if (node.importClause.name) declare(scope, node.importClause.name, clauseNamespaces);
    const bindings = node.importClause.namedBindings;
    if (bindings && ts.isNamespaceImport(bindings)) declare(scope, bindings.name, clauseNamespaces);
    if (bindings && ts.isNamedImports(bindings)) {
      for (const element of bindings.elements)
        declare(scope, element.name, element.isTypeOnly ? ['type'] : clauseNamespaces);
    }
  }

  function buildScopes(node, inheritedScope) {
    const type = scopeType(node);
    const parentScope = inheritedScope;
    const scope = ts.isSourceFile(node)
      ? rootScope
      : type
        ? {
            parent: inheritedScope,
            type,
            typeDeclarations: new Set(),
            valueDeclarations: new Set()
          }
        : inheritedScope;
    nodeScopes.set(node, scope);

    const ambient = isAmbient(node);
    if (ts.isFunctionDeclaration(node) && node.name && !ambient) {
      declare(parentScope, node.name);
    } else if (ts.isFunctionExpression(node) && node.name) {
      declare(scope, node.name);
    } else if (ts.isClassDeclaration(node) && node.name) {
      const namespaces = ambient ? ['type'] : ['type', 'value'];
      declare(parentScope, node.name, namespaces);
      declare(scope, node.name, namespaces);
    } else if (ts.isClassExpression(node) && node.name) {
      declare(scope, node.name, ['type', 'value']);
    } else if (ts.isEnumDeclaration(node)) {
      declare(scope, node.name, ambient ? ['type'] : ['type', 'value']);
    } else if (ts.isModuleDeclaration(node) && ts.isIdentifier(node.name)) {
      declare(scope, node.name, ambient ? ['type'] : ['type', 'value']);
    } else if (ts.isImportEqualsDeclaration(node)) {
      declare(scope, node.name, node.isTypeOnly ? ['type'] : ['type', 'value']);
    } else if (ts.isInterfaceDeclaration(node) || ts.isTypeAliasDeclaration(node)) {
      declare(parentScope, node.name, ['type']);
    }

    if (ts.isTypeParameterDeclaration(node) && !ts.isInferTypeNode(node.parent)) declare(scope, node.name, ['type']);
    if (ts.isFunctionLike(node)) {
      for (const parameter of node.parameters) declare(scope, parameter.name);
    }
    for (const parameter of node.typeParameters ?? []) declare(scope, parameter.name, ['type']);
    if (ts.isCatchClause(node) && node.variableDeclaration) declare(scope, node.variableDeclaration.name);
    if (ts.isVariableDeclaration(node) && !ambient) {
      const declarationList = ts.isVariableDeclarationList(node.parent) ? node.parent : undefined;
      const declarationScope =
        declarationList && !(declarationList.flags & ts.NodeFlags.BlockScoped) ? nearestFunctionScope(scope) : scope;
      declare(declarationScope, node.name);
    }
    declareImports(node, scope);
    if (ts.isConditionalTypeNode(node)) {
      const trueScope = {
        parent: scope,
        type: 'type',
        typeDeclarations: new Set(),
        valueDeclarations: new Set()
      };
      function declareInferredTypes(current) {
        if (ts.isInferTypeNode(current)) declare(trueScope, current.typeParameter.name, ['type']);
        ts.forEachChild(current, declareInferredTypes);
      }
      declareInferredTypes(node.extendsType);
      buildScopes(node.checkType, scope);
      buildScopes(node.extendsType, scope);
      buildScopes(node.trueType, trueScope);
      buildScopes(node.falseType, scope);
      return;
    }
    ts.forEachChild(node, child => buildScopes(child, scope));
  }

  function isReferenceIdentifier(node) {
    if (declarationIdentifiers.has(node)) return false;
    if (ts.isDeclarationName(node)) return false;
    const parent = node.parent;
    if (ts.isPropertyAccessExpression(parent) && parent.name === node) return false;
    if (ts.isQualifiedName(parent) && parent.right === node) return false;
    if (
      ((ts.isPropertyAssignment(parent) ||
        ts.isMethodDeclaration(parent) ||
        ts.isPropertyDeclaration(parent) ||
        ts.isPropertySignature(parent) ||
        ts.isMethodSignature(parent) ||
        ts.isGetAccessorDeclaration(parent) ||
        ts.isSetAccessorDeclaration(parent) ||
        ts.isEnumMember(parent)) &&
        parent.name === node) ||
      (ts.isBindingElement(parent) && parent.propertyName === node)
    )
      return false;
    if (ts.isImportSpecifier(parent) || ts.isExportSpecifier(parent) || ts.isLabeledStatement(parent)) return false;
    if (ts.isJsxAttribute(parent) && parent.name === node) return false;
    if ((ts.isBreakStatement(parent) || ts.isContinueStatement(parent)) && parent.label === node) return false;
    return true;
  }

  function referenceNamespace(node) {
    let current = node;
    while (current.parent && ts.isPartOfTypeNode(current)) {
      if (ts.isTypeQueryNode(current.parent)) return 'value';
      current = current.parent;
    }
    return ts.isPartOfTypeNode(node) ? 'type' : 'value';
  }

  function isDeclared(name, scope, namespace = 'value') {
    let current = scope;
    while (current) {
      if (current[`${namespace}Declarations`].has(name)) return true;
      current = current.parent;
    }
    return false;
  }

  function add(node) {
    if (node && ts.isStringLiteralLike(node)) specifiers.add(node.text);
  }
  function visit(node) {
    if (
      ts.isPropertyAccessExpression(node) &&
      ts.isIdentifier(node.expression) &&
      node.expression.text === 'globalThis' &&
      browserGlobals.has(node.name.text) &&
      !isDeclared('globalThis', nodeScopes.get(node))
    )
      globals.add(node.name.text);
    if (
      ts.isElementAccessExpression(node) &&
      ts.isIdentifier(node.expression) &&
      node.expression.text === 'globalThis' &&
      ts.isStringLiteralLike(node.argumentExpression) &&
      browserGlobals.has(node.argumentExpression.text) &&
      !isDeclared('globalThis', nodeScopes.get(node))
    )
      globals.add(node.argumentExpression.text);
    if (
      ts.isIdentifier(node) &&
      browserGlobals.has(node.text) &&
      isReferenceIdentifier(node) &&
      !isDeclared(node.text, nodeScopes.get(node), referenceNamespace(node))
    )
      globals.add(node.text);
    if (ts.isImportDeclaration(node) || ts.isExportDeclaration(node)) add(node.moduleSpecifier);
    if (ts.isImportEqualsDeclaration(node) && ts.isExternalModuleReference(node.moduleReference))
      add(node.moduleReference.expression);
    if (ts.isCallExpression(node) && node.arguments.length === 1) {
      const dynamicImport = node.expression.kind === ts.SyntaxKind.ImportKeyword;
      const commonJsRequire = ts.isIdentifier(node.expression) && node.expression.text === 'require';
      if (dynamicImport || commonJsRequire) add(node.arguments[0]);
    }
    ts.forEachChild(node, visit);
  }
  buildScopes(sourceFile, rootScope);
  visit(sourceFile);
  return {
    browserGlobals: globals,
    specifiers,
    errors: sourceFile.parseDiagnostics.map(item => ts.flattenDiagnosticMessageText(item.messageText, ' '))
  };
}

function parsedImports(source, fileName) {
  if (!fileName.endsWith('.vue')) return collectAstImports(source, fileName);
  const result = { browserGlobals: new Set(), specifiers: new Set(), errors: [] };
  const parsed = parseVueSfc(source, { filename: fileName });
  for (const error of parsed.errors) result.errors.push(error instanceof Error ? error.message : String(error));
  for (const block of [parsed.descriptor.script, parsed.descriptor.scriptSetup].filter(Boolean)) {
    const blockResult = collectAstImports(block.content, fileName, block.lang);
    for (const globalName of blockResult.browserGlobals) result.browserGlobals.add(globalName);
    for (const specifier of blockResult.specifiers) result.specifiers.add(specifier);
    result.errors.push(...blockResult.errors);
  }
  return result;
}

function findCycles(packagesByName, graph) {
  const cycles = [];
  const state = new Map();
  const stack = [];
  const seen = new Set();
  function visit(name) {
    state.set(name, 'visiting');
    stack.push(name);
    for (const target of [...(graph.get(name) ?? [])].toSorted()) {
      if (!packagesByName.has(target)) continue;
      if (!state.has(target)) visit(target);
      else if (state.get(target) === 'visiting') {
        const nodes = stack.slice(stack.indexOf(target));
        const rotations = nodes.map((_, index) => [...nodes.slice(index), ...nodes.slice(0, index)]);
        const sorted = rotations.toSorted((a, b) => a.join('\u0000').localeCompare(b.join('\u0000')));
        const canonical = [...sorted[0], sorted[0][0]];
        const key = canonical.join('\u0000');
        if (!seen.has(key)) {
          seen.add(key);
          cycles.push(canonical);
        }
      }
    }
    stack.pop();
    state.set(name, 'visited');
  }
  for (const name of [...packagesByName.keys()].toSorted()) if (!state.has(name)) visit(name);
  return cycles;
}

function baselineEntry(entry) {
  return {
    rule: String(entry.rule ?? ''),
    source: String(entry.source ?? ''),
    target: String(entry.target ?? ''),
    path: String(entry.path ?? ''),
    detail: String(entry.detail ?? '')
  };
}

function normalizedImporterPath(path) {
  if (path === '.' || path === '') return '.';
  return path.replace(/^\.\//, '').replace(/\/$/, '');
}

async function validatePnpmState(root, packages) {
  const detected = [];
  let workspace;
  let lockfile;
  try {
    workspace = await readYaml(join(root, 'pnpm-workspace.yaml'));
  } catch (error) {
    detected.push(
      violation(
        'workspace-config',
        'pnpm-workspace.yaml',
        'structured YAML',
        'pnpm-workspace.yaml',
        error instanceof Error ? error.message : String(error)
      )
    );
  }
  try {
    lockfile = await readYaml(join(root, 'pnpm-lock.yaml'));
  } catch (error) {
    detected.push(
      violation(
        'lockfile-parity',
        'pnpm-lock.yaml',
        'structured YAML',
        'pnpm-lock.yaml',
        error instanceof Error ? error.message : String(error)
      )
    );
  }

  const actualGlobs = Array.isArray(workspace?.packages) ? workspace.packages.map(String).toSorted() : [];
  if (JSON.stringify(actualGlobs) !== JSON.stringify(requiredWorkspaceGlobs.toSorted())) {
    detected.push(
      violation(
        'workspace-config',
        'pnpm-workspace.yaml',
        'required workspace globs',
        'pnpm-workspace.yaml',
        `expected=${requiredWorkspaceGlobs.join(',')} actual=${actualGlobs.join(',')}`
      )
    );
  }
  const catalog = workspace?.catalog && typeof workspace.catalog === 'object' ? workspace.catalog : {};
  for (const name of sharedCatalogPackages) {
    if (!(name in catalog))
      detected.push(
        violation(
          'workspace-config',
          'pnpm-workspace.yaml',
          name,
          'pnpm-workspace.yaml#catalog',
          `Missing shared catalog entry ${name}`
        )
      );
  }

  for (const item of packages) {
    const sourceName = item.manifest.name ?? item.relativeDirectory;
    for (const field of dependencyFields) {
      for (const [targetName, specification] of Object.entries(item.manifest[field] ?? {})) {
        const exception = catalogReferenceExceptions.get(`${sourceName}\0${field}\0${targetName}`);
        if (sharedCatalogPackages.includes(targetName) && specification !== 'catalog:' && specification !== exception) {
          detected.push(
            violation(
              'catalog-reference',
              sourceName,
              targetName,
              `${item.relativeDirectory}/package.json#${field}`,
              `Shared dependency must use catalog:; received ${specification}`
            )
          );
        }
      }
    }
  }

  if (!lockfile || typeof lockfile !== 'object') return detected;
  if (String(lockfile.lockfileVersion) !== '9.0') {
    detected.push(
      violation(
        'lockfile-parity',
        'pnpm-lock.yaml',
        'lockfileVersion 9.0',
        'pnpm-lock.yaml#lockfileVersion',
        `received=${lockfile.lockfileVersion}`
      )
    );
  }
  const lockCatalog = lockfile.catalogs?.default ?? {};
  for (const name of sharedCatalogPackages) {
    if (name in catalog && lockCatalog[name]?.specifier !== catalog[name]) {
      detected.push(
        violation(
          'lockfile-parity',
          'pnpm-lock.yaml',
          name,
          'pnpm-lock.yaml#catalogs.default',
          `expected=${catalog[name]} actual=${lockCatalog[name]?.specifier ?? 'missing'}`
        )
      );
    }
  }

  const importers = lockfile.importers && typeof lockfile.importers === 'object' ? lockfile.importers : {};
  const activePaths = new Set(packages.map(item => item.relativeDirectory));
  for (const item of packages) {
    const importer = importers[item.relativeDirectory];
    const sourceName = item.manifest.name ?? item.relativeDirectory;
    if (!importer) {
      detected.push(
        violation(
          'lockfile-parity',
          sourceName,
          'active importer',
          'pnpm-lock.yaml#importers',
          `Missing importer ${item.relativeDirectory}`
        )
      );
      continue;
    }
    for (const field of dependencyFields) {
      const manifestDependencies = item.manifest[field] ?? {};
      const lockDependencies = importer[field] ?? {};
      for (const [targetName, specification] of Object.entries(manifestDependencies)) {
        if (lockDependencies[targetName]?.specifier !== specification) {
          detected.push(
            violation(
              'lockfile-parity',
              sourceName,
              targetName,
              `pnpm-lock.yaml#importers.${item.relativeDirectory}.${field}`,
              `expected=${specification} actual=${lockDependencies[targetName]?.specifier ?? 'missing'}`
            )
          );
        }
      }
      for (const targetName of Object.keys(lockDependencies)) {
        if (!Object.hasOwn(manifestDependencies, targetName)) {
          detected.push(
            violation(
              'lockfile-parity',
              sourceName,
              targetName,
              `pnpm-lock.yaml#importers.${item.relativeDirectory}.${field}`,
              'Lock importer contains a dependency absent from package.json'
            )
          );
        }
      }
    }
  }
  for (const importerPath of Object.keys(importers)) {
    const normalized = normalizedImporterPath(importerPath);
    if (!activePaths.has(normalized))
      detected.push(
        violation(
          'lockfile-parity',
          normalized,
          'active workspace importer',
          'pnpm-lock.yaml#importers',
          'Stale or inactive importer'
        )
      );
  }
  for (const placeholder of inactivePlaceholders) {
    if (Object.keys(importers).some(path => normalizedImporterPath(path) === placeholder)) {
      detected.push(
        violation(
          'lockfile-parity',
          placeholder,
          'no inactive importer',
          'pnpm-lock.yaml#importers',
          'Inactive placeholder has a lock importer'
        )
      );
    }
  }
  return detected;
}

export async function inspectWorkspace({ root }) {
  const absoluteRoot = resolve(root);
  const detected = [];
  const manifestLocations = await discoverManifestDirectories(absoluteRoot);
  const packages = [];
  const packagesByName = new Map();

  for (const location of manifestLocations) {
    const relativeDirectory = toPosix(relative(absoluteRoot, location.directory)) || '.';
    let manifest;
    try {
      manifest = await readJson(location.manifestPath);
    } catch (error) {
      detected.push(
        violation(
          'manifest-validity',
          relativeDirectory,
          'valid JSON',
          `${relativeDirectory}/package.json`,
          error instanceof Error ? error.message : String(error)
        )
      );
      continue;
    }
    const item = { directory: location.directory, relativeDirectory, manifest, layer: packageLayer(relativeDirectory) };
    packages.push(item);
    if (typeof manifest.name !== 'string' || !manifest.name) {
      detected.push(
        violation(
          'manifest-name',
          relativeDirectory,
          'unique package name',
          `${relativeDirectory}/package.json`,
          'Package name is required'
        )
      );
      continue;
    }
    if (packagesByName.has(manifest.name)) {
      detected.push(
        violation(
          'manifest-name',
          manifest.name,
          packagesByName.get(manifest.name).relativeDirectory,
          `${relativeDirectory}/package.json`,
          'Package name must be unique'
        )
      );
    } else packagesByName.set(manifest.name, item);
  }

  for (const placeholder of inactivePlaceholders) {
    const directory = join(absoluteRoot, placeholder);
    const files = await allFiles(directory);
    const manifestPath = join(directory, 'package.json');
    if (await exists(manifestPath)) {
      let name = placeholder;
      try {
        name = (await readJson(manifestPath)).name ?? placeholder;
      } catch {
        // Manifest validity is reported by discovery.
      }
      detected.push(
        violation(
          'placeholder-activation',
          name,
          'inactive placeholder',
          `${placeholder}/package.json`,
          'Inactive App or Taro adapter cannot contain a manifest'
        )
      );
    }
    for (const file of files) {
      const relativeFile = toPosix(relative(absoluteRoot, file));
      if (relativeFile === `${placeholder}/README.md`) continue;
      detected.push(
        violation(
          'inactive-placeholder-content',
          placeholder,
          'README-only placeholder',
          relativeFile,
          `Unexpected inactive asset ${relativeFile}`
        )
      );
    }
  }

  for (const item of packages) {
    if (item.layer !== 'app' || item.relativeDirectory === '.') continue;
    const apiDirectory = join(item.directory, 'src/api');
    for (const file of await allFiles(apiDirectory)) {
      const sourcePath = toPosix(relative(absoluteRoot, file));
      detected.push(
        violation(
          'app-api-facade',
          item.manifest.name ?? item.relativeDirectory,
          'domain public service',
          sourcePath,
          'Activated Apps must compose domain services instead of owning a duplicated src/api facade'
        )
      );
    }
  }

  const graph = new Map();
  for (const item of packages) {
    const name = item.manifest.name ?? item.relativeDirectory;
    graph.set(name, new Set());
    if (item.layer === 'unknown')
      detected.push(
        violation(
          'package-layout',
          name,
          'recognized workspace layer',
          `${item.relativeDirectory}/package.json`,
          'Activated package is outside the supported package layout'
        )
      );
    if (item.manifest.private !== true)
      detected.push(
        violation(
          'package-private',
          name,
          'private workspace package',
          `${item.relativeDirectory}/package.json`,
          'Activated packages, including root, must set private=true'
        )
      );
    if (item.layer !== 'app' && !item.manifest.exports)
      detected.push(
        violation(
          'public-entry',
          name,
          'declared exports',
          `${item.relativeDirectory}/package.json#exports`,
          'Non-App packages must declare public exports'
        )
      );
    for (const script of requiredGateScripts) {
      if (typeof item.manifest.scripts?.[script] !== 'string' || item.manifest.scripts[script].trim() === '') {
        detected.push(
          violation(
            'workspace-gate-script',
            name,
            script,
            `${item.relativeDirectory}/package.json#scripts`,
            `Activated workspace must define a non-empty ${script} script`
          )
        );
      }
    }

    for (const field of dependencyFields) {
      for (const [targetName, specification] of Object.entries(item.manifest[field] ?? {})) {
        const terminal = terminalPackage(targetName);
        if (terminal && ['domain', 'platform'].includes(item.layer) && runtimeDependencyFields.has(field)) {
          detected.push(
            violation(
              'terminal-purity',
              name,
              terminal,
              `${item.relativeDirectory}/package.json#${field}`,
              `Terminal dependency ${targetName} is forbidden in ${item.layer}`
            )
          );
        }
        const target = packagesByName.get(targetName);
        if (!target) continue;
        graph.get(name).add(targetName);
        const dependencyPath = `${item.relativeDirectory}/package.json#${field}`;
        if (specification !== 'workspace:*')
          detected.push(
            violation(
              'workspace-reference',
              name,
              targetName,
              dependencyPath,
              `Internal dependency must use workspace:*; received ${specification}`
            )
          );
        if (!allowedInternalEdge(item.layer, target.layer, field))
          detected.push(
            violation(
              'dependency-direction',
              name,
              targetName,
              dependencyPath,
              `field=${field} ${item.layer}->${target.layer} is not allowed`
            )
          );
      }
    }
  }

  for (const cycle of findCycles(packagesByName, graph))
    detected.push(
      violation(
        'dependency-cycle',
        cycle[0],
        cycle[0],
        'dependency graph',
        `Internal package cycle: ${cycle.join(' -> ')}`
      )
    );

  const packagesByDepth = packages.toSorted((a, b) => b.directory.length - a.directory.length);
  for (const item of packages) {
    const sourceName = item.manifest.name ?? item.relativeDirectory;
    const excluded = new Set(
      packages
        .filter(candidate => candidate.directory.startsWith(`${item.directory}${sep}`))
        .map(candidate => candidate.directory)
    );
    if (item.relativeDirectory === '.')
      for (const placeholder of inactivePlaceholders) excluded.add(join(absoluteRoot, placeholder));
    for (const file of await sourceFiles(item.directory, excluded)) {
      const sourcePath = toPosix(relative(absoluteRoot, file));
      const source = await readFile(file, 'utf8');
      const parsed = parsedImports(source, sourcePath);
      const retiredNavigation = adminNavigationBoundaryViolation(sourcePath, source);
      if (retiredNavigation) {
        detected.push(
          violation(
            'admin-navigation-boundary',
            sourceName,
            retiredNavigation.target,
            sourcePath,
            retiredNavigation.detail
          )
        );
      }
      for (const error of parsed.errors)
        detected.push(violation('source-parse', sourceName, 'valid source AST', sourcePath, error));
      if (['domain', 'platform'].includes(item.layer)) {
        for (const globalName of parsed.browserGlobals) {
          detected.push(
            violation(
              'terminal-purity',
              sourceName,
              globalName,
              sourcePath,
              `Unshadowed browser global ${globalName} is forbidden in ${item.layer}`
            )
          );
        }
      }
      for (const specifier of parsed.specifiers) {
        const terminal = terminalPackage(specifier);
        if (terminal && ['domain', 'platform'].includes(item.layer))
          detected.push(
            violation(
              'terminal-purity',
              sourceName,
              terminal,
              sourcePath,
              `Terminal import ${specifier} is forbidden in ${item.layer}`
            )
          );
        if (specifier.startsWith('.')) {
          const importedPath = resolve(dirname(file), specifier);
          const target = packagesByDepth.find(candidate => isWithin(importedPath, candidate.directory));
          if (target && target !== item)
            detected.push(
              violation(
                'cross-workspace-relative-import',
                sourceName,
                target.manifest.name ?? target.relativeDirectory,
                sourcePath,
                `specifier=${specifier}`
              )
            );
          continue;
        }
        const targetName = internalPackageName(specifier);
        const target = packagesByName.get(targetName);
        if (!target) continue;
        const declared = dependencyFields.some(field => Object.hasOwn(item.manifest[field] ?? {}, targetName));
        if (targetName !== sourceName && !declared)
          detected.push(
            violation(
              'internal-dependency-declaration',
              sourceName,
              targetName,
              sourcePath,
              `specifier=${specifier} requires a workspace:* dependency declaration`
            )
          );
        const sourceField = /(?:^|\/)(?:test|tests|__tests__)(?:\/|$)|\.(?:spec|test)\.[^.]+$/.test(sourcePath)
          ? 'devDependencies'
          : 'dependencies';
        if (targetName !== sourceName && !allowedInternalEdge(item.layer, target.layer, sourceField))
          detected.push(
            violation(
              'dependency-direction',
              sourceName,
              targetName,
              sourcePath,
              `specifier=${specifier} ${item.layer}->${target.layer} is not allowed`
            )
          );
        if (!exportedSubpath(target.manifest, specifier))
          detected.push(
            violation(
              'public-entry',
              sourceName,
              targetName,
              sourcePath,
              `specifier=${specifier} is not exported by ${targetName}`
            )
          );
      }
    }
  }

  detected.push(...(await validatePnpmState(absoluteRoot, packages)));
  return { root: absoluteRoot, packages, detected };
}

export async function createBaseline({ root }) {
  const inspection = await inspectWorkspace({ root });
  const entries = inspection.detected
    .map(baselineEntry)
    .toSorted((a, b) => fingerprint(a).localeCompare(fingerprint(b)));
  return { schemaVersion: 1, maximumViolations: entries.length, violations: entries };
}

export async function runArchitectureCheck({ root }) {
  const inspection = await inspectWorkspace({ root });
  const failures = [];
  let baseline;
  try {
    baseline = await readJson(join(inspection.root, 'tooling/architecture/baseline.json'));
  } catch (error) {
    failures.push(
      violation(
        'baseline-integrity',
        'tooling/architecture/baseline.json',
        'readable explicit baseline',
        'tooling/architecture/baseline.json',
        error instanceof Error ? error.message : String(error)
      )
    );
    baseline = { schemaVersion: 1, maximumViolations: 0, violations: [] };
  }
  const entries = Array.isArray(baseline.violations) ? baseline.violations.map(baselineEntry) : [];
  if (baseline.schemaVersion !== 1 || !Number.isInteger(baseline.maximumViolations) || baseline.maximumViolations < 0)
    failures.push(
      violation(
        'baseline-integrity',
        'tooling/architecture/baseline.json',
        'valid metadata',
        'tooling/architecture/baseline.json',
        'Expected schemaVersion=1 and non-negative integer maximumViolations'
      )
    );
  if (!Array.isArray(baseline.violations))
    failures.push(
      violation(
        'baseline-integrity',
        'tooling/architecture/baseline.json',
        'violations array',
        'tooling/architecture/baseline.json',
        'Baseline violations must be an array'
      )
    );
  else if (entries.length > baseline.maximumViolations)
    failures.push(
      violation(
        'baseline-growth',
        'tooling/architecture/baseline.json',
        'reviewed baseline',
        'tooling/architecture/baseline.json',
        `entries=${entries.length} maximum=${baseline.maximumViolations}`
      )
    );

  const baselineCounts = new Map();
  const actualCounts = new Map();
  for (const entry of entries)
    baselineCounts.set(fingerprint(entry), (baselineCounts.get(fingerprint(entry)) ?? 0) + 1);
  for (const item of inspection.detected)
    actualCounts.set(fingerprint(item), (actualCounts.get(fingerprint(item)) ?? 0) + 1);
  for (const item of inspection.detected) {
    const key = fingerprint(item);
    const remaining = baselineCounts.get(key) ?? 0;
    if (remaining > 0) baselineCounts.set(key, remaining - 1);
    else failures.push(item);
  }
  for (const entry of entries) {
    const key = fingerprint(entry);
    const remaining = actualCounts.get(key) ?? 0;
    if (remaining > 0) actualCounts.set(key, remaining - 1);
    else
      failures.push(
        violation(
          'baseline-stale',
          entry.source,
          entry.target,
          entry.path,
          `Remove resolved ${entry.rule}: ${entry.detail}`
        )
      );
  }

  const sortedFailures = failures.toSorted((a, b) => fingerprint(a).localeCompare(fingerprint(b)));
  const summary = sortedFailures.length
    ? `Architecture check failed: ${sortedFailures.length} violation(s).`
    : `Architecture check passed: ${inspection.packages.length} workspace package(s), ${entries.length} reviewed baseline violation(s).`;
  return {
    exitCode: sortedFailures.length ? 1 : 0,
    output: [...sortedFailures.map(formatViolation), summary].join('\n'),
    packages: inspection.packages.map(item => ({ name: item.manifest.name, path: item.relativeDirectory })),
    violations: sortedFailures
  };
}
