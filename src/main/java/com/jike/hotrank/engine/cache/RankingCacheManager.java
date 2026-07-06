package com.jike.hotrank.engine.cache;

import com.jike.hotrank.engine.dto.RankingResponseDTO;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 榜单缓存管理器
 * <p>
 * 使用JVM本地缓存，5秒TTL，支持缓存穿透防护和缓存雪崩防护
 * <p>
 * 缓存策略：
 * 1. 5秒过期TTL
 * 2. 随机化过期时间（±500ms）防止缓存雪崩
 * 3. 空值缓存防止缓存穿透
 *
 * @author JikeHotRank Team
 */
@Slf4j
@Component
public class RankingCacheManager {

    /** 缓存存储 */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /** 基础TTL（毫秒） */
    private static final long BASE_TTL_MS = 5000;

    /** TTL随机范围（毫秒） */
    private static final long TTL_JITTER_MS = 500;

    /** 定时清理器 */
    private final ScheduledExecutorService cleaner;

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        final RankingResponseDTO data;
        final long expireTime;

        CacheEntry(RankingResponseDTO data, long expireTime) {
            this.data = data;
            this.expireTime = expireTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    /**
     * 缓存查询结果。hit=true 且 data=null 表示命中了空值缓存。
     */
    public record CacheResult(boolean hit, RankingResponseDTO data) {

        public static CacheResult miss() {
            return new CacheResult(false, null);
        }

        public static CacheResult hit(RankingResponseDTO data) {
            return new CacheResult(true, data);
        }
    }

    public RankingCacheManager() {
        // 启动定时清理器，每秒清理过期缓存
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-cleaner");
            t.setDaemon(true);
            return t;
        });
        this.cleaner.scheduleAtFixedRate(this::cleanExpired, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存
     *
     * @param key 缓存key
     * @return 缓存的榜单数据，如果不存在或已过期返回null
     */
    public RankingResponseDTO get(String key) {
        return getResult(key).data();
    }

    /**
     * 获取缓存查询结果，可区分未命中和命中空值。
     *
     * @param key 缓存key
     * @return 缓存查询结果
     */
    public CacheResult getResult(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            log.debug("缓存未命中：key={}", key);
            return CacheResult.miss();
        }

        if (entry.isExpired()) {
            cache.remove(key);
            log.debug("缓存已过期：key={}", key);
            return CacheResult.miss();
        }

        log.debug("缓存命中：key={}", key);
        return CacheResult.hit(entry.data);
    }

    /**
     * 设置缓存
     *
     * @param key 缓存key
     * @param data 榜单数据
     */
    public void put(String key, RankingResponseDTO data) {
        long ttl = calculateTTL();
        long expireTime = System.currentTimeMillis() + ttl;
        cache.put(key, new CacheEntry(data, expireTime));
        log.debug("缓存写入：key={}, ttl={}ms", key, ttl);
    }

    /**
     * 设置空值缓存（防止缓存穿透）
     *
     * @param key 缓存key
     */
    public void putNull(String key) {
        // 空值缓存使用较短的TTL
        long ttl = BASE_TTL_MS / 2;
        long expireTime = System.currentTimeMillis() + ttl;
        cache.put(key, new CacheEntry(null, expireTime));
        log.debug("空值缓存写入：key={}, ttl={}ms", key, ttl);
    }

    /**
     * 删除缓存
     *
     * @param key 缓存key
     */
    public void evict(String key) {
        cache.remove(key);
        log.debug("缓存删除：key={}", key);
    }

    /**
     * 清除所有缓存
     */
    public void evictAll() {
        cache.clear();
        log.info("清除所有缓存");
    }

    /**
     * 根据key前缀清除缓存
     *
     * @param prefix key前缀
     */
    public void evictByPrefix(String prefix) {
        cache.keySet().removeIf(key -> key.startsWith(prefix));
        log.debug("清除前缀缓存：prefix={}", prefix);
    }

    /**
     * 计算随机化TTL（防止缓存雪崩）
     *
     * @return TTL毫秒数
     */
    private long calculateTTL() {
        long jitter = (long) (Math.random() * TTL_JITTER_MS * 2) - TTL_JITTER_MS;
        return BASE_TTL_MS + jitter;
    }

    /**
     * 清理过期缓存
     */
    private void cleanExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 应用关闭时释放清理线程，避免线程池资源泄漏。
     */
    @PreDestroy
    public void shutdown() {
        cleaner.shutdownNow();
        log.info("榜单缓存清理线程已关闭");
    }

    /**
     * 生成全站热榜缓存key
     */
    public static String globalRankKey(Integer limit) {
        return "ranking:global:" + (limit != null ? limit : 50);
    }

    /**
     * 生成圈子热榜缓存key
     */
    public static String circleRankKey(Long circleId, Integer limit) {
        return "ranking:circle:" + circleId + ":" + (limit != null ? limit : 20);
    }

    /**
     * 生成新星榜缓存key
     */
    public static String newcomerRankKey(Integer limit) {
        return "ranking:newcomer:" + (limit != null ? limit : 10);
    }

    /**
     * 生成飙升榜缓存key
     */
    public static String surgingRankKey(Integer limit) {
        return "ranking:surging:" + (limit != null ? limit : 10);
    }

    public static String personalizedRankKey(Long userId, Integer limit) {
        return personalizedRankPrefix(userId) + ":" + (limit != null ? limit : 50);
    }

    public static String personalizedRankPrefix(Long userId) {
        return "ranking:personalized:" + userId;
    }

    public static String rankingPrefix() {
        return "ranking:";
    }
}
