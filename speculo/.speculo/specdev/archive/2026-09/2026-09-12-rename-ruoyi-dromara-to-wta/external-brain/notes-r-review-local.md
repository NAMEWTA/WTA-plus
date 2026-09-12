# External brain — R-review-architecture 本地审阅要点

**Updated:** 2026-09-12T11:01:00+08:00  
**Change:** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/</Path>`  
**Note:** 本文件为 R-review **本地追加**；不覆盖 `notes.md` locked 段落。ChatGPT 外脑仍 blocked，无远端审阅正文。

## R 裁决（本地）

- **有条件通过**；可开 **S-spec**  
- **无高置信 code-judo 候选**（治理/门禁审查）  
- 残留 AMBIGUOUS **不阻塞 R**

## 接受并强化（与 ADR 一致，不改 ADR 正文）

1. KEEP：`org.dromara.sms4j` / `warm` / `easyes` / `mica.mqtt` + 对应 Maven groupId  
2. W0b：旧仓冻结 → orphan 新 public monorepo → 再命名波次  
3. ADR-008：**换前缀 `wta-X`**，禁止裸名  
4. ADR-006：双读必须有**可验收退出门禁**（时长/度量交 S-spec）  
5. 门禁：ChatGPT → 本地迭代 → 问 CTO；未授权不建仓不改代码  

## 文档漂移（交 S-spec 收敛；本轮不改 CONTEXT/ADR）

- CONTEXT 仍有「ADR-005 建议本期 KEEP 聚合路径」「模块目录无 ruoyi- 前缀」等易误读句  
- 封面首次补记仍写「去掉前缀」；二次补记已修正为换前缀  
- spec §5 风险仍写 Nacos「默认延期」、聚合路径 KEEP —— 与已拍板 ADR 投影不一致  

## tickets-map 缺口

- 主表 T-01…T-08 未含 W0b；LOG-011 增补主题（freeze/layout/orphan/review-gate）须在 T 前并入  

## 外脑

- 上传仍 Auto-review blocked；G-Doc 未关闭；不豁免实施门禁  
