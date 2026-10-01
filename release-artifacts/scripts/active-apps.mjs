// The shipped registry is build inventory. Enabled Apps are the sealed deployment selection.
import fs from 'node:fs';
import path from 'node:path';

export const ACTIVE_FRONTEND_COMPOSE = 'docker/docker-compose-frontend-active.yml';

export function enabledApps(registered, value) {
  const ids = registered.map((app) => app.id);
  const requested = value === undefined ? ids : value.split(',').map((id) => id.trim());
  if (requested.length === 0 || requested.some((id) => !ids.includes(id))
      || new Set(requested).size !== requested.length) {
    throw new Error('NAMEWTA_ENABLED_APPS must contain unique shipped App IDs');
  }
  if (!requested.includes('admin-web')) throw new Error('NAMEWTA_ENABLED_APPS must include admin-web');
  return ids.filter((id) => requested.includes(id));
}

function escaped(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** Render only repository-owned block layouts; inventory validation runs before this function. */
export function activeFrontendFiles(release, registered, enabled) {
  const disabled = registered.filter((app) => !enabled.includes(app.id));
  let compose = fs.readFileSync(path.join(release, 'docker/docker-compose-frontend.yml'), 'utf8');
  for (const app of disabled) {
    for (const endpoint of [app, ...(app.tls ? [app.tls] : [])]) {
      compose = compose.replace(new RegExp(`^  ${escaped(endpoint.composeService)}:\\n[\\s\\S]*?(?=^\\S|^  \\S|(?![\\s\\S]))`, 'm'), '');
    }
    compose = compose.split('\n').filter((line) => !line.includes(`APP_${app.prefixEnv}:`)).join('\n');
  }
  const files = {};
  for (const protocol of ['http', 'tls']) {
    const source = `docker/frontend/nginx/lb/nginx-lb-${protocol}.conf.template`;
    const destination = source.replace('.conf.template', '-active.conf.template');
    let template = fs.readFileSync(path.join(release, source), 'utf8');
    for (const app of disabled) {
      const upstreams = [];
      template = template.replace(/^upstream ([\w-]+) \{\n[\s\S]*?^\}\n/gm, (block, name) => {
        if (!block.includes(`server ${app.composeService}:`)) return block;
        upstreams.push(name);
        return '';
      });
      template = template.replace(/^    location [^\n]+ \{\n[\s\S]*?^    \}\n/gm, (block) => {
        const ownsRoute = block.includes(`\${APP_${app.prefixEnv}}`)
          || upstreams.some((name) => new RegExp(`proxy_pass http://${escaped(name)}(?:/|;)`).test(block));
        return ownsRoute ? '' : block;
      });
      if (template.includes(`\${APP_${app.prefixEnv}}`) || template.includes(app.composeService)
          || upstreams.some((name) => new RegExp(`proxy_pass http://${escaped(name)}(?:/|;)`).test(template))) {
        throw new Error(`unsupported Nginx block layout for disabled App: ${app.id}`);
      }
    }
    files[destination] = template;
    compose = compose.replaceAll(source.slice('docker/'.length), destination.slice('docker/'.length));
  }
  for (const app of disabled) {
    if (compose.includes(app.composeService) || compose.includes(`\${${app.prefixEnv}`)
        || compose.includes(`\${${app.originEnv}`)) {
      throw new Error(`unsupported Compose block layout for disabled App: ${app.id}`);
    }
  }
  files[ACTIVE_FRONTEND_COMPOSE] = compose;
  return files;
}
