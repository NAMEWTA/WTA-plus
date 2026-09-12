export function parseTime(time: unknown) {
  if (!time) return null;
  const date = new Date(typeof time === 'string' ? time.replace(/-/g, '/').replace('T', ' ') : (time as number));
  const part = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${part(date.getMonth() + 1)}-${part(date.getDate())} ${part(date.getHours())}:${part(date.getMinutes())}:${part(date.getSeconds())}`;
}
export function selectDictLabel(options: readonly { label: string; value: string }[], value: string | number) {
  return options.find(item => item.value === String(value))?.label ?? String(value ?? '');
}
