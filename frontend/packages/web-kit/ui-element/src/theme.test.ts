import { describe, expect, it } from 'vitest';
import { applyPrimaryColor, initializeTheme } from './theme';

function target() {
  const values = new Map<string, string>();
  const classes = new Set<string>();
  return {
    values,
    classes,
    style: {
      setProperty: (name: string, value: string | null) => {
        values.set(name, value ?? '');
      }
    },
    classList: {
      toggle: (name: string, enabled?: boolean) => {
        if (enabled) classes.add(name);
        else classes.delete(name);
        return Boolean(enabled);
      }
    }
  };
}
describe('shared theme initialization', () => {
  it('restores the existing Admin appearance before rendering in any App', () => {
    const root = target();
    initializeTheme({
      root,
      storage: { getItem: key => (key === 'layout-setting' ? '{"theme":"#409EFF","radiusBase":20}' : 'dark') }
    });
    expect(root.classes.has('dark')).toBe(true);
    expect(root.values.get('--app-radius-base')).toBe('20px');
    expect(root.values.get('--el-color-primary')).toBe('#409eff');
    expect(root.values.get('--app-action-primary')).toBe('#2b6bd3');
  });
  it('keeps first visits light and works when browser storage is blocked', () => {
    const root = target();
    initializeTheme({
      root,
      storage: {
        getItem: () => {
          throw new Error('blocked');
        }
      },
      systemDark: true
    });
    expect(root.classes.has('light')).toBe(true);
    expect(root.values.get('--app-radius-base')).toBe('14px');
  });
  it('ignores malformed stored colors and bounds legacy radius settings', () => {
    const root = target();
    initializeTheme({
      root,
      storage: { getItem: key => (key === 'layout-setting' ? '{"theme":"url(unsafe)","radiusBase":99}' : 'auto') },
      systemDark: true
    });
    expect(root.values.get('--el-color-primary')).toBe('#409eff');
    expect(root.values.get('--app-radius-base')).toBe('32px');
    expect(root.classes.has('dark')).toBe(true);
  });
  it('keeps white foreground readable even for a custom pale primary', () => {
    const root = target();
    applyPrimaryColor('#ffffff', root);
    const action = root.values.get('--app-action-primary') ?? '';
    const values = [1, 3, 5]
      .map(index => parseInt(action.slice(index, index + 2), 16) / 255)
      .map(value => (value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4));
    const brightness = values[0] * 0.2126 + values[1] * 0.7152 + values[2] * 0.0722;
    expect(1.05 / (brightness + 0.05)).toBeGreaterThanOrEqual(4.5);
  });
});
