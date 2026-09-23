#!/usr/bin/env node
// Fail CI when any requested real-service test did not run successfully this time.

import fs from 'node:fs';
import path from 'node:path';

const [backend, sinceNs, ...expected] = process.argv.slice(2);
if (expected.length === 0) {
  console.error('No required external-service tests were declared');
  process.exit(1);
}

const since = BigInt(sinceNs);
const reports = [];
const walk = (directory) => {
  if (!fs.existsSync(directory)) return;
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    if (!entry.isDirectory()) continue;
    const full = path.join(directory, entry.name);
    if (entry.name === 'surefire-reports' && path.basename(directory) === 'target') {
      for (const name of fs.readdirSync(full)) {
        if (name.startsWith('TEST-') && name.endsWith('.xml')) reports.push(path.join(full, name));
      }
    } else walk(full);
  }
};
walk(backend);

function rootAttributes(file) {
  const xml = fs.readFileSync(file, 'utf8');
  const start = xml.match(/<testsuite\b([^>]*)\/?>/);
  if (!start) throw new Error(`${file}: missing testsuite root`);
  const attributes = {};
  for (const match of start[1].matchAll(/([A-Za-z_:][\w:.-]*)\s*=\s*"([^"]*)"/g)) attributes[match[1]] = match[2];
  return attributes;
}

let total = 0;
for (const name of expected) {
  const matching = reports.filter((file) => path.basename(file).endsWith(`.${name}.xml`) && fs.statSync(file, { bigint: true }).mtimeNs >= since);
  if (matching.length !== 1) {
    console.error(`${name}: expected one fresh report, found ${matching.length}`);
    process.exit(1);
  }
  const suite = rootAttributes(matching[0]);
  const tests = Number(suite.tests);
  const failed = ['failures', 'errors', 'skipped'].some((key) => Number(suite[key] ?? '0') > 0);
  if (!Number.isInteger(tests) || tests <= 0 || failed) {
    console.error(`${name}: required tests must execute with zero failures, errors and skips`);
    process.exit(1);
  }
  total += tests;
}
console.log(`External-service evidence: ${expected.length} required classes, ${total} passed tests, zero skipped`);
