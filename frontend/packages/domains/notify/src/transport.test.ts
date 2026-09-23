import { describe, expect, it, vi } from 'vitest';
import { createNotificationService } from './transport';

describe('通知传输映射', () => {
  it('将服务端分页和详情分别映射为领域消息，保留全局未读总数', async () => {
    const request = vi.fn()
      .mockResolvedValueOnce({ data: { rows: [{ messageId: '9000000000000000001', category: 'notice', message: '摘要' }], total: 501, unreadTotal: 481 } })
      .mockResolvedValueOnce({ data: { messageId: '9000000000000000001', category: 'notice', content: '完整正文' } });
    const service = createNotificationService({ request } as never);
    const page = await service.inbox.list(26, 20);
    expect(request).toHaveBeenNthCalledWith(1, {
      url: '/notify/inbox', method: 'get', params: { pageNum: 26, pageSize: 20 }
    });
    expect(page.data).toEqual({ rows: [{ messageId: '9000000000000000001', category: 'notice', message: '摘要' }], total: 501, unreadTotal: 481 });
    const detail = await service.inbox.detail('9000000000000000001');
    expect(request).toHaveBeenNthCalledWith(2, { url: '/notify/inbox/9000000000000000001', method: 'get' });
    expect(detail.data.content).toBe('完整正文');
    await expect(service.inbox.detail('../2')).rejects.toThrow('收件箱消息编号无效');
    expect(() => service.inbox.read('../2')).toThrow('收件箱消息编号无效');
    expect(() => service.inbox.seen(Number.MAX_SAFE_INTEGER + 1)).toThrow('收件箱消息编号无效');
    expect(request).toHaveBeenCalledTimes(2);
  });

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
  it('按路径通知主键和单个投递编号发送重试，取消作用整个通知', async () => {
    const request = vi.fn().mockResolvedValue({ data: { notificationId: '101', status: 'QUEUED', queuedCount: 1 } });
    const service = createNotificationService({ request } as never);
    await service.notification.retry('101', '303');
    expect(request).toHaveBeenLastCalledWith({
      url: '/notify/notification/101/retry', method: 'post', data: { deliveryId: '303', reason: 'manual' }
    });
    await service.notification.cancel('101');
    expect(request).toHaveBeenLastCalledWith({ url: '/notify/notification/101/cancel', method: 'post' });
    expect(() => service.notification.retry('../202', '303')).toThrow('通知或投递编号无效');
    expect(request).toHaveBeenCalledTimes(2);
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
    const mailAccount = {
      channel: 'MAIL' as const,
      configKey: 'smtp-ops',
      enabled: 'Y',
      host: 'smtp.example.com',
      port: 465,
      mailFrom: 'ops@example.com',
      mailUser: 'ops',
      mailPass: 'secret',
      minuteMax: 60
    };
    await service.config.addAccount(mailAccount);
    expect(request).toHaveBeenLastCalledWith({
      url: '/notify/config/account',
      method: 'post',
      data: mailAccount
    });
    const smsAccount = {
      channel: 'SMS' as const,
      configKey: 'alibaba-ops',
      enabled: 'Y',
      supplier: 'alibaba',
      accessKeyId: 'ak',
      accessKeySecret: 'sk',
      signature: 'WTA',
      minuteMax: 30
    };
    await service.config.addAccount(smsAccount);
    expect(request).toHaveBeenLastCalledWith({
      url: '/notify/config/account',
      method: 'post',
      data: smsAccount
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
