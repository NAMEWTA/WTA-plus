export interface DomainModule {
  backendModules: readonly string[];
  capabilities: readonly string[];
  id: string;
}

export * from './routeAssembler';
export * from './navigationRecovery';

export type WebViewLoader<View = unknown> = () => Promise<View>;

export interface WebComponentRegistration<View = unknown> {
  componentName: string;
  load: WebViewLoader<View>;
}

export interface WebRegistration<View = unknown> extends WebComponentRegistration<View> {
  componentKey: string;
  id: string;
}

export interface WebMessageContribution {
  messages: Readonly<Record<string, string>>;
  namespace: string;
}

export interface WebPermissionContribution {
  id: string;
  permissions: readonly string[];
}

export interface WebDomainManifest<View = unknown> {
  domainId: string;
  id: string;
  messages: readonly WebMessageContribution[];
  permissions: readonly WebPermissionContribution[];
  registrations: readonly WebRegistration<View>[];
}

export type AppRuntimeErrorCode =
  | 'duplicate-component-key'
  | 'duplicate-domain-module'
  | 'duplicate-message-namespace'
  | 'duplicate-permission-contribution-id'
  | 'duplicate-web-domain-manifest'
  | 'duplicate-web-registration-id'
  | 'missing-component-key'
  | 'missing-domain-module'
  | 'missing-web-domain-manifest'
  | 'unselected-domain';

export interface AppRuntimeErrorOptions {
  appId: string;
  code: AppRuntimeErrorCode;
  componentKey: string;
  conflictingDomainId?: string;
  conflictingManifestId?: string;
  domainId: string;
  manifestId?: string;
  messageNamespace?: string;
  permissionContributionId?: string;
  registrationId?: string;
}

export class AppRuntimeError extends Error {
  readonly appId: string;
  readonly code: AppRuntimeErrorCode;
  readonly componentKey: string;
  readonly conflictingDomainId?: string;
  readonly conflictingManifestId?: string;
  readonly domainId: string;
  readonly manifestId?: string;
  readonly messageNamespace?: string;
  readonly permissionContributionId?: string;
  readonly registrationId?: string;

  constructor(options: AppRuntimeErrorOptions) {
    const details = [
      `app=${options.appId}`,
      `domain=${options.domainId}`,
      `key=${options.componentKey}`,
      options.manifestId && `manifest=${options.manifestId}`,
      options.messageNamespace && `namespace=${options.messageNamespace}`,
      options.permissionContributionId && `permissionContribution=${options.permissionContributionId}`,
      options.registrationId && `registration=${options.registrationId}`,
      options.conflictingDomainId && `conflictingDomain=${options.conflictingDomainId}`,
      options.conflictingManifestId && `conflictingManifest=${options.conflictingManifestId}`
    ].filter(Boolean);
    super(`[${options.code}] ${details.join(' ')}`);
    this.name = 'AppRuntimeError';
    this.appId = options.appId;
    this.code = options.code;
    this.componentKey = options.componentKey;
    this.conflictingDomainId = options.conflictingDomainId;
    this.conflictingManifestId = options.conflictingManifestId;
    this.domainId = options.domainId;
    this.manifestId = options.manifestId;
    this.messageNamespace = options.messageNamespace;
    this.permissionContributionId = options.permissionContributionId;
    this.registrationId = options.registrationId;
  }
}

export interface AppRuntime<View = unknown> {
  readonly appId: string;
  componentKeys(): string[];
  messages(): readonly WebMessageContribution[];
  permissionContributions(): readonly WebPermissionContribution[];
  resolve(input: { componentKey: string; domainId: string }): WebComponentRegistration<View>;
  webRegistrations(): readonly WebRegistration<View>[];
}

export interface ComposeAppRuntimeOptions<View = unknown> {
  appId: string;
  domainModules: readonly DomainModule[];
  manifests: readonly WebDomainManifest<View>[];
  selectedDomainIds: readonly string[];
  selectedManifestIds: readonly string[];
}

type ContributionOwner = {
  domainId: string;
  manifestId: string;
};

function uniqueById<T extends { id: string }>(
  items: readonly T[],
  createError: (duplicate: T) => AppRuntimeError
): Map<string, T> {
  const result = new Map<string, T>();
  for (const item of items) {
    if (result.has(item.id)) throw createError(item);
    result.set(item.id, item);
  }
  return result;
}

const freezeMessage = (contribution: WebMessageContribution): WebMessageContribution =>
  Object.freeze({ namespace: contribution.namespace, messages: Object.freeze({ ...contribution.messages }) });

const freezePermission = (contribution: WebPermissionContribution): WebPermissionContribution =>
  Object.freeze({ id: contribution.id, permissions: Object.freeze([...contribution.permissions]) });

const freezeRegistration = <View>(registration: WebRegistration<View>): WebRegistration<View> =>
  Object.freeze({
    id: registration.id,
    componentKey: registration.componentKey,
    componentName: registration.componentName,
    load: registration.load
  });

function orderedStrings(values: Iterable<string>): string[] {
  const result: string[] = [];
  for (const value of values) {
    const index = result.findIndex(existing => existing > value);
    if (index === -1) result.push(value);
    else result.splice(index, 0, value);
  }
  return result;
}

