export interface ProtectedNavigationRecoveryOptions<Route, Result> {
  addRoute(route: Route): void;
  createReplacement(): Result;
  isExternal(route: Route): boolean;
  isCurrent?(): boolean;
  loadIdentity(): Promise<void>;
  loadRoutes(): Promise<readonly Route[]>;
}

export async function restoreProtectedNavigation<Route, Result>({
  addRoute,
  createReplacement,
  isExternal,
  isCurrent = () => true,
  loadIdentity,
  loadRoutes
}: ProtectedNavigationRecoveryOptions<Route, Result>): Promise<Result> {
  await loadIdentity();
  if (!isCurrent()) throw new Error('Session changed during navigation recovery');
  const routes = await loadRoutes();
  if (!isCurrent()) throw new Error('Session changed during navigation recovery');
  for (const route of routes) {
    if (!isExternal(route)) addRoute(route);
  }
  return createReplacement();
}
