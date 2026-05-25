package br.ufc.llm.shared.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarmupConfig {

    private final R2dbcEntityTemplate r2dbc;
    private final ReactiveStringRedisTemplate redis;

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        log.info("JIT warmup iniciado...");
        Flux.range(0, 50)
                .flatMap(i -> r2dbc.getDatabaseClient()
                        .sql("SELECT 1")
                        .fetch()
                        .rowsUpdated()
                        .onErrorResume(e -> Mono.just(0L)), 10)
                .then(Flux.range(0, 50)
                        .flatMap(i -> redis.opsForValue()
                                .set("_warmup", "ok", Duration.ofSeconds(5))
                                .onErrorResume(e -> Mono.just(false)), 10)
                        .then())
                .doOnSuccess(v -> log.info("JIT warmup concluido — DB e Redis aquecidos"))
                .subscribe();
    }
}
