package br.ufc.llm.lesson;

import br.ufc.llm.lesson.domain.FileType;
import br.ufc.llm.lesson.domain.Lesson;
import br.ufc.llm.lesson.dto.LessonRequest;
import br.ufc.llm.lesson.dto.LessonResponse;
import br.ufc.llm.lesson.repository.LessonRepository;
import br.ufc.llm.lesson.service.LessonService;
import br.ufc.llm.module.domain.Module;
import br.ufc.llm.module.repository.ModuleRepository;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @InjectMocks
    private LessonService service;

    private Module moduleMock() {
        return Module.builder().id(1L).name("Módulo 1").orderNum(1).build();
    }

    @Test
    void deveCriarAulaSemArquivo() {
        Module module = moduleMock();
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.countByModuleId(1L)).thenReturn(0);

        Lesson saved = Lesson.builder()
                .id(1L).name("Aula 1").orderNum(1).module(module).build();
        when(lessonRepository.save(any())).thenReturn(saved);

        LessonResponse response = service.criar(1L, new LessonRequest("Aula 1", null), null);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Aula 1");
        assertThat(response.orderNum()).isEqualTo(1);
        verify(lessonRepository).save(any());
    }

    @Test
    void deveIncrementarOrdemAoCriarAula() {
        Module module = moduleMock();
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.countByModuleId(1L)).thenReturn(2);

        Lesson saved = Lesson.builder()
                .id(3L).name("Aula 3").orderNum(3).module(module).build();
        when(lessonRepository.save(any())).thenReturn(saved);

        LessonResponse response = service.criar(1L, new LessonRequest("Aula 3", null), null);

        assertThat(response.orderNum()).isEqualTo(3);
    }

    @Test
    void deveLancarExcecaoQuandoModuloNaoEncontrado() {
        when(moduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(99L, new LessonRequest("X", null), null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveLancarExcecaoParaArquivoDeFormatoInvalido() {
        Module module = moduleMock();
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(lessonRepository.countByModuleId(1L)).thenReturn(0);

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "planilha.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "conteudo".getBytes()
        );

        assertThatThrownBy(() -> service.criar(1L, new LessonRequest("X", null), arquivo))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não suportado");
    }

    @Test
    void deveListarAulasPorModulo() {
        Module module = moduleMock();
        when(moduleRepository.existsById(1L)).thenReturn(true);
        Lesson lesson = Lesson.builder().id(1L).name("A1").orderNum(1).module(module).build();
        when(lessonRepository.findByModuleIdOrderByOrderNumAsc(1L)).thenReturn(List.of(lesson));

        List<LessonResponse> lista = service.listarPorModulo(1L);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).name()).isEqualTo("A1");
    }

    @Test
    void deveLancarExcecaoAoListarAulasDeModuloInexistente() {
        when(moduleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.listarPorModulo(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveBuscarAulaPorId() {
        Module module = moduleMock();
        Lesson lesson = Lesson.builder().id(1L).name("A1").orderNum(1).module(module).build();
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));

        LessonResponse response = service.buscarPorId(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void deveLancarExcecaoQuandoAulaNaoEncontrada() {
        when(lessonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }
}
