package br.ufc.llm.module.controller;

import br.ufc.llm.module.dto.ModuleRequest;
import br.ufc.llm.module.dto.ModuleResponse;
import br.ufc.llm.module.service.ModuleService;
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
public class ModuleController {

    private final ModuleService service;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/courses/{courseId}/modules", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ModuleResponse>> criar(
            @PathVariable Long courseId,
            @RequestParam("dados") String dadosJson,
            @RequestPart(value = "imagem", required = false) MultipartFile imagem) throws Exception {
        ModuleRequest request = objectMapper.readValue(dadosJson, ModuleRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Módulo criado com sucesso", service.criar(courseId, request, imagem)));
    }

    @GetMapping("/courses/{courseId}/modules")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> listar(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.ok(service.listarPorCurso(courseId)));
    }

    @GetMapping("/modules/{id}")
    public ResponseEntity<ApiResponse<ModuleResponse>> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorId(id)));
    }
}
