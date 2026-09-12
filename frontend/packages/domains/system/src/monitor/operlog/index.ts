export type { OperLogForm, OperLogQuery, OperLogVO } from './types';

export const systemMonitorOperlogResource = Object.freeze({
  controller: 'SysOperlogController',
  basePath: '/monitor/operlog'
});
