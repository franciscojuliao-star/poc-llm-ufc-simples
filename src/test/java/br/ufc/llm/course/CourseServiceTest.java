package br.ufc.llm.course;

import br.ufc.llm.course.domain.Course;
import br.ufc.llm.course.dto.CourseRequest;
import br.ufc.llm.course.dto.CourseResponse;
import br.ufc.llm.course.repository.CourseRepository;
import br.ufc.llm.course.service.CourseService;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private CourseService service;

    private CourseRequest request() {
        return new CourseRequest("Java Avançado", "Tecnologia", "Curso de Java");
    }

    private Course courseSalvo() {
        return Course.builder().id(1L).title("Java Avançado").category("Tecnologia").description("Curso de Java").build();
    }

    @Test
    void deveCriarCursoSemImagem() {
        when(repository.save(any())).thenReturn(courseSalvo());

        CourseResponse response = service.criar(request(), null);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Java Avançado");
        verify(repository).save(any());
    }

    @Test
    void deveCriarCursoComImagemValida() {
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        when(repository.save(any())).thenReturn(courseSalvo());

        MockMultipartFile imagem = new MockMultipartFile(
                "imagem", "capa.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}
        );

        CourseResponse response = service.criar(request(), imagem);

        assertThat(response.id()).isEqualTo(1L);
        verify(repository).save(any());
    }

    @Test
    void deveLancarExcecaoParaImagemInvalida() {
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

        MockMultipartFile arquivo = new MockMultipartFile(
                "imagem", "documento.pdf", "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46}
        );

        assertThatThrownBy(() -> service.criar(request(), arquivo))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não é uma imagem válida");
    }

    @Test
    void deveListarCursos() {
        when(repository.findAll()).thenReturn(List.of(courseSalvo()));

        List<CourseResponse> lista = service.listar();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).title()).isEqualTo("Java Avançado");
    }

    @Test
    void deveBuscarCursoPorId() {
        when(repository.findById(1L)).thenReturn(Optional.of(courseSalvo()));

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
