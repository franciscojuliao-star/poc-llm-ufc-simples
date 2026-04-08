package br.ufc.llm.quiz.service;

import br.ufc.llm.lesson.domain.FileType;
import br.ufc.llm.lesson.domain.Lesson;
import br.ufc.llm.lesson.repository.LessonRepository;
import br.ufc.llm.module.domain.Module;
import br.ufc.llm.module.repository.ModuleRepository;
import br.ufc.llm.quiz.domain.Alternative;
import br.ufc.llm.quiz.domain.Question;
import br.ufc.llm.quiz.domain.Quiz;
import br.ufc.llm.quiz.dto.AlternativeResponse;
import br.ufc.llm.quiz.dto.QuestionResponse;
import br.ufc.llm.quiz.dto.QuizGeneratedResponse;
import br.ufc.llm.quiz.dto.QuizResponse;
import br.ufc.llm.quiz.repository.QuizRepository;
import br.ufc.llm.shared.client.RagIntegracaoClient;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAiService {

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final RagIntegracaoClient ragClient;

    private final Map<Long, QuizGeneratedResponse> pendingQuizzes = new ConcurrentHashMap<>();

    public QuizGeneratedResponse gerarQuiz(Long moduleId, int quantidade) {
        moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Módulo não encontrado: " + moduleId));

        String conteudo = coletarConteudo(moduleId);

        QuizGeneratedResponse gerado = ragClient.gerarQuiz(conteudo, quantidade);
        pendingQuizzes.put(moduleId, gerado);
        return gerado;
    }

    public QuizGeneratedResponse buscarPendente(Long moduleId) {
        QuizGeneratedResponse pendente = pendingQuizzes.get(moduleId);
        if (pendente == null) {
            throw new RegraDeNegocioException("Nenhum quiz pendente para o módulo: " + moduleId);
        }
        return pendente;
    }

    public QuizResponse confirmarQuiz(Long moduleId) {
        QuizGeneratedResponse pendente = buscarPendente(moduleId);

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Módulo não encontrado: " + moduleId));

        if (quizRepository.existsByModuleId(moduleId)) {
            throw new RegraDeNegocioException("Quiz já existe para o módulo: " + moduleId);
        }

        Quiz quiz = Quiz.builder().module(module).build();

        int ordem = 1;
        List<Question> questions = new ArrayList<>();
        for (QuestionResponse qr : pendente.questions()) {
            Question question = Question.builder()
                    .statement(qr.statement())
                    .points(qr.points())
                    .orderNum(ordem++)
                    .quiz(quiz)
                    .build();

            List<Alternative> alternatives = qr.alternatives().stream()
                    .map(ar -> Alternative.builder()
                            .text(ar.text())
                            .correct(ar.correct())
                            .question(question)
                            .build())
                    .toList();
            question.setAlternatives(alternatives);
            questions.add(question);
        }
        quiz.setQuestions(questions);

        Quiz saved = quizRepository.save(quiz);
        pendingQuizzes.remove(moduleId);
        return QuizResponse.from(saved);
    }

    private String coletarConteudo(Long moduleId) {
        List<Lesson> aulas = lessonRepository.findByModuleIdOrderByOrderNumAsc(moduleId);
        StringBuilder sb = new StringBuilder();

        for (Lesson lesson : aulas) {
            if (lesson.getContentEditor() != null && !lesson.getContentEditor().isBlank()) {
                sb.append(lesson.getContentEditor()).append("\n");
            } else if (lesson.getContentGenerated() != null && !lesson.getContentGenerated().isBlank()) {
                sb.append(lesson.getContentGenerated()).append("\n");
            } else if (lesson.getFilePath() != null && lesson.getFileType() == FileType.PDF) {
                try {
                    sb.append(extrairTextoPdf(lesson.getFilePath())).append("\n");
                } catch (IOException e) {
                    log.warn("Falha ao extrair PDF da aula {}. Ignorando.", lesson.getId(), e);
                }
            }
        }

        if (sb.isEmpty()) {
            throw new RegraDeNegocioException("O módulo não possui conteúdo legível para gerar quiz via IA");
        }

        return sb.toString();
    }

    private String extrairTextoPdf(String filePath) throws IOException {
        try (PDDocument doc = Loader.loadPDF(new File(filePath))) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
