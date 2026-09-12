import {
  createRenderer,
  h,
  resolveDirective,
  withDirectives,
  type App,
  type Directive,
  type DirectiveBinding
} from 'vue';
import { createAccessEvaluator, type AccessEvaluator } from '@namewta/platform-permission';
import { describe, expect, it, vi } from 'vitest';
import { installWebPermissionHost } from './index';

type MountedDirective = {
  mounted(element: HTMLElement, binding: DirectiveBinding<readonly string[]>): void;
};

function install(provider: () => AccessEvaluator) {
  const directives = new Map<string, Directive>();
  const app = {
    directive(name: string, directive: Directive) {
      directives.set(name, directive);
      return app;
    }
  } as unknown as App;
  installWebPermissionHost(app, provider);
  return directives;
}

function run(directive: Directive | undefined, value: unknown) {
  const removeChild = vi.fn();
  const element = { parentNode: { removeChild } } as unknown as HTMLElement;
  (directive as MountedDirective).mounted(element, { value } as DirectiveBinding<readonly string[]>);
  return removeChild;
}

interface HostNode {
  parentNode: HostElement | null;
}

interface HostElement extends HostNode {
  children: HostNode[];
  removeChild(child: HostNode): void;
  tag: string;
  text: string;
}

function createHostElement(tag: string): HostElement {
  const element: HostElement = {
    children: [],
    parentNode: null,
    removeChild(child) {
      const index = element.children.indexOf(child);
      if (index >= 0) element.children.splice(index, 1);
      child.parentNode = null;
    },
    tag,
    text: ''
  };
  return element;
}

function mountDirective(value: unknown, provider: () => AccessEvaluator | undefined) {
  const errors: unknown[] = [];
  const renderer = createRenderer<HostNode, HostElement>({
    createComment: () => ({ parentNode: null }),
    createElement: tag => createHostElement(tag),
    createText: () => ({ parentNode: null }),
    insert(child, parent, anchor) {
      const index = anchor ? parent.children.indexOf(anchor) : -1;
      parent.children.splice(index >= 0 ? index : parent.children.length, 0, child);
      child.parentNode = parent;
    },
    nextSibling(node) {
      if (!node.parentNode) return null;
      const index = node.parentNode.children.indexOf(node);
      return node.parentNode.children[index + 1] ?? null;
    },
    parentNode: node => node.parentNode,
    patchProp: () => undefined,
    remove(node) {
      node.parentNode?.removeChild(node);
    },
    setElementText(element, text) {
      element.text = text;
    },
    setText: () => undefined
  });
  const root = createHostElement('root');
  const app = renderer.createApp({
    render() {
      const directive = resolveDirective('hasPermi');
      if (!directive) throw new Error('v-hasPermi is unavailable');
      return withDirectives(h('button'), [[directive, value]]);
    }
  });
  installWebPermissionHost(app as unknown as App, provider);
  app.config.errorHandler = error => errors.push(error);
  app.mount(root);
  return { errors, root };
}

describe('web permission host', () => {
  it('registers only the two public permission directives', () => {
    const directives = install(() => createAccessEvaluator({ permissions: [], roles: [] }));

    expect([...directives.keys()]).toEqual(['hasPermi', 'hasRoles']);
  });

  it.each([
    { name: 'hasPermi', snapshot: { permissions: ['system:user:add'] }, required: ['system:user:add'] },
    { name: 'hasPermi', snapshot: { permissions: ['*:*:*'] }, required: ['system:user:add'] },
    { name: 'hasRoles', snapshot: { roles: ['operator'] }, required: ['operator'] },
    { name: 'hasRoles', snapshot: { roles: ['superadmin'] }, required: ['operator'] }
  ])('keeps an element when $name matches the platform evaluator', ({ name, snapshot, required }) => {
    const directives = install(() => createAccessEvaluator(snapshot));

    expect(run(directives.get(name), required)).not.toHaveBeenCalled();
  });

  it.each([
    { name: 'hasPermi', required: ['system:user:add'] },
    { name: 'hasRoles', required: ['operator'] }
  ])('removes an element when $name fails closed', ({ name, required }) => {
    const directives = install(() => createAccessEvaluator({ permissions: [], roles: [] }));

    expect(run(directives.get(name), required)).toHaveBeenCalledTimes(1);
  });

  it('reads the current evaluator every time a directive is mounted', () => {
    let evaluator = createAccessEvaluator({ permissions: [], roles: [] });
    const directives = install(() => evaluator);

    expect(run(directives.get('hasPermi'), ['system:user:add'])).toHaveBeenCalledTimes(1);
    evaluator = createAccessEvaluator({ permissions: ['system:user:add'], roles: [] });
    expect(run(directives.get('hasPermi'), ['system:user:add'])).not.toHaveBeenCalled();
  });

  it.each([undefined, 'system:user:add', [], [''], ['system:user:add', '  ']])(
    'rejects an invalid directive binding: %j',
    value => {
      const directives = install(() => createAccessEvaluator({ permissions: ['*:*:*'], roles: ['superadmin'] }));

      expect(() => run(directives.get('hasPermi'), value)).toThrow('v-hasPermi requires a non-empty string array');
    }
  );

  it('fails explicitly when the evaluator provider has no current identity', () => {
    const directives = install(() => undefined as unknown as AccessEvaluator);

    expect(() => run(directives.get('hasRoles'), ['operator'])).toThrow('AccessEvaluator is unavailable');
  });

  it.each([
    { label: 'an invalid binding', value: [] },
    { label: 'a missing evaluator', value: ['system:user:add'] }
  ])('removes the mounted element before reporting $label', ({ label, value }) => {
    const provider = label === 'a missing evaluator' ? () => undefined : () => createAccessEvaluator({});

    const { errors, root } = mountDirective(value, provider);

    expect(root.children).toHaveLength(0);
    expect(errors).toHaveLength(1);
    expect(errors[0]).toBeInstanceOf(Error);
  });
});
