# Claim channel subquery: unconfirmed lock-order risk

Lead source review observed channel discrimination inside the claim UPDATE SET scalar subquery. The existing claim transaction already locks Outbox candidates; ordinary retry currently updates Delivery before requeueing Outbox. A locking subquery could add an Outbox→Delivery edge to that path.

Primary reference read 2026-09-23: [MySQL 8.4, locks set by SQL statements](https://dev.mysql.com/doc/refman/8.4/en/innodb-locks-set.html), especially its discussion of UPDATE with a SELECT subquery. The manual's example is WHERE IN, not this exact SET expression. Therefore the exact lock behavior is an inference needing a two-connection test, not a reproduced defect.

Suggested safe design: bounded ordinary consistent read of the candidate delivery channel set in DAO, then pass only an IN_APP cleanup boolean to the Outbox-only claim UPDATE. Channel is immutable, and beginInAppAttempt still verifies the locked current delivery and fence; this avoids introducing reliance on scalar subquery lock behavior and avoids per-row channel queries. Alternatively retain the expression only after a real two-connection assertion proves the intended lock behavior on MySQL8.4.

No database command was run for this analysis. Sent to the sole product writer for design/verification disposition; not a final candidate verdict.
