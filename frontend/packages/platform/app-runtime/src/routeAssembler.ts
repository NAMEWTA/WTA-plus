export interface ServerRouteNode<Component = unknown, Meta = unknown> {
  alwaysShow?: boolean;
  children?: readonly ServerRouteNode<Component, Meta>[];
  component?: string | Component;
  ext?: string;
  hidden?: boolean;
  meta?: Meta;
  name?: string;
  path: string;
  permissions?: readonly string[];
  query?: string;
  redirect?: string;
}

export interface NamedRouteNode {
  children?: readonly NamedRouteNode[];
  name?: unknown;
  path?: unknown;
}

export interface MissingComponentDiagnostic {
  appId: string;
  code: 'missing-component-key';
  componentKey: string;
  domainId: string;
}

export interface RouteRegistration<Component> {
  componentName: string;
  load: Component;
}

export interface AssembleServerRoutesOptions<Component, Meta = unknown> {
  appId: string;
  createDiagnostic(details: MissingComponentDiagnostic): Component;
  resolveRegistration(input: {
    componentKey: string;
    domainId: string;
    routeName?: string;
  }): RouteRegistration<Component> | undefined;
  routes: readonly ServerRouteNode<Component, Meta>[];
  specialComponents?: Readonly<Record<string, Component>>;
}

export interface ProjectServerRoutesOptions<Component, Meta = unknown>
  extends AssembleServerRoutesOptions<Component, Meta> {
  flattenParentView?: boolean;
  parentViewComponentKey?: string;
}

export interface DuplicateRouteNameDiagnostic {
  code: 'duplicate-route-name';
  routeName: string;
}

export function inferDomainId(componentKey: string): string {
  const [domainId] = componentKey.split('/').filter(Boolean);
  return domainId || 'unknown';
}

export function assembleServerRoutes<Component, Meta = unknown>({
  appId,
  createDiagnostic,
  resolveRegistration,
  routes,
  specialComponents = {}
}: AssembleServerRoutesOptions<Component, Meta>): ServerRouteNode<Component, Meta>[] {
  return routes.map(route => {
    const assembled: ServerRouteNode<Component, Meta> = { ...route };
    if (Array.isArray(route.children)) {
      assembled.children = assembleServerRoutes({
        appId,
        createDiagnostic,
        resolveRegistration,
        routes: route.children,
        specialComponents
      });
    }
    if (typeof route.component !== 'string') return assembled;

    if (Object.prototype.hasOwnProperty.call(specialComponents, route.component)) {
      assembled.component = specialComponents[route.component];
      return assembled;
    }
    const domainId = inferDomainId(route.component);
    const registration = resolveRegistration({
      componentKey: route.component,
      domainId,
      routeName: typeof route.name === 'string' ? route.name : undefined
    });
    assembled.component =
      registration?.load ??
      createDiagnostic({
        appId,
        code: 'missing-component-key',
        componentKey: route.component,
        domainId
      });
    return assembled;
  });
}

function joinRoutePath(parentPath: string, childPath: string): string {
  return `${parentPath.replace(/\/$/, '')}/${childPath.replace(/^\//, '')}`;
}

function normalizeProjection<Component, Meta>(
  routes: readonly ServerRouteNode<Component, Meta>[],
  flattenParentView: boolean,
  parentViewComponentKey: string,
  parentViewPath?: string
): ServerRouteNode<Component, Meta>[] {
  const result: ServerRouteNode<Component, Meta>[] = [];
  for (const source of routes) {
    const route: ServerRouteNode<Component, Meta> = { ...source };
    const sourcePath = source.path;
    if (parentViewPath && sourcePath) route.path = joinRoutePath(parentViewPath, sourcePath);

    const children: readonly ServerRouteNode<Component, Meta>[] = Array.isArray(source.children)
      ? source.children
      : [];
    if (flattenParentView && source.component === parentViewComponentKey && children.length > 0) {
      result.push(
        ...normalizeProjection(
          children,
          flattenParentView,
          parentViewComponentKey,
          typeof route.path === 'string' ? route.path : undefined
        )
      );
      continue;
    }

    if (children.length > 0) {
      route.children = normalizeProjection(children, flattenParentView, parentViewComponentKey);
    } else {
      delete route.children;
      delete route.redirect;
    }
    result.push(route);
  }
  return result;
}

export function projectServerRoutes<Component, Meta = unknown>({
  flattenParentView = false,
  parentViewComponentKey = 'ParentView',
  ...assembleOptions
}: ProjectServerRoutesOptions<Component, Meta>): ServerRouteNode<Component, Meta>[] {
  return assembleServerRoutes({
    ...assembleOptions,
    routes: normalizeProjection(assembleOptions.routes, flattenParentView, parentViewComponentKey)
  });
}

export function findDuplicateRouteNames(
  routeGroups: readonly (readonly NamedRouteNode[])[]
): readonly DuplicateRouteNameDiagnostic[] {
  const seen = new Set<string>();
  const reported = new Set<string>();
  const diagnostics: DuplicateRouteNameDiagnostic[] = [];

  const visit = (routes: readonly NamedRouteNode[]) => {
    for (const route of routes) {
      const routeName = typeof route.name === 'string' ? route.name.trim() : '';
      if (routeName) {
        if (seen.has(routeName) && !reported.has(routeName)) {
          reported.add(routeName);
          diagnostics.push(Object.freeze({ code: 'duplicate-route-name', routeName }));
        }
        seen.add(routeName);
      }
      if (Array.isArray(route.children)) visit(route.children);
    }
  };

  for (const routes of routeGroups) visit(routes);
  return Object.freeze(diagnostics);
}
