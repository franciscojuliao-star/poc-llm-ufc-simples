package br.ufc.llm.shared.client;

import br.ufc.llm.quiz.dto.AlternativeResponse;
import br.ufc.llm.quiz.dto.QuizGeneratedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;

/**
 * Teste de integração real: Java → RAG API (Python/FastAPI).
 * Requer o container RAG rodando em http://localhost:8000.
 * Rodar com: mvn test -Dtest=RagIntegracaoClientIT
 */
@Tag("integration")
class RagIntegracaoClientIT {

    private static final String RAG_URL = "http://localhost:8000";
    private static final String API_KEY = "integracoes-dev-key-mude-em-producao";

    private static final String CONTEUDO_TESTE = """
            A fotossíntese é o processo pelo qual plantas, algas e algumas bactérias
            convertem luz solar em energia química. Esse processo ocorre nos cloroplastos,
            organelas presentes nas células vegetais que contêm clorofila.
            A clorofila absorve principalmente luz vermelha e azul, refletindo a luz verde.
            O processo se divide em duas etapas: as reações luminosas, que ocorrem nas
            membranas dos tilacoides e convertem energia luminosa em ATP e NADPH;
            e o Ciclo de Calvin, que usa esses compostos para fixar CO2 e produzir glicose.
            """;

    private RagIntegracaoClient client;

    @BeforeEach
    void setUp() {
        client = new RagIntegracaoClient(new RestTemplate(), RAG_URL, API_KEY);
    }

    // ── Quiz ─────────────────────────────────────────────────────────────────

    @Test
    void deveGerarQuizComQuestoesDaApiReal() {
        QuizGeneratedResponse response = client.gerarQuiz(CONTEUDO_TESTE, 3);

        assertThat(response.questions())
                .hasSize(3)
                .allSatisfy(q -> {
                    assertThat(q.statement()).isNotBlank();
                    assertThat(q.alternatives()).hasSize(4);
                    assertThat(q.points()).isGreaterThan(0);
                });
    }

    @Test
    void deveGerarQuizComExatamenteUmaAlternativaCorreta() {
        QuizGeneratedResponse response = client.gerarQuiz(CONTEUDO_TESTE, 2);

        assertThat(response.questions()).allSatisfy(q -> {
            long corretas = q.alternatives().stream()
                    .filter(AlternativeResponse::correct)
                    .count();
            assertThat(corretas)
                    .as("Questão '%s' deve ter exatamente 1 correta", q.statement())
                    .isEqualTo(1);
        });
    }

    @Test
    void deveGerarQuizComQuantidadeSolicitada() {
        int quantidade = 5;

        QuizGeneratedResponse response = client.gerarQuiz(CONTEUDO_TESTE, quantidade);

        assertThat(response.questions()).hasSize(quantidade);
    }

    // ── Conteúdo HTML ────────────────────────────────────────────────────────

    @Test
    void deveGerarHtmlComTagsSemanticas() {
        String html = client.gerarConteudoHtml(CONTEUDO_TESTE);

        assertThat(html)
                .isNotBlank()
                .containsPattern("<h[1-6]")
                .contains("<p>");
    }

    @Test
    void deveGerarHtmlSemDelimitadoresDeCodigoMarkdown() {
        String html = client.gerarConteudoHtml(CONTEUDO_TESTE);

        assertThat(html)
                .doesNotContain("```")
                .doesNotContain("```html");
    }
}
