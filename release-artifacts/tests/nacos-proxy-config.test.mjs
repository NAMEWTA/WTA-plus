import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const releaseRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const templateRoot = path.join(releaseRoot, 'docker/frontend/nginx/lb');
const templates = [
  ['nginx-lb-http.conf.template', '$scheme'],
  ['nginx-lb-tls.conf.template', 'https'],
];

function readTemplate(filename) {
  return fs.readFileSync(path.join(templateRoot, filename), 'utf8');
}

function block(config, pattern, label) {
  const match = config.match(pattern);
  assert.ok(match, `${label} block is missing`);
  return match[1];
}

test('HTTP and TLS load balancers proxy the complete Nacos context path', () => {
  for (const [filename, forwardedProto] of templates) {
    const config = readTemplate(filename);
    const upstream = block(config, /upstream nacos_server \{([\s\S]*?)\n\}/, `${filename} upstream`);
    const location = block(config, /location \/nacos\/ \{([\s\S]*?)\n    \}/, `${filename} location`);

    assert.match(upstream, /server nacos:8848;/);
    assert.match(location, /proxy_pass http:\/\/nacos_server\/nacos\/;/);
    assert.match(location, /proxy_http_version 1\.1;/);
    assert.match(location, /proxy_set_header Host \$http_host;/);
    assert.match(location, /proxy_set_header X-Real-IP \$remote_addr;/);
    assert.match(location, /proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;/);
    assert.match(location, new RegExp(`proxy_set_header X-Forwarded-Proto ${forwardedProto.replace('$', '\\$')};`));
    assert.match(location, /proxy_set_header Upgrade \$http_upgrade;/);
    assert.match(location, /proxy_set_header Connection \$connection_upgrade;/);
    assert.match(location, /proxy_connect_timeout 5s;/);
    assert.match(location, /proxy_read_timeout 86400s;/);
    assert.match(location, /proxy_buffering off;/);
  }
});

test('Nacos proxy keeps authentication and frame policy owned by Nacos', () => {
  for (const [filename] of templates) {
    const config = readTemplate(filename);
    const location = block(config, /location \/nacos\/ \{([\s\S]*?)\n    \}/, `${filename} location`);

    assert.doesNotMatch(location, /proxy_set_header\s+(?:Authorization|Cookie)\b/i);
    assert.doesNotMatch(location, /proxy_hide_header\s+(?:Content-Security-Policy|X-Frame-Options)\b/i);
    assert.doesNotMatch(location, /Access-Control-Allow-Origin/i);
    assert.doesNotMatch(location, /proxy_pass\s+http:\/\/nacos_server\/;/);
  }
});

test('HTTP and TLS expose the same Nacos route contract', () => {
  const normalize = (config) => block(
    config,
    /location \/nacos\/ \{([\s\S]*?)\n    \}/,
    'Nacos location',
  )
    .replace('proxy_set_header X-Forwarded-Proto https;', 'proxy_set_header X-Forwarded-Proto $scheme;')
    .trim();

  assert.equal(normalize(readTemplate(templates[0][0])), normalize(readTemplate(templates[1][0])));
});
