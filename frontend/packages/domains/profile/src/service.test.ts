import type { HttpClient, HttpRequest } from '@namewta/platform-contracts';
import { describe, expect, it, vi } from 'vitest';
import type { EnterpriseIdentity, PersonIdentity } from './index';
import { profilePermissions } from './permissions';
import { createProfileService } from './service';

const personIdentity: PersonIdentity = {
  fullName: 'Example',
  documentTypeCode: 'CN_RESIDENT_ID',
  documentNumber: '110101199001010000',
  gender: '0',
  birthDate: '1990-01-01',
  validFrom: '2020-01-01',
  validUntil: '2040-01-01'
};

const enterpriseIdentity: EnterpriseIdentity = {
  enterpriseName: 'Example Ltd',
  unifiedCreditCode: '91110000123456789X',
  enterpriseType: 'LIMITED',
  legalRepresentativeName: 'Example',
  legalDocumentTypeCode: 'CN_RESIDENT_ID',
  legalDocumentNumber: '110101199001010000',
  establishedDate: '2020-01-01',
  businessTermFrom: '2020-01-01',
  businessTermUntil: '2040-01-01',
  registeredAddress: 'Address',
  businessScope: 'Scope',
  contactName: 'Contact',
  contactPhone: '13800000000',
  email: 'contact@example.com',
  registeredCapital: 100,
  industryCode: 'I01',
  website: 'https://example.com'
};

