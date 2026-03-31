package br.ufc.llm.module.controller;

import br.ufc.llm.module.dto.ModuleRequest;
import br.ufc.llm.module.dto.ModuleResponse;
import br.ufc.llm.module.service.ModuleService;
import br.ufc.llm.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService service;

    @PostMapping("/courses/{courseId}/modules")
    public ResponseEntity<ApiResponse<ModuleResponse>> criar(
            @PathVariable Long courseId,
            @RequestBody @Valid ModuleRequest request) {
        ModuleResponse response = service.criar(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Módulo criado com sucesso", response));
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
