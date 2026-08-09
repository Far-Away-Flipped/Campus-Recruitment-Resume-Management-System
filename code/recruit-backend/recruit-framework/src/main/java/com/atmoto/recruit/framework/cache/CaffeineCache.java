package com.atmoto.recruit.framework.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 本类替代 RuoYi RedisCache，单机 Caffeine 实现，不可用于多实例部署。
 * <p>
 * 内部持有 configCache（无限期缓存）作为主存储。
 * 带 TTL 的写入会为每个 key 创建临时 Caffeine 缓存，存入主缓存的 "ttl:" + key 键下；
 * 读取时优先检查 TTL 缓存，key 过期后自动回退到主缓存值或被 Caffeine 淘汰。
 * </p>
 *
 * @author atmoto
 */
@Component
public class CaffeineCache {

    private final Cache<String, Object> caffeine;

    public CaffeineCache(@Qualifier("configCache") Cache<String, Object> caffeine) {
        this.caffeine = caffeine;
    }

    // ==================== 基础缓存操作 ====================

    /**
     * 写入缓存（无过期时间，跟随主缓存的淘汰策略）
     */
    public void setCacheObject(String key, Object value) {
        caffeine.put(key, value);
    }

    /**
     * 写入缓存（指定过期时间）
     * <p>
     * 为单个 key 创建临时 Caffeine 缓存，存入主缓存的 "ttl:" + key 键下。
     * 读取时优先检查该临时缓存，过期后 Caffeine 自动淘汰。
     * </p>
     */
    public void setCacheObject(String key, Object value, long timeout, TimeUnit unit) {
        Cache<String, Object> temp = Caffeine.newBuilder()
                .expireAfterWrite(timeout, unit)
                .build();
        temp.put(key, value);
        caffeine.put("ttl:" + key, temp);
        // 同时存入主缓存作为无 TTL 读取的兜底
        caffeine.put(key, value);
    }

    /**
     * 读取缓存对象，优先检查 TTL 缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T getCacheObject(String key) {
        // 优先检查 TTL 缓存
        Object ttlEntry = caffeine.getIfPresent("ttl:" + key);
        if (ttlEntry instanceof Cache) {
            Cache<String, Object> ttlCache = (Cache<String, Object>) ttlEntry;
            Object value = ttlCache.getIfPresent(key);
            if (value != null) {
                return (T) value;
            }
        }
        // 回退到主缓存
        return (T) caffeine.getIfPresent(key);
    }

    /**
     * 删除缓存对象（同时清理 TTL 记录）
     */
    public void deleteObject(String key) {
        caffeine.invalidate(key);
        caffeine.invalidate("ttl:" + key);
    }

    /**
     * 判断 key 是否存在
     */
    @SuppressWarnings("unchecked")
    public boolean hasKey(String key) {
        if (caffeine.getIfPresent(key) != null) {
            return true;
        }
        // 检查 TTL 缓存（主缓存无此 key 但有 TTL 记录的场景）
        Object ttlEntry = caffeine.getIfPresent("ttl:" + key);
        if (ttlEntry instanceof Cache) {
            return ((Cache<String, Object>) ttlEntry).getIfPresent(key) != null;
        }
        return false;
    }

    // ==================== 集合操作 ====================

    /**
     * 写入 List 到缓存
     */
    public <T> void setCacheList(String key, List<T> list) {
        caffeine.put(key, list);
    }

    /**
     * 从缓存读取 List
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getCacheList(String key) {
        return getCacheObject(key);
    }

    /**
     * 写入 Map 到缓存
     */
    public <T> void setCacheMap(String key, Map<String, T> map) {
        caffeine.put(key, map);
    }

    /**
     * 从缓存读取 Map
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getCacheMap(String key) {
        return getCacheObject(key);
    }

    // ==================== 查询 ====================

    /**
     * 根据 pattern 查询匹配的 key 集合。
     * <p>
     * pattern 为 "*" 时返回全部 key；否则用简单通配符匹配（* 匹配任意字符序列，? 匹配单个字符）。
     * 单机 500 条以内够用，数据量更大时考虑换用更高效的实现。
     * </p>
     */
    public Collection<String> keys(String pattern) {
        Set<String> allKeys = new HashSet<>(caffeine.asMap().keySet());
        if ("*".equals(pattern)) {
            return allKeys;
        }
        Set<String> matched = new HashSet<>();
        String regex = toRegex(pattern);
        for (String key : allKeys) {
            if (key.matches(regex)) {
                matched.add(key);
            }
        }
        return matched;
    }

    /**
     * 获取 key 的剩余过期时间（秒）。
     * <p>
     * Caffeine 不直接暴露单 key 的剩余 TTL，此处返回 -2 表示无法获取。
     * </p>
     */
    public long getExpire(String key) {
        return -2L;
    }

    // ==================== 过期 ====================

    /**
     * 为已有 key 设置过期时间。
     * <p>
     * 先从缓存取出该 key 的当前值，再以指定 TTL 重新写入临时缓存。
     * 若 key 不存在则返回 false。
     * </p>
     */
    @SuppressWarnings("unchecked")
    public boolean expire(String key, long timeout, TimeUnit unit) {
        Object value = caffeine.getIfPresent(key);
        if (value == null) {
            // 尝试从 TTL 缓存中获取
            Object ttlEntry = caffeine.getIfPresent("ttl:" + key);
            if (ttlEntry instanceof Cache) {
                value = ((Cache<String, Object>) ttlEntry).getIfPresent(key);
            }
        }
        if (value == null) {
            return false;
        }
        Cache<String, Object> temp = Caffeine.newBuilder()
                .expireAfterWrite(timeout, unit)
                .build();
        temp.put(key, value);
        caffeine.put("ttl:" + key, temp);
        caffeine.put(key, value);
        return true;
    }

    // ==================== 内部工具 ====================

    /**
     * 将简单通配符 pattern 转换为 Java 正则表达式
     */
    private String toRegex(String pattern) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch (c) {
                case '*':
                    sb.append(".*");
                    break;
                case '?':
                    sb.append('.');
                    break;
                case '.':
                case '[':
                case ']':
                case '(':
                case ')':
                case '{':
                case '}':
                case '\\':
                case '^':
                case '$':
                case '|':
                case '+':
                    sb.append('\\').append(c);
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }
}
