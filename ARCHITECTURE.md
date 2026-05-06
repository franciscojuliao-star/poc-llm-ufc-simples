# ARCHITECTURE.md

Documentação sobre decisões arquiteturais, padrões de design e metodologia de desenvolvimento desta PoC.

---

## Por Que Este Projeto?

Esta PoC explora como integrar LLMs (Large Language Models) em uma plataforma educacional para **automatizar e acelerar a criação de conteúdo**. O objetivo é validar fluxos de:

- Geração de conteúdo estruturado a partir de texto/PDF
- Criação automática de quizzes com base no conteúdo
- Persistência segura em PostgreSQL
- Processamento **síncrono** (diferente de Python async)

**Escopo intencional**: sem autenticação, sem permissões, sem frontend. Apenas o backend REST e a integração com LLM.

---

## Decisões Arquiteturais

### 1. Spring Boot 3.4 (em vez de Quarkus ou Micronaut)

**Por quê?**

- **Ecossistema maduro**: Maior comunidade, mais bibliotecas, padrão de fato em Java.
- **Spring Data JPA**: Abstração excelente para banco de dados, menos boilerplate.
- **Spring AI**: Integração nativa com LLMs (OpenAI, Groq, etc.), sem HTTP direto.
- **Documentação automática**: SpringDoc OpenAPI (Swagger) com uma anotação.
- **Configuração flexível**: application.yml centraliza tudo, suporta profiles (dev, test, prod).

```yaml
# application.yml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/poc_llm_simples}
    username: ${DB_USERNAME:poc_user}
    password: ${DB_PASSWORD:poc123}
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway gerencia
```

### 2. PostgreSQL 16 + Spring Data JPA (em vez de MongoDB ou Oracle)

**Por quê?**

- **Production-ready**: Transações ACID, índices complexos, confiável.
- **Spring Data JPA**: Menos boilerplate que JDBC, queries type-safe com Criteria API.
- **Migrations via Flyway**: SQL puro, versionado, reproducível.

```java
// CourseRepository.java
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByCategory(String category);
    Optional<Course> findByTitle(String title);
}

// Service usa repository
courseRepository.findById(id)
    .orElseThrow(() -> new RecursoNaoEncontradoException("Course", id));
```

### 3. Flyway para Migrations (em vez de Hibernate ddl-auto)

**Por quê?**

- **Controle fino**: Migrations em SQL puro, não dependem de ORM.
- **Versionamento**: V001__, V002__ fazem rollback/forward seguro.
- **Auditoria**: Histórico claro de mudanças no banco.
- **Professor seedado**: Uma migration cria dados iniciais sem dependência de fixtures Java.

```sql
-- db/migration/V001__create_courses_table.sql
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- db/migration/V002__insert_professor.sql
INSERT INTO professors (name, email) VALUES ('Professor UFC', 'prof@ufc.br');
```

### 4. Spring AI para Integração com LLM (em vez de chamadas HTTP diretas)

**Por quê?**

- **Abstração de provider**: Trocar Groq → OpenAI é uma linha de config.
- **Type-safety**: Menos erros, auto-complete no IDE.
- **Tratamento de retry**: Spring AI lida com rate-limiting automático.

```java
// LessonAiService.java
@Service
public class LessonAiService {
    @Autowired
    private ChatClient chatClient;
    
    public String generateContent(String prompt) {
        // Groq configurado via application.yml
        ChatResponse response = chatClient.call(
            new Prompt(prompt)
        );
        return response.getResult().getOutput().getContent();
    }
}

// application.yml
spring:
  ai:
    openai:
      api-key: ${GROQ_API_KEY}
      base-url: https://api.groq.com/openai/v1
      model: llama-3.3-70b-versatile
```

### 5. ConcurrentHashMap em Memória para Background Tasks (em vez de Celery/Redis)

**Por quê?**

