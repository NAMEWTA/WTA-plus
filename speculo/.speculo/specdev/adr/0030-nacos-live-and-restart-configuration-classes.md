# ADR-0030: Nacos 即时与重启生效配置分级

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-31-optional-nacos-dynamic-config/ADR.md</Path>` ADR-006

## Context

Spring Environment 的值变化不等于已经创建的监听器、数据源、条件 Bean 或普通字段会被安全重建。把远程发布成功描述为所有配置已经热更新，会制造无法验证的运行承诺。

## Decision

只有规格明确列出且经测试验证的属性前缀进入即时生效配置清单。其他通过校验的远程配置允许保存和装载，但分类为重启生效。发布结果、运行日志和实例状态必须区分远程版本已收到、已校验、即时应用成功和等待重启。

## Consequences

混合文档中的重启项不会使整份合法文档失败，但当前进程不得谎报它们已经生效。即时生效清单需要随实现和测试共同维护；状态输出不得泄露配置正文或敏感值。
