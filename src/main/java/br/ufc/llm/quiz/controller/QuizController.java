package br.ufc.llm.quiz.controller;

import br.ufc.llm.quiz.dto.AlternativeRequest;
import br.ufc.llm.quiz.dto.AlternativeResponse;
import br.ufc.llm.quiz.dto.QuestionRequest;
import br.ufc.llm.quiz.dto.QuestionResponse;
import br.ufc.llm.quiz.dto.QuizConfigRequest;
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

import java.util.List;

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

    @PostMapping("/quiz/{quizId}")
    public ResponseEntity<ApiResponse<QuizResponse>> configurar(
            @PathVariable Long quizId,
            @RequestBody QuizConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Quiz configurado com sucesso", service.configurar(quizId, request)));
    }

    @PostMapping("/quiz/{quizId}/questions")
    public ResponseEntity<ApiResponse<QuestionResponse>> adicionarPergunta(
            @PathVariable Long quizId,
            @RequestBody @Valid QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Pergunta adicionada com sucesso", service.adicionarPergunta(quizId, request)));
    }

    @GetMapping("/quiz/{quizId}/questions")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> listarPerguntas(@PathVariable Long quizId) {
        return ResponseEntity.ok(ApiResponse.ok(service.listarPerguntas(quizId)));
    }

    @PostMapping("/questions/{questionId}/alternatives")
    public ResponseEntity<ApiResponse<AlternativeResponse>> adicionarAlternativa(
            @PathVariable Long questionId,
            @RequestBody @Valid AlternativeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Alternativa adicionada com sucesso", service.adicionarAlternativa(questionId, request)));
    }

    @GetMapping("/questions/{questionId}/alternatives")
    public ResponseEntity<ApiResponse<List<AlternativeResponse>>> listarAlternativas(@PathVariable Long questionId) {
        return ResponseEntity.ok(ApiResponse.ok(service.listarAlternativas(questionId)));
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
