export type LegacyAuthOptions = Record<string, string | boolean | number | string[]>;
export interface LegacyAuthImportInput {
  businessClientId: string;
  firstLoginPolicy: 'BIND_ONLY' | 'AUTO_REGISTER';
  dryRun: boolean;
  type: Record<string, LegacyAuthOptions>;
}
export interface LegacyAuthImportItem {
  source: string;
  providerKey: string;
  externalClientId: string;
  redirectUri: string;
  secretConfigured: boolean;
  status: 'READY' | 'SKIPPED' | 'EXISTS' | 'IMPORTED' | 'INVALID';
  message: string;
  registrationId?: string;
}
function object(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('旧配置格式无效');
  return value as Record<string, unknown>;
}
/** 只提取 JustAuth 身份源，不把整份部署文件中的数据库等凭据传到导入 API。 */
export function extractLegacyAuthProviders(documents: unknown[]): LegacyAuthImportInput['type'] {
  const result: LegacyAuthImportInput['type'] = {};
  for (const document of documents) {
    if (document == null) continue;
    const root = object(document);
    const justauth = root.justauth == null ? root : object(root.justauth);
    if (justauth.type == null) continue;
    for (const [source, input] of Object.entries(object(justauth.type))) {
      if (!/^[a-zA-Z0-9_-]+$/.test(source) || source in result) throw new Error('身份源名称无效或重复');
      const options: LegacyAuthOptions = {};
      for (const [key, value] of Object.entries(object(input))) {
        const normalized = key.replace(/-([a-z])/g, (_whole, letter: string) => letter.toUpperCase());
        if (['__proto__', 'prototype', 'constructor'].includes(normalized)) throw new Error('配置包含不支持的字段');
        if (
          typeof value === 'string' ||
          typeof value === 'boolean' ||
          (typeof value === 'number' && Number.isFinite(value))
        )
          options[normalized] = value;
        else if (Array.isArray(value) && value.every(item => typeof item === 'string'))
          options[normalized] = [...value];
        else if (value != null) throw new Error('旧配置包含不支持的嵌套参数');
      }
      const redirect = options.redirectUri;
      if (typeof redirect === 'string' && typeof justauth.address === 'string') {
        options.redirectUri = redirect.replaceAll('${justauth.address}', justauth.address);
      }
      result[source] = options;
    }
  }
  if (!Object.keys(result).length) throw new Error('文件中没有找到 justauth.type 配置');
  return result;
}
export function readLegacyAuthImportItems(value: unknown): LegacyAuthImportItem[] {
  const root = object(value);
  if (!Array.isArray(root.items)) throw new Error('导入预览响应无效');
  return root.items.map(item => {
    const row = object(item);
    if (!['READY', 'SKIPPED', 'EXISTS', 'IMPORTED', 'INVALID'].includes(String(row.status)))
      throw new Error('导入状态无效');
    const text = (key: string) => (typeof row[key] === 'string' ? (row[key] as string) : '');
    return {
      source: text('source'),
      providerKey: text('providerKey'),
      externalClientId: text('externalClientId'),
      redirectUri: text('redirectUri'),
      secretConfigured: row.secretConfigured === true,
      status: row.status as LegacyAuthImportItem['status'],
      message: text('message'),
      ...(typeof row.registrationId === 'string' ? { registrationId: row.registrationId } : {})
    };
  });
}
