import type { components, operations, paths } from '../generated/openapi';

export type { components, operations, paths } from '../generated/openapi';

export type OpenApiSchema<Name extends keyof components['schemas']> = components['schemas'][Name];

export type OpenApiOperation<Name extends keyof operations> = operations[Name];

export type OpenApiPath<Name extends keyof paths> = paths[Name];
