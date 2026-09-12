import { describe, expect, it } from 'vitest';
import { projectAiUserTransport, type AiUserTransport } from './transport';

describe('AI OpenAPI transport boundary', () => {
  it('projects the generated registration response into the stable domain user', () => {
    const transport: AiUserTransport = { openId: 'open-1', nickname: 'Name', created: true };
    expect(projectAiUserTransport(transport)).toEqual(transport);
  });
});
