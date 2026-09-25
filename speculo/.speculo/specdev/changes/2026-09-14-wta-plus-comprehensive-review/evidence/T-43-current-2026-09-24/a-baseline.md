# T-43 A baseline (production still row-by-row)

Source HEAD `7933bdff61a5dbd620b869be7171b61b83fed845`, tree `1922e7eafefe6943bbf566fced742768db72592a`, clean. Probe SHA256 `94ae510b968474abc30a34fbd8a8a7a9431b90ecfd9fcfc0ed36b0f64e78186a`. Driver SHA256 `93edf8f32d861365ea9140da31ea9304f8c3409493b514de0e957561329e33bb`. Owned MySQL 8.4.9 and Redis 8.6.3, JDBC URL included `rewriteBatchedStatements=true`. 21/21 runs accepted. `smoke_passed` is false only because this file is matrix mode.

AC-043 is **not** closed. These counts are the before side. No SLA is claimed. Lock-wait delta 0 means this sample saw no contention, not that production has zero lock cost.

| run | updates | batch calls | commits | rollbacks | lock waits | lock ms | elapsed ms | recipient/delivery/outbox | for-update rows |
|---|---:|---:|---:|---:|---:|---:|---:|---|---:|
| n100-r1-measurePublishedAllInAppFanout | 305 | 0 | 1 | 0 | 0 | 0 | 584.2 | 100/100/100 | 0 |
| n100-r1-measureBoundedInAppResultAggregation | 71 | 0 | 40 | 0 | 0 | 0 | 710.4 | 100/100/100 | 1030 |
| n100-r2-measurePublishedAllInAppFanout | 305 | 0 | 1 | 0 | 0 | 0 | 675.0 | 100/100/100 | 0 |
| n100-r2-measureBoundedInAppResultAggregation | 71 | 0 | 40 | 0 | 0 | 0 | 617.5 | 100/100/100 | 1030 |
| n100-r3-measurePublishedAllInAppFanout | 305 | 0 | 1 | 0 | 0 | 0 | 610.4 | 100/100/100 | 0 |
| n100-r3-measureBoundedInAppResultAggregation | 71 | 0 | 40 | 0 | 0 | 0 | 593.7 | 100/100/100 | 1030 |
| n1000-r1-measurePublishedAllInAppFanout | 3005 | 0 | 1 | 0 | 0 | 0 | 3111.1 | 1000/1000/1000 | 0 |
| n1000-r1-measureBoundedInAppResultAggregation | 71 | 0 | 40 | 0 | 0 | 0 | 585.4 | 1000/1000/1000 | 10030 |
| n1000-r2-measurePublishedAllInAppFanout | 3005 | 0 | 1 | 0 | 0 | 0 | 3176.0 | 1000/1000/1000 | 0 |
| n1000-r2-measureBoundedInAppResultAggregation | 71 | 0 | 40 | 0 | 0 | 0 | 595.5 | 1000/1000/1000 | 10030 |
| n1000-r3-measurePublishedAllInAppFanout | 3005 | 0 | 1 | 0 | 0 | 0 | 3166.0 | 1000/1000/1000 | 0 |
| n1000-r3-measureBoundedInAppResultAggregation | 71 | 0 | 40 | 0 | 0 | 0 | 735.4 | 1000/1000/1000 | 10030 |
| n10000-r1-measurePublishedAllInAppFanout | 30005 | 0 | 1 | 0 | 0 | 0 | 22664.6 | 10000/10000/10000 | 0 |
| n10000-r1-measureBoundedInAppResultAggregation | 71 | 0 | 40 | 0 | 0 | 0 | 890.1 | 10000/10000/10000 | 100030 |
| n10000-r1-failedFanoutInsertRollsBackNoticeAndWake | 3003 | 0 | 0 | 1 | 0 | 0 | 3274.0 | 0/0/0 | 0 |
| n10000-r2-measurePublishedAllInAppFanout | 30005 | 0 | 1 | 0 | 0 | 0 | 20215.6 | 10000/10000/10000 | 0 |
| n10000-r2-measureBoundedInAppResultAggregation | 71 | 0 | 40 | 0 | 0 | 0 | 1122.1 | 10000/10000/10000 | 100030 |
| n10000-r2-failedFanoutInsertRollsBackNoticeAndWake | 3003 | 0 | 0 | 1 | 0 | 0 | 3199.1 | 0/0/0 | 0 |
| n10000-r3-measurePublishedAllInAppFanout | 30005 | 0 | 1 | 0 | 0 | 0 | 22142.3 | 10000/10000/10000 | 0 |
| n10000-r3-measureBoundedInAppResultAggregation | 71 | 0 | 40 | 0 | 0 | 0 | 1070.9 | 10000/10000/10000 | 100030 |
| n10000-r3-failedFanoutInsertRollsBackNoticeAndWake | 3003 | 0 | 0 | 1 | 0 | 0 | 3698.0 | 0/0/0 | 0 |

Raw per-run metrics are the sibling `metrics.json` files. Original runner directory was `/tmp/wta-t43/runs/af951e784d5c79e4`; this copy is the durable record.

