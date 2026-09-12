export { create${BusinessName}Service, type ${BusinessName}Service } from './service';
export {
<#if table.tree>
  build${BusinessName}Tree,
</#if>
  project${BusinessName}Transport,
  type ${BusinessName}Transport
} from './transport';
export type {
  ${BusinessName}Form,
  ${BusinessName}Query,
  ${BusinessName}VO
} from './types';

export const ${businessName}Resource = Object.freeze({
  controller: '${ClassName}Controller',
  basePath: '/${moduleName}/${businessName}'
});
