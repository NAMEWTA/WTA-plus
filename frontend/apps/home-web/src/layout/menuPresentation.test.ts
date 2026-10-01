import type { RouteRecordRaw } from 'vue-router';
import { describe, expect, it } from 'vitest';
import { homeMenuPath, singleVisibleMenuChild } from './menuPresentation';

describe('Home menu presentation', () => {
  it('uses the real title, icon and path for a server-generated top-level layout wrapper', () => {
    const leaf = { path: 'taskWaiting', meta: { title: '待办任务', icon: 'tabler:list-check' } } as RouteRecordRaw;
    const wrapper = { path: '/', children: [leaf, { path: 'review', hidden: true }] } as RouteRecordRaw;
    expect(singleVisibleMenuChild(wrapper)).toBe(leaf);
    expect(homeMenuPath(wrapper.path, leaf.path)).toBe('/taskWaiting');
    expect(wrapper.children).toHaveLength(2);
  });
  it('preserves explicit groups, multiple visible children and nested groups', () => {
    const leaf = { path: 'profile' } as RouteRecordRaw;
    expect(singleVisibleMenuChild({ path: '/', alwaysShow: true, children: [leaf] } as RouteRecordRaw)).toBeUndefined();
    expect(
      singleVisibleMenuChild({ path: '/', children: [leaf, { path: 'tasks' }] } as RouteRecordRaw)
    ).toBeUndefined();
    expect(
      singleVisibleMenuChild({ path: '/', children: [{ path: 'group', children: [leaf] }] } as RouteRecordRaw)
    ).toBeUndefined();
    expect(homeMenuPath('/tasks', '/profile')).toBe('/profile');
  });
});
