package br.ufc.llm.api;

import br.ufc.llm.course.domain.Course;
import br.ufc.llm.course.repository.CourseRepository;
import br.ufc.llm.lesson.domain.Lesson;
import br.ufc.llm.lesson.repository.LessonRepository;
import br.ufc.llm.module.domain.Module;
import br.ufc.llm.module.repository.ModuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Teste de API (end-to-end): sobe o Spring Boot completo, semeia dados no H2,
 * e faz requisições HTTP reais nos endpoints — que por sua vez chamam a RAG API Python.
 * Requer o container RAG rodando em http://localhost:8000.
 * Rodar com: mvn test -Dtest=GeracaoConteudoApiTest
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GeracaoConteudoApiTest {

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate rest;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private LessonRepository lessonRepository;

    private Long lessonId;
    private Long moduleId;

    private static final String CONTEUDO_AULA = """
            A fotossíntese é o processo pelo qual plantas, algas e algumas bactérias
            convertem luz solar em energia química. Esse processo ocorre nos cloroplastos,
            organelas presentes nas células vegetais que contêm clorofila.
            A clorofila absorve principalmente luz vermelha e azul, refletindo a luz verde.
            O processo se divide em duas etapas: as reações luminosas, que ocorrem nas
            membranas dos tilacoides e convertem energia luminosa em ATP e NADPH;
            e o Ciclo de Calvin, que usa esses compostos para fixar CO2 e produzir glicose.
            """;

    @BeforeEach
    void seed() {
        lessonRepository.deleteAll();
        moduleRepository.deleteAll();
        courseRepository.deleteAll();

        Course course = courseRepository.save(Course.builder()
                .title("Biologia")
                .category("Ciências")
                .description("Curso de biologia básica")
                .build());

        Module module = moduleRepository.save(Module.builder()
                .name("Fotossíntese")
                .orderNum(1)
                .course(course)
                .build());

        Lesson lesson = lessonRepository.save(Lesson.builder()
                .name("Introdução à Fotossíntese")
                .orderNum(1)
                .contentEditor(CONTEUDO_AULA)
                .module(module)
                .build());

        moduleId = module.getId();
        lessonId = lesson.getId();
    }

    // ── Gerar conteúdo de aula ───────────────────────────────────────────────

    @Test
    void deveGerarConteudoHtmlDaAula() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/lessons/{id}/gerar-conteudo", null, Map.class, lessonId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat((Boolean) body.get("sucesso")).isTrue();

        String html = (String) body.get("dados");
        assertThat(html)
                .isNotBlank()
                .hasSizeGreaterThan(100)
                .doesNotContain("```");

        System.out.println("\n===== JSON: gerar-conteudo =====");
        System.out.println("sucesso : " + body.get("sucesso"));
        System.out.println("mensagem: " + body.get("mensagem"));
        System.out.println("dados   :\n" + html);
    }

    // ── Gerar quiz do módulo ─────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void deveGerarQuizDoModulo() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/modules/{moduleId}/quiz/gerar?quantidade=3", null, Map.class, moduleId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat((Boolean) body.get("sucesso")).isTrue();

        Map<?, ?> dados = (Map<?, ?>) body.get("dados");
        List<Map<?, ?>> questions = (List<Map<?, ?>>) dados.get("questions");

        assertThat(questions)
                .hasSize(3)
                .allSatisfy(q -> {
                    assertThat(q.get("statement")).asString().isNotBlank();
                    List<Map<?, ?>> alternatives = (List<Map<?, ?>>) q.get("alternatives");
                    assertThat(alternatives).hasSize(4);
                    long corretas = alternatives.stream()
                            .filter(a -> Boolean.TRUE.equals(a.get("correct")))
                            .count();
                    assertThat(corretas).isEqualTo(1);
                });

        System.out.println("\n===== JSON: gerar quiz =====");
        System.out.println("sucesso : " + body.get("sucesso"));
        System.out.println("mensagem: " + body.get("mensagem"));
        System.out.println("questões: " + questions.size());
        questions.forEach(q -> {
            System.out.println("\n  statement: " + q.get("statement"));
            ((List<Map<?, ?>>) q.get("alternatives")).forEach(a ->
                    System.out.println("    [" + (Boolean.TRUE.equals(a.get("correct")) ? "X" : " ") + "] " + a.get("text"))
            );
        });
    }
}
