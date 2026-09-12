# ${moduleName}

## Scope

覆盖 `${modulePath}`；更深层 manifest 的 AGENTS 负责其自身实现。工程规范、五层边界和注释规则由 `.agents/skills` 统一维护。

## Purpose

角色：`${agentsRole}`。${modulePurpose}

## Components

<#list moduleComponents as item>
- ${item}
</#list>

## Entry Points

<#list entryPoints as item>
- ${item}
</#list>

## Dependencies

${dependencies}

## Verification

<#list verificationCommands as command>
- `${command}`
</#list>

## Read Next

<#list readNext as item>
- [${item.label}](${item.path})
</#list>
