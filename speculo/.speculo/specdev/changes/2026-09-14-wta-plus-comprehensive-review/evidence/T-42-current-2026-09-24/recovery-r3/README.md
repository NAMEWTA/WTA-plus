# T42 recovery R3 — NOT PASSED

Exact clean source `14140907c448f3d3f6030a7bb46102ca7496c2bd`, tree `dfc77b7383ecbe674f911f312dae5e48d21ea977`; source unchanged before/after. MAIL v5 run `24dcd5b287e478ad`: fresh six SQL / 104 tables, 20 actual test methods, 4 passed / 16 failures / 0 errors / 0 skipped; Maven and acceptance exit 1. No attachment business completion claim.

Passed methods: duplicateKeepsOneIntentAndRejectsChangedAttachmentSet, invalidOwnerAndQueuedSourceRevocationNeverReachSmtp, sameIdempotencyKeyConcurrentSubmissionReturnsOneOwnedRelation, attachmentMapperRejectsStaleVersionAndHidesLogicalDelete. The queued-source negative alone is not positive proof of Worker authorization because normal Worker dispatch also failed. Concurrent submit is not forced unique-key collision proof.

Worker claimed jobs but did not reach copy/physical SMTP. Most relation states stayed QUEUED. Two release tests reached RELEASED but counted historical logically deleted references. Source PUT/cleanup traffic is not successful snapshot evidence.

All owned Maven/Java/proxy processes, three containers, two anonymous volumes and five loopback ports were removed/closed; cleanup.errors empty. Logs/XML copied only from the runner's sanitized retained outputs. R1/R2 remain immutable. This is the third failure of the first recovery batch; a four-part Lead retrospective/new dispatch is required before another candidate.
