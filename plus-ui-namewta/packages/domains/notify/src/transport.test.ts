import { describe, expect, it, vi } from 'vitest';
import { createNotificationService } from './transport';

describe('通知传输映射', () => {
  it('保存三类目标及渠道快照，发布为独立请求', async () => {
    const request = vi.fn().mockResolvedValue({ data: null });
    const service = createNotificationService({ request } as never);
    const targets = [
      { recipientType: 'ALL' as const, recipientIds: [], userTypeIds: [] },
      { recipientType: 'USER' as const, recipientIds: ['1761100000000000001'], userTypeIds: [] },
      { recipientType: 'USER_TYPE' as const, recipientIds: [], userTypeIds: ['10', '11'] }
    ];
    for (const target of targets) {
      const data = { noticeTitle: '目标合同', ...target, channels: ['IN_APP' as const, 'MAIL' as const] };
      await service.notices.save(data);
      expect(request).toHaveBeenLastCalledWith({ url: '/notify/notice/save', method: 'post', data });
    }
    expect(request).toHaveBeenCalledTimes(3);
    await service.notices.publish('1761100000000000001');
    expect(request).toHaveBeenLastCalledWith({ url: '/notify/notice/1761100000000000001/publish', method: 'post' });
  });
  it('使用统一监控资源并保留筛选参数', async () => {
    const request = vi.fn().mockResolvedValue({ data: [] });
    const service = createNotificationService({ request } as never);
    await service.deliveries({ channel: 'SMS', status: 'FAILED' });
    expect(request).toHaveBeenCalledWith({
      url: '/notify/monitor/deliveries',
      method: 'get',
      params: { channel: 'SMS', status: 'FAILED' }
    });
  });
  it('映射通知配置账号、场景绑定和试发', async () => {
    const request = vi.fn().mockResolvedValue({ data: { rows: [], total: 0 } });
    const service = createNotificationService({ request } as never);
    await service.config.accounts('MAIL', 1, 10);
    expect(request).toHaveBeenLastCalledWith({
      url: '/notify/config/account/list',
      method: 'get',
      params: { channel: 'MAIL', pageNum: 1, pageSize: 10 }
    });
    await service.config.saveScene({
      sceneCode: 'auth-captcha',
      channel: 'SMS',
      smsTemplateCode: 'SMS_1',
      smsParamMapping: { code: 'code' }
    });
    expect(request).toHaveBeenLastCalledWith({
      url: '/notify/config/scene/save',
      method: 'post',
      data: {
        sceneCode: 'auth-captcha',
        channel: 'SMS',
        smsTemplateCode: 'SMS_1',
        smsParamMapping: { code: 'code' }
      }
    });
    await service.config.testTemplate({ sceneCode: 'auth-captcha', channel: 'MAIL', target: 'a@b.c' });
    expect(request).toHaveBeenLastCalledWith({
      url: '/notify/config/test/template',
      method: 'post',
      data: { sceneCode: 'auth-captcha', channel: 'MAIL', target: 'a@b.c' }
    });
  });
});
