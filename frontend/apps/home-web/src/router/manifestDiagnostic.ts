import type { MissingComponentDiagnostic } from '@namewta/platform-app-runtime';
import { defineComponent, h, type Component } from 'vue';

export const formatHomeManifestDiagnostic = (details: MissingComponentDiagnostic): string =>
  `页面组件不可用 [${details.code}] app=${details.appId} domain=${details.domainId} key=${details.componentKey}`;

export function createHomeManifestDiagnostic(details: MissingComponentDiagnostic): Component {
  const message = formatHomeManifestDiagnostic(details);
  return defineComponent({
    name: 'HomeManifestRouteDiagnostic',
    setup: () => () =>
      h('main', { class: 'home-route-diagnostic', role: 'alert', 'data-testid': 'home-route-diagnostic' }, [
        h('h1', '页面暂不可用'),
        h('p', message)
      ])
  });
}
