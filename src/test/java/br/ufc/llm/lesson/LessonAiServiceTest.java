package br.ufc.llm.lesson;

import br.ufc.llm.lesson.domain.Lesson;
import br.ufc.llm.lesson.dto.LessonResponse;
import br.ufc.llm.lesson.service.LessonAiService;
import br.ufc.llm.lesson.service.LessonService;
import br.ufc.llm.module.domain.Module;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonAiServiceTest {

    @Mock private ChatClient chatClient;
    @Mock private LessonService lessonService;

    @InjectMocks
    private LessonAiService service;

    @SuppressWarnings("unchecked")
    private void mockChatClient(String retorno) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(retorno);
    }

    private Module moduleMock() {
        return Module.builder().id(1L).name("M1").orderNum(1).build();
    }

    private Lesson lessonComConteudo() {
        return Lesson.builder()
                .id(1L).name("Aula 1").orderNum(1)
                .contentEditor("<p>Conteúdo sobre Java</p>")
                .module(moduleMock())
                .build();
    }

    @Test
    void deveGerarConteudoEArmazenarEmMemoria() {
        Lesson lesson = lessonComConteudo();
        when(lessonService.buscarEntidade(1L)).thenReturn(lesson);
        mockChatClient("<h2>Java</h2><p>Conteúdo gerado</p>");

        String conteudo = service.gerarConteudo(1L);

        assertThat(conteudo).contains("<h2>Java</h2>");
        // não persiste no banco — apenas fica em memória
        verify(lessonService, never()).salvarConteudoGerado(any());
    }

    @Test
    void deveLancarExcecaoQuandoAulaSemConteudoLegivel() {
        Lesson lesson = Lesson.builder()
                .id(1L).name("Aula vazia").orderNum(1)
                .module(moduleMock())
                .build();
        when(lessonService.buscarEntidade(1L)).thenReturn(lesson);

        assertThatThrownBy(() -> service.gerarConteudo(1L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não possui conteúdo");
    }

    @Test
    void deveBuscarConteudoPendenteDaMemoria() {
        Lesson lesson = lessonComConteudo();
        when(lessonService.buscarEntidade(1L)).thenReturn(lesson);
        mockChatClient("<h2>Gerado</h2>");
        service.gerarConteudo(1L);

        String pendente = service.buscarConteudoPendente(1L);

        assertThat(pendente).isEqualTo("<h2>Gerado</h2>");
    }

    @Test
    void deveLancarExcecaoQuandoNaoHaConteudoPendente() {
        assertThatThrownBy(() -> service.buscarConteudoPendente(1L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Nenhum conteúdo pendente");
    }

    @Test
    void deveConfirmarConteudoPersistindoNoContentEditor() {
        Lesson lesson = lessonComConteudo();
        when(lessonService.buscarEntidade(1L)).thenReturn(lesson);
        mockChatClient("<h2>Gerado</h2>");
        service.gerarConteudo(1L);

        LessonResponse response = service.confirmarConteudo(1L);

        assertThat(lesson.getContentEditor()).isEqualTo("<h2>Gerado</h2>");
        verify(lessonService).salvarConteudoGerado(lesson);
        // após confirmar, pendente é removido do mapa
        assertThatThrownBy(() -> service.buscarConteudoPendente(1L))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveLancarExcecaoAoConfirmarSemConteudoPendente() {
        assertThatThrownBy(() -> service.confirmarConteudo(1L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Nenhum conteúdo pendente");
    }

    @Test
    void deveRegerarConteudoSobrescrevendoPendente() {
        Lesson lesson = lessonComConteudo();
        when(lessonService.buscarEntidade(1L)).thenReturn(lesson);

        mockChatClient("<h2>Primeiro</h2>");
        service.gerarConteudo(1L);

        mockChatClient("<h2>Segundo</h2>");
        service.gerarConteudo(1L);

        assertThat(service.buscarConteudoPendente(1L)).isEqualTo("<h2>Segundo</h2>");
    }
}
