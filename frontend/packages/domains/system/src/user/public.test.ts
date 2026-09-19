import type { HttpRequest } from '@namewta/platform-contracts';
import { describe, expect, it } from 'vitest';
import { createUserQueryPort } from './public';

describe('system public user seam', () => {
  it('exposes directory fields for workflow and notification selection and preserves queries', async () => {
    const requests: HttpRequest[] = [];
    const users = [
      {
        userId: '7',
        userName: 'owner',
        nickName: '流程负责人',
        deptName: '研发部',
        status: '0',
        email: 'must-not-cross@example.test',
        phoneNumber: '13800000000',
        roles: [{ roleId: 'secret-role' }]
      }
    ];
    const port = createUserQueryPort({
      request: async request => {
        requests.push(request);
        return request.url.includes('optionselect')
          ? ({ code: 200, data: users } as never)
          : request.url.includes('deptTree')
            ? ({ code: 200, data: [{ id: '10', label: '研发部', children: [] }] } as never)
            : ({ code: 200, data: { rows: users, total: 1 } } as never);
      }
    });

    await expect(port.list({ pageNum: 1, pageSize: 10, userName: 'owner' })).resolves.toEqual({
      code: 200,
      data: {
        rows: [
          {
            userId: '7',
            userName: 'owner',
            nickName: '流程负责人',
            phoneNumber: '13800000000',
            deptName: '研发部',
            status: '0'
          }
        ],
        total: 1
      }
    });
    await port.list({ pageNum: 1, pageSize: 10, userIds: ['8'] });
    const projectedOptions = await port.options(['7', 'a/b']);
    expect(projectedOptions).toEqual({
      code: 200,
      data: [
        {
          userId: '7',
          userName: 'owner',
          nickName: '流程负责人',
          phoneNumber: '13800000000',
          deptName: '研发部',
          status: '0'
        }
      ]
    });
    await port.departmentTree();
    expect(requests).toEqual([
      { url: '/system/user/list', method: 'get', params: { pageNum: 1, pageSize: 10, userName: 'owner' } },
      { url: '/system/user/list', method: 'get', params: { pageNum: 1, pageSize: 10, userIds: ['8'] } },
      { url: '/system/user/optionselect?userIds=7,a%2Fb', method: 'get' },
      { url: '/system/user/deptTree', method: 'get' }
    ]);
    expect(JSON.stringify(projectedOptions)).not.toMatch(/must-not-cross|roles|secret-role/);
  });

  it('uses the unified keyword without combining account and phone filters with AND', async () => {
    const requests: HttpRequest[] = [];
    const port = createUserQueryPort({
      request: async request => {
        requests.push(request);
        return { data: { rows: [], total: 0 } } as never;
      }
    });
    await port.list({ keyword: '138', pageNum: 2, pageSize: 20, status: '0' });
    expect(requests[0]).toEqual({
      url: '/system/user/list',
      method: 'get',
      params: { keyword: '138', pageNum: 2, pageSize: 20, status: '0' }
    });
  });

  it('propagates query failures without inventing empty success data', async () => {
    const failure = new Error('user query unavailable');
    const port = createUserQueryPort({ request: async () => Promise.reject(failure) });
    await expect(port.list({})).rejects.toBe(failure);
  });
});


describe('untrusted user transport', () => {
  it.each([null, undefined, [], {}, { userId: null }, { userId: '' }, { userId: {} }, { userId: 7, nickName: 123 }, { userId: 7, phoneNumber: [] }])('rejects malformed user %j', async user => {
    const port = createUserQueryPort({ request: async <T>() => ({ data: [user] }) as T });
    await expect(port.options([7])).rejects.toThrow('用户响应不可用');
  });

  it('keeps nullable optional fields and the empty page legitimate', async () => {
    const port = createUserQueryPort({ request: async <T>() => ({ data: [{ userId: 0, nickName: null, userName: null, phoneNumber: null, deptName: null, status: null }] }) as T });
    await expect(port.options([0])).resolves.toEqual({ data: [{ userId: 0, nickName: '' }] });
    const empty = createUserQueryPort({ request: async <T>() => ({ data: { rows: [], total: 0 } }) as T });
    await expect(empty.list({})).resolves.toEqual({ data: { rows: [], total: 0 } });
  });

  it.each([null, {}, { data: null }, { data: { rows: null, total: 0 } }, { data: { rows: [], total: '0' } }, { data: { rows: [], total: -1 } }])('rejects malformed page %j', async value => {
    const port = createUserQueryPort({ request: async <T>() => value as T });
    await expect(port.list({})).rejects.toThrow('用户响应不可用');
  });
});
