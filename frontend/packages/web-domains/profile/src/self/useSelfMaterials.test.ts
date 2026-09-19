import { createProfileService, type MaterialReference } from '@namewta/domain-profile';
import type { MaterialNode } from '@namewta/domain-profile/material-tags';
import { effectScope, reactive } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ProfileSelfWebRuntime } from './runtime';
import { useSelfMaterials } from './useSelfMaterials';

const tag: MaterialNode = { children: [], enabled: true, materialNodeId: 'tag-front', materialTagCode: 'PERSON_ID_CARD_PORTRAIT', nodeDepth: 1, nodeName: '身份证人像面', nodeType: 'TAG', orderNum: 1, parentId: 'root', scope: 'PERSON', systemRequired: true, version: 0 };
const material = (id = 'ref-1', ossId = 'oss-1'): MaterialReference => ({ attached: true, attachedTime: '', detachedTime: null, fileExtension: 'png', fileName: 'front.png', fileSize: 8, immutableEvidence: false, materialNodeId: tag.materialNodeId, materialRefId: id, materialTagCode: tag.materialTagCode!, materialTagName: tag.nodeName, mimeType: 'image/png', ossId, owner: { ownerId: 'draft-1', ownerType: 'WORKING', profileType: 'PERSON' }, version: 0 });
const file = () => new File(['material'], 'front.png', { type: 'image/png' });
const scopes: ReturnType<typeof effectScope>[] = [];
afterEach(() => { scopes.splice(0).forEach(scope => scope.stop()); });

async function fixture() {
  const context = reactive({ ownerId: 'draft-1' as string | undefined, profileType: 'PERSON' as const, documentTypeCode: 'CN_RESIDENT_ID', handlerIsLegalRepresentative: true, editable: true });
  const base = createProfileService({ request: async () => { throw new Error('Unexpected domain request'); } });
  const list = vi.fn(async () => ({ data: [] as MaterialReference[] }));
  const attach = vi.fn(async () => ({ data: material() }));
  const detach = vi.fn(async () => ({ data: null }));
  const accessUrl = vi.fn(async () => ({ data: { accessType: 'PRIVATE', expiresAt: '2099-01-01', fileName: 'front.png', url: 'https://storage.test/front' } }));
  const runtime: ProfileSelfWebRuntime = {
    confirm: async () => undefined, error: vi.fn(), success: vi.fn(), warning: vi.fn(), hasPermission: vi.fn(() => true),
    uploadMaterial: vi.fn(async () => ({ ossId: 'oss-1' })),
    service: { ...base,
      materialTags: { ...base.materialTags, tree: vi.fn(async () => ({ data: [tag] })), requirements: vi.fn(async () => ({ data: [{ materialTagCode: tag.materialTagCode!, minimumCount: 1 }] })) },
      person: { ...base.person, materials: { list, attach, detach, accessUrl } }
    }
  };
  const scope = effectScope(); scopes.push(scope);
  const state = scope.run(() => useSelfMaterials(runtime, () => context))!;
  await state.load();
  return { attach, context, detach, list, runtime, scope, state, accessUrl };
}

