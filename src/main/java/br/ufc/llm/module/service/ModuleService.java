package br.ufc.llm.module.service;

import br.ufc.llm.course.domain.Course;
import br.ufc.llm.course.repository.CourseRepository;
import br.ufc.llm.module.domain.Module;
import br.ufc.llm.module.dto.ModuleRequest;
import br.ufc.llm.module.dto.ModuleResponse;
import br.ufc.llm.module.repository.ModuleRepository;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;

    public ModuleResponse criar(Long courseId, ModuleRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado: " + courseId));

        int ordem = moduleRepository.countByCourseId(courseId) + 1;

        Module module = Module.builder()
                .name(request.name())
                .orderNum(ordem)
                .course(course)
                .build();

        return ModuleResponse.from(moduleRepository.save(module));
    }

    public List<ModuleResponse> listarPorCurso(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new RecursoNaoEncontradoException("Curso não encontrado: " + courseId);
        }
        return moduleRepository.findByCourseIdOrderByOrderNumAsc(courseId).stream()
                .map(ModuleResponse::from)
                .toList();
    }

    public ModuleResponse buscarPorId(Long id) {
        return moduleRepository.findById(id)
                .map(ModuleResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Módulo não encontrado: " + id));
    }
}
