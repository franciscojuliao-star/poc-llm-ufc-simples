package br.ufc.llm.quiz.service;

import br.ufc.llm.lesson.repository.LessonRepository;
import br.ufc.llm.module.repository.ModuleRepository;
import br.ufc.llm.quiz.dto.*;
import br.ufc.llm.quiz.repository.QuizRepository;
import br.ufc.llm.shared.cache.CacheService;
import br.ufc.llm.shared.client.RagIntegracaoClient;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAiService {

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final QuizService quizService;
    private final RagIntegracaoClient ragClient;
    private final CacheService cache;
    private final ObjectMapper objectMapper;

    private static String keyPending(Long moduleId) {
        return "pending:quiz:" + moduleId;
    }

    public Mono<Map<String, String>> gerarQuiz(Long moduleId, int quantidade) {
        return moduleRepository.existsById(moduleId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RecursoNaoEncontradoException("Módulo não encontrado: " + moduleId));
                    }
                    return lessonRepository.findByModuleIdOrderByOrderNumAsc(moduleId).collectList();
                })
                .flatMap(aulas -> {
                    StringBuilder sb = new StringBuilder();
                    for (var lesson : aulas) {
                        if (lesson.getContentEditor() != null && !lesson.getContentEditor().isBlank()) {
                            sb.append(lesson.getContentEditor()).append("\n");
                        } else if (lesson.getContentGenerated() != null && !lesson.getContentGenerated().isBlank()) {
                            sb.append(lesson.getContentGenerated()).append("\n");
                        } else if (lesson.getFilePath() != null && "PDF".equals(lesson.getFileType())) {
                            try {
                                sb.append(extrairTextoPdf(lesson.getFilePath())).append("\n");
                            } catch (Exception e) {
                                log.warn("Falha ao extrair PDF da aula {}. Ignorando.", lesson.getId(), e);
                            }
                        }
                    }
                    if (sb.isEmpty()) {
                        return Mono.error(new RegraDeNegocioException(
                                "O módulo não possui conteúdo legível para gerar quiz via IA"));
                    }
                    String taskId = UUID.randomUUID().toString();
                    gerarQuizAsync(moduleId, sb.toString(), quantidade);
                    return Mono.just(Map.of("task_id", taskId, "status", "PROCESSING"));
                });
    }

    @Async("asyncExecutor")
    public void gerarQuizAsync(Long moduleId, String conteudo, int quantidade) {
        try {
            QuizGeneratedResponse gerado = ragClient.gerarQuiz(conteudo, quantidade);
            String json = objectMapper.writeValueAsString(gerado);
            cache.setPending(keyPending(moduleId), json).block();
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar quiz para o módulo {}: {}", moduleId, e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao gerar quiz para o módulo {}: {}", moduleId, e.getMessage());
        }
    }

    public Mono<QuizGeneratedResponse> buscarPendente(Long moduleId) {
        return cache.get(keyPending(moduleId))
                .switchIfEmpty(Mono.error(new RegraDeNegocioException("Nenhum quiz pendente para o módulo: " + moduleId)))
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, QuizGeneratedResponse.class));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RegraDeNegocioException("Falha ao interpretar quiz pendente"));
                    }
                });
    }

    public Mono<QuizResponse> confirmarQuiz(Long moduleId) {
        return buscarPendente(moduleId)
                .flatMap(pendente -> quizService.salvarQuizCompleto(moduleId, pendente.questions()))
                .flatMap(quizResponse ->
                        cache.delete(keyPending(moduleId)).thenReturn(quizResponse));
    }

    private String extrairTextoPdf(String filePath) throws IOException {
        try (PDDocument doc = Loader.loadPDF(new File(filePath))) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
