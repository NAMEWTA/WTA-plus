import type { AccessEvaluator } from '@namewta/platform-permission';
import type { App, Directive, DirectiveBinding } from 'vue';

export type AccessEvaluatorProvider = () => AccessEvaluator | undefined;

function requireValues(value: unknown, directiveName: string): readonly string[] {
  if (!Array.isArray(value) || value.length === 0 || value.some(item => typeof item !== 'string' || !item.trim())) {
    throw new Error(`v-${directiveName} requires a non-empty string array`);
  }
  return value;
}

function requireEvaluator(provider: AccessEvaluatorProvider): AccessEvaluator {
  const evaluator = provider();
  if (!evaluator) throw new Error('AccessEvaluator is unavailable');
  return evaluator;
}

function createPermissionDirective(
  directiveName: 'hasPermi' | 'hasRoles',
  provider: AccessEvaluatorProvider
): Directive<HTMLElement, unknown> {
  return {
    mounted(element: HTMLElement, binding: DirectiveBinding<unknown>) {
      try {
        const required = requireValues(binding.value, directiveName);
        const evaluator = requireEvaluator(provider);
        const allowed =
          directiveName === 'hasPermi' ? evaluator.hasAnyPermission(required) : evaluator.hasAnyRole(required);
        if (!allowed) element.parentNode?.removeChild(element);
      } catch (error) {
        element.parentNode?.removeChild(element);
        throw error;
      }
    }
  };
}

export function installWebPermissionHost(app: App, provider: AccessEvaluatorProvider): void {
  app.directive('hasPermi', createPermissionDirective('hasPermi', provider));
  app.directive('hasRoles', createPermissionDirective('hasRoles', provider));
}
