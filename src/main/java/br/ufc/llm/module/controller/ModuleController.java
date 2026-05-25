package br.ufc.llm.module.controller;

import br.ufc.llm.module.dto.ModuleRequest;
import br.ufc.llm.module.dto.ModuleResponse;
import br.ufc.llm.module.service.ModuleService;
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

@RestController
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService service;
    private final CacheService cache;
    private final ObjectMapper objectMapper;

    private static String keyList(Long courseId) { return "modules:course:" + courseId; }
    private static String keyOne(Long id)        { return "module:" + id; }

    @PostMapping(value = "/courses/{courseId}/modules", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<ModuleResponse>>> criar(
            @PathVariable Long courseId,
            @RequestPart("dados") String dadosJson,
            @RequestPart(value = "imagem", required = false) FilePart imagem) {
        return Mono.fromCallable(() -> objectMapper.readValue(dadosJson, ModuleRequest.class))
                .flatMap(request -> service.criar(courseId, request, imagem))
                .flatMap(dados -> cache.delete(keyList(courseId)).thenReturn(dados))
                .map(dados -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok("Módulo criado com sucesso", dados)));
    }

    @GetMapping("/courses/{courseId}/modules")
    public Mono<ResponseEntity<String>> listar(@PathVariable Long courseId) {
        String key = keyList(courseId);
        return cache.get(key)
                .map(cached -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON).body(cached))
                .switchIfEmpty(
                        service.listarPorCurso(courseId)
                                .flatMap(dados -> Mono.fromCallable(
                                        () -> objectMapper.writeValueAsString(ApiResponse.ok(dados))))
                                .flatMap(body -> cache.set(key, body).thenReturn(body))
                                .map(body -> ResponseEntity.ok()
                                        .contentType(MediaType.APPLICATION_JSON).body(body))
                );
    }

    @GetMapping("/modules/{id}")
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
}
