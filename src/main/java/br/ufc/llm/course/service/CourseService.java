package br.ufc.llm.course.service;

import br.ufc.llm.course.domain.Course;
import br.ufc.llm.course.dto.CourseRequest;
import br.ufc.llm.course.dto.CourseResponse;
import br.ufc.llm.course.repository.CourseRepository;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository repository;

    public CourseResponse criar(CourseRequest request) {
        Course course = Course.builder()
                .title(request.title())
                .category(request.category())
                .description(request.description())
                .build();
        return CourseResponse.from(repository.save(course));
    }

    public List<CourseResponse> listar() {
        return repository.findAll().stream()
                .map(CourseResponse::from)
                .toList();
    }

    public CourseResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(CourseResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado: " + id));
    }
}
