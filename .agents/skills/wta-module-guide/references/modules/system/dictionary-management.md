# 字典管理与前端回显合同

字典管理同时负责“值如何存储”和“值如何被用户看到”。修改字典类型、字典数据、后端字段或前端页面时，必须沿完整链路核对，不能只验证管理页能保存。

## 数据与请求链路

一条字典数据由 `dictType + dictValue` 唯一定位，`dictLabel` 是用户看到的文本，`dictSort` 决定返回顺序，`listClass` 与 `cssClass` 决定标签样式：

```text
sys_dict_type(dict_type)
        +
sys_dict_data(dict_type, dict_value, dict_label, list_class, css_class)
        -> SysDictTypeServiceImpl.selectDictDataByType()
        -> GET /system/dict/data/type/{dictType}
        -> domain 映射 { value: dictValue, label: dictLabel }
        -> App runtime.dicts() / Pinia dict store
        -> 页面控件或 DictTag 回显
```

后端管理面位于 `SysDictTypeController` 的 `/system/dict/type` 和 `SysDictDataController` 的 `/system/dict/data`。跨模块 Java 代码使用 `DictService` common SPI；不要注入 `ISysDict*`、Mapper、Entity 或 VO。字典查询结果由后端缓存，字典数据新增、编辑、删除后必须让对应前端缓存失效；全量刷新使用已有 `resetDictCache` 管理接口。

## `1、2、3、4、5` 到底显示什么

数字本身没有通用含义，含义由同一个 `dictType` 下的 `dictValue -> dictLabel` 定义。例如当前 `sys_oper_type` 数据为：

| 存储值 `dictValue` | 标签 `dictLabel` | 默认使用方式 |
|---|---|---|
| `1` | 新增 | 操作日志的“操作类型”下拉、表格标签和详情文本显示“新增” |
| `2` | 修改 | 显示“修改” |
| `3` | 删除 | 显示“删除” |
| `4` | 授权 | 显示“授权” |
| `5` | 导出 | 显示“导出” |

如果业务字段保存 `3`，页面只有在加载 `sys_oper_type` 后才能显示“删除”；换成 `sys_user_gender` 后，`3` 可能是“未知”，这不是同一个值的冲突，而是字典类型不同。新增或修改字典值时必须盘点所有引用该 `dictType` 的查询、表单、表格、导出、翻译和校验。

## 前端渲染规则

- `runtime.dicts('sys_oper_type')` 通过 Admin 的 `useDict` 异步加载数据，先返回空数组，再以响应式引用更新；不要在页面初始化时把字典数组复制成一次性快照。
- `useDict` 将后端字段映射为 `{ label: dictLabel, value: dictValue, elTagType: listClass, elTagClass: cssClass }`，并按 `dictType` 在 Pinia 中缓存；重复请求会共享 pending request。编辑或删除字典数据后清除该类型缓存，刷新缓存后清空全部缓存。
- `el-select`、`el-radio`、`el-option` 使用 `label` 给用户看，提交和查询使用 `value`。因此“显示文本”和“接口值”不能互换，也不能把 `1、2、3、4、5` 硬编码成页面文案。
- `DictTag` 匹配字符串化/宽松相等的值，可处理数字响应与字符串字典值；匹配到的项显示 `label`。`listClass` 为 `default` 或空且没有 `cssClass` 时渲染普通文本；否则渲染 Element Plus `el-tag`，`primary/success/info/warning/danger` 映射为对应颜色，未知样式回退为 `primary`，`cssClass` 作为额外 class。
- `selectDictLabel` 等纯文本翻译匹配不到时回退显示原始值；`DictTag` 默认也会显示未匹配的原始值。字典加载失败、值不存在或类型写错时，页面不应静默显示另一条字典的标签。
- 管理页预览字典数据时同样遵循 `listClass`/`cssClass`：默认样式显示文本，其他样式显示标签。`dictSort` 只影响列表顺序，不改变值到标签的映射。

## 修改与验收清单

1. 先确认 `dictType`、允许的 `dictValue`、标签、排序、启用/默认语义和样式；值需要保持稳定，标签可以按产品文案调整。
2. 盘点所有 `runtime.dicts()`、`DictTag`、`selectDictLabel`、`@ExcelDictFormat`、`DictTypeTranslationImpl` 和 `@DictPattern` 引用。
3. 分别验证空值、未加载、匹配值、未匹配值、数字/字符串类型、逗号分隔多值和缓存失效；至少覆盖一个下拉/单选控件与一个表格/详情回显。
4. 跨前后端变更时同步后端 VO/HTTP、domain transport、web-domain runtime、页面权限和初始化 SQL；初始化字典数据只能进入 `release-artifacts/docker/infrastructure/mysql/init/10-wta-base.sql` 或项目约定的 `60-namewta-dml.sql` owner。

相关源码：

- `wta-vue-plus-namewta/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysDictTypeServiceImpl.java`
- `wta-vue-plus-namewta/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysDictDataController.java`
- `plus-ui-namewta/apps/admin-web/src/utils/dict.ts`
- `plus-ui-namewta/apps/admin-web/src/components/DictTag/index.vue`
- `plus-ui-namewta/packages/web-domains/system/src/runtime.ts`