- **Simplificidade**: Sem depender de Redis externo.
- **Adequado para PoC**: Geração de conteúdo é síncrona, rápida (~2s).
- **Bloqueador é esperado**: UI aguarda resposta HTTP (não async/await em Python).

**Fluxo (síncrono)**:

```
POST /lessons/1/gerar-conteudo
  ↓
LessonAiService gera conteúdo via Spring AI (sincronamente, ~2s)
  ↓
Salva result em ConcurrentHashMap<UUID, ConteudoGerado>
  ↓
Retorna { "conteudoGerado": "...", "timestamp": "..." } imediatamente
  ↓
Sem polling necessário (diferente de Python com Celery)
```

**Diferença Python vs Java**:
- Python: Enfileira em Celery, retorna task_id, cliente faz polling
- Java: Processa sincronamente (Spring threads do pool gerenciam), retorna resultado direto

### 6. Apache PDFBox + Apache Tika para PDF (em vez de pdfplumber)

**Por quê?**

- **Robustez**: PDFBox é padrão em Java para manipulação PDF.
- **Tika para detecção**: Identifica tipo de arquivo, extrai metadados.
- **Sem dependências externas**: Não precisa de ImageMagick.

```java
// LessonService.java
public String extractTextFromPdf(MultipartFile file) throws IOException {
    try (PDDocument document = PDDocument.load(file.getInputStream())) {
        PDFTextStripper stripper = new PDFTextStripper();
        return stripper.getText(document);
    }
}
```

### 7. MapStruct para Mapeamento Entity ↔ DTO

**Por quê?**

- **Zero overhead**: Gera código de mapeamento em compile time (não reflection em runtime).
- **Type-safe**: Compilador avisa se campo foi esquecido.
- **Spring integration**: Auto-wiring de mappers como beans.

```java
// mapper/CourseMapper.java
@Mapper(componentModel = "spring")
public interface CourseMapper {
    CourseResponse toResponse(Course entity);
    Course toEntity(CourseRequest request);
}

// service/CourseService.java
courseMapper.toResponse(course);  // Mapeamento zero-cost
```

### 8. Lombok para Reduzir Boilerplate

**Por quê?**

- **Menos código**: `@Data` gera getters, setters, equals, hashCode, toString.
- **Compilação**: Annotations processadas em compile time, sem overhead runtime.
- **Segurança de tipos**: Mantém refatorações seguras.

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}

// Gera automaticamente:
// getters, setters, equals(), hashCode(), toString()
// plus: AllArgsConstructor, NoArgsConstructor
```

---

## Metodologia de Desenvolvimento

### TDD — Test Driven Development

**Ciclo**:

1. **Escreve o teste** → deve falhar (red)
2. **Roda `mvn test`** → confirma falha
3. **Implementa mínimo** → faz o teste passar (green)
4. **Refatora se necessário** → sem quebrar testes (refactor)

**Benefício**: Testes são especificação viva, cobertura é automática, refatorações são seguras.

**Exemplo**:

```java
// CourseServiceTest.java (escrito PRIMEIRO)
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {
    @Mock
    private CourseRepository courseRepository;
    
    @InjectMocks
    private CourseService courseService;
    
    @Test
    void testCreateCourse() {
        // Arrange
        CourseRequest request = new CourseRequest("Java 101", "Linguagem", "Aprenda Java");
        Course course = new Course(1L, "Java 101", "Linguagem", "Aprenda Java", now(), now());
        when(courseRepository.save(any())).thenReturn(course);
        
        // Act
        CourseResponse response = courseService.create(request);
        
        // Assert
        assertNotNull(response.id());
        assertEquals("Java 101", response.title());
        verify(courseRepository, times(1)).save(any());
    }
}
```

Teste falha (CourseService não existe). Aí implementamos.

```java
// CourseService.java (implementação mínima)
@Service
public class CourseService {
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private CourseMapper courseMapper;
    