export function composeAppRuntime<View = unknown>({
  appId,
  domainModules,
  manifests,
  selectedDomainIds,
  selectedManifestIds
}: ComposeAppRuntimeOptions<View>): AppRuntime<View> {
  const selectedDomainIdSet = new Set(selectedDomainIds);
  const selectedManifestIdSet = new Set(selectedManifestIds);
  const domainsById = uniqueById(
    domainModules.filter(item => selectedDomainIdSet.has(item.id)),
    item =>
      new AppRuntimeError({
        appId,
        code: 'duplicate-domain-module',
        componentKey: '*',
        domainId: item.id
      })
  );
  const manifestsById = uniqueById(
    manifests.filter(item => selectedManifestIdSet.has(item.id)),
    item =>
      new AppRuntimeError({
        appId,
        code: 'duplicate-web-domain-manifest',
        componentKey: '*',
        domainId: item.domainId,
        manifestId: item.id
      })
  );

  for (const domainId of selectedDomainIdSet) {
    if (!domainsById.has(domainId))
      throw new AppRuntimeError({
        appId,
        code: 'missing-domain-module',
        componentKey: '*',
        domainId
      });
  }

  const selectedManifests: WebDomainManifest<View>[] = [];
  for (const manifestId of selectedManifestIdSet) {
    const manifest = manifestsById.get(manifestId);
    if (!manifest)
      throw new AppRuntimeError({
        appId,
        code: 'missing-web-domain-manifest',
        componentKey: '*',
        domainId: 'unknown',
        manifestId
      });
    if (!selectedDomainIdSet.has(manifest.domainId))
      throw new AppRuntimeError({
        appId,
        code: 'unselected-domain',
        componentKey: '*',
        domainId: manifest.domainId,
        manifestId
      });
    selectedManifests.push(manifest);
  }

  const components = new Map<string, { owner: ContributionOwner; registration: WebComponentRegistration<View> }>();
  const messages: WebMessageContribution[] = [];
  const messageOwners = new Map<string, ContributionOwner>();
  const permissionContributions: WebPermissionContribution[] = [];
  const permissionOwners = new Map<string, ContributionOwner>();
  const registrations: WebRegistration<View>[] = [];
  const registrationOwners = new Map<string, ContributionOwner>();

  for (const manifest of selectedManifests) {
    const owner = { domainId: manifest.domainId, manifestId: manifest.id };
    for (const contribution of manifest.messages) {
      const existing = messageOwners.get(contribution.namespace);
      if (existing)
        throw new AppRuntimeError({
          appId,
          code: 'duplicate-message-namespace',
          componentKey: '*',
          domainId: manifest.domainId,
          manifestId: manifest.id,
          messageNamespace: contribution.namespace,
          conflictingDomainId: existing.domainId,
          conflictingManifestId: existing.manifestId
        });
      messageOwners.set(contribution.namespace, owner);
      messages.push(freezeMessage(contribution));
    }

    for (const contribution of manifest.permissions) {
      const existing = permissionOwners.get(contribution.id);
      if (existing)
        throw new AppRuntimeError({
          appId,
          code: 'duplicate-permission-contribution-id',
          componentKey: '*',
          domainId: manifest.domainId,
          manifestId: manifest.id,
          permissionContributionId: contribution.id,
          conflictingDomainId: existing.domainId,
          conflictingManifestId: existing.manifestId
        });
      permissionOwners.set(contribution.id, owner);
      permissionContributions.push(freezePermission(contribution));
    }

    for (const sourceRegistration of manifest.registrations) {
      const existingRegistration = registrationOwners.get(sourceRegistration.id);
      if (existingRegistration)
        throw new AppRuntimeError({
          appId,
          code: 'duplicate-web-registration-id',
          componentKey: sourceRegistration.componentKey,
          domainId: manifest.domainId,
          manifestId: manifest.id,
          registrationId: sourceRegistration.id,
          conflictingDomainId: existingRegistration.domainId,
          conflictingManifestId: existingRegistration.manifestId
        });
      const existingComponent = components.get(sourceRegistration.componentKey);
      if (existingComponent)
        throw new AppRuntimeError({
          appId,
          code: 'duplicate-component-key',
          componentKey: sourceRegistration.componentKey,
          domainId: manifest.domainId,
          manifestId: manifest.id,
          registrationId: sourceRegistration.id,
          conflictingDomainId: existingComponent.owner.domainId,
          conflictingManifestId: existingComponent.owner.manifestId
        });
      const registration = freezeRegistration(sourceRegistration);
      registrationOwners.set(registration.id, owner);
      registrations.push(registration);
      components.set(registration.componentKey, { owner, registration });
    }
  }

  const frozenMessages = Object.freeze(messages);
  const frozenPermissions = Object.freeze(permissionContributions);
  const frozenRegistrations = Object.freeze(registrations);

  return Object.freeze({
    appId,
    componentKeys: () => orderedStrings(components.keys()),
    messages: () => frozenMessages,
    permissionContributions: () => frozenPermissions,
    resolve: ({ componentKey, domainId }) => {
      const component = components.get(componentKey);
      if (!component)
        throw new AppRuntimeError({
          appId,
          code: 'missing-component-key',
          componentKey,
          domainId
        });
      return component.registration;
    },
    webRegistrations: () => frozenRegistrations
  });
}
