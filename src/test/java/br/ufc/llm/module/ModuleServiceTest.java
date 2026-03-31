package br.ufc.llm.module;

import br.ufc.llm.course.domain.Course;
import br.ufc.llm.course.repository.CourseRepository;
import br.ufc.llm.module.domain.Module;
import br.ufc.llm.module.dto.ModuleRequest;
import br.ufc.llm.module.dto.ModuleResponse;
import br.ufc.llm.module.repository.ModuleRepository;
import br.ufc.llm.module.service.ModuleService;
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
class ModuleServiceTest {

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ModuleService service;

    private Course cursoMock() {
        return Course.builder().id(1L).title("Java").category("Tech").description("Desc").build();
    }

    @Test
    void deveCriarModulo() {
        Course course = cursoMock();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(moduleRepository.countByCourseId(1L)).thenReturn(0);

        Module saved = Module.builder().id(1L).name("Módulo 1").orderNum(1).course(course).build();
        when(moduleRepository.save(any())).thenReturn(saved);

        ModuleResponse response = service.criar(1L, new ModuleRequest("Módulo 1"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.orderNum()).isEqualTo(1);
    }

    @Test
    void deveIncrementarOrdemAoCriarModulo() {
        Course course = cursoMock();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(moduleRepository.countByCourseId(1L)).thenReturn(2);

        Module saved = Module.builder().id(3L).name("Módulo 3").orderNum(3).course(course).build();
        when(moduleRepository.save(any())).thenReturn(saved);

        ModuleResponse response = service.criar(1L, new ModuleRequest("Módulo 3"));

        assertThat(response.orderNum()).isEqualTo(3);
    }

    @Test
    void deveLancarExcecaoQuandoCursoNaoEncontrado() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(99L, new ModuleRequest("X")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveListarModulosPorCurso() {
        Course course = cursoMock();
        when(courseRepository.existsById(1L)).thenReturn(true);
        Module m = Module.builder().id(1L).name("M1").orderNum(1).course(course).build();
        when(moduleRepository.findByCourseIdOrderByOrderNumAsc(1L)).thenReturn(List.of(m));

        List<ModuleResponse> lista = service.listarPorCurso(1L);

        assertThat(lista).hasSize(1);
    }
}
