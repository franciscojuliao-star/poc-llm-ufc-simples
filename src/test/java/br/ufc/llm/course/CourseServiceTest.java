package br.ufc.llm.course;

import br.ufc.llm.course.domain.Course;
import br.ufc.llm.course.dto.CourseRequest;
import br.ufc.llm.course.dto.CourseResponse;
import br.ufc.llm.course.repository.CourseRepository;
import br.ufc.llm.course.service.CourseService;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private CourseService service;

    @Test
    void deveCriarCurso() {
        CourseRequest request = new CourseRequest("Java Avançado", "Tecnologia", "Curso de Java");
        Course saved = Course.builder().id(1L).title("Java Avançado").category("Tecnologia").description("Curso de Java").build();
        when(repository.save(any())).thenReturn(saved);

        CourseResponse response = service.criar(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Java Avançado");
        verify(repository).save(any());
    }

    @Test
    void deveListarCursos() {
        Course course = Course.builder().id(1L).title("Java").category("Tech").description("Desc").build();
        when(repository.findAll()).thenReturn(List.of(course));

        List<CourseResponse> lista = service.listar();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).title()).isEqualTo("Java");
    }

    @Test
    void deveBuscarCursoPorId() {
        Course course = Course.builder().id(1L).title("Java").category("Tech").description("Desc").build();
        when(repository.findById(1L)).thenReturn(Optional.of(course));

        CourseResponse response = service.buscarPorId(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void deveLancarExcecaoQuandoCursoNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }
}
