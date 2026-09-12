import { describe, expect, it } from 'vitest';
import profile from './index.vue?raw';
import wrapper from './openApi.vue?raw';

describe('profile OpenAPI composition', () => {
  it('adds a static personal OpenAPI tab backed by the shared current-user workspace', () => {
    expect(profile).toContain('label="OpenAPI"');
    expect(profile).toContain('name="openApi"');
    expect(profile).toContain('<open-api />');
    expect(wrapper).toContain('<open-api-workspace');
    expect(wrapper).toContain("kind: 'current-user'");
    expect(wrapper).not.toContain('userId:');
  });
});
