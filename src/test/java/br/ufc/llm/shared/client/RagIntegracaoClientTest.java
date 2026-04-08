package br.ufc.llm.shared.client;

import br.ufc.llm.quiz.dto.AlternativeResponse;
import br.ufc.llm.quiz.dto.QuizGeneratedResponse;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagIntegracaoClientTest {

    @Mock
    private RestTemplate restTemplate;

    private RagIntegracaoClient client;

    @BeforeEach
    void setUp() {
        client = new RagIntegracaoClient(restTemplate, "http://localhost:8000", "test-api-key");
    }

    // ── gerarQuiz ────────────────────────────────────────────────────────────

    @Test
    void deveGerarQuizComSucesso() {
        var respostaApi = List.of(
                Map.of("statement", "O que é Java?", "points", 1,
                        "alternatives", List.of(
                                Map.of("text", "Linguagem", "correct", true),
                                Map.of("text", "Framework", "correct", false),
                                Map.of("text", "Banco de dados", "correct", false),
                                Map.of("text", "Sistema operacional", "correct", false)
                        ))
        );

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(List.class)))
                .thenReturn(ResponseEntity.ok(respostaApi));

        QuizGeneratedResponse response = client.gerarQuiz("Conteúdo sobre Java", 1);

        assertThat(response.questions()).hasSize(1);
        assertThat(response.questions().get(0).statement()).isEqualTo("O que é Java?");
        assertThat(response.questions().get(0).alternatives()).hasSize(4);
    }

    @Test
    void deveGerarQuizComAlternativaCorretaMarcada() {
        var respostaApi = List.of(
                Map.of("statement", "Pergunta?", "points", 1,
                        "alternatives", List.of(
                                Map.of("text", "Certa", "correct", true),
                                Map.of("text", "Errada", "correct", false),
                                Map.of("text", "Errada 2", "correct", false),
                                Map.of("text", "Errada 3", "correct", false)
                        ))
        );

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(List.class)))
                .thenReturn(ResponseEntity.ok(respostaApi));

        QuizGeneratedResponse response = client.gerarQuiz("Conteúdo", 1);

        List<AlternativeResponse> alternatives = response.questions().get(0).alternatives();
        long corretas = alternatives.stream().filter(AlternativeResponse::correct).count();
        assertThat(corretas).isEqualTo(1);
        assertThat(alternatives.get(0).text()).isEqualTo("Certa");
        assertThat(alternatives.get(0).correct()).isTrue();
    }

    @Test
    void deveLancarExcecaoQuandoRagRetornarListaVazia() {
        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(List.class)))
                .thenReturn(ResponseEntity.ok(List.of()));

        assertThatThrownBy(() -> client.gerarQuiz("Conteúdo", 5))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nenhuma questão");
    }

    @Test
    void deveLancarExcecaoQuandoRagRetornarNullNoQuiz() {
        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(List.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> client.gerarQuiz("Conteúdo", 5))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nenhuma questão");
    }

    @Test
    void deveLancarExcecaoQuandoApiKeyInvalida() {
        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(List.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

        assertThatThrownBy(() -> client.gerarQuiz("Conteúdo", 5))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("API key");
    }

    @Test
    void deveLancarExcecaoQuandoRagIndisponivelNoQuiz() {
        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(List.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> client.gerarQuiz("Conteúdo", 5))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("indisponível");
    }

    // ── gerarConteudoHtml ────────────────────────────────────────────────────

    @Test
    void deveGerarConteudoHtmlComSucesso() {
        var respostaApi = Map.of("conteudo_html", "<h2>Java</h2><p>Conteúdo gerado</p>");

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(respostaApi));

        String html = client.gerarConteudoHtml("Conteúdo sobre Java");

        assertThat(html).isEqualTo("<h2>Java</h2><p>Conteúdo gerado</p>");
    }

    @Test
    void deveLancarExcecaoQuandoRagRetornarHtmlVazio() {
        var respostaApi = Map.of("conteudo_html", "");

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(respostaApi));

        assertThatThrownBy(() -> client.gerarConteudoHtml("Conteúdo"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("HTML");
    }

    @Test
    void deveLancarExcecaoQuandoRagRetornarNullNoHtml() {
        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> client.gerarConteudoHtml("Conteúdo"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("HTML");
    }

    @Test
    void deveLancarExcecaoQuandoRagIndisponivelNoHtml() {
        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> client.gerarConteudoHtml("Conteúdo"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("indisponível");
    }
}