    public CourseResponse create(CourseRequest request) {
        Course course = courseMapper.toEntity(request);
        Course saved = courseRepository.save(course);
        return courseMapper.toResponse(saved);
    }
}
```

Teste passa. Pronto.

### Método Akita

Conjuntos de princípios de simplificidade:

- **Nunca pule etapas**: CRUD antes de features avançadas. Testes antes de código.
- **Uma responsabilidade**: `CourseService` só sabe criar/atualizar cursos. `LessonAiService` só gera conteúdo com IA.
- **Não antecipe funcionalidades**: Se não está no requisito, não implementa. Complexidade só quando necessária.
- **Código simples**: Sem patterns sofisticados. Se duplicação surge, refatora. Se não, deixa como está.
- **Problema? Entenda a causa raiz**: Não gambi. Se teste falha, debugga até a raiz, não só trata o sintoma.

**Resultado**: Código fácil de entender, manutenível, sem overhead desnecessário.

---

## Padrões de Código

### Domain-Driven Design (DDD)

O projeto está organizado por **domínios de negócio**, não por camada técnica:

```
br.ufc.llm/
  course/          ← domínio "Curso"
    controller/    ← endpoints HTTP
    service/       ← lógica de negócio
    repository/    ← acesso a dados (Spring Data JPA)
    domain/        ← entidades JPA
    dto/           ← DTOs Request/Response
    exception/     ← exceções customizadas

  lesson/          ← domínio "Aula"
    controller/
    service/
    repository/
    domain/
    dto/
    exception/
    mapper/        ← MapStruct para mapeamento

  shared/          ← código compartilhado
    config/        ← Beans, Filters, GlobalExceptionHandler
    dto/           ← ApiResponse padrão
    exception/     ← exceções globais
```

**Benefício**: Fácil adicionar novo domínio (ex: "Certificado"). Cada time gerencia seu domínio.

### Entity + DTO (Separação)

**Entity** (banco de dados):

```java
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, length = 100)
    private String category;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

**DTOs** (API contract):

```java
// CourseRequest.java (validação de entrada)
public record CourseRequest(
    @NotBlank(message = "Title is required") String title,
    @NotBlank(message = "Category is required") String category,
    @NotBlank(message = "Description is required") String description
) {}

// CourseResponse.java (serialização de saída)
public record CourseResponse(
    Long id,
    String title,
    String category,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

**MapStruct** (mapeamento):

```java
@Mapper(componentModel = "spring")
public interface CourseMapper {
    CourseResponse toResponse(Course entity);
    Course toEntity(CourseRequest request);
    List<CourseResponse> toResponseList(List<Course> entities);
}
```

**Benefício**: Evolução da API sem quebrar clients. Validação type-safe. Mapeamento zero-cost.

### Repository Pattern

```java
// CourseRepository.java
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByCategory(String category);
    Optional<Course> findByTitle(String title);
}

// CourseService.java
@Service
public class CourseService {
    @Autowired
    private CourseRepository courseRepository;
    
