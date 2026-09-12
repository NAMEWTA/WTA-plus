export type { AiApiResponse, SnailOpenApiUser } from './types';
export { createAiService, type AiService } from '../index';

export const aiSnailResource = Object.freeze({ controller: 'SnailAiController', basePath: '/snail-ai' });
