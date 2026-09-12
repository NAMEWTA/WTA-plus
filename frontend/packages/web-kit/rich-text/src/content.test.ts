import { describe, expect, it } from 'vitest';
import { canonicalizeRichText, editorHtmlFromCanonical, extractOssIds, resolveRichTextMedia } from './content';

describe('rich text content protocol', () => {
  it('canonicalizes media URLs to OSS markers and is idempotent', () => {
    const html = '<h2>Title</h2><strong>Bold</strong><img data-oss-id="123" src="https://signed.example/a.png" onerror="x">';
    const canonical = canonicalizeRichText(html);
    expect(canonical).toContain('src="oss://123"');
    expect(canonical).not.toContain('onerror');
    expect(canonicalizeRichText(canonical)).toBe(canonical);
  });
  it('rejects invalid media IDs and external media', () => {
    expect(canonicalizeRichText('<img data-oss-id="0" src="data:image/png;base64,x"><video src="https://x">')).not.toContain('<img');
    expect(extractOssIds('<img data-oss-id="12" src="oss://12"><img data-oss-id="bad">')).toEqual(['12']);
  });
  it('resolves only authorized assets', () => {
    const html = resolveRichTextMedia('<img data-oss-id="12" src="oss://12"><a data-oss-id="13" href="oss://13">x</a>', [{ ossId: '12', status: 'available', url: '/download/12' }, { ossId: '13', status: 'unavailable' }]);
    expect(html).toContain('src="/download/12"');
    expect(html).toContain('data-richtext-unavailable="true"');
  });
  it('keeps audio editable through an editor-safe placeholder', () => {
    const editorHtml = editorHtmlFromCanonical('<audio data-oss-id="14" src="oss://14" controls></audio>');
    expect(editorHtml).toContain('data-richtext-asset-kind="audio"');
    expect(canonicalizeRichText(editorHtml)).toContain('<audio');
  });
});