    public CourseResponse getById(Long id) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Course", id));
        return courseMapper.toResponse(course);
    }
}
```

**Benefício**: Queries centralizadas em um lugar. Fácil testar com mocks. Trocar banco sem mexer em service.

### Global Exception Handler

```java
// shared/config/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleRecursoNaoEncontrado(
        RecursoNaoEncontradoException ex
    ) {
        ApiResponse<Void> response = new ApiResponse<>(
            false,
            ex.getMessage(),
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}

// Service lança exceção
throw new RecursoNaoEncontradoException("Course", id);
// Handler captura, serializa resposta ApiResponse automática
```

**Benefício**: Erro consistente em toda API, sem try-catch por endpoint.

---

## Stack Detalhado

| Camada | Tecnologia | Por quê |
|---|---|---|
| **Linguagem** | Java 21 | Modern syntax, records, pattern matching |
| **Framework Web** | Spring Boot 3.4 | Ecossistema maduro, Spring Data JPA, Spring AI |
| **Banco de Dados** | PostgreSQL 16 | Production-ready, confiável, escalável |
| **ORM** | Spring Data JPA | Abstração excelente, menos boilerplate |
| **Migrations** | Flyway | SQL puro, versionado, reproducível |
| **IA** | Spring AI + Groq | Abstração de provider, tratamento de retry nativo |
| **PDF** | Apache PDFBox + Tika | Robusto, padrão em Java |
| **Validação** | javax.validation | Bean Validation, integrado com Spring |
| **Mapeamento** | MapStruct | Zero overhead, compile-time gerado |
| **Boilerplate** | Lombok | Menos código, compile-time processado |
| **Docs** | SpringDoc OpenAPI | Swagger automático |
| **Testes** | JUnit 5 + Mockito | Async-aware, fixtures poderosas |
| **Build** | Maven | Padrão, vasta compatibilidade |

---

## Fluxo de Desenvolvimento: Exemplo Prático

Vamos implementar um novo endpoint: **"Adicionar quiz manual"**.

### 1. Entender o requisito

```
POST /modules/{moduleId}/quiz
Content-Type: application/json

{
  "questions": [
    {
      "statement": "Pergunta?",
      "points": 1,
      "alternatives": [
        { "text": "Correta", "correct": true },
        { "text": "Errada", "correct": false }
      ]
    }
  ]
}
```

### 2. Escrever testes (TDD)

```java
// QuizServiceTest.java

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class QuizServiceTest {
    @Mock
    private QuizRepository quizRepository;
    
    @Mock
    private ModuleRepository moduleRepository;
    
    @InjectMocks
    private QuizService quizService;
    
    @Test
    void testCreateQuizWithQuestions() {
        // Arrange
        Long moduleId = 1L;
        Module module = new Module(moduleId, "Module 1", ...);
        QuizRequest request = new QuizRequest(
            List.of(
                new QuestionRequest(
                    "O que é Java?",
                    1,
                    List.of(
                        new AlternativeRequest("Uma linguagem", true),
                        new AlternativeRequest("Um animal", false)
                    )
                )
            )
        );
        
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        
        Quiz savedQuiz = new Quiz(1L, moduleId, ...);
        when(quizRepository.save(any())).thenReturn(savedQuiz);
        
        // Act
        QuizResponse response = quizService.createWithQuestions(moduleId, request);
        
        // Assert
        assertNotNull(response.id());
        assertEquals(1, response.questions().size());
        verify(quizRepository, times(1)).save(any());
    }
}
```

Teste falha. Prosseguir.

### 3. Criar DTOs

```java
// dto/AlternativeRequest.java
public record AlternativeRequest(
    @NotBlank String text,
    boolean correct
) {}

// dto/QuestionRequest.java
public record QuestionRequest(
    @NotBlank String statement,
    @Positive int points,
    @NotEmpty List<AlternativeRequest> alternatives
) {}

// dto/QuizRequest.java
public record QuizRequest(
    @NotEmpty List<QuestionRequest> questions
) {}
```

### 4. Implementar service

```java
// QuizService.java

@Service
public class QuizService {
    @Autowired
    private QuizRepository quizRepository;
    
    @Autowired
    private ModuleRepository moduleRepository;
    
    @Autowired
    private QuizMapper quizMapper;
    
    public QuizResponse createWithQuestions(Long moduleId, QuizRequest request) {
        // Validar módulo existe
        Module module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Module", moduleId));
        
        // Criar quiz
        Quiz quiz = new Quiz();
        quiz.setModuleId(moduleId);
        Quiz saved = quizRepository.save(quiz);
        
        // Adicionar questões
        for (QuestionRequest q : request.questions()) {
            Question question = new Question();
            question.setQuizId(saved.getId());
            question.setStatement(q.statement());
            question.setPoints(q.points());
            Question savedQuestion = quizRepository.saveQuestion(question);
            
            // Adicionar alternativas
            for (AlternativeRequest alt : q.alternatives()) {
                Alternative alternative = new Alternative();
                alternative.setQuestionId(savedQuestion.getId());
                alternative.setText(alt.text());
                alternative.setCorrect(alt.correct());
                quizRepository.saveAlternative(alternative);
            }
        }
        
        return quizMapper.toResponse(saved);
    }
}
```

### 5. Implementar controller

```java
// QuizController.java

@RestController
@RequestMapping("/modules/{moduleId}/quiz")
public class QuizController {
    @Autowired
    private QuizService quizService;
    
    @PostMapping
    public ApiResponse<QuizResponse> createWithQuestions(
        @PathVariable Long moduleId,
        @RequestBody @Valid QuizRequest request
    ) {
        QuizResponse data = quizService.createWithQuestions(moduleId, request);
        return new ApiResponse<>(true, "Quiz criado com sucesso", data);
    }
}
```

### 6. Rodar testes

```bash
mvn test -Dtest=QuizServiceTest
```

✅ Passa.

### 7. Testar manualmente

```bash
curl -X POST http://localhost:8080/modules/1/quiz \
  -H "Content-Type: application/json" \
  -d '{
    "questions": [
      {
        "statement": "O que é Java?",
        "points": 1,
        "alternatives": [
          {"text": "Linguagem", "correct": true},
          {"text": "Animal", "correct": false}
        ]
      }
    ]
  }'
