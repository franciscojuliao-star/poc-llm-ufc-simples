package br.ufc.llm.course.controller;

import br.ufc.llm.course.dto.CourseRequest;
import br.ufc.llm.course.dto.CourseResponse;
import br.ufc.llm.course.service.CourseService;
import br.ufc.llm.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService service;

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> criar(@RequestBody @Valid CourseRequest request) {
        CourseResponse response = service.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Curso criado com sucesso", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(service.listar()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorId(id)));
    }
}
