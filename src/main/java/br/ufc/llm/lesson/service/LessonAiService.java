package br.ufc.llm.lesson.service;

import br.ufc.llm.lesson.domain.FileType;
import br.ufc.llm.lesson.domain.Lesson;
import br.ufc.llm.lesson.dto.LessonResponse;
import br.ufc.llm.shared.client.RagIntegracaoClient;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LessonAiService {

    private final RagIntegracaoClient ragClient;
    private final LessonService lessonService;

    private final Map<Long, String> pendingContent = new ConcurrentHashMap<>();

    public String gerarConteudo(Long lessonId) {
        Lesson lesson = lessonService.buscarEntidade(lessonId);

        String fonte = extrairFonte(lesson);
        if (fonte == null || fonte.isBlank()) {
            throw new RegraDeNegocioException("A aula não possui conteúdo legível para gerar via IA");
        }

        String conteudo = ragClient.gerarConteudoHtml(fonte);
        pendingContent.put(lessonId, conteudo);
        return conteudo;
    }

    public String buscarConteudoPendente(Long lessonId) {
        String pendente = pendingContent.get(lessonId);
        if (pendente == null || pendente.isBlank()) {
            throw new RegraDeNegocioException("Nenhum conteúdo pendente para esta aula");
        }
        return pendente;
    }

    public LessonResponse confirmarConteudo(Long lessonId) {
        String pendente = buscarConteudoPendente(lessonId);
        Lesson lesson = lessonService.buscarEntidade(lessonId);
        lesson.setContentEditor(pendente);
        lessonService.salvarConteudoGerado(lesson);
        pendingContent.remove(lessonId);
        return LessonResponse.from(lesson);
    }

    private String extrairFonte(Lesson lesson) {
        if (lesson.getFilePath() != null && lesson.getFileType() == FileType.PDF) {
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
