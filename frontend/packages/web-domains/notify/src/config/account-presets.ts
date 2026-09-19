import type { NotifyChannelAccount, NotifyConfigChannel } from '@namewta/domain-notify';

/** 只用于新账号表单的连接预填；发送时仍以服务端已保存、已启用的账号为准。 */
export const accountPresets = [
  { key: 'mail-qq', label: 'QQ 邮箱', channel: 'MAIL', host: 'smtp.qq.com' },
  { key: 'mail-163', label: '网易 163 邮箱', channel: 'MAIL', host: 'smtp.163.com' },
  { key: 'mail-tencent-enterprise', label: '腾讯企业邮箱（企业微信）', channel: 'MAIL', host: 'smtp.exmail.qq.com' },
  { key: 'sms-tencent', label: '腾讯云短信', channel: 'SMS', supplier: 'tencent' },
  { key: 'sms-alibaba', label: '阿里云短信', channel: 'SMS', supplier: 'alibaba' }
] as const;

/** 切换预设生成新草稿，禁止把另一个供应商尚未保存的密钥带入新账号。 */
export function accountFromPreset(channel: NotifyConfigChannel, key = ''): NotifyChannelAccount {
  const preset = accountPresets.find(item => item.channel === channel && item.key === key);
  const account: NotifyChannelAccount = {
    channel,
    configKey: preset?.key ?? '',
    enabled: 'N',
    minuteMax: 60,
    sslEnable: channel === 'MAIL' ? 'Y' : 'N',
    starttlsEnable: 'N',
    mailPass: '',
    accessKeySecret: ''
  };
  if (preset?.channel === 'MAIL') {
    account.host = preset.host;
    account.port = 465;
  }
  if (preset?.channel === 'SMS') account.supplier = preset.supplier;
  return account;
}
