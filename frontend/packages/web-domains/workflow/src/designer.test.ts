import { describe, expect, it } from 'vitest';
import { isDesignerCloseMessage } from './designer';

const frame = {} as Window;
const base = 'https://admin.example/app/workflow/design';
const event = { source: frame, origin: 'https://admin.example', data: { method: 'close' } };

describe('owned designer messages', () => {
  it('accepts the vendor close payload from a relative designer URL', () => {
    expect(isDesignerCloseMessage(event, frame, '/prod-api/warm-flow-ui/index.html', base)).toBe(true);
  });
  it('uses the configured absolute designer origin, including its port', () => {
    const url = 'https://designer.example:8443/index.html';
    expect(isDesignerCloseMessage({ ...event, origin: 'https://designer.example:8443' }, frame, url, base)).toBe(true);
    expect(isDesignerCloseMessage({ ...event, origin: 'https://designer.example' }, frame, url, base)).toBe(false);
  });
  it.each([null, undefined, {} as Window])('rejects an absent or different current window: %s', current => {
    expect(isDesignerCloseMessage(event, current, '/designer', base)).toBe(false);
  });
  it.each(['https://evil.example', 'http://admin.example', 'https://admin.example.evil', 'null', ''])('rejects origin %s', origin => {
    expect(isDesignerCloseMessage({ ...event, origin }, frame, '/designer', base)).toBe(false);
  });
  it.each([null, 'close', { method: 'save' }, { method: 'publish' }, { method: 1 }, [{ method: 'close' }], Object.create({ method: 'close' })])('rejects unknown payload %j', data => {
    expect(isDesignerCloseMessage({ ...event, data }, frame, '/designer', base)).toBe(false);
  });
  it.each(['', 'http://[', 'javascript:close()', 'data:text/html,', 'about:blank'])('rejects missing, malformed or opaque designer URL %s', url => {
    expect(isDesignerCloseMessage({ ...event, origin: 'null' }, frame, url, base)).toBe(false);
  });
  it('rejects a detached frame after replacement even when its origin still matches', () => {
    const replacement = {} as Window;
    expect(isDesignerCloseMessage(event, replacement, '/designer', base)).toBe(false);
    expect(isDesignerCloseMessage({ ...event, source: replacement }, replacement, '/designer', base)).toBe(true);
  });
});
