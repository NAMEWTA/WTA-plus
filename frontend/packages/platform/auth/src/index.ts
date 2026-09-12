import type { ErrorPresenter, NavigationPort, SessionPort } from '@namewta/platform-contracts';

export interface ReloginState {
  show: boolean;
}

export interface ReloginDependencies {
  navigation: NavigationPort;
  presenter: ErrorPresenter;
  session: SessionPort;
  state: ReloginState;
}

export function requestRelogin({ navigation, presenter, session, state }: ReloginDependencies): void {
  if (state.show) return;
  state.show = true;
  void (async () => {
    try {
      await presenter.confirmSessionExpired();
      await session.logout();
      await navigation.replaceWithLogin(encodeURIComponent(navigation.currentLocation() || '/'));
    } catch {
      // Cancellation and unavailable UI/session adapters both end the recovery attempt.
    } finally {
      state.show = false;
    }
  })();
}
