export const profilePermissions = Object.freeze({
  materialTag: Object.freeze({
    manage: 'profile:material-tag:manage',
    query: 'profile:material-tag:query'
  }),
  person: Object.freeze({
    apply: 'profile:person:apply',
    manage: 'profile:person:manage',
    material: 'profile:person:material',
    override: 'profile:person:override',
    query: 'profile:person:query',
    review: 'profile:person:review'
  }),
  enterprise: Object.freeze({
    apply: 'profile:enterprise:apply',
    manage: 'profile:enterprise:manage',
    material: 'profile:enterprise:material',
    override: 'profile:enterprise:override',
    query: 'profile:enterprise:query',
    review: 'profile:enterprise:review'
  })
});
