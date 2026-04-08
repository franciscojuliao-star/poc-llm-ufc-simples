package br.ufc.llm.shared.client;

import br.ufc.llm.quiz.dto.AlternativeResponse;
import br.ufc.llm.quiz.dto.QuestionResponse;
import br.ufc.llm.quiz.dto.QuizGeneratedResponse;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RagIntegracaoClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public RagIntegracaoClient(
            RestTemplate restTemplate,
            @Value("${rag.api.base-url:http://localhost:8000}") String baseUrl,
            @Value("${rag.api.key:integracoes-dev-key-mude-em-producao}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public QuizGeneratedResponse gerarQuiz(String conteudo, int quantidade) {
        var body = Map.of("conteudo", conteudo, "quantidade", quantidade);
        var entity = new HttpEntity<>(body, headers());

        try {
            var response = restTemplate.exchange(
                    baseUrl + "/api/integracao/gerar/quiz",
                    HttpMethod.POST, entity, List.class);

            List<?> lista = response.getBody();
            if (lista == null || lista.isEmpty()) {
                throw new RegraDeNegocioException("RAG retornou nenhuma questão");
            }

            return parseQuiz(lista);

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new RegraDeNegocioException("API key inválida para o serviço RAG");
        } catch (ResourceAccessException e) {
            log.error("Serviço RAG indisponível: {}", e.getMessage());
            throw new RegraDeNegocioException("Serviço de IA indisponível. Tente novamente em instantes.");
        }
    }

    public String gerarConteudoHtml(String conteudo) {
        var body = Map.of("conteudo", conteudo);
        var entity = new HttpEntity<>(body, headers());

        try {
            var response = restTemplate.exchange(
                    baseUrl + "/api/integracao/gerar/conteudo-html",
                    HttpMethod.POST, entity, Map.class);

            Map<?, ?> resultado = response.getBody();
            if (resultado == null) {
                throw new RegraDeNegocioException("RAG retornou HTML vazio");
            }

            String html = (String) resultado.get("conteudo_html");
            if (html == null || html.isBlank()) {
                throw new RegraDeNegocioException("RAG retornou HTML vazio");
            }

            return html;

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new RegraDeNegocioException("API key inválida para o serviço RAG");
        } catch (ResourceAccessException e) {
            log.error("Serviço RAG indisponível: {}", e.getMessage());
            throw new RegraDeNegocioException("Serviço de IA indisponível. Tente novamente em instantes.");
        }
    }

    private HttpHeaders headers() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private QuizGeneratedResponse parseQuiz(List<?> lista) {
        List<QuestionResponse> questions = lista.stream().map(item -> {
            Map<String, Object> q = (Map<String, Object>) item;
            String statement = (String) q.get("statement");
            int points = ((Number) q.getOrDefault("points", 1)).intValue();

            List<Map<String, Object>> alts = (List<Map<String, Object>>) q.get("alternatives");
            List<AlternativeResponse> alternatives = alts == null ? List.of() : alts.stream()
                    .map(a -> new AlternativeResponse(null, (String) a.get("text"), Boolean.TRUE.equals(a.get("correct"))))
                    .toList();

            return new QuestionResponse(null, statement, points, 0, alternatives);
        }).toList();

        return new QuizGeneratedResponse(questions);
    }
}
