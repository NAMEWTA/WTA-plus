import type { LoadingInstance } from 'element-plus';
import axiosModule from 'axios';
import { extractErrorMessage, globalHeaders } from '@/application/http';
import { systemService } from '@/application/services';
import errorCode from '@/utils/errorCode';
import { saveBlob } from '@/utils/save';

const axios = axiosModule as any;
const baseURL = import.meta.env.VITE_APP_BASE_API;
let downloadLoadingInstance: LoadingInstance | undefined;

const ZIP_LOCAL_FILE = 0x04034b50;
const ZIP_CENTRAL_FILE = 0x02014b50;
const ZIP_EOCD = 0x06054b50;
const ZIP_DATA_DESCRIPTOR = 0x08074b50;
const EOCD_MIN_SIZE = 22;
const MAX_EOCD_SEARCH = EOCD_MIN_SIZE + 0xffff;
const sanitizedMessage = (value: unknown) =>
  [...String(value ?? '')]
    .map(character => {
      const code = character.charCodeAt(0);
      return code < 32 || code === 127 ? ' ' : character;
    })
    .join('')
    .replace(/<[^>]*>/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 200);

export async function isZipPayload(data: unknown): Promise<boolean> {
  let bytes: Uint8Array;
  if (data instanceof Blob) bytes = new Uint8Array(await data.arrayBuffer());
  else if (data instanceof ArrayBuffer) bytes = new Uint8Array(data);
  else if (ArrayBuffer.isView(data)) {
    bytes = new Uint8Array(data.byteLength);
    bytes.set(new Uint8Array(data.buffer, data.byteOffset, data.byteLength));
  } else return false;
  if (bytes.byteLength < EOCD_MIN_SIZE) return false;

  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const u16 = (offset: number) => view.getUint16(offset, true);
  const u32 = (offset: number) => view.getUint32(offset, true);
  let eocdOffset = -1;
  for (
    let offset = bytes.byteLength - EOCD_MIN_SIZE;
    offset >= Math.max(0, bytes.byteLength - MAX_EOCD_SEARCH);
    offset--
  ) {
    if (u32(offset) === ZIP_EOCD) {
      eocdOffset = offset;
      break;
    }
  }
  if (eocdOffset < 0 || eocdOffset + EOCD_MIN_SIZE + u16(eocdOffset + 20) !== bytes.byteLength) return false;

  const disk = u16(eocdOffset + 4);
  const centralDisk = u16(eocdOffset + 6);
  const diskEntries = u16(eocdOffset + 8);
  const totalEntries = u16(eocdOffset + 10);
  const centralSize = u32(eocdOffset + 12);
  const centralOffset = u32(eocdOffset + 16);
  if (disk || centralDisk || diskEntries !== totalEntries || centralOffset + centralSize !== eocdOffset) return false;
  if (totalEntries === 0) return centralOffset === 0 && centralSize === 0 && eocdOffset === 0;
  if (totalEntries === 0xffff || centralSize === 0xffffffff || centralOffset === 0xffffffff) return false;

  let cursor = centralOffset;
  const localSpans: Array<{ start: number; end: number }> = [];
  for (let entry = 0; entry < totalEntries; entry++) {
    if (cursor + 46 > eocdOffset || u32(cursor) !== ZIP_CENTRAL_FILE) return false;
    const centralEntrySize = 46 + u16(cursor + 28) + u16(cursor + 30) + u16(cursor + 32);
    const localOffset = u32(cursor + 42);
    if (cursor + centralEntrySize > eocdOffset || localOffset + 30 > centralOffset) return false;
    if (u32(localOffset) !== ZIP_LOCAL_FILE) return false;
    const localHeaderSize = 30 + u16(localOffset + 26) + u16(localOffset + 28);
    if (localOffset + localHeaderSize > centralOffset) return false;
    const compressedSize = u32(cursor + 20);
    const uncompressedSize = u32(cursor + 24);
    const crc = u32(cursor + 16);
    const localCompressedSize = u32(localOffset + 18);
    const localFlags = u16(localOffset + 6);
    const usesDataDescriptor = (localFlags & 0x0008) !== 0;
    if ((u16(cursor + 8) & 0x0008) !== (localFlags & 0x0008)) return false;
    if (!usesDataDescriptor && localCompressedSize !== compressedSize) return false;
    const fileDataEnd = localOffset + localHeaderSize + compressedSize;
    let localEnd = fileDataEnd;
    if (usesDataDescriptor) {
      const descriptorMatches = (offset: number) =>
        offset + 12 <= centralOffset &&
        u32(offset) === crc &&
        u32(offset + 4) === compressedSize &&
        u32(offset + 8) === uncompressedSize;
      const signaturelessMatches = descriptorMatches(fileDataEnd);
      const signedMatches =
        fileDataEnd + 4 <= centralOffset &&
        u32(fileDataEnd) === ZIP_DATA_DESCRIPTOR &&
        descriptorMatches(fileDataEnd + 4);
      if (!signaturelessMatches && !signedMatches) return false;
      localEnd = fileDataEnd + (signaturelessMatches ? 12 : 16);
    }
    if (localEnd > centralOffset) return false;
    localSpans.push({ start: localOffset, end: localEnd });
    cursor += centralEntrySize;
  }
  if (cursor !== eocdOffset) return false;
  localSpans.sort((left, right) => left.start - right.start);
  return localSpans.every((span, index) => index === 0 || localSpans[index - 1].end <= span.start);
}
export default {
  async oss(ossId: string | number) {
    downloadLoadingInstance = ElLoading.service({
      text: '正在下载数据，请稍候',
      background: 'rgba(0, 0, 0, 0.7)'
    });
    try {
      const response = await systemService.resources.oss.downloadUrl(ossId);
      const download = response.data;
      if (!download?.url) throw new Error('未取得 OSS 下载授权');
      const link = document.createElement('a');
      link.href = download.url;
      link.download = download.fileName;
      link.style.display = 'none';
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (r) {
      console.error(r);
      const errMsg = await extractErrorMessage(r);
      ElMessage.error(errMsg || '下载文件出现错误，请联系管理员！');
    } finally {
      downloadLoadingInstance?.close();
    }
  },
  async zip(url: string, name: string) {
    url = baseURL + url;
    downloadLoadingInstance = ElLoading.service({
      text: '正在下载数据，请稍候',
      background: 'rgba(0, 0, 0, 0.7)'
    });
    try {
      const res = await axios({
        method: 'get',
        url: url,
        responseType: 'blob',
        headers: globalHeaders()
      });
      if (await isZipPayload(res.data)) {
        const blob = new Blob([res.data], { type: 'application/zip' });
        saveBlob(blob, name);
      } else {
        await this.printErrMsg(res.data);
      }
    } catch (r) {
      const errMsg = await extractErrorMessage(r);
      ElMessage.error(sanitizedMessage(errMsg) || '下载文件出现错误，请联系管理员！');
    } finally {
      downloadLoadingInstance?.close();
    }
  },
  async printErrMsg(data: any) {
    let message = '';
    try {
      const text = data instanceof Blob ? await data.text() : String(data ?? '');
      const response = JSON.parse(text) as { code?: keyof typeof errorCode; msg?: unknown };
      message = errorCode[response.code ?? 'default'] || sanitizedMessage(response.msg);
    } catch {
      message = '';
    }
    ElMessage.error(sanitizedMessage(message) || '下载文件出现错误，请联系管理员！');
  }
};
