# Round3 票作废说明（superseded-r3）

本目录收纳 change `2026-09-12-wta-sso` 在 Round3 落盘的正式票与对应 Evidence，**已作废**，不得与 Round4 新票共存于 `ticket/`，亦不得再当作完成依据。

## 作废原因

1. **创建主路径停在客户端管理**：Round3 票将 OpsFlow「创建应用 / 拿配置」隐含落在客户端管理（旧 AC-017 笼统管理面），与 Round4 产品纠偏冲突。
2. **红色「没有接入」伪绿**：现 E2E/管理面仍可呈现红色「没有接入」，却曾以旧票 `status: done` + 残缺 Evidence 对抗新 Spec；Round4 明示此态不合格。
3. **无 AC-024**：Round3 票集未覆盖完成态门禁 AC-024（须同时具备 SSO 管理 (a)(b)(c) 成功 + 自有接入成功态 + 三门硬验收）。
4. **相对 ADR-007 / DEC-200…203 superseded**：D-200/201/202/203=A 锁定独立「SSO 管理」+ 双面分工 + 仍扩展 `sys_client`；旧 DEC/ADR-003「客户端管理页 SSO 分组作创建主路径」产品句废止。

## 本目录内容

| 路径 | 说明 |
|---|---|
| `ticket/` | 旧 T-01…T-06（均为 `status: done`，规划合同已过时） |
| `evidence/` | 旧 `T-01.md`…`T-06.md` 与 `T-06-rework-t290u.md`（残缺，不当完成证据） |

## 截图

`../evidence/` 下历史截图（如 `sso-ac00*.png`、`sso-admin-config-*.png`）可保留作历史参考，**不当 Round4 完成证据**。

## Round4 权威

- Spec：`<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`（Round4 ready）
- Map / 新票：同 change 下 `tickets-map.md` 与 `ticket/`（7 张，全部 `blocked` / `ready: false`）
