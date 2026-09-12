import type { RichTextAssetKind, RichTextAssetAccess } from './types';

const ID = /^[1-9]\d*$/;
const MEDIA = new Set(['IMG', 'AUDIO', 'VIDEO']);
const TAGS = new Set(['P', 'BR', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'STRONG', 'B', 'EM', 'I', 'U', 'S', 'STRIKE', 'SUB', 'SUP', 'BLOCKQUOTE', 'PRE', 'CODE', 'UL', 'OL', 'LI', 'TABLE', 'THEAD', 'TBODY', 'TR', 'TH', 'TD', 'SPAN', 'IMG', 'AUDIO', 'VIDEO', 'A']);

export function isOssId(value: string | null | undefined): value is string {
  return !!value && ID.test(value);
}

export function assetKindForElement(element: Element): RichTextAssetKind | null {
  if (element.tagName === 'IMG') return 'image';
  if (element.tagName === 'AUDIO') return 'audio';
  if (element.tagName === 'VIDEO') return 'video';
  if (element.tagName === 'A' && element.hasAttribute('data-oss-id')) return 'attachment';
  return null;
}

export function extractOssIds(html: string): string[] {
  if (!html || typeof DOMParser === 'undefined') return [];
  const doc = new DOMParser().parseFromString(html, 'text/html');
  return [...new Set([...doc.querySelectorAll('[data-oss-id]')].map(element => element.getAttribute('data-oss-id')).filter(isOssId))];
}

function safeStyle(value: string): string {
  const allowed = /^(color|background-color|font-size|text-align|line-height|text-indent|width|height)\s*:\s*([#(),.%\w\s-]+)$/i;
  return value.split(';').map(item => item.trim()).filter(item => allowed.test(item) && !/url\s*\(|expression\s*\(/i.test(item)).join(';');
}

export function canonicalizeRichText(html: string): string {
  if (!html || typeof DOMParser === 'undefined') return '';
  const doc = new DOMParser().parseFromString(html, 'text/html');
  doc.body.querySelectorAll('span[data-richtext-asset-kind="audio"][data-oss-id]').forEach(element => {
    const id = element.getAttribute('data-oss-id');
    if (isOssId(id)) {
      const audio = doc.createElement('audio');
      audio.setAttribute('data-oss-id', id);
      audio.setAttribute('src', `oss://${id}`);
      audio.setAttribute('controls', '');
      element.replaceWith(audio);
    }
  });
  [...doc.body.querySelectorAll('*')].forEach(element => {
    if (!TAGS.has(element.tagName)) {
      element.replaceWith(...Array.from(element.childNodes));
      return;
    }
    [...element.attributes].forEach(attribute => {
      const name = attribute.name.toLowerCase();
      if (name.startsWith('on') || name === 'srcset' || name === 'poster' || (name === 'style' && !safeStyle(attribute.value))) element.removeAttribute(attribute.name);
    });
    if (element.hasAttribute('style')) {
      const style = safeStyle(element.getAttribute('style') ?? '');
      style ? element.setAttribute('style', style) : element.removeAttribute('style');
    }
    const kind = assetKindForElement(element);
    if (kind) {
      const id = element.getAttribute('data-oss-id');
      if (!isOssId(id)) {
        element.remove();
        return;
      }
      element.setAttribute('data-oss-id', id);
      const attr = element.tagName === 'A' ? 'href' : 'src';
      element.setAttribute(attr, `oss://${id}`);
      [...element.attributes].forEach(attribute => {
        if (!['data-oss-id', attr, 'alt', 'controls', 'width', 'height', 'style', 'target', 'rel'].includes(attribute.name)) element.removeAttribute(attribute.name);
      });
      if (element.tagName === 'A') {
        element.setAttribute('target', '_blank');
        element.setAttribute('rel', 'noopener noreferrer');
      }
    } else if (element.tagName === 'A') {
      const href = element.getAttribute('href') ?? '';
      if (!/^https?:\/\//i.test(href)) element.removeAttribute('href');
      element.setAttribute('target', '_blank');
      element.setAttribute('rel', 'noopener noreferrer');
    }
  });
  return doc.body.innerHTML;
}

/** Converts canonical audio nodes to editor-safe inline placeholders. */
export function editorHtmlFromCanonical(html: string): string {
  if (!html || typeof DOMParser === 'undefined') return '';
  const doc = new DOMParser().parseFromString(html, 'text/html');
  doc.body.querySelectorAll('audio[data-oss-id]').forEach(element => {
    const id = element.getAttribute('data-oss-id');
    if (!isOssId(id)) return;
    const span = doc.createElement('span');
    span.setAttribute('data-richtext-asset-kind', 'audio');
    span.setAttribute('data-oss-id', id);
    span.textContent = '音频';
    element.replaceWith(span);
  });
  return doc.body.innerHTML;
}

export function resolveRichTextMedia(html: string, assets: readonly RichTextAssetAccess[]): string {
  if (!html || typeof DOMParser === 'undefined') return '';
  const byId = new Map(assets.map(asset => [asset.ossId, asset]));
  const doc = new DOMParser().parseFromString(canonicalizeRichText(html), 'text/html');
  doc.body.querySelectorAll('[data-oss-id]').forEach(element => {
    const id = element.getAttribute('data-oss-id');
    const asset = id ? byId.get(id) : undefined;
    if (!asset || asset.status !== 'available' || !asset.url) {
      element.setAttribute('data-richtext-unavailable', 'true');
      return;
    }
    element.setAttribute(element.tagName === 'A' ? 'href' : 'src', asset.url);
    if (asset.fileName && element.tagName === 'A' && !element.textContent?.trim()) element.textContent = asset.fileName;
  });
  return doc.body.innerHTML;
}
