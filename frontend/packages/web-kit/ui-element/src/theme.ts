export const defaultPrimary = '#409eff';

interface ThemeTarget {
  style: Pick<CSSStyleDeclaration, 'setProperty'>;
  classList: Pick<DOMTokenList, 'toggle'>;
}

interface ThemeOptions {
  root?: ThemeTarget;
  storage?: Pick<Storage, 'getItem'>;
  systemDark?: boolean;
}

const validColor = (value: unknown): value is string => typeof value === 'string' && /^#[\da-f]{6}$/i.test(value);
const channels = (color: string) => [1, 3, 5].map(index => parseInt(color.slice(index, index + 2), 16));
const hex = (values: number[]) => '#' + values.map(value => Math.floor(value).toString(16).padStart(2, '0')).join('');
const mix = (color: string, amount: number, target: number) =>
  hex(channels(color).map(value => value + (target - value) * amount));

function luminance(color: string): number {
  return channels(color)
    .map(value => value / 255)
    .map(value => (value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4))
    .reduce((sum, value, index) => sum + value * ([0.2126, 0.7152, 0.0722][index] ?? 0), 0);
}

/** 保留品牌色，白字操作色单独加深，避免浅色主题按钮失去对比度。 */
export function applyPrimaryColor(value: string, root: ThemeTarget = document.documentElement): void {
  const primary = validColor(value) ? value.toLowerCase() : defaultPrimary;
  let action = primary === defaultPrimary ? '#2b6bd3' : primary;
  while (1.05 / (luminance(action) + 0.05) < 4.6) action = mix(action, 0.08, 0);
  root.style.setProperty('--el-color-primary', primary);
  root.style.setProperty('--app-action-primary', action);
  root.style.setProperty('--app-action-primary-hover', mix(action, 0.12, 0));
  root.style.setProperty('--app-action-primary-active', mix(action, 0.22, 0));
  for (let index = 1; index <= 9; index++) {
    root.style.setProperty(`--el-color-primary-light-${index}`, mix(primary, index / 10, 255));
    root.style.setProperty(`--el-color-primary-dark-${index}`, mix(primary, index / 10, 0));
  }
}

/** 由每个 App 挂载前显式调用；只恢复当前 Origin 的外观设置。 */
export function initializeTheme(options: ThemeOptions = {}): void {
  const root = options.root ?? document.documentElement;
  let primary = defaultPrimary;
  let radius = 14;
  let dark = false;
  try {
    const storage = options.storage ?? window.localStorage;
    const raw = storage.getItem('layout-setting');
    const setting: unknown = raw ? JSON.parse(raw) : undefined;
    if (setting && typeof setting === 'object') {
      if ('theme' in setting && validColor(setting.theme)) primary = setting.theme;
      if ('radiusBase' in setting && typeof setting.radiusBase === 'number' && Number.isFinite(setting.radiusBase))
        radius = Math.min(32, Math.max(0, setting.radiusBase));
    }
    const mode = storage.getItem('useDarkKey');
    dark =
      mode === 'dark' ||
      (mode === 'auto' && (options.systemDark ?? window.matchMedia('(prefers-color-scheme: dark)').matches));
  } catch {
    /* 存储禁用或历史值损坏时仍使用可用的默认主题。 */
  }
  root.classList.toggle('dark', dark);
  root.classList.toggle('light', !dark);
  root.style.setProperty('--app-radius-base', `${radius}px`);
  applyPrimaryColor(primary, root);
}
