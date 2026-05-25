package br.ufc.llm.shared.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class CacheService {

    private final ReactiveStringRedisTemplate redis;
    private final Duration ttl;
    private final Duration pendingTtl;

    public CacheService(
            ReactiveStringRedisTemplate redis,
            @Value("${app.cache-ttl-seconds:30}") long ttlSeconds,
            @Value("${app.pending-ttl-seconds:86400}") long pendingTtlSeconds) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.pendingTtl = Duration.ofSeconds(pendingTtlSeconds);
    }

    public Mono<String> get(String key) {
        return redis.opsForValue().get(key);
    }

    public Mono<Boolean> set(String key, String value) {
        return redis.opsForValue().set(key, value, ttl);
    }

    public Mono<Boolean> setPending(String key, String value) {
        return redis.opsForValue().set(key, value, pendingTtl);
    }

    public Mono<Long> delete(String... keys) {
        return redis.delete(keys);
    }
}
