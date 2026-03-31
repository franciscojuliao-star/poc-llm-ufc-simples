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
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
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

    private static final String PROMPT_TEMPLATE = """
            Você é um especialista em avaliações educacionais. A partir do conteúdo abaixo, gere %d perguntas \
            de múltipla escolha em formato JSON. Retorne APENAS um array JSON sem nenhum texto adicional, \
            seguindo exatamente este formato:
            [{"statement":"...","points":1,"alternatives":[{"text":"...","correct":true},{"text":"...","correct":false}]}]

            Cada pergunta deve ter exatamente 4 alternativas, com apenas 1 correta.

            Conteúdo do módulo:
            %s
            """;

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    private final Map<Long, QuizGeneratedResponse> pendingQuizzes = new ConcurrentHashMap<>();

    public QuizGeneratedResponse gerarQuiz(Long moduleId, int quantidade) {
        moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Módulo não encontrado: " + moduleId));

        String conteudo = coletarConteudo(moduleId);

        String json = chatClient.prompt()
                .user(PROMPT_TEMPLATE.formatted(quantidade, conteudo))
                .call()
                .content();

        QuizGeneratedResponse gerado = parseQuiz(json);
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

    @SuppressWarnings("unchecked")
    private QuizGeneratedResponse parseQuiz(String json) {
        try {
            String jsonLimpo = json.trim()
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("```", "")
                    .trim();

            List<Map<String, Object>> lista = objectMapper.readValue(jsonLimpo, new TypeReference<>() {});
            List<QuestionResponse> questions = lista.stream().map(m -> {
                String statement = (String) m.get("statement");
                int points = ((Number) m.getOrDefault("points", 1)).intValue();
                List<Map<String, Object>> alts = (List<Map<String, Object>>) m.get("alternatives");
                List<AlternativeResponse> alternatives = alts == null ? List.of() : alts.stream()
                        .map(a -> new AlternativeResponse(null, (String) a.get("text"), Boolean.TRUE.equals(a.get("correct"))))
                        .toList();
                return new QuestionResponse(null, statement, points, 0, alternatives);
            }).toList();

            return new QuizGeneratedResponse(questions);
        } catch (Exception e) {
            log.error("Falha ao parsear JSON do quiz gerado pela IA. Resposta: {}", json, e);
            throw new RegraDeNegocioException("IA retornou formato inválido ao gerar quiz");
        }
    }

    private String extrairTextoPdf(String filePath) throws IOException {
        try (PDDocument doc = Loader.loadPDF(new File(filePath))) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
