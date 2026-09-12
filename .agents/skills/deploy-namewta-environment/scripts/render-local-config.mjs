#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  isPlaceholder, parseArgs, readJson, renderTemplate, requireArg, validateProfile, validateSecrets,
  writePrivateFile, yamlSingleQuote
} from './lib.mjs';

try {
  const args = parseArgs(process.argv.slice(2));
  const profile = readJson(requireArg(args, 'profile'));
  const secrets = readJson(requireArg(args, 'secrets'));
  const output = requireArg(args, 'output');
  const errors = [...validateProfile(profile), ...validateSecrets(secrets)];
  const openApiEnabled = profile.schemaVersion === 2 && profile.capabilities?.openApi?.enabled === true;
  if (openApiEnabled && (!secrets.openApiKek || isPlaceholder(secrets.openApiKek))) {
    errors.push('启用 OpenAPI 时敏感字段 OpenAPI KEK 缺失或仍是占位值');
  }
  if (errors.length) throw new Error(errors.join('；'));

  const templates = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../assets/templates');
  const ports = profile.network.ports;
  const frontendContextPath = profile.release?.frontend?.contextPath ?? `/${profile.routes.adminPrefix}/`;
  const frontendBuildEnvironment = profile.release?.frontend?.buildEnvironment ?? profile.deployment.environment;
  const frontendBaseApi = profile.release?.frontend?.baseApi ?? `${frontendContextPath}${frontendBuildEnvironment}-api`;
  const common = {
    bindHost: profile.network.bindHost,
    serverRoot: profile.server.root,
    dockerNetwork: profile.network.dockerNetwork,
    environment: profile.deployment.environment,
    adminPrefix: profile.routes.adminPrefix,
    ingressHttpPort: ports.ingressHttp,
    ingressHttpsPort: ports.ingressHttps,
    adminWebPort: ports.adminWeb,
    backend1Port: ports.backend1,
    redisPort: ports.redis,
    serverName: profile.network.serverName,
    serverHost: profile.server.host,
    mysqlDatabase: profile.services.mysqlDatabase,
    mysqlUser: profile.services.mysqlUser,
    redisDatabase: profile.services.redisDatabase,
    minioEndpoint: profile.services.minioEndpoint,
    minioBucket: profile.services.minioBucket,
    minioDiagnosticObject: profile.services.minioDiagnosticObject,
    nacosNamespace: profile.services.nacosNamespace,
    monitorRoute: profile.routes.monitor,
    snailJobRoute: profile.routes.snailJob,
    snailAiRoute: profile.routes.snailAi,
    nacosRoute: profile.routes.nacos,
    adminImage: profile.release?.images?.admin ?? '',
    frontendContextPath,
    frontendBaseApi,
    openApiEnabled: String(openApiEnabled),
    openApiKekVersion: openApiEnabled ? profile.capabilities.openApi.kekVersion : '',
    ...Object.fromEntries(Object.entries(secrets).map(([key, value]) => [key, yamlSingleQuote(value)])),
    openApiKek: openApiEnabled ? yamlSingleQuote(secrets.openApiKek) : ''
  };
  common.jdbcUrl = `jdbc:mysql://${profile.server.host}:${ports.mysql}/${profile.services.mysqlDatabase}?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&autoReconnect=true&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true`;

  const outputs = [
    ['release.env.template', 'release.env'],
    ['application-local.yml.template', 'application-local.yml'],
    ['admin-web.env.development.local.template', 'admin-web.env.development.local'],
    ['admin-web.env.production.local.template', 'admin-web.env.production.local']
  ];
  for (const [templateName, outputName] of outputs) {
    const template = fs.readFileSync(path.join(templates, templateName), 'utf8');
    writePrivateFile(path.join(output, outputName), renderTemplate(template, common));
    console.log(`[生成] ${path.join(output, outputName)}（权限 0600）`);
  }
} catch (error) {
  console.error(`[错误] ${error.message}`);
  process.exitCode = 1;
}
