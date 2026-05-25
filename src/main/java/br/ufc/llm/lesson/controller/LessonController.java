package br.ufc.llm.lesson.controller;

import br.ufc.llm.lesson.dto.LessonRequest;
import br.ufc.llm.lesson.dto.LessonResponse;
import br.ufc.llm.lesson.service.LessonAiService;
import br.ufc.llm.lesson.service.LessonService;
import br.ufc.llm.shared.cache.CacheService;
import br.ufc.llm.shared.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LessonController {

    private final LessonService service;
    private final LessonAiService aiService;
    private final CacheService cache;
    private final ObjectMapper objectMapper;

    private static String keyList(Long moduleId) { return "lessons:module:" + moduleId; }
    private static String keyOne(Long id)        { return "lesson:" + id; }

    @PostMapping(value = "/modules/{moduleId}/lessons", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<LessonResponse>>> criar(
            @PathVariable Long moduleId,
            @RequestPart("dados") String dadosJson,
            @RequestPart(value = "arquivo", required = false) FilePart arquivo) {
        return Mono.fromCallable(() -> objectMapper.readValue(dadosJson, LessonRequest.class))
                .flatMap(request -> service.criar(moduleId, request, arquivo))
                .flatMap(dados -> cache.delete(keyList(moduleId)).thenReturn(dados))
                .map(dados -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok("Aula criada com sucesso", dados)));
    }

    @GetMapping("/modules/{moduleId}/lessons")
    public Mono<ResponseEntity<String>> listar(@PathVariable Long moduleId) {
        String key = keyList(moduleId);
        return cache.get(key)
                .map(cached -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON).body(cached))
                .switchIfEmpty(
                        service.listarPorModulo(moduleId)
                                .flatMap(dados -> Mono.fromCallable(
                                        () -> objectMapper.writeValueAsString(ApiResponse.ok(dados))))
                                .flatMap(body -> cache.set(key, body).thenReturn(body))
                                .map(body -> ResponseEntity.ok()
                                        .contentType(MediaType.APPLICATION_JSON).body(body))
                );
    }

    @GetMapping("/lessons/{id}")
    public Mono<ResponseEntity<String>> buscar(@PathVariable Long id) {
        String key = keyOne(id);
        return cache.get(key)
                .map(cached -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON).body(cached))
                .switchIfEmpty(
                        service.buscarPorId(id)
                                .flatMap(dados -> Mono.fromCallable(
                                        () -> objectMapper.writeValueAsString(ApiResponse.ok(dados))))
                                .flatMap(body -> cache.set(key, body).thenReturn(body))
                                .map(body -> ResponseEntity.ok()
                                        .contentType(MediaType.APPLICATION_JSON).body(body))
                );
    }

    @PostMapping("/lessons/{id}/gerar-conteudo")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> gerarConteudo(@PathVariable Long id) {
        return aiService.gerarConteudo(id)
                .map(task -> ResponseEntity.ok(ApiResponse.ok("Conteúdo enfileirado para geração via IA", task)));
    }

    @GetMapping("/lessons/{id}/conteudo-pendente")
    public Mono<ResponseEntity<ApiResponse<String>>> conteudoPendente(@PathVariable Long id) {
        return aiService.buscarConteudoPendente(id)
                .map(content -> ResponseEntity.ok(ApiResponse.ok(content)));
    }

    @PostMapping("/lessons/{id}/confirmar-conteudo")
    public Mono<ResponseEntity<ApiResponse<LessonResponse>>> confirmarConteudo(@PathVariable Long id) {
        return aiService.confirmarConteudo(id)
                .flatMap(dados -> cache.delete(keyOne(id)).thenReturn(dados))
                .map(dados -> ResponseEntity.ok(ApiResponse.ok("Conteúdo confirmado e salvo", dados)));
    }

    @PostMapping("/lessons/{id}/regerar-conteudo")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> regerarConteudo(@PathVariable Long id) {
        return aiService.gerarConteudo(id)
                .map(task -> ResponseEntity.ok(ApiResponse.ok("Conteúdo regerado e enfileirado via IA", task)));
    }
}