```

Documentação automática em http://localhost:8080/swagger-ui.html ← já atualizado.

---

## Comparação Python vs Java

| Aspecto | Python (FastAPI) | Java (Spring Boot) |
|---|---|---|
| **Framework** | FastAPI 0.115 | Spring Boot 3.4 |
| **Async** | asyncio (async/await) | Síncrono (thread pool) |
| **ORM** | SQLAlchemy 2.0 (async) | Spring Data JPA |
| **Migrations** | Alembic | Flyway |
| **IA** | OpenAI SDK direto | Spring AI |
| **PDF** | pdfplumber | Apache PDFBox + Tika |
| **Validação** | Pydantic v2 | Bean Validation (javax) |
| **Mapeamento** | Pydantic automático | MapStruct (compile-time) |
| **Background Tasks** | Celery + Redis | ConcurrentHashMap (in-memory) |
| **Testes** | pytest + pytest-asyncio | JUnit 5 + Mockito |
| **Build** | pip + pyproject.toml | Maven + pom.xml |
| **Boilerplate** | Menos | Lombok reduz |
| **Docs** | Automático (OpenAPI) | SpringDoc (OpenAPI) |

**Diferença principal**: 
- Python: async-native, não-bloqueante, enfileira long-running tasks em Celery
- Java: síncrono, bloqueante, mas Spring threads gerenciam concorrência, tasks em memória

---

## O Que Não Está Aqui

- **Autenticação**: Propositalmente ignorada. Uma feature futura.
- **Permissões**: Sem controle de acesso. Todos os endpoints acessíveis.
- **Frontend**: Backend-only. Use Swagger, Postman, ou curl.
- **Cache sofisticado**: Sem Redis ou Memcached.
- **Message Queue**: Sem RabbitMQ. ConcurrentHashMap é suficiente para PoC.
- **Search avançado**: Sem Elasticsearch.

---

## Estatísticas

- **55 testes unitários** cobrindo todos os serviços
- **5 domínios** (Course, Module, Lesson, Quiz, shared)
- **15+ endpoints** REST
- **~2500 linhas** de código (sem testes)

---

## Referências

- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring AI](https://spring.io/projects/spring-ai)
- [Flyway](https://flywaydb.org/)
- [MapStruct](https://mapstruct.org/)
- [Lombok](https://projectlombok.org/)
- [SpringDoc OpenAPI](https://springdoc.org/)
- [JUnit 5](https://junit.org/junit5/)
- [Mockito](https://site.mockito.org/)

---

## Autor

**Aglayrton Julião**  
Desenvolvido com assistência de IA (Claude) usando metodologia TDD + Método Akita.