describe('profile service contracts', () => {
  it('keeps legacy application methods aligned with dedicated resource services', () => {
    const service = createProfileService(fixtureHttp([]));

    expect(service.person.application.probeRebind).toBe(service.person.rebind.probe);
    expect(service.person.application.matchRebind).toBe(service.person.rebind.match);
    expect(service.person.application.confirmRebind).toBe(service.person.rebind.confirm);
    expect(service.enterprise.application.sendTransfer).toBe(service.enterprise.transfer.send);
    expect(service.enterprise.application.confirmTransfer).toBe(service.enterprise.transfer.confirm);
  });

  it('uses exact application, privacy probe and transfer endpoints', async () => {
    const requests: HttpRequest[] = [];
    const service = createProfileService(fixtureHttp(requests));

    await service.person.application.probeRebind({
      documentTypeCode: 'CN_RESIDENT_ID',
      documentNumber: '110101199001010000'
    });
    await service.person.application.matchRebind(personIdentity);
    await service.person.application.confirmRebind(personIdentity, 3);
    await service.person.application.submitRebind(4);
    await service.person.application.unbind();
    await service.enterprise.application.probe('91110000123456789X');
    await service.enterprise.application.sendTransfer({
      fullName: 'Example',
      documentLastFour: '0000',
      phone: '13800000000'
    });
    await service.enterprise.application.confirmTransfer({ challengeId: 'challenge', code: '123456' });
    await service.enterprise.application.unbind();

    expect(requests).toEqual([
      {
        url: '/profile/person/rebind/probe',
        method: 'post',
        data: { documentTypeCode: 'CN_RESIDENT_ID', documentNumber: '110101199001010000' }
      },
      { url: '/profile/person/rebind/match', method: 'post', data: { identity: personIdentity } },
      {
        url: '/profile/person/rebind/confirm',
        method: 'post',
        data: { identity: personIdentity, expectedVersion: 3 }
      },
      { url: '/profile/person/rebind/submit', method: 'post', data: { expectedVersion: 4 } },
      { url: '/profile/person/rebind/unbind', method: 'post' },
      {
        url: '/profile/enterprise/application/probe',
        method: 'post',
        data: { unifiedCreditCode: '91110000123456789X' }
      },
      {
        url: '/profile/enterprise/transfer/send',
        method: 'post',
        data: { fullName: 'Example', documentLastFour: '0000', phone: '13800000000' }
      },
      {
        url: '/profile/enterprise/transfer/confirm',
        method: 'post',
        data: { challengeId: 'challenge', code: '123456' }
      },
      { url: '/profile/enterprise/transfer/unbind', method: 'post' }
    ]);
  });

  it('keeps material tags and references on GET/POST lifecycle contracts', async () => {
    const requests: HttpRequest[] = [];
    const service = createProfileService(fixtureHttp(requests));
    const command = {
      parentId: 0,
      nodeType: 'CATEGORY' as const,
      scope: 'PERSON' as const,
      materialTagCode: null,
      nodeName: 'Identity',
      systemRequired: false,
      orderNum: 1,
      expectedVersion: 0
    };

    await service.materialTags.tree('PERSON', true);
    await service.materialTags.create(command);
    await service.materialTags.update('tag/1', { ...command, expectedVersion: 1 });
    await service.materialTags.changeStatus('tag/1', false, 2);
    await service.materialTags.archive('tag/1', 3);
    await service.person.materials.list('VERSION', 'profile/1');
    await service.person.materials.attach('WORKING', 'draft/1', { ossId: 7, materialNodeId: 8 });
    await service.person.materials.detach('WORKING', 'draft/1', 'ref/1');
    await service.enterprise.materials.accessUrl('VERSION', 9, 10);

    expect(requests).toEqual([
      { url: '/profile/material-tags/tree', method: 'get', params: { scope: 'PERSON', includeDisabled: true } },
      { url: '/profile/material-tags', method: 'post', data: command },
      { url: '/profile/material-tags/tag%2F1', method: 'post', data: { ...command, expectedVersion: 1 } },
      {
        url: '/profile/material-tags/tag%2F1/status',
        method: 'post',
        data: { enabled: false, expectedVersion: 2 }
      },
      { url: '/profile/material-tags/tag%2F1/archive', method: 'post', data: { expectedVersion: 3 } },
      { url: '/profile/person/materials/VERSION/profile%2F1', method: 'get' },
      {
        url: '/profile/person/materials/WORKING/draft%2F1',
        method: 'post',
        data: { ossId: 7, materialNodeId: 8 }
      },
      { url: '/profile/person/materials/WORKING/draft%2F1/ref%2F1/detach', method: 'post' },
      { url: '/profile/enterprise/materials/VERSION/9/10/access-url', method: 'get' }
    ]);
  });

  it('provides closed person and enterprise archive command sets without export or delete', async () => {
    const requests: HttpRequest[] = [];
    const service = createProfileService(fixtureHttp(requests));

    await service.person.archive.page({ fullName: 'Example', pageNum: 1, pageSize: 10 });
    await service.person.archive.eligibleUsers('alice');
    await service.person.archive.review('app/1');
    await service.person.archive.decide('app/1', { decision: 'APPROVE', reason: 'verified' });
    await service.person.archive.create({ identity: personIdentity, bindUserId: 1, reason: 'create', materials: [] });
    await service.person.archive.revise(1, { identity: personIdentity, reason: 'revise', expectedVersion: 2 });
    await service.person.archive.assign(1, { userId: 2, reason: 'assign' });
    await service.person.archive.manageBinding(1, { action: 'SUSPEND', reason: 'risk', expectedBindingVersion: 3 });
    await service.person.archive.revoke(1, { reason: 'closed', expectedVersion: 4 });
    await service.enterprise.archive.create({
      identity: enterpriseIdentity,
      bindUserId: 2,
      reason: 'create',
      materials: []
    });
    await service.enterprise.archive.eligibleUsers('owner');
    await service.enterprise.archive.reviewMaterial('app/2', 'ref/2');

    expect(requests.map(item => `${item.method} ${item.url}`)).toEqual([
      'get /profile/person/archive',
      'get /profile/person/archive/eligible-users',
      'get /profile/person/archive/application/app%2F1/review-context',
      'post /profile/person/archive/application/app%2F1/decision',
      'post /profile/person/archive/admin-create',
      'post /profile/person/archive/1/revision',
      'post /profile/person/archive/1/assign',
      'post /profile/person/archive/1/binding',
      'post /profile/person/archive/1/revoke',
      'post /profile/enterprise/archive/admin-create',
      'get /profile/enterprise/archive/eligible-users',
      'get /profile/enterprise/archive/application/app%2F2/material/ref%2F2/access-url'
    ]);
    expect(requests.every(item => item.method === 'get' || item.method === 'post')).toBe(true);
    expect(requests.every(item => !item.url.includes('export'))).toBe(true);
  });

  it('publishes exactly the backend capability IDs and preserves transport errors', async () => {
    expect(profilePermissions).toEqual({
      materialTag: { manage: 'profile:material-tag:manage', query: 'profile:material-tag:query' },
      person: {
        apply: 'profile:person:apply',
        manage: 'profile:person:manage',
        material: 'profile:person:material',
        override: 'profile:person:override',
        query: 'profile:person:query',
        review: 'profile:person:review'
      },
      enterprise: {
        apply: 'profile:enterprise:apply',
        manage: 'profile:enterprise:manage',
        material: 'profile:enterprise:material',
        override: 'profile:enterprise:override',
        query: 'profile:enterprise:query',
        review: 'profile:enterprise:review'
      }
    });
    const forbidden = Object.freeze({ kind: 'business', code: 403, message: 'forbidden' });
    const http: HttpClient = { request: vi.fn(async () => Promise.reject(forbidden)) };
    await expect(createProfileService(http).person.archive.detail(1)).rejects.toBe(forbidden);
  });
});

function fixtureHttp(requests: HttpRequest[]): HttpClient {
  return {
    async request<T>(request: HttpRequest): Promise<T> {
      requests.push(request);
      const isMatch = request.url.endsWith('/match');
      const data = isMatch ? { status: 'MATCHED', maskedPhone: '138****0000' } : { status: 'AVAILABLE' };
      return { code: 200, msg: 'ok', data } as T;
    }
  };
}
