package br.ufc.llm.lesson.service;

import br.ufc.llm.lesson.domain.Lesson;
import br.ufc.llm.lesson.dto.LessonResponse;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class LessonAiService {

    private final ChatClient chatClient;
    private final LessonService lessonService;

    public String gerarConteudo(Long lessonId) {
        Lesson lesson = lessonService.buscarEntidade(lessonId);

        String fonte = extrairFonte(lesson);
        if (fonte == null || fonte.isBlank()) {
            throw new RegraDeNegocioException("A aula não possui conteúdo legível para gerar via IA");
        }

        String conteudo = chatClient.prompt()
                .user("""
                        Você é um especialista em educação online. A partir do conteúdo abaixo, \
                        gere um conteúdo de aula formatado em HTML semântico, bem estruturado, \
                        com títulos (h2, h3), parágrafos, listas e destaques onde apropriado. \
                        Retorne apenas o HTML sem delimitadores de código.

                        Conteúdo:
                        """ + fonte)
                .call()
                .content();

        lesson.setContentGenerated(conteudo);
        lessonService.salvarConteudoGerado(lesson);
        return conteudo;
    }

    public String buscarConteudoPendente(Long lessonId) {
        Lesson lesson = lessonService.buscarEntidade(lessonId);
        if (lesson.getContentGenerated() == null || lesson.getContentGenerated().isBlank()) {
            throw new RegraDeNegocioException("Nenhum conteúdo pendente para esta aula");
        }
        return lesson.getContentGenerated();
    }

    public LessonResponse confirmarConteudo(Long lessonId) {
        Lesson lesson = lessonService.buscarEntidade(lessonId);
        if (lesson.getContentGenerated() == null || lesson.getContentGenerated().isBlank()) {
            throw new RegraDeNegocioException("Nenhum conteúdo pendente para confirmar");
        }
        lesson.setContentEditor(lesson.getContentGenerated());
        lesson.setContentGenerated(null);
        lessonService.salvarConteudoGerado(lesson);
        return LessonResponse.from(lesson);
    }

    private String extrairFonte(Lesson lesson) {
        if (lesson.getFilePath() != null && lesson.getFilePath().endsWith(".pdf")) {
            return extrairTextoPdf(lesson.getFilePath());
        }
        return lesson.getContentEditor();
    }

    private String extrairTextoPdf(String filePath) {
        try (PDDocument doc = PDDocument.load(new File(filePath))) {
            return new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw new RegraDeNegocioException("Erro ao extrair texto do PDF: " + e.getMessage());
        }
    }
}
