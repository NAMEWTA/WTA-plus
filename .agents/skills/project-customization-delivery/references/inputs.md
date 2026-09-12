# 输入与工作目录

## 参数约定

| 字段 | 含义与处理 |
|---|---|
| `brand.display_name` | 必填，产品展示品牌。 |
| `brand.slug` | 模块、前端 scope、容器等代码标识；无歧义英文品牌可推导小写 kebab-case，否则询问。禁止把未转写的中文直接作为 Maven/容器标识。 |
| `brand.java_package` | 默认为 `org.<合法单段标识>`，例如品牌 Atlas → `org.atlas`；多段域名按用户提供。slug 含连字符、保留字等不能直接当 Java 包名，需给出合法映射。 |
| `brand.author` | 沿用本流程默认 `wta`，用户可覆盖；替换自有作者注释为 `@author <author>`，维护者字段使用相应格式。 |
| `logo_path` | 必填，本地可读图像；检查格式、尺寸、透明度后替换实际使用的 Logo/favicon。 |
| `source_root` | 默认当前项目根；从文件证据识别前后端目录，不假定它们叫 backend/frontend。 |
| `local_root` | 必填，定制项目最终所在的绝对目录；不是仅报告目录。若用户表达不清，再澄清用途。 |
| `report_root` | 默认 `<local_root>/temp/relase`；其中固定创建 `deployment-docs/` 和 `tests/`，用户指定根目录时覆盖。 |
| `middleware` | 可省略或 `none`；省略不代表允许安装本地 Docker 或部署远程服务。已有连接配置可继续验证。 |
| `middleware.host / ssh_port / ssh_user / credential_source` | 部署时需要的主机、端口、账户和凭据来源；支持 SSH key/agent 或私密密码配置，不强制 root 或明文密码。 |
| `middleware.data_root` | 部署时必填，远程持久化根目录，与 `local_root` 分开；端口、数据库、Compose project、bucket 等在审计后确定并记录。 |
| `external_services` | 对实际启用的 AI、短信、邮件、解析器、MQTT 等给出配置来源或用户明确排除的功能；不要求无关供应商凭据。 |

先展示简明参数摘要，标明推导值。来源足够明确时不把摘要变成重复确认门槛。仅缺少 Logo 等文件时让用户直接提供文件或路径；文本输入工具不用于索取文件上传。

## 目标目录规则

- 解析真实路径和符号链接，检查源目标相等、包含关系、已有文件及空间。目标非空时先识别它是否为本次可恢复成果，禁止直接覆盖无关工作。
- `local_root == source_root`：原地替换，先保存现有改动与可恢复基线。重命名使用移动，目标内没有两套旧新模块。
- `local_root != source_root`：将源项目所需的自有源码、文档、技能和交付资产准备到目标后，在目标内执行迁移；保留原始源项目。源模板的存在不属于目标内的重复实现，不得为“零旧前缀”删除源目录。
- 新目标不得递归拷贝自身或带入 node_modules、target、dist、日志、会话 Token、私密本地配置；需要的连接配置在目标单独安全生成。原有 ignore 与工具链文件要保留有效语义。
- Git/Submodule 的管理方式依据当前真实拓扑选择并记录。禁止复制失效的 `.git` 指针、伪造提交 SHA 或随意移除嵌套仓库。无 Git 的合法源码归档用显式文件清单与 SHA-256 记录来源；不能 catch 任意 Git 错误后静默视作归档。

## 最小调用示例

```text
使用 $project-customization-delivery
品牌：Atlas（模块前缀 atlas-，Java 包 org.atlas）
Logo：/absolute/path/atlas.png
本地持久化根目录：/absolute/path/atlas-project
中间件：不部署，复用现有本地配置
```

部署模式另补服务器、凭据来源和服务器数据根目录即可。示例值不是运行默认值。工作记录至少保存规范化参数、执行授权范围、来源、当前阶段和下一步；秘密值不进入普通记录。
