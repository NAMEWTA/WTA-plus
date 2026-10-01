package org.namewta.system.auth;

import lombok.extern.slf4j.Slf4j;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.system.domain.SysAuthProvider;
import org.namewta.system.domain.SysAuthRegistration;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Collection;

/**
 * 共享 Redis 快照缓存，不保存解密后的密钥，也不维护节点私有缓存。
 * 缓存键包含数据库双版本；提交后删除旧键，其他节点自动读取新版本。
 * Redis 丢数据或暂不可用时回源数据库，不将缓存当作启用状态的权威。
 */
@Slf4j
@Component
public class ExternalAuthConfigurationCache {
    private static final Duration TTL = Duration.ofMinutes(10);

    /** 生成不可变配置版本的键。 */
    public static String key(long id, long version, long providerVersion) {
        return "system:external-auth:v1:" + id + ":" + version + ":" + providerVersion;
    }

    /** 读取加密快照；缓存不可用或损坏视为未命中。 */
    public Snapshot get(String key) {
        try {
            String value = RedisUtils.getCacheObject(key);
            return value == null ? null : JsonUtils.parseObject(value, Snapshot.class);
        } catch (RuntimeException error) {
            log.warn("外部认证配置缓存读取失败，将回源数据库");
            return null;
        }
    }

    /** 写入有限寿命的加密快照。 */
    public void put(String key, Snapshot snapshot) {
        try {
            RedisUtils.setCacheObject(key, JsonUtils.toJsonString(snapshot), TTL);
        } catch (RuntimeException error) {
            log.warn("外部认证配置缓存写入失败，后续请求将回源数据库");
        }
    }

    /** 提交后的共享失效；失败时双版本校验仍保证旧缓存不能被使用。 */
    public void invalidate(Collection<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        try {
            RedisUtils.deleteObject(keys);
        } catch (RuntimeException error) {
            log.warn("外部认证配置缓存失效失败，旧版本缓存将由 TTL 清理");
        }
    }

    /** 内部缓存格式；registration 仅含加密密钥。 */
    public record Snapshot(SysAuthProvider provider, SysAuthRegistration registration) {
        @Override
        public String toString() {
            return "ExternalAuthSnapshot[credentials=<redacted>]";
        }
    }
}
