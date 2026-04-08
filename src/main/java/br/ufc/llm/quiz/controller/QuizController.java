package br.ufc.llm.quiz.controller;

import br.ufc.llm.quiz.dto.QuizGeneratedResponse;
import br.ufc.llm.quiz.dto.QuizRequest;
import br.ufc.llm.quiz.dto.QuizResponse;
import br.ufc.llm.quiz.service.QuizAiService;
import br.ufc.llm.quiz.service.QuizService;
import br.ufc.llm.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class QuizController {

    private final QuizService service;
    private final QuizAiService aiService;

    @PostMapping("/modules/{moduleId}/quiz")
    public ResponseEntity<ApiResponse<QuizResponse>> criar(
            @PathVariable Long moduleId,
            @RequestBody @Valid QuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Quiz criado com sucesso", service.criar(moduleId, request)));
    }

    @GetMapping("/modules/{moduleId}/quiz")
    public ResponseEntity<ApiResponse<QuizResponse>> buscar(@PathVariable Long moduleId) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorModulo(moduleId)));
    }

    @PostMapping("/modules/{moduleId}/quiz/gerar")
    public ResponseEntity<ApiResponse<QuizGeneratedResponse>> gerarQuiz(
            @PathVariable Long moduleId,
            @RequestParam(defaultValue = "5") int quantidade) {
        return ResponseEntity.ok(ApiResponse.ok("Quiz gerado pela IA", aiService.gerarQuiz(moduleId, quantidade)));
    }

    @GetMapping("/modules/{moduleId}/quiz/pendente")
    public ResponseEntity<ApiResponse<QuizGeneratedResponse>> buscarPendente(@PathVariable Long moduleId) {
        return ResponseEntity.ok(ApiResponse.ok(aiService.buscarPendente(moduleId)));
    }

    @PostMapping("/modules/{moduleId}/quiz/confirmar")
    public ResponseEntity<ApiResponse<QuizResponse>> confirmarQuiz(@PathVariable Long moduleId) {
        return ResponseEntity.ok(ApiResponse.ok("Quiz confirmado e salvo com sucesso", aiService.confirmarQuiz(moduleId)));
    }

    @PostMapping("/modules/{moduleId}/quiz/regerar")
    public ResponseEntity<ApiResponse<QuizGeneratedResponse>> regerarQuiz(
            @PathVariable Long moduleId,
            @RequestParam(defaultValue = "5") int quantidade) {
        return ResponseEntity.ok(ApiResponse.ok("Quiz regerado pela IA", aiService.gerarQuiz(moduleId, quantidade)));
    }
}
