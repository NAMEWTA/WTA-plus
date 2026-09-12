import type { MaterialNode, MaterialNodeType } from '@namewta/domain-profile';

export function allowedChildTypes(parent: MaterialNode): readonly MaterialNodeType[] {
  if (parent.nodeType !== 'CATEGORY') return [];
  if (parent.nodeDepth === 1) return ['CATEGORY', 'TAG'];
  if (parent.nodeDepth === 2) return ['TAG'];
  return [];
}

export function canEditMaterialCode(node: MaterialNode): boolean {
  return node.nodeType === 'TAG' && !node.systemRequired;
}

export function canChangeMaterialLifecycle(node: MaterialNode): boolean {
  return !node.systemRequired;
}

export function materialNodeTypeLabel(nodeType: MaterialNodeType): string {
  return nodeType === 'CATEGORY' ? '分类' : '标签';
}

export function countMaterialNodes(nodes: readonly MaterialNode[]): number {
  return nodes.reduce((total, node) => total + 1 + countMaterialNodes(node.children), 0);
}

export async function executeMaterialCommand(
  command: () => Promise<unknown>,
  onSuccess: () => void,
  onFailure: (error: unknown) => void
): Promise<boolean> {
  try {
    await command();
    onSuccess();
    return true;
  } catch (error) {
    onFailure(error);
    return false;
  }
}
