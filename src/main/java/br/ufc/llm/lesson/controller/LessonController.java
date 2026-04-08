package br.ufc.llm.lesson.controller;

import br.ufc.llm.lesson.dto.LessonRequest;
import br.ufc.llm.lesson.dto.LessonResponse;
import br.ufc.llm.lesson.service.LessonAiService;
import br.ufc.llm.lesson.service.LessonService;
import br.ufc.llm.shared.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LessonController {

    private final LessonService service;
    private final LessonAiService aiService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/modules/{moduleId}/lessons", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LessonResponse>> criar(
            @PathVariable Long moduleId,
            @RequestParam("dados") String dadosJson,
            @RequestParam(value = "arquivo", required = false) MultipartFile arquivo) throws Exception {
        LessonRequest request = objectMapper.readValue(dadosJson, LessonRequest.class);
        LessonResponse response = service.criar(moduleId, request, arquivo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Aula criada com sucesso", response));
    }

    @GetMapping("/modules/{moduleId}/lessons")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> listar(@PathVariable Long moduleId) {
        return ResponseEntity.ok(ApiResponse.ok(service.listarPorModulo(moduleId)));
    }

    @GetMapping("/lessons/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorId(id)));
    }

    @PostMapping("/lessons/{id}/gerar-conteudo")
    public ResponseEntity<ApiResponse<String>> gerarConteudo(@PathVariable Long id) {
        String conteudo = aiService.gerarConteudo(id);
        return ResponseEntity.ok(ApiResponse.ok("Conteúdo gerado pela IA", conteudo));
    }

    @GetMapping("/lessons/{id}/conteudo-pendente")
    public ResponseEntity<ApiResponse<String>> conteudoPendente(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(aiService.buscarConteudoPendente(id)));
    }

    @PostMapping("/lessons/{id}/confirmar-conteudo")
    public ResponseEntity<ApiResponse<LessonResponse>> confirmarConteudo(@PathVariable Long id) {
        LessonResponse response = aiService.confirmarConteudo(id);
        return ResponseEntity.ok(ApiResponse.ok("Conteúdo confirmado e salvo", response));
    }

    @PostMapping("/lessons/{id}/regerar-conteudo")
    public ResponseEntity<ApiResponse<String>> regerarConteudo(@PathVariable Long id) {
        String conteudo = aiService.gerarConteudo(id);
        return ResponseEntity.ok(ApiResponse.ok("Conteúdo regerado pela IA", conteudo));
    }
}
