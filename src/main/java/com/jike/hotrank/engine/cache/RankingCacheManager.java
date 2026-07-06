package com.jike.hotrank.engine.cache;

import com.jike.hotrank.engine.dto.RankingResponseDTO;
import com.jike.hotrank.engine.util.RankingLimits;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class RankingCacheManager {

    private static final long BASE_TTL_MS = 5000;
    private static final long TTL_JITTER_MS = 500;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner;
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong nullHitCount = new AtomicLong();
    private final AtomicLong putCount = new AtomicLong();
    private final AtomicLong evictionCount = new AtomicLong();

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

    public record CacheResult(boolean hit, RankingResponseDTO data) {

        public static CacheResult miss() {
            return new CacheResult(false, null);
        }

        public static CacheResult hit(RankingResponseDTO data) {
            return new CacheResult(true, data);
        }
    }

    public record CacheStats(long size,
                             long hitCount,
                             long missCount,
                             long nullHitCount,
                             long putCount,
                             long evictionCount,
                             double hitRate) {
    }

    public RankingCacheManager() {
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "cache-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        this.cleaner.scheduleAtFixedRate(this::cleanExpired, 1, 1, TimeUnit.SECONDS);
    }

    public RankingResponseDTO get(String key) {
        return getResult(key).data();
    }

    public CacheResult getResult(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            missCount.incrementAndGet();
            log.debug("Ranking cache miss: key={}", key);
            return CacheResult.miss();
        }

        if (entry.isExpired()) {
            cache.remove(key);
            missCount.incrementAndGet();
            log.debug("Ranking cache expired: key={}", key);
            return CacheResult.miss();
        }

        hitCount.incrementAndGet();
        if (entry.data == null) {
            nullHitCount.incrementAndGet();
        }
        log.debug("Ranking cache hit: key={}", key);
        return CacheResult.hit(entry.data);
    }

    public void put(String key, RankingResponseDTO data) {
        long ttl = calculateTTL();
        cache.put(key, new CacheEntry(data, System.currentTimeMillis() + ttl));
        putCount.incrementAndGet();
        log.debug("Ranking cache put: key={}, ttl={}ms", key, ttl);
    }

    public void putNull(String key) {
        long baseNullTtl = BASE_TTL_MS / 2;
        long jitter = (long) (Math.random() * TTL_JITTER_MS) - TTL_JITTER_MS / 2;
        long ttl = baseNullTtl + jitter;
        cache.put(key, new CacheEntry(null, System.currentTimeMillis() + ttl));
        putCount.incrementAndGet();
        log.debug("Ranking cache put null: key={}, ttl={}ms", key, ttl);
    }

    public void evict(String key) {
        if (cache.remove(key) != null) {
            evictionCount.incrementAndGet();
        }
        log.debug("Ranking cache evict: key={}", key);
    }

    public void evictAll() {
        evictionCount.addAndGet(cache.size());
        cache.clear();
        log.info("Ranking cache cleared");
    }

    public void evictByPrefix(String prefix) {
        long before = cache.size();
        cache.keySet().removeIf(key -> key.startsWith(prefix));
        evictionCount.addAndGet(before - cache.size());
        log.debug("Ranking cache evict by prefix: prefix={}", prefix);
    }

    public CacheStats getStats() {
        long hits = hitCount.get();
        long misses = missCount.get();
        long reads = hits + misses;
        double hitRate = reads == 0 ? 0.0 : (double) hits / reads;
        return new CacheStats(
            cache.size(),
            hits,
            misses,
            nullHitCount.get(),
            putCount.get(),
            evictionCount.get(),
            hitRate
        );
    }

    private long calculateTTL() {
        long jitter = (long) (Math.random() * TTL_JITTER_MS * 2) - TTL_JITTER_MS;
        return BASE_TTL_MS + jitter;
    }

    private void cleanExpired() {
        long before = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        evictionCount.addAndGet(before - cache.size());
    }

    @PreDestroy
    public void shutdown() {
        cleaner.shutdownNow();
        log.info("Ranking cache cleaner stopped");
    }

    public static String globalRankKey(Integer limit) {
        return "ranking:global:" + RankingLimits.global(limit);
    }

    public static String circleRankKey(Long circleId, Integer limit) {
        return "ranking:circle:" + circleId + ":" + RankingLimits.circle(limit);
    }

    public static String newcomerRankKey(Integer limit) {
        return "ranking:newcomer:" + RankingLimits.newcomer(limit);
    }

    public static String surgingRankKey(Integer limit) {
        return "ranking:surging:" + RankingLimits.surging(limit);
    }

    public static String personalizedRankKey(Long userId, Integer limit) {
        return personalizedRankPrefix(userId) + ":" + RankingLimits.personalized(limit);
    }

    public static String personalizedRankPrefix(Long userId) {
        return "ranking:personalized:" + userId;
    }

    public static String rankingPrefix() {
        return "ranking:";
    }
}
