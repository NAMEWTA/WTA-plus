---
name: java-api-compatibility
description: 修改 wta-api 或其他公开 Java Service、SPI、DTO、扩展点并涉及兼容演进时使用。
---

# Java API 兼容演进

先读取项目 `engineering-standards`，再用本 Skill 执行 Java 公共合同的兼容迁移。以当前源码、根 POM 版本、调用方和测试为事实来源，不凭方法名推断兼容性。

## 判断兼容影响

1. 用 `rg` 查找声明、实现、重写、反射、序列化和全部调用方。
2. 判断变更是否影响源码兼容、二进制兼容、运行时语义、异常/null、副作用、序列化字段或 HTTP 合同。
3. 优先新增替代 API，再让旧 API 委托到新实现或共享实现；不得先删除、改名或改变旧签名。
4. 仅修改 private 或模块内部方法且没有外部合同时，不为形式统一滥用弃用标记。

`@Deprecated` 只向 Java 调用方表达迁移意图，不能修复二进制、JSON、数据库或 HTTP 不兼容。DTO 字段、序列化名称和 HTTP 行为发生变化时，另行提供兼容字段、适配层或版本化合同。

## 标记旧 API

对保留兼容但不建议新代码继续使用的方法，同时添加迁移 Javadoc 和 Java 注解：

```java
/**
 * 旧查询入口，仅为存量调用方兼容保留。
 *
 * <p>不建议新代码优先使用；该合同无法表达访问类型和到期时间。
 * 新代码在完成业务授权后使用 {@link #resolveAccessUrl(Long)}。</p>
 *
 * @deprecated 仅为旧调用方保留；新代码使用 {@link #resolveAccessUrl(Long)}
 */
@Deprecated(since = "6.0.0", forRemoval = false)
String legacyMethod(String ids);
```

执行以下要求：

- 当前项目弃用版本统一使用 `since = "6.0.0"`；根项目版本策略明确变化时，再同步调整新产生的弃用版本，不回写篡改历史 `since`。
- 默认使用 `forRemoval = false`，表示保留兼容且没有已批准的删除承诺。不得将它理解为可以立即删除。
- 在公开接口声明和可被直接引用的公开实现方法上都标记；重载方法逐个评估、逐个标记。
- 在 Javadoc 中写明“不建议新代码优先使用”、推荐替代方法、迁移前置条件，以及调用方必须知道的权限、有效期、返回值或副作用差异。
- 使用 `{@link ...}` 指向真实替代 API。没有可用替代方案时，不虚构链接，明确说明保留原因和当前限制。
- 让旧方法保持既有签名和已承诺语义。需要修复安全缺陷时，以安全要求为先，并同步记录行为差异和迁移路径。

## 约束新代码

- 新接口、新业务代码和新 HTTP 合同优先调用替代 API，不再扩散旧方法。
- 存量兼容桥可以继续调用旧方法；在触及对应 owner 时逐步迁移，不发动无授权的全仓重写。
- 不使用全局 `@SuppressWarnings("deprecation")` 隐藏迁移信号。兼容适配器确需抑制时，只在最小作用域添加，并用注释说明保留原因。
- 不通过改变参数含义、返回类型、异常、null 语义或副作用来偷偷复用旧方法名。

## 删除门槛

只有同时满足以下条件，才讨论删除旧 API 或改为 `forRemoval = true`：

- 已获得明确的移除版本和发布授权；
- 仓库内调用方已经清零，外部调用方已有迁移窗口；
- 替代 API 已稳定并覆盖旧能力或明确记录不再支持的行为；
- 发布说明、公共文档、实现和测试已经同步；
- 编译、契约测试及适用的二进制/集成验证已经通过。

未满足任一条件时，保持 `forRemoval = false`，不得仅为清理警告删除兼容入口。

## 验证

按影响范围完成以下检查：

1. 用 `rg` 确认旧方法、新方法、实现和调用方清单符合预期。
2. 编译声明模块、实现模块及主要消费者，确认注解不会破坏构建。
3. 增加或更新契约测试，断言替代 API 结构、旧 API 仍存在及弃用元数据正确。
4. 对兼容桥测试旧行为；对新 API 测试新增语义和安全边界。
5. 运行 `git diff --check`，确认没有无关生成物、版本漂移或调用方遗漏。

可使用反射固定弃用元数据：

```java
Method method = Api.class.getMethod("legacyMethod", String.class);
Deprecated deprecated = method.getAnnotation(Deprecated.class);
assertThat(deprecated).isNotNull();
assertThat(deprecated.since()).isEqualTo("6.0.0");
assertThat(deprecated.forRemoval()).isFalse();
```

交付时报告旧 API、替代 API、仍保留的调用方、实际验证命令和残余兼容风险。不要把“已标记弃用”描述成“已完成迁移”或“可以删除”。
