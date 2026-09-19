import type { Identifier, MaterialReference, ProfileType } from '@namewta/domain-profile';
import type { MaterialNode, MaterialRequirement } from '@namewta/domain-profile/material-tags';
import { computed, onScopeDispose, ref, watch } from 'vue';
import type { ProfileSelfWebRuntime } from './runtime';

interface SelfMaterialContext {
  documentTypeCode: string;
  editable: boolean;
  handlerIsLegalRepresentative: boolean;
  ownerId: Identifier | undefined;
  profileType: ProfileType;
}

function flatten(nodes: MaterialNode[]): MaterialNode[] {
  return nodes.flatMap(node => [node, ...flatten(node.children ?? [])]);
}

/** 当前草稿独占上传和登记；离开 owner 后的响应不再修改页面。 */
export function useSelfMaterials(runtime: ProfileSelfWebRuntime, context: () => SelfMaterialContext) {
  const tags = ref<MaterialNode[]>([]);
  const requirements = ref<MaterialRequirement[]>([]);
  const materials = ref<MaterialReference[]>([]);
  const loading = ref(false);
  const ready = ref(false);
  const phase = ref<'idle' | 'uploading' | 'registering' | 'removing'>('idle');
  const progress = ref(0);
  const error = ref('');
  const missingTag = ref('');
  const pending = ref<{ materialNodeId: Identifier; ossId: string; replaces?: Identifier }>();
  let generation = 0;
  let controller: AbortController | undefined;
  const busy = computed(() => loading.value || phase.value !== 'idle');
  const service = () => context().profileType === 'PERSON' ? runtime.service.person.materials : runtime.service.enterprise.materials;
  const permission = () => runtime.hasPermission(`profile:${context().profileType.toLowerCase()}:material`);
  const writable = computed(() => context().editable && context().ownerId !== undefined && permission());
  const canUpload = computed(() => writable.value && ready.value && !busy.value && !pending.value && runtime.hasPermission('system:oss:upload'));

  function describeError(cause: unknown, fallback: string) {
    const message = cause instanceof Error ? cause.message : fallback;
    const code = /MISSING_REQUIRED_MATERIAL:([A-Z_]+)/.exec(message)?.[1];
    missingTag.value = code ?? '';
    error.value = code ? `请补充${tags.value.find(tag => tag.materialTagCode === code)?.nodeName ?? code}` : message;
    return error.value;
  }

  async function load() {
    const epoch = ++generation;
    controller?.abort();
    controller = undefined;
    phase.value = 'idle';
    pending.value = undefined;
    ready.value = false;
    loading.value = true;
    error.value = '';
    missingTag.value = '';
    materials.value = [];
    tags.value = [];
    requirements.value = [];
    const { profileType, documentTypeCode, handlerIsLegalRepresentative, ownerId } = context();
    try {
      if (!permission()) throw new Error('当前账号没有材料访问权限，请联系管理员');
      const tree = await runtime.service.materialTags.tree(profileType);
      if (epoch !== generation) return;
      tags.value = flatten(tree.data).filter(tag => tag.enabled && tag.nodeType === 'TAG');
      const required = await runtime.service.materialTags.requirements(profileType, documentTypeCode, handlerIsLegalRepresentative);
      if (epoch !== generation) return;
      requirements.value = required.data;
      if (ownerId !== undefined) {
        const response = await service().list('WORKING', ownerId);
        if (epoch !== generation) return;
        materials.value = response.data.filter(item => item.attached);
      }
      ready.value = true;
    } catch (cause) {
      if (epoch === generation) describeError(cause, '材料加载失败，请重试');
    } finally {
      if (epoch === generation) loading.value = false;
    }
  }

  async function register(reconcile: boolean, epoch: number) {
    const item = pending.value;
    const ownerId = context().ownerId;
    if (!item || ownerId === undefined) return;
    phase.value = 'registering';
    // 上次请求可能已在服务端成功；先查询归属，避免丢失响应后重复登记。
    let registered = false;
    if (reconcile) {
      const response = await service().list('WORKING', ownerId);
      if (epoch !== generation) return;
      materials.value = response.data.filter(reference => reference.attached);
      registered = materials.value.some(reference => String(reference.ossId) === item.ossId && String(reference.materialNodeId) === String(item.materialNodeId));
    }
    if (!registered) {
      const response = await service().attach('WORKING', ownerId, { materialNodeId: item.materialNodeId, ossId: item.ossId });
      if (epoch !== generation) return;
      if (!response.data?.materialRefId) throw new Error('材料登记结果缺失，请重试登记');
      materials.value.push(response.data);
    }
    // 新引用确认存在后才移除旧引用；中途失败仍保留两份，可查询后重试。
    if (item.replaces !== undefined && materials.value.some(reference => reference.materialRefId === item.replaces)) {
      await service().detach('WORKING', ownerId, item.replaces);
      if (epoch !== generation) return;
      materials.value = materials.value.filter(reference => reference.materialRefId !== item.replaces);
    }
    pending.value = undefined;
    missingTag.value = '';
  }

  async function upload(tag: MaterialNode, file: File, replaces?: Identifier) {
    if (!canUpload.value) return;
    if (!/\.(?:jpe?g|png|pdf)$/i.test(file.name) || file.size === 0 || file.size > 10 * 1024 * 1024) {
      error.value = '请选择 10 MiB 以内的 JPG、PNG 或 PDF 文件';
      return;
    }
    if (materials.value.length >= 10) {
      error.value = '最多可登记 10 份材料，请先移除不需要的材料';
      return;
    }
    const epoch = generation;
    const owned = new AbortController();
    controller = owned;
    phase.value = 'uploading';
    progress.value = 0;
    error.value = '';
    try {
      const uploaded = await runtime.uploadMaterial(file, { signal: owned.signal, onProgress: value => { if (epoch === generation && !owned.signal.aborted) progress.value = value; } });
      if (epoch !== generation || owned.signal.aborted) return;
      if (!uploaded.ossId) throw new Error('上传结果缺少文件编号，请重试');
      pending.value = { materialNodeId: tag.materialNodeId, ossId: uploaded.ossId, replaces };
      await register(false, epoch);
    } catch (cause) {
      if (epoch === generation && !owned.signal.aborted) describeError(cause, '材料上传或登记失败，请重试');
    } finally {
      if (epoch === generation) { phase.value = 'idle'; controller = undefined; }
    }
  }

  function cancel() {
    if (phase.value !== 'uploading') return;
    controller?.abort();
    ++generation;
    controller = undefined;
    phase.value = 'idle';
    error.value = '上传已取消';
  }

  async function retryRegistration() {
    if (!writable.value || busy.value || !pending.value) return;
    const epoch = generation;
    error.value = '';
    try { await register(true, epoch); }
    catch (cause) { if (epoch === generation) describeError(cause, '登记失败，请重试'); }
    finally { if (epoch === generation) phase.value = 'idle'; }
  }

  async function remove(item: MaterialReference) {
    const ownerId = context().ownerId;
    if (!writable.value || busy.value || pending.value || ownerId === undefined) return;
    const epoch = generation;
    phase.value = 'removing';
    error.value = '';
    try {
      await service().detach('WORKING', ownerId, item.materialRefId);
      if (epoch === generation) materials.value = materials.value.filter(reference => reference.materialRefId !== item.materialRefId);
    } catch (cause) { if (epoch === generation) describeError(cause, '移除失败，请重试'); }
    finally { if (epoch === generation) phase.value = 'idle'; }
  }

  async function access(item: MaterialReference) {
    const ownerId = context().ownerId;
    if (ownerId === undefined) return undefined;
    const epoch = generation;
    error.value = '';
    try {
      const response = await service().accessUrl('WORKING', ownerId, item.materialRefId);
      if (epoch !== generation) return undefined;
      if (!response.data?.url) throw new Error('预览地址暂不可用，请重试');
      return response.data;
    } catch (cause) { if (epoch === generation) describeError(cause, '预览失败，请重试'); }
    return undefined;
  }

  function validate() {
    if (!ready.value || busy.value || pending.value) {
      error.value = '请等待材料加载、上传和登记完成后再提交';
      return false;
    }
    const missing = requirements.value.find(requirement => materials.value.filter(item => item.materialTagCode === requirement.materialTagCode).length < requirement.minimumCount);
    if (missing) {
      describeError(new Error(`MISSING_REQUIRED_MATERIAL:${missing.materialTagCode}`), '请补充必填材料');
      return false;
    }
    return true;
  }

  watch(() => [context().profileType, context().ownerId, context().documentTypeCode, context().handlerIsLegalRepresentative], load, { immediate: true });
  onScopeDispose(() => { ++generation; controller?.abort(); });
  return { access, busy, cancel, canUpload, describeError, error, load, loading, materials, missingTag, pending, phase, progress, ready, remove, requirements, retryRegistration, tags, upload, validate, writable };
}
