# T44 v2 fallback attempt: bounded private diagnosis

Input: `/tmp/wta-t44/runs/16f76a742e134fef/result.json` and its private
`backend.raw.log`, read only. No raw log line, SQL, credential, token, HTTP
body, message title/content or exception message is reproduced here. Source
before/after was clean `e11c1b6f3a4a4a8bdc1746044d65fe8445c0865c`;
all owned containers, anonymous volume, process group and ports were cleared.
The attempt is **failed**, not fallback acceptance.

The runner reported `redis_scheduled_fallback` but collapsed the exception
class to `Exception`. Core readiness/liveness, real login/menu and pre-diagnosis
zero OSS connections had already passed. The private backend log contains one
empty `POLL` event and one Notice draft-save handler invocation; it contains
no Notice publish handler, no wake-publisher-failure marker and no positive
POLL claim. Near the second login after ACL mutation, the log contains one
`org.namewta.common.redis.cache.ClusterCacheInvalidationException` (private
log line 525), followed by the login handler. The log also has startup codec
exception class names; their presence alone does not locate this failure.

The v2 runner had changed the app's Redis ACL root to global `-publish` before
calling `control_login` a second time. Production cache invalidation publishes
to `namewta:cache:invalidation:v1` in
`backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/cache/RedissonCacheInvalidationTransport.java`.
The most specific supported diagnosis is that the broad fault injection broke
ordinary login/cache invalidation before the intended Notice wake failure
could be exercised. The sanitized v2 result cannot prove the exact thrown
HTTP-control type or Redis server error text; it must not be counted as a
Notify fallback product defect.

The separate private v3 runner uses Redis 7+ ACL selector semantics: root
`-publish` with `(+publish &namewta:cache:invalidation:v1)`. It requires
`ACL DRYRUN` to allow cache-channel PUBLISH, deny
`notify:outbox:wake` PUBLISH and keep wake-channel SUBSCRIBE allowed; the real
second login/menu remains a strict positive control. It also retains only an
allowlisted HTTP-control stage/kind/status/code and this runner's numeric
source line on failure. No v3 service run has occurred here.
