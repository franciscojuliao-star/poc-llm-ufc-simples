package br.ufc.llm.quiz.controller;

import br.ufc.llm.quiz.dto.*;
import br.ufc.llm.quiz.service.QuizAiService;
import br.ufc.llm.quiz.service.QuizService;
import br.ufc.llm.shared.cache.CacheService;
import br.ufc.llm.shared.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class QuizController {

    private final QuizService service;
    private final QuizAiService aiService;
    private final CacheService cache;
    private final ObjectMapper objectMapper;

    private static String keyQuiz(Long moduleId) { return "quiz:module:" + moduleId; }

    @PostMapping("/modules/{moduleId}/quiz")
    public Mono<ResponseEntity<ApiResponse<QuizResponse>>> criar(
            @PathVariable Long moduleId,
            @RequestBody @Valid QuizRequest request) {
        return service.criar(moduleId, request)
                .flatMap(dados -> cache.delete(keyQuiz(moduleId)).thenReturn(dados))
                .map(dados -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok("Quiz criado com sucesso", dados)));
    }

    @GetMapping("/modules/{moduleId}/quiz")
    public Mono<ResponseEntity<String>> buscar(@PathVariable Long moduleId) {
        String key = keyQuiz(moduleId);
        return cache.get(key)
                .map(cached -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON).body(cached))
                .switchIfEmpty(
                        service.buscarPorModulo(moduleId)
                                .flatMap(dados -> Mono.fromCallable(
                                        () -> objectMapper.writeValueAsString(ApiResponse.ok(dados))))
                                .flatMap(body -> cache.set(key, body).thenReturn(body))
                                .map(body -> ResponseEntity.ok()
                                        .contentType(MediaType.APPLICATION_JSON).body(body))
                );
    }

    @PostMapping("/quiz/{quizId}")
    public Mono<ResponseEntity<ApiResponse<QuizResponse>>> configurar(
            @PathVariable Long quizId,
            @RequestBody QuizConfigRequest request) {
        return service.configurar(quizId, request)
                .map(dados -> ResponseEntity.ok(ApiResponse.ok("Quiz configurado com sucesso", dados)));
    }

    @PostMapping("/quiz/{quizId}/questions")
    public Mono<ResponseEntity<ApiResponse<QuestionResponse>>> adicionarPergunta(
            @PathVariable Long quizId,
            @RequestBody @Valid QuestionRequest request) {
        return service.adicionarPergunta(quizId, request)
                .map(dados -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok("Pergunta adicionada com sucesso", dados)));
    }

    @GetMapping("/quiz/{quizId}/questions")
    public Mono<ResponseEntity<ApiResponse<List<QuestionResponse>>>> listarPerguntas(@PathVariable Long quizId) {
        return service.listarPerguntas(quizId)
                .map(dados -> ResponseEntity.ok(ApiResponse.ok(dados)));
    }

    @PostMapping("/questions/{questionId}/alternatives")
    public Mono<ResponseEntity<ApiResponse<AlternativeResponse>>> adicionarAlternativa(
            @PathVariable Long questionId,
            @RequestBody @Valid AlternativeRequest request) {
        return service.adicionarAlternativa(questionId, request)
                .map(dados -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok("Alternativa adicionada com sucesso", dados)));
    }

    @GetMapping("/questions/{questionId}/alternatives")
    public Mono<ResponseEntity<ApiResponse<List<AlternativeResponse>>>> listarAlternativas(@PathVariable Long questionId) {
        return service.listarAlternativas(questionId)
                .map(dados -> ResponseEntity.ok(ApiResponse.ok(dados)));
    }

    @PostMapping("/modules/{moduleId}/quiz/gerar")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> gerarQuiz(
            @PathVariable Long moduleId,
            @RequestParam(defaultValue = "5") int quantidade) {
        return aiService.gerarQuiz(moduleId, quantidade)
                .map(task -> ResponseEntity.ok(ApiResponse.ok("Quiz enfileirado para geração via IA", task)));
    }

    @GetMapping("/modules/{moduleId}/quiz/pendente")
    public Mono<ResponseEntity<ApiResponse<QuizGeneratedResponse>>> buscarPendente(@PathVariable Long moduleId) {
        return aiService.buscarPendente(moduleId)
                .map(dados -> ResponseEntity.ok(ApiResponse.ok(dados)));
    }

    @PostMapping("/modules/{moduleId}/quiz/confirmar")
    public Mono<ResponseEntity<ApiResponse<QuizResponse>>> confirmarQuiz(@PathVariable Long moduleId) {
        return aiService.confirmarQuiz(moduleId)
                .flatMap(dados -> cache.delete(keyQuiz(moduleId)).thenReturn(dados))
                .map(dados -> ResponseEntity.ok(ApiResponse.ok("Quiz confirmado e salvo com sucesso", dados)));
    }

    @PostMapping("/modules/{moduleId}/quiz/regerar")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> regerarQuiz(
            @PathVariable Long moduleId,
            @RequestParam(defaultValue = "5") int quantidade) {
        return aiService.gerarQuiz(moduleId, quantidade)
                .map(task -> ResponseEntity.ok(ApiResponse.ok("Quiz regerado e enfileirado via IA", task)));
    }
}
