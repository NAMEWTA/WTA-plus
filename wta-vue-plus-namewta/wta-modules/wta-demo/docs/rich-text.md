# 富文本通用能力示例

`TestRichTextController` 展示富文本在业务模块中的完整接入方式。内容提交前由
`wta-common-richtext` 规范化，图片、音频、视频和附件只能使用 `oss://<正整数>` 引用，
不会把 Base64 或签名 URL 写入数据库。

## API

* `GET /demo/rich-text/list`：当前用户和 Client 的文档列表。
* `GET /demo/rich-text/{id}`：读取文档 HTML。
* `POST /demo/rich-text/create`：提交 `{title, html}`。
* `POST /demo/rich-text/{id}/update`：提交 `{title, html, version}`，使用乐观锁。
* `POST /demo/rich-text/{id}/remove`：提交 `{version}` 删除文档并释放 OSS 引用。
* `GET /demo/rich-text/assets?ossIds=1,2&richTextId=3`：在文档授权通过后获取短时访问地址。

上传统一调用 OSS 直传策略 `richtext-image`、`richtext-audio`、`richtext-video` 和
`richtext-file`，权限为 `common:richtext:upload`。上传完成的对象会记录用户和 Client 归属，
业务保存时通过 `OssService.reconcileReferences` 原子对账；业务代码应先完成文档和资源权限校验。

## 接入片段

```java
RichTextContent content = RichTextProcessor.normalize(request.getHtml());
// 校验 content.assets() 的业务归属和媒体类型后保存 content.html()
ossService.reconcileReferences("your_table", id.toString(), previousIds, content.ossIds());
```

删除编辑器中的节点只更新 HTML，不要立即调用 OSS 删除接口，以保证撤销、重做和并发保存安全。
