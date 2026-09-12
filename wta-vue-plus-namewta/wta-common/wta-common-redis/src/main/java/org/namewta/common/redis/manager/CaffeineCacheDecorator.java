package org.namewta.common.redis.manager;

import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.redis.cache.CacheInvalidationHandler;
import org.namewta.common.redis.cache.ClusterCacheInvalidationCoordinator;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * Cache 装饰器模式(用于扩展 Caffeine 一级缓存)
 *
 * @author LionLi
 */
public class CaffeineCacheDecorator implements Cache {

    private final String name;
    private final Cache cache;
    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeine;
    private final ClusterCacheInvalidationCoordinator invalidationCoordinator;
    private final String invalidationNamespace;

    /**
     * 创建带 Caffeine 一级缓存的缓存装饰器。
     *
     * @param name     缓存名称
     * @param cache    被装饰的缓存实例
     * @param caffeine 本地一级缓存实例
     */
    public CaffeineCacheDecorator(String name, Cache cache, com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeine) {
        this(name, cache, caffeine, null);
    }

    /**
     * Creates a local cache decorator participating in cluster invalidation.
     *
     * @param name                    cache name
     * @param cache                   distributed cache
     * @param caffeine                local cache
     * @param invalidationCoordinator cluster invalidation coordinator
     */
    public CaffeineCacheDecorator(String name, Cache cache,
                                  com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeine,
                                  ClusterCacheInvalidationCoordinator invalidationCoordinator) {
        this.name = name;
        this.cache = cache;
        this.caffeine = caffeine;
        this.invalidationCoordinator = invalidationCoordinator;
        this.invalidationNamespace = "spring-cache:" + ClusterCacheInvalidationCoordinator.fingerprint(name);
        if (invalidationCoordinator != null) {
            invalidationCoordinator.register(invalidationNamespace, new CacheInvalidationHandler() {
                @Override
                public void invalidate(String keyFingerprint) {
                    invalidateLocalKey(keyFingerprint);
                }

                @Override
                public void clear() {
                    clearLocalCache();
                }
            });
        }
    }

    /**
     * 获取缓存名称。
     *
     * @return 缓存名称
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 获取底层原生缓存对象。
     *
     * @return 原生缓存对象
     */
    @Override
    public Object getNativeCache() {
        return cache.getNativeCache();
    }

    /**
     * 构造 Caffeine 一级缓存使用的唯一键。
     *
     * @param key 缓存键
     * @return 唯一键
     */
    public String getUniqueKey(Object key) {
        return name + StringUtils.COLON + key;
    }

    /**
     * 获取缓存值。
     *
     * @param key 缓存键
     * @return 缓存值包装对象
     */
    @Override
    public ValueWrapper get(Object key) {
        Object o = caffeine.get(getUniqueKey(key), k -> cache.get(key));
        return (ValueWrapper) o;
    }

    /**
     * 按指定类型获取缓存值。
     *
     * @param key  缓存键
     * @param type 值类型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Class<T> type) {
        Object o = caffeine.get(getUniqueKey(key), k -> cache.get(key, type));
        return (T) o;
    }

    /**
     * 写入缓存值，并清理本地一级缓存。
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    @Override
    public void put(Object key, Object value) {
        cache.put(key, value);
        invalidateClusterKey(key);
    }

    /**
     * 当键不存在时写入缓存值。
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 原缓存值包装对象
     */
    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existing = cache.putIfAbsent(key, value);
        if (existing == null) {
            invalidateClusterKey(key);
        }
        return existing;
    }

    /**
     * 删除缓存值。
     *
     * @param key 缓存键
     */
    @Override
    public void evict(Object key) {
        evictIfPresent(key);
    }

    /**
     * 删除缓存值并返回是否删除成功。
     *
     * @param key 缓存键
     * @return 是否删除成功
     */
    @Override
    public boolean evictIfPresent(Object key) {
        boolean b = cache.evictIfPresent(key);
        invalidateClusterKey(key);
        return b;
    }

    /**
     * 清空缓存。
     */
    @Override
    public void clear() {
        cache.clear();
        invalidateClusterNamespace();
    }

    /**
     * 使缓存整体失效。
     *
     * @return 是否失效成功
     */
    @Override
    public boolean invalidate() {
        boolean invalidated = cache.invalidate();
        invalidateClusterNamespace();
        return invalidated;
    }

    /**
     * 获取缓存值，不存在时通过回调加载。
     *
     * @param key         缓存键
     * @param valueLoader 回调加载器
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object o = caffeine.get(getUniqueKey(key), k -> cache.get(key, valueLoader));
        return (T) o;
    }

    /**
     * 清理当前缓存命名空间下的本地一级缓存。
     */
    private void clearLocalCache() {
        String prefix = name + StringUtils.COLON;
        caffeine.asMap().keySet().removeIf(key -> key instanceof String cacheKey && cacheKey.startsWith(prefix));
    }

    private void invalidateLocalKey(String keyFingerprint) {
        String prefix = name + StringUtils.COLON;
        caffeine.asMap().keySet().removeIf(key -> key instanceof String cacheKey
            && cacheKey.startsWith(prefix)
            && ClusterCacheInvalidationCoordinator.fingerprint(cacheKey.substring(prefix.length()))
            .equals(keyFingerprint));
    }

    private void invalidateClusterKey(Object key) {
        if (invalidationCoordinator == null) {
            caffeine.invalidate(getUniqueKey(key));
            return;
        }
        invalidationCoordinator.invalidate(invalidationNamespace, key);
    }

    private void invalidateClusterNamespace() {
        if (invalidationCoordinator == null) {
            clearLocalCache();
            return;
        }
        invalidationCoordinator.clear(invalidationNamespace);
    }

}
