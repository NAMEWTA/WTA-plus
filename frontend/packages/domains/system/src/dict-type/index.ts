export * from './public';
export type { DictTypeForm, DictTypeQuery, DictTypeVO } from './types';

export const systemDictTypeResource = Object.freeze({
  controller: 'SysDictTypeController',
  basePath: '/system/dict/type'
});
