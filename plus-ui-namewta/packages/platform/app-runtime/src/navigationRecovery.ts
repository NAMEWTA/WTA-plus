export interface ProtectedNavigationRecoveryOptions<Route, Result> {
  addRoute(route: Route): void;
  createReplacement(): Result;
  isExternal(route: Route): boolean;
  loadIdentity(): Promise<void>;
  loadRoutes(): Promise<readonly Route[]>;
}

export async function restoreProtectedNavigation<Route, Result>({
  addRoute,
  createReplacement,
  isExternal,
  loadIdentity,
  loadRoutes
}: ProtectedNavigationRecoveryOptions<Route, Result>): Promise<Result> {
  await loadIdentity();
  const routes = await loadRoutes();
  for (const route of routes) {
    if (!isExternal(route)) addRoute(route);
  }
  return createReplacement();
}
