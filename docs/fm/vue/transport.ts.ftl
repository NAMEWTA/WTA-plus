import type { OpenApiSchema } from '@namewta/api-contracts';
import type { ${BusinessName}VO } from './types';

export type ${BusinessName}Transport = OpenApiSchema<'${ClassName}Vo'>;

const requireTransportField = <T>(value: T | undefined, field: string): T => {
  if (value === undefined) throw new Error(`Invalid ${BusinessName} transport: missing ${r'${field}'}`);
  return value;
};

export function project${BusinessName}Transport(value: ${BusinessName}Transport): ${BusinessName}VO {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('Invalid ${BusinessName} transport: expected an object');
  }
  return {
<#list columns as column>
<#if column.list || column.pk || column.insert || column.edit>
    ${column.javaField}: requireTransportField(value.${column.javaField}, '${column.javaField}') as ${BusinessName}VO['${column.javaField}'],
<#if column.htmlType == "imageUpload">
    ${column.javaField}Url: value.${column.javaField}Url ?? '',
</#if>
</#if>
</#list>
<#if table.tree>
    children: []
</#if>
  };
}

<#if table.tree>
const treeKey = (value: unknown): string => `${r'${typeof value}'}:${r'${String(value)}'}`;

const compareTreeOrder = (left: ${BusinessName}VO, right: ${BusinessName}VO): number =>
  String(left.${treeOrderField} ?? '').localeCompare(String(right.${treeOrderField} ?? ''), undefined, { numeric: true });

export function build${BusinessName}Tree(values: readonly ${BusinessName}VO[]): ${BusinessName}VO[] {
  type MutableNode = Omit<${BusinessName}VO, 'children'> & { children: MutableNode[] };
  const nodes = new Map<string, MutableNode>();
  const roots: MutableNode[] = [];
  const rootKey = treeKey(${treeRootValueTsLiteral});

  for (const value of values) {
    const key = treeKey(value.${treeCode});
    if (nodes.has(key)) throw new Error(`Invalid ${BusinessName} tree: duplicate id ${r'${String(value.${treeCode})}'}`);
    nodes.set(key, { ...value, children: [] });
  }

  for (const node of nodes.values()) {
    const parentKey = treeKey(node.${treeParentCode});
    if (parentKey === rootKey) {
      roots.push(node);
      continue;
    }
    const parent = nodes.get(parentKey);
    if (!parent) throw new Error(`Invalid ${BusinessName} tree: orphan id ${r'${String(node.${treeCode})}'}`);
    parent.children.push(node);
  }

  const visiting = new Set<string>();
  const visited = new Set<string>();
  const visit = (node: MutableNode): void => {
    const key = treeKey(node.${treeCode});
    if (visiting.has(key)) throw new Error(`Invalid ${BusinessName} tree: cycle at ${r'${String(node.${treeCode})}'}`);
    if (visited.has(key)) return;
    visiting.add(key);
    node.children.sort(compareTreeOrder).forEach(visit);
    visiting.delete(key);
    visited.add(key);
  };
  nodes.forEach(visit);
  roots.sort(compareTreeOrder);
  return roots;
}
</#if>
