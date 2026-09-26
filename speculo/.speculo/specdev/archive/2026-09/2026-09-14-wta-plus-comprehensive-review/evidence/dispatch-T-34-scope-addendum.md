# T-34 Dispatch addendum 01

原Packet/base/owner不变。在任何产品修改前，Lead追加 frontend/packages/web-domains/system/src/oss/OssPage.vue，仅文件展示与操作两列的 `<!-- @vue-generic {OssVO} -->` 声明，已有SsoAppPage先例；不得修改运行逻辑。目的为关闭全量typecheck既存诊断，无类型逃逸。owner=cors_audit，其余代理只读。工程CRUD/UI规则及最近web-domain-system AGENTS适用。T47合同不提前关闭。
