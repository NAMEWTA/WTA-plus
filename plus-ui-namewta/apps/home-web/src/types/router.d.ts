import type { RouteMeta as VueRouteMeta } from 'vue-router';

declare module 'vue-router' {
  interface RouteMeta extends VueRouteMeta {
    activeMenu?: string;
    icon?: string;
    link?: string;
    noCache?: boolean;
    title?: string;
  }

  interface _RouteRecordBase {
    alwaysShow?: boolean;
    ext?: string;
    hidden?: boolean | string | number;
    permissions?: string[];
    query?: string;
  }
}
