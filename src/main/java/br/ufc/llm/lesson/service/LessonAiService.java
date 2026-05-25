package br.ufc.llm.lesson.service;

import br.ufc.llm.lesson.domain.Lesson;
import br.ufc.llm.lesson.dto.LessonResponse;
import br.ufc.llm.shared.cache.CacheService;
import br.ufc.llm.shared.client.RagIntegracaoClient;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonAiService {

    private final RagIntegracaoClient ragClient;
    private final LessonService lessonService;
    private final CacheService cache;

    private static String keyPending(Long lessonId) {
        return "pending:lesson:" + lessonId;
    }

    public Mono<Map<String, String>> gerarConteudo(Long lessonId) {
        return lessonService.buscarEntidade(lessonId)
                .flatMap(lesson -> {
                    String fonte = extrairFonte(lesson);
                    if (fonte == null || fonte.isBlank()) {
                        return Mono.error(new RegraDeNegocioException("A aula não possui conteúdo legível para gerar via IA"));
                    }
                    String taskId = UUID.randomUUID().toString();
                    gerarConteudoAsync(lessonId, fonte);
                    return Mono.just(Map.of("task_id", taskId, "status", "PROCESSING"));
                });
    }

    @Async("asyncExecutor")
    public void gerarConteudoAsync(Long lessonId, String fonte) {
        try {
            String conteudo = ragClient.gerarConteudoHtml(fonte);
            cache.setPending(keyPending(lessonId), conteudo).block();
        } catch (Exception e) {
            log.error("Erro ao gerar conteúdo para a aula {}: {}", lessonId, e.getMessage());
        }
    }

    public Mono<String> buscarConteudoPendente(Long lessonId) {
        return cache.get(keyPending(lessonId))
                .switchIfEmpty(Mono.error(new RegraDeNegocioException("Nenhum conteúdo pendente para esta aula")));
    }

    public Mono<LessonResponse> confirmarConteudo(Long lessonId) {
        return Mono.zip(
                buscarConteudoPendente(lessonId),
                lessonService.buscarEntidade(lessonId)
        ).flatMap(tuple -> {
            String pendente = tuple.getT1();
            Lesson lesson = tuple.getT2();
            lesson.setContentEditor(pendente);
            return lessonService.salvarConteudoGerado(lesson)
                    .then(cache.delete(keyPending(lessonId)))
                    .thenReturn(LessonResponse.from(lesson));
        });
    }

    private String extrairFonte(Lesson lesson) {
        if (lesson.getFilePath() != null && "PDF".equals(lesson.getFileType())) {
            return extrairTextoPdf(lesson.getFilePath());
        }
        return lesson.getContentEditor();
    }

    private static final int MAX_CHARS = 12_000;

    private String extrairTextoPdf(String filePath) {
        try (PDDocument doc = Loader.loadPDF(new File(filePath))) {
            String texto = new PDFTextStripper().getText(doc);
            return texto.length() > MAX_CHARS ? texto.substring(0, MAX_CHARS) : texto;
        } catch (IOException e) {
            throw new RegraDeNegocioException("Erro ao extrair texto do PDF: " + e.getMessage());
        }
    }
}
