package br.ufc.llm.course.controller;

import br.ufc.llm.course.dto.CourseRequest;
import br.ufc.llm.course.dto.CourseResponse;
import br.ufc.llm.course.service.CourseService;
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
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private static final int PER_PAGE = 20;

    private final CourseService service;
    private final CacheService cache;
    private final ObjectMapper objectMapper;

    private static String keyPage(int page) { return "courses:p" + page; }
    private static String keyOne(Long id)   { return "course:" + id; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<CourseResponse>>> criar(
            @RequestPart("dados") String dadosJson,
            @RequestPart(value = "imagem", required = false) FilePart imagem) {
        return Mono.fromCallable(() -> objectMapper.readValue(dadosJson, CourseRequest.class))
                .flatMap(request -> service.criar(request, imagem))
                .flatMap(r -> cache.delete(keyPage(1)).thenReturn(r))
                .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok("Curso criado com sucesso", r)));
    }

    @GetMapping
    public Mono<ResponseEntity<String>> listar(@RequestParam(defaultValue = "1") int page) {
        String key = keyPage(page);
        return cache.get(key)
                .map(cached -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON).body(cached))
                .switchIfEmpty(
                        service.listar(page, PER_PAGE)
                                .flatMap(dados -> Mono.fromCallable(
                                        () -> objectMapper.writeValueAsString(ApiResponse.ok(dados))))
                                .flatMap(body -> cache.set(key, body).thenReturn(body))
                                .map(body -> ResponseEntity.ok()
                                        .contentType(MediaType.APPLICATION_JSON).body(body))
                );
    }

    @GetMapping("/{id}")
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