describe('self material ownership and submission readiness', () => {
  it('locates the missing required tag and only becomes valid after registration', async () => {
    const { state, attach } = await fixture();
    expect(state.validate()).toBe(false);
    expect(state.error.value).toBe('请补充身份证人像面');
    await state.upload(tag, file());
    expect(attach).toHaveBeenCalledWith('WORKING', 'draft-1', { materialNodeId: 'tag-front', ossId: 'oss-1' });
    expect(state.validate()).toBe(true);
  });

  it('blocks submission while registration is pending', async () => {
    const { state, attach } = await fixture();
    let finish!: (value: { data: MaterialReference }) => void;
    let entered!: () => void;
    const registering = new Promise<void>(resolve => { entered = resolve; });
    attach.mockImplementationOnce(() => { entered(); return new Promise(resolve => { finish = resolve; }); });
    const uploading = state.upload(tag, file());
    await registering;
    expect(state.phase.value).toBe('registering');
    expect(state.busy.value).toBe(true);
    expect(state.validate()).toBe(false);
    finish({ data: material() });
    await uploading;
    expect(state.validate()).toBe(true);
  });

  it('reconciles a lost attach response before retrying without reuploading', async () => {
    const { state, attach, list, runtime } = await fixture();
    attach.mockRejectedValueOnce(new Error('连接中断'));
    await state.upload(tag, file());
    expect(state.pending.value?.ossId).toBe('oss-1');
    expect(state.validate()).toBe(false);
    list.mockResolvedValueOnce({ data: [material()] });
    await state.retryRegistration();
    expect(attach).toHaveBeenCalledTimes(1);
    expect(runtime.uploadMaterial).toHaveBeenCalledTimes(1);
    expect(state.validate()).toBe(true);
  });

  it('retains the pending object if reconciliation fails and retries attach only after an empty list', async () => {
    const { state, attach, list } = await fixture();
    attach.mockRejectedValueOnce(new Error('登记失败'));
    await state.upload(tag, file());
    list.mockRejectedValueOnce(new Error('查询失败'));
    await state.retryRegistration();
    expect(attach).toHaveBeenCalledTimes(1);
    expect(state.pending.value).toBeDefined();
    await state.retryRegistration();
    expect(attach).toHaveBeenCalledTimes(2);
    expect(state.pending.value).toBeUndefined();
  });

  it('ignores a late completed upload after cancellation and permits retry', async () => {
    const { state, runtime, attach } = await fixture();
    let finish!: (value: { ossId: string }) => void;
    vi.mocked(runtime.uploadMaterial).mockImplementationOnce(() => new Promise(resolve => { finish = resolve; }));
    const uploading = state.upload(tag, file());
    const signal = vi.mocked(runtime.uploadMaterial).mock.calls[0][1].signal;
    state.cancel();
    expect(signal.aborted).toBe(true);
    finish({ ossId: 'late' });
    await uploading;
    expect(attach).not.toHaveBeenCalled();
    expect(state.busy.value).toBe(false);
    await state.upload(tag, file());
    expect(attach).toHaveBeenCalledTimes(1);
  });

  it('aborts uploads when the component scope is disposed', async () => {
    const { state, runtime, scope, attach } = await fixture();
    let finish!: (value: { ossId: string }) => void;
    vi.mocked(runtime.uploadMaterial).mockImplementationOnce(() => new Promise(resolve => { finish = resolve; }));
    const uploading = state.upload(tag, file());
    scope.stop();
    expect(vi.mocked(runtime.uploadMaterial).mock.calls[0][1].signal.aborted).toBe(true);
    finish({ ossId: 'late' });
    await uploading;
    expect(attach).not.toHaveBeenCalled();
  });

  it('restores only attached references on refresh and detaches through the current owner', async () => {
    const { state, list, detach } = await fixture();
    list.mockResolvedValueOnce({ data: [material(), { ...material('detached'), attached: false }] });
    await state.load();
    expect(state.materials.value).toHaveLength(1);
    await state.remove(state.materials.value[0]);
    expect(detach).toHaveBeenCalledWith('WORKING', 'draft-1', 'ref-1');
    expect(state.validate()).toBe(false);
  });

  it('keeps the previous material until its replacement is registered', async () => {
    const { state, list, attach, detach } = await fixture();
    list.mockResolvedValueOnce({ data: [material('old', 'old-oss')] });
    await state.load();
    attach.mockRejectedValueOnce(new Error('登记暂不可用'));
    await state.upload(tag, file(), 'old');
    expect(detach).not.toHaveBeenCalled();
    expect(state.materials.value[0].materialRefId).toBe('old');
    list.mockResolvedValueOnce({ data: [material('old', 'old-oss')] });
    await state.retryRegistration();
    expect(detach).toHaveBeenCalledWith('WORKING', 'draft-1', 'old');
    expect(state.materials.value.map(item => item.materialRefId)).toEqual(['ref-1']);
  });

  it('does not permit upload or detach for a read-only application or missing upload permission', async () => {
    const { state, context, runtime, attach, detach } = await fixture();
    context.editable = false;
    await state.upload(tag, file());
    await state.remove(material());
    expect(attach).not.toHaveBeenCalled();
    expect(detach).not.toHaveBeenCalled();
    context.editable = true;
    vi.mocked(runtime.hasPermission).mockImplementation(permission => permission !== 'system:oss:upload');
    expect(state.canUpload.value).toBe(false);
  });

  it('rejects unsupported or empty files without contacting OSS', async () => {
    const { state, runtime } = await fixture();
    await state.upload(tag, new File(['script'], 'file.svg'));
    await state.upload(tag, new File([], 'empty.png'));
    expect(runtime.uploadMaterial).not.toHaveBeenCalled();
    expect(state.validate()).toBe(false);
  });

  it('retries access through the Profile owner and never removes material on URL failure', async () => {
    const { state, accessUrl } = await fixture();
    await state.upload(tag, file());
    accessUrl.mockRejectedValueOnce(new Error('URL_EXPIRED'));
    expect(await state.access(material())).toBeUndefined();
    expect(state.materials.value).toHaveLength(1);
    expect((await state.access(material()))?.url).toBe('https://storage.test/front');
    expect(state.error.value).toBe('');
    expect(accessUrl).toHaveBeenCalledWith('WORKING', 'draft-1', 'ref-1');
  });
});
